/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sdk.pairing;

import static com.google.android.jacquard.sdk.pairing.TagPairingState.Type.BLUETOOTH_CONNECTED;
import static com.google.android.jacquard.sdk.pairing.TagPairingState.Type.DISCONNECTED;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import com.google.android.jacquard.sdk.ConnectState;
import com.google.android.jacquard.sdk.StateMachine;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.JacquardError;
import com.google.android.jacquard.sdk.model.Peripheral;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.util.BluetoothSig;
import java.util.UUID;

/**
 * State machine for managing state during the pairing phase.
 */
public class TagPairingStateMachine implements StateMachine<TagPairingState, ConnectState> {

  private static final String TAG = TagPairingStateMachine.class.getSimpleName();
  private final Signal<TagPairingState> stateSignal = Signal.<TagPairingState>create().sticky();
  private final StateMachineContext stateMachineContext = new StateMachineContext();
  private TagPairingState state = TagPairingState.ofDisconnected();

  /** Constructs a new instance. */
  public TagPairingStateMachine() {
    stateSignal.next(state);
  }

  @Override
  public Signal<TagPairingState> getState() {
    return stateSignal;
  }

  @Override
  public void onStateEvent(ConnectState state) {
    PrintLogger.d(TAG,"onConnectStateEvent # " + state.getType().name());
    switch (state.getType()) {
      case VALUE_WRITTEN:
        handleEvent(TagPairingEvent.ofNotificationStateUpdated(state.valueWritten()));
        break;
      case CHARACTERISTIC_UPDATED:
        handleEvent(TagPairingEvent.ofNotificationStateUpdated(state.characteristicUpdated()));
        break;
      case SERVICES_DISCOVERED:
        handleEvent(TagPairingEvent.ofServicesDiscovered(state.servicesDiscovered()));
        break;
      case CONNECTED:
        handleEvent(TagPairingEvent.ofConnected(state.connected()));
        break;
      case FAILED_TO_CONNECT:
        handleEvent(TagPairingEvent
            .ofFailedToConnect(state.failedToConnect()));
        break;
    }
  }

  /**
   * Called when an event is received.
   * @param event the {@link TagPairingEvent} that will trigger a state change.
   */
  private void handleEvent(TagPairingEvent event) {
    PrintLogger.d(TAG, "handleEvent: " + event.getType().name());
    if (state.isTerminal()) {
      PrintLogger.d(TAG, "State machine is already terminal, ignoring event");
      return;
    }

    if (event.getType() == TagPairingEvent.Type.MISC_CORE_BLUETOOTH_ERROR) {
      updateState(TagPairingState
          .ofError(JacquardError.ofBluetoothConnectionError(event.miscCoreBluetoothError())));
      return;
    }

    switch (event.getType()) {
      case CONNECTED:
        onConnectedEvent(event);
        break;
      case FAILED_TO_CONNECT:
        onFailedToConnectEvent(event);
        break;
      case MISC_CORE_BLUETOOTH_ERROR:
        updateState(TagPairingState
            .ofError(JacquardError.ofBluetoothConnectionError(event.miscCoreBluetoothError())));
        break;
      case SERVICES_DISCOVERED:
        onServicesDiscoveredEvent(event);
        break;
      case NOTIFICATION_STATE_UPDATED:
        onNotificationStateUpdatedEvent(event);
        break;
    }
  }

  /**
   * Called when a notification is received from the bluetooth adapter.
   * @param event the {@link TagPairingEvent} holding the notification.
   */
  private void onNotificationStateUpdatedEvent(
      TagPairingEvent event) {
    PrintLogger.d(TAG, "onNotificationStateUpdatedEvent");
    if (state.getType() != TagPairingState.Type.AWAITING_NOTIFICATION_UPDATES) {
      return;
    }
    PrintLogger.d(TAG, "onNotificationStateUpdatedEvent enter");

    // Wait until we have callbacks for all the characteristic.
    if (--stateMachineContext.expectedCharacteristicUpdates > 0) {
      PrintLogger.d(TAG,
          "onNotificationStateUpdatedEvent: " + stateMachineContext.expectedCharacteristicUpdates);
      return;
    }
    PrintLogger.d(TAG, "onNotificationStateUpdatedEvent: complete");

    RequiredCharacteristics requiredCharacteristics = new RequiredCharacteristics(
        stateMachineContext.commandCharacteristic,
        stateMachineContext.responseCharacteristic,
        stateMachineContext.notifyCharacteristic,
        stateMachineContext.batteryCharacteristic,
        stateMachineContext.rawCharacteristic);

    updateState(TagPairingState.ofTagPaired(event.notificationStateUpdated().peripheral(),
        requiredCharacteristics));
  }

  /**
   * Called then a failed to connect event is received.
   * @param event the failed to connect event
   */
  private void onFailedToConnectEvent(TagPairingEvent event) {
    updateState(TagPairingState
        .ofError(JacquardError
            .ofBluetoothConnectionError(new Exception("Failed to connect"))));
  }

  /**
   * Called when a services discovered event is received.
   * <p>
   * The required characteristics are extracted from the services received in the event and
   * notification are enabled for some of the characteristics.
   *
   * @param event services discovered event
   */
  private void onServicesDiscoveredEvent(TagPairingEvent event) {
    if (state.getType() != BLUETOOTH_CONNECTED) {
      return;
    }

    if (event.servicesDiscovered().services().isEmpty()) {
      PrintLogger.e(TAG, "No services discovered!");
      updateState(TagPairingState
          .ofError(JacquardError.ofServiceDiscoveryError()));
      return;
    }

    updateState(TagPairingState.ofAwaitingNotificationUpdates()); // Wait for the last notification

    Peripheral peripheral = event.servicesDiscovered().peripheral();

    for (BluetoothGattService service : event.servicesDiscovered().services()) {
      UUID uuid = service.getUuid();
      if (BluetoothSig.SERVICE_BATTERY_SERVICE.equals(uuid)) {
        stateMachineContext.batteryService = service;
        stateMachineContext.batteryCharacteristic = stateMachineContext.batteryService
            .getCharacteristic(BluetoothSig.CHARACTERISTIC_BATTERY_LEVEL);
        peripheral.readCharacteristic(stateMachineContext.batteryCharacteristic);
        peripheral.enableNotification(stateMachineContext.batteryCharacteristic, true);
      }
      if (BluetoothSig.SERVICE_GENERIC_ACCESS.equals(uuid)) {
        stateMachineContext.genericAccess = service;
        stateMachineContext.deviceNameCharacteristic = stateMachineContext.genericAccess
            .getCharacteristic(BluetoothSig.CHARACTERISTIC_GAP_DEVICE_NAME);
        peripheral.readCharacteristic(stateMachineContext.deviceNameCharacteristic);
      }
    }

    // Negotiate JQ Device
    BluetoothGattService jqService = event.servicesDiscovered().peripheral().getJacquardService();

    stateMachineContext.responseCharacteristic = jqService
        .getCharacteristic(BluetoothSig.JQ_CHARACTERISTIC_RESPONSE);
    peripheral.enableNotification(stateMachineContext.responseCharacteristic, true);

    stateMachineContext.notifyCharacteristic = jqService
        .getCharacteristic(BluetoothSig.JQ_CHARACTERISTIC_NOTIFICATION);
    peripheral.enableNotification(stateMachineContext.notifyCharacteristic, true);

    stateMachineContext.commandCharacteristic = jqService
        .getCharacteristic(BluetoothSig.JQ_CHARACTERISTIC_COMMAND);

    stateMachineContext.rawCharacteristic = jqService.getCharacteristic(BluetoothSig.JQ_RAW_CHARACTERISTIC);
    peripheral.enableNotification(stateMachineContext.rawCharacteristic, true);
  }

  /**
   * Called when a connected event is received.
   * @param event services discovered event
   */
  private void onConnectedEvent(TagPairingEvent event) {
    PrintLogger.d(TAG, "onConnectedEvent: " + event);
    if (state.getType() != DISCONNECTED) {
      return;
    }
    updateState(TagPairingState.ofBluetoothConnected());
    event.connected().discoverServices();
  }

  /**
   * Called to update the state.
   * <p>
   * The state is emitted to observers of the state machine.
   *
   * @param state the new state of this state machine.
   */
  private void updateState(TagPairingState state) {
    PrintLogger.d(TAG, "updateState: " + state);
    this.state = state;
    stateSignal.next(this.state);
  }

  @Override
  public void destroy() {
    // Empty
  }

  /** Class holding the inner state of the state machine. */
  private static class StateMachineContext {

    // TODO(b/201270651): Keep track of the callback required until we can proceed. Can we find a better way?
    int expectedCharacteristicUpdates = 6;

    BluetoothGattService batteryService;
    BluetoothGattService genericAccess;
    BluetoothGattCharacteristic deviceNameCharacteristic;
    BluetoothGattCharacteristic commandCharacteristic;
    BluetoothGattCharacteristic responseCharacteristic;
    BluetoothGattCharacteristic notifyCharacteristic;
    BluetoothGattCharacteristic batteryCharacteristic;
    BluetoothGattCharacteristic rawCharacteristic;
  }
}
