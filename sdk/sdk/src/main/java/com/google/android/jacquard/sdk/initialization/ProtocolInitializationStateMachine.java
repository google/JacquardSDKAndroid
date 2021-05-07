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
package com.google.android.jacquard.sdk.initialization;

import static com.google.android.jacquard.sdk.initialization.InitializationState.Type.CREATING_TAG_INSTANCE;
import static com.google.android.jacquard.sdk.initialization.InitializationState.Type.PAIRED;

import com.google.android.jacquard.sdk.ConnectState;
import com.google.android.jacquard.sdk.JacquardManager;
import com.google.android.jacquard.sdk.StateMachine;
import com.google.android.jacquard.sdk.command.DeviceInfo;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.model.Peripheral;
import com.google.android.jacquard.sdk.model.Peripheral.WriteType;
import com.google.android.jacquard.sdk.model.ProtocolSpec;
import com.google.android.jacquard.sdk.pairing.RequiredCharacteristics;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTagImpl;
import com.google.atap.jacquard.protocol.JacquardProtocol.BeginRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.DeviceInfoRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.DeviceInfoResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.HelloResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;

/** State machine for handling the protocol initialization phase. */
public class ProtocolInitializationStateMachine implements
    StateMachine<InitializationState, ConnectState> {

  private static final int HELLO_PROTOCOL_VERSION = 2;
  private static final String TAG = ProtocolInitializationStateMachine.class.getSimpleName();

  private final Signal<InitializationState> stateSignal = Signal.create();
  private final StateMachineContext stateMachineContext;
  private InitializationState state = InitializationState.ofPaired();

  /** Creates a new instance of ProtocolInitializationStateMachine. */
  public ProtocolInitializationStateMachine(Peripheral peripheral,
      RequiredCharacteristics requiredCharacteristics) {
    stateMachineContext = new StateMachineContext(peripheral, requiredCharacteristics);
    updateState(InitializationState.ofPaired());
  }

  @Override
  public Signal<InitializationState> getState() {
    return stateSignal;
  }

  @Override
  public void onStateEvent(ConnectState state) {
    PrintLogger.d(TAG,"onConnectStateEvent # " + state.getType().name());
    switch (state.getType()) {
      case CHARACTERISTIC_UPDATED:
        stateMachineContext.transport.characteristicUpdated(state.characteristicUpdated());
        break;
      case VALUE_RSSI:
        stateMachineContext.transport.onRSSIValueUpdated(state.valueRssi().value());
        break;
    }
  }

  /** Starts negotiation with the tag. */
  public void startNegotiation() {
    PrintLogger.d(TAG, "startNegotiation");
    handleEvent(InitializationEvent.ofStartNegotiation());
  }

  /** Call to reacts to events. */
  private void handleEvent(InitializationEvent event) {
    PrintLogger.d(TAG, "handleEvent: " + event);

    if (state.isTerminal()) {
      PrintLogger.d(TAG, "State machine is already terminal, ignoring event");
      return;
    }

    switch (event.getType()) {
      case START_NEGOTIATION:
        onStartNegotiationEvent();
        break;
      case RECEIVED_RESPONSE:
        onReceivedResponseEvent(event);
        break;
      case RECEIVED_RESPONSE_WITH_ERROR:
        onReceivedResponseWithError(event);
        break;
      case CREATED_CONNECTED_TAG_INSTANCE:
        onCreatedConnectedTagInstanceEvent(event);
        break;
    }
  }

  /** Called when a connected tag has been created. */
  private void onCreatedConnectedTagInstanceEvent(InitializationEvent event) {
    PrintLogger.d(TAG, "onCreatedConnectedTagInstanceEvent: " + event);
    if (!state.isType(CREATING_TAG_INSTANCE)) {
      return;
    }
    updateState(InitializationState.ofTagInitialized(event.createdConnectedTagInstance()));
  }

  /** Called when a response is received from the peripheral. */
  private void onReceivedResponseEvent(InitializationEvent event) {
    PrintLogger.d(TAG, "onReceivedResponseEvent: " + event + " in state: " + state);
    switch (state.getType()) {
      // If we call connected on an already paired device the response from sendBegin() command will
      // come before the ack.
      case BEGIN_SENT:
        getComponentInfo();
        break;
      case HELLO_SENT:
        onHelloResponseReceived(event);
        break;
      case COMPONENT_INFO_SENT:
        updateState(InitializationState.ofCreatingTagInstance());
        onComponentInfoResponseReceived(event);
        break;
      default:
        PrintLogger.d(TAG, "Invalid state # onReceivedResponseEvent in state: " + state);
    }
  }

  /** Called when the Hello Response Received. */
  private void onHelloResponseReceived(InitializationEvent event) {
    PrintLogger.d(TAG, "onReceivedHelloAck: " + event);
    Response response = event.receivedResponse();
    if (!response.hasExtension(HelloResponse.hello)) {
      PrintLogger.e(TAG, "Response does not contain hello: " + response);
      return;
    }
    HelloResponse helloResponse = response.getExtension(HelloResponse.hello);

    if (helloResponse.getProtocolMin() >= HELLO_PROTOCOL_VERSION
        && helloResponse.getProtocolMax() <= HELLO_PROTOCOL_VERSION) {
      try {
        sendBegin();
        updateState(InitializationState.ofBeginSent());
      } catch (Exception e) {
        updateState(InitializationState.ofError(e));
      }
    }
  }

  /** Called when the DeviceInfo Response Received. */
  private void onComponentInfoResponseReceived(InitializationEvent event) {
    PrintLogger.d(TAG, "onReceivedDeviceInfoAck: " + event);
    Response response = event.receivedResponse();
    if (!response.hasExtension(DeviceInfoResponse.deviceInfo)) {
      PrintLogger.e(TAG, "Response does not contain deviceInfo: " + response);
      return;
    }
    DeviceInfo deviceInfo = DeviceInfo.ofTag(response.getExtension(DeviceInfoResponse.deviceInfo));
    JacquardManager.getInstance().getMemoryCache()
        .putDeviceInfo(stateMachineContext.transport.getPeripheralIdentifier(), deviceInfo);
    createConnectedTag(deviceInfo);
  }

  /** Creates a connected tag. */
  private void createConnectedTag(DeviceInfo deviceInfo) {
    PrintLogger.d(TAG, "createConnectedTag");
    ConnectedJacquardTag connectedJacquardTag = new ConnectedJacquardTagImpl(
        stateMachineContext.transport, deviceInfo);
    handleEvent(InitializationEvent.ofCreatedConnectedTagInstance(connectedJacquardTag));
  }

  /** Sends the begin request. */
  private void sendBegin() {
    PrintLogger.d(TAG, "sendBegin");
    BeginRequest beginRequest = BeginRequest.newBuilder()
        .setProtocol(ProtocolSpec.VERSION_2.getProtocolId())
        .build();

    Request request = Request.newBuilder().setId(0).setOpcode(Opcode.BEGIN).setDomain(Domain.BASE)
        .setExtension(
            BeginRequest.begin, beginRequest).build();

    stateMachineContext.transport.enqueue(request, WriteType.WITHOUT_RESPONSE, /*retries=*/2)
        .tapError(error -> handleEvent(InitializationEvent.ofReceivedResponseWithError(error)))
        .onNext(response -> handleEvent(InitializationEvent.ofReceivedResponse(response)));
  }

  /** Called when response with an error has been received. */
  private void onReceivedResponseWithError(InitializationEvent event) {
    PrintLogger.d(TAG, "onReceivedResponseWithError: " + event);
    updateState(InitializationState.ofError(event.receivedResponseWithError()));
  }

  /** Starts negotiating by sending the Hello request. */
  private void onStartNegotiationEvent() {
    PrintLogger.d(TAG, "onStartNegotiationEvent");
    if (!state.isType(PAIRED)) {
      return;
    }
    // put in an artificial 1 second delay before starting protocol negotiation
    // seems to alleviate negotiation timeout issues.
    Signal.from(1).delay(1000).onNext(ignore -> {
      try {
        sendHello();
        updateState(InitializationState.ofHelloSent());
      } catch (Exception e) {
        updateState(InitializationState.ofError(e));
      }
    });
  }

  /** Sends the hello request. */
  private void sendHello() {
    PrintLogger.d(TAG, "sendHello");
    Request request = Request.newBuilder().setOpcode(Opcode.HELLO).setId(0).setDomain(Domain.BASE)
        .build();

    stateMachineContext.transport.enqueue(request, WriteType.WITHOUT_RESPONSE, /*retries=*/2)
        .tapError(error -> handleEvent(InitializationEvent.ofReceivedResponseWithError(error)))
        .onNext(response -> handleEvent(InitializationEvent.ofReceivedResponse(response)));
  }

  /** Get the device info of tag from JacquardManager if available or send the request. */
  private void getComponentInfo() {
    PrintLogger.d(TAG, "getDeviceInfo");
    DeviceInfo deviceInfo = JacquardManager.getInstance().getMemoryCache()
        .getDeviceInfo(stateMachineContext.transport.getPeripheralIdentifier());
    PrintLogger.d(TAG, "deviceInfo: " + deviceInfo + " identifier: "
        + stateMachineContext.transport.getPeripheralIdentifier());
    if (deviceInfo == null) {
      sendDeviceInfo();
    } else {
      updateState(InitializationState.ofCreatingTagInstance());
      createConnectedTag(deviceInfo);
    }
  }

  /** Sends the device info request. */
  private void sendDeviceInfo() {
    PrintLogger.d(TAG, "sendDeviceInfo");
    updateState(InitializationState.ofComponentInfoSent());
    DeviceInfoRequest deviceInfoRequest = DeviceInfoRequest.newBuilder()
        .setComponent(Component.TAG_ID)
        .build();
    Request request = Request
        .newBuilder()
        .setComponentId(Component.TAG_ID)
        .setId(0)
        .setOpcode(Opcode.DEVICEINFO)
        .setExtension(DeviceInfoRequest.deviceInfo, deviceInfoRequest).setDomain(Domain.BASE)
        .build();
    stateMachineContext.transport.enqueue(request, WriteType.WITHOUT_RESPONSE, /*retries=*/2)
        .tapError(error -> handleEvent(InitializationEvent.ofReceivedResponseWithError(error)))
        .onNext(response -> handleEvent(InitializationEvent.ofReceivedResponse(response)));
  }

  /** Updates the internal state and emits the update to observers. */
  private void updateState(InitializationState state) {
    this.state = state;
    stateSignal.next(this.state);
  }

  @Override
  public void destroy() {
    stateMachineContext.negotiationSubscription.unsubscribe();
  }

  /**
   * Holds context for the state machine.
   */
  private static class StateMachineContext {

    private final Transport transport;
    private final TransportState transportState = new TransportState();
    private Subscription negotiationSubscription = new Subscription();

    public StateMachineContext(Peripheral peripheral, RequiredCharacteristics characteristics) {
      transport = new TransportImpl(peripheral, characteristics, transportState);
    }
  }
}
