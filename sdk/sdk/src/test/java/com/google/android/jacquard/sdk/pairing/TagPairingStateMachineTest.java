/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.google.android.jacquard.sdk.pairing;

import static com.google.android.jacquard.sdk.pairing.TagPairingState.Type.AWAITING_NOTIFICATION_UPDATES;
import static com.google.android.jacquard.sdk.pairing.TagPairingState.Type.BLUETOOTH_CONNECTED;
import static com.google.android.jacquard.sdk.pairing.TagPairingState.Type.DISCONNECTED;
import static com.google.android.jacquard.sdk.pairing.TagPairingState.Type.ERROR;
import static com.google.android.jacquard.sdk.pairing.TagPairingState.Type.TAG_PAIRED;
import static com.google.android.jacquard.sdk.util.BluetoothSig.JQ_SERVICE_2;
import static com.google.android.jacquard.sdk.util.BluetoothSig.RESPONSE_UUID;
import static com.google.android.jacquard.sdk.util.BluetoothSig.SERVICE_BATTERY_SERVICE;
import static com.google.android.jacquard.sdk.util.BluetoothSig.SERVICE_GENERIC_ACCESS;
import static com.google.common.truth.Truth.assertThat;

import android.bluetooth.BluetoothGattService;
import android.os.Build.VERSION_CODES;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.jacquard.sdk.ConnectState;
import com.google.android.jacquard.sdk.FakeBluetoothGattCharacteristic;
import com.google.android.jacquard.sdk.FakeConnectState;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.CharacteristicUpdate;
import com.google.android.jacquard.sdk.model.FakePeripheral;
import com.google.android.jacquard.sdk.pairing.TagPairingState.Type;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link TagPairingStateMachine}.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public class TagPairingStateMachineTest {

  /**
   * Update when there is change in {@link RequiredCharacteristics}.
   */
  private static final int REQUIRED_CHARACTERISTICS = 6;
  private final FakePeripheral peripheral = new FakePeripheral(/* bleQueue= */ null);
  private final FakeConnectState connectState = new FakeConnectState();

  private TagPairingStateMachine tagPairingStateMachine;
  private TagPairingState tagPairingState;


  @Before
  public void setUp() {
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    tagPairingStateMachine = new TagPairingStateMachine();
    tagPairingStateMachine.getState().onNext(state -> tagPairingState = state);
  }

  @Test
  public void getState_returnsTagPairingStateSignal() {
    // Act
    Signal<TagPairingState> stateSignal = tagPairingStateMachine.getState();
    // Assert
    assertThat(stateSignal).isNotNull();
  }

  @Test
  public void onConnectStateEvent_connectedState_emitsBluetoothConnectedState() {
    // Arrange
    connectState.setPeripheral(peripheral);
    // Act
    tagPairingStateMachine.onStateEvent(connectState.getConnectedState());
    // Assert
    assertThat(tagPairingState.getType()).isEqualTo(Type.BLUETOOTH_CONNECTED);
  }

  @Test
  public void onConnectStateEvent_connectedStatePresentlyNotDisconnected_doesNotUpdateState() {
    // Arrange
    onTagPairingStateConnected();
    // Act
    tagPairingStateMachine.onStateEvent(connectState.getConnectedState());
    // Assert
    assertThat(tagPairingState.getType()).isEqualTo(BLUETOOTH_CONNECTED);
  }

  @Test
  public void onConnectStateEvent_serviceDiscoveredState_doesNotUpdatesAwaitingNotification() {
    List<BluetoothGattService> services = ImmutableList
        .of(new BluetoothGattService(SERVICE_BATTERY_SERVICE, /* serviceType= */0));
    connectState.setBluetoothGattServices(services);
    connectState.setPeripheral(peripheral);
    // Act
    tagPairingStateMachine
        .onStateEvent(connectState.getServiceDiscoveredState());
    // Assert
    assertThat(tagPairingState.getType()).isEqualTo(DISCONNECTED);
  }

  @Test
  public void onConnectStateEvent_serviceDiscoveredEmptyServices_doesNotUpdatesAwaitingNotification() {
    // Arrange
    connectState.setPeripheral(peripheral);
    connectState.setBluetoothGattServices(ImmutableList.of());
    onTagPairingStateConnected();
    // Act
    tagPairingStateMachine
        .onStateEvent(connectState.getServiceDiscoveredState());
    // Assert
    assertThat(tagPairingState.getType()).isEqualTo(ERROR);
  }

  @Test
  public void onConnectStateEvent_serviceDiscoveredBatteryService_emitsAwaitingNotificationUpdates() {
    // Arrange
    List<BluetoothGattService> services = ImmutableList
        .of(new BluetoothGattService(SERVICE_BATTERY_SERVICE, /* serviceType= */0));
    connectState.setBluetoothGattServices(services);
    peripheral.setBluetoothSigUuid(JQ_SERVICE_2);
    onTagPairingStateConnected();
    // Act
    tagPairingStateMachine
        .onStateEvent(connectState.getServiceDiscoveredState());
    // Assert
    assertThat(tagPairingState.getType()).isEqualTo(AWAITING_NOTIFICATION_UPDATES);
  }

  @Test
  public void onConnectStateEvent_serviceDiscoveredGenericAccess_emitsAwaitingNotificationUpdates() {
    // Arrange
    List<BluetoothGattService> services = ImmutableList
        .of(new BluetoothGattService(SERVICE_GENERIC_ACCESS, /* serviceType= */0));
    connectState.setBluetoothGattServices(services);
    peripheral.setBluetoothSigUuid(JQ_SERVICE_2);
    onTagPairingStateConnected();
    // Act
    tagPairingStateMachine
        .onStateEvent(connectState.getServiceDiscoveredState());
    // Assert
    assertThat(tagPairingState.getType()).isEqualTo(AWAITING_NOTIFICATION_UPDATES);
  }

  @Test
  public void onConnectStateEvent_valueWrittenState_emitsTagPaired() {
    // Arrange
    FakeBluetoothGattCharacteristic bluetoothGattCharacteristic = new FakeBluetoothGattCharacteristic(
        RESPONSE_UUID, /* properties= */0, /* permissions= */ 0);
    CharacteristicUpdate characteristicUpdate = CharacteristicUpdate
        .of(peripheral, bluetoothGattCharacteristic);
    connectState.setCharacteristicUpdate(characteristicUpdate);
    onTagPairingStateConnected();
    onTagPairingStateServiceDiscovered();
    // Act
    for (int i = 0; i < REQUIRED_CHARACTERISTICS; i++) {
      tagPairingStateMachine
          .onStateEvent(connectState.getValueWrittenState());
    }
    // Assert
    assertThat(tagPairingState.getType()).isEqualTo(TAG_PAIRED);
  }

  @Test
  public void onConnectStateEvent_characteristicsUpdatedState_emitsTagPaired() {
    // Arrange
    FakeBluetoothGattCharacteristic bluetoothGattCharacteristic = new FakeBluetoothGattCharacteristic(
        RESPONSE_UUID, /* properties= */0, /* permissions= */ 0);
    CharacteristicUpdate characteristicUpdate = CharacteristicUpdate
        .of(peripheral, bluetoothGattCharacteristic);
    connectState.setCharacteristicUpdate(characteristicUpdate);
    onTagPairingStateConnected();
    onTagPairingStateServiceDiscovered();
    // Act
    for (int i = 0; i < REQUIRED_CHARACTERISTICS; i++) {
      tagPairingStateMachine
          .onStateEvent(connectState.getCharacteristicsUpdatedState());
    }
    // Assert
    assertThat(tagPairingState.getType()).isEqualTo(TAG_PAIRED);
  }

  @Test
  public void onConnectStateEvent_connectedStatePresentlyTagPaired_ignoresEvent() {
    // Arrange
    FakeBluetoothGattCharacteristic bluetoothGattCharacteristic = new FakeBluetoothGattCharacteristic(
        RESPONSE_UUID, /* properties= */0, /* permissions= */ 0);
    CharacteristicUpdate characteristicUpdate = CharacteristicUpdate
        .of(peripheral, bluetoothGattCharacteristic);
    connectState.setCharacteristicUpdate(characteristicUpdate);
    onTagPairingStateConnected();
    onTagPairingStateServiceDiscovered();
    for (int i = 0; i < REQUIRED_CHARACTERISTICS; i++) {
      tagPairingStateMachine
          .onStateEvent(connectState.getCharacteristicsUpdatedState());
    }
    // Act
    tagPairingStateMachine.onStateEvent(connectState.getConnectedState());
    // Assert
    assertThat(tagPairingState.getType()).isEqualTo(TAG_PAIRED);
  }

  @Test
  public void onConnectStateEvent_characteristicsUpdateStateWaitingForCallback_doesNotUpdatesState() {
    // Arrange
    FakeBluetoothGattCharacteristic bluetoothGattCharacteristic = new FakeBluetoothGattCharacteristic(
        RESPONSE_UUID, /* properties= */0, /* permissions= */ 0);
    CharacteristicUpdate characteristicUpdate = CharacteristicUpdate
        .of(peripheral, bluetoothGattCharacteristic);
    connectState.setCharacteristicUpdate(characteristicUpdate);
    connectState.setPeripheral(peripheral);
    // Act
    tagPairingStateMachine
        .onStateEvent(connectState.getCharacteristicsUpdatedState());
    // Assert
    assertThat(tagPairingState.getType()).isEqualTo(DISCONNECTED);
  }

  @Test
  public void onConnectStateEvent_failedToConnectState_emitsError() {
    // Act
    connectState.setPeripheral(peripheral);
    tagPairingStateMachine
        .onStateEvent(connectState.getFailedToConnectState());
    // Assert
    assertThat(tagPairingState.getType()).isEqualTo(ERROR);
  }

  @Test
  public void destroy_failedToConnectState_doesNotUpdateState() {
    // Act
    tagPairingStateMachine.destroy();
    // Assert
    assertThat(tagPairingState.getType()).isEqualTo(DISCONNECTED);
  }

  private void onTagPairingStateConnected() {
    connectState.setType(ConnectState.Type.CONNECTED);
    connectState.setPeripheral(peripheral);
    tagPairingStateMachine.onStateEvent(connectState.getConnectedState());
  }

  private void onTagPairingStateServiceDiscovered() {
    List<BluetoothGattService> services = ImmutableList
        .of(new BluetoothGattService(SERVICE_BATTERY_SERVICE, /* serviceType= */0));
    connectState.setBluetoothGattServices(services);
    peripheral.setBluetoothSigUuid(JQ_SERVICE_2);
    connectState.setType(ConnectState.Type.SERVICES_DISCOVERED);
    connectState.setPeripheral(peripheral);
    tagPairingStateMachine.onStateEvent(connectState.getServiceDiscoveredState());
  }
}
