/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.jacquard.sdk.initialization;

import static android.os.Looper.getMainLooper;
import static com.google.android.jacquard.sdk.util.BluetoothSig.RESPONSE_UUID;
import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.jacquard.sdk.ConnectState;
import com.google.android.jacquard.sdk.FakeConnectState;
import com.google.android.jacquard.sdk.JacquardManager;
import com.google.android.jacquard.sdk.JacquardManagerInitialization;
import com.google.android.jacquard.sdk.command.FakeComponent;
import com.google.android.jacquard.sdk.datastore.DataProvider;
import com.google.android.jacquard.sdk.initialization.TransportTest.FakeBluetoothGattCharacteristic;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.CharacteristicUpdate;
import com.google.android.jacquard.sdk.model.FakePeripheral;
import com.google.android.jacquard.sdk.model.Product;
import com.google.android.jacquard.sdk.model.Product.Capability;
import com.google.android.jacquard.sdk.model.ProtocolSpec;
import com.google.android.jacquard.sdk.model.SdkConfig;
import com.google.android.jacquard.sdk.model.Vendor;
import com.google.android.jacquard.sdk.pairing.RequiredCharacteristics;
import com.google.android.jacquard.sdk.util.Fragmenter;
import com.google.atap.jacquard.protocol.JacquardProtocol.BeginResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.DeviceInfoResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.HelloResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;
import com.google.atap.jacquard.protocol.JacquardProtocol.Status;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for {@link ProtocolInitializationStateMachine}
 */
@RunWith(AndroidJUnit4.class)
public final class ProtocolInitializationStateMachineTest {

  private FakePeripheral peripheral;
  private ProtocolInitializationStateMachine protocolInitializationStateMachine;

  @Before
  public void setUp() {
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    peripheral = new FakePeripheral(/* bleQueue= */null);
    RequiredCharacteristics requiredCharacteristics = new RequiredCharacteristics();
    protocolInitializationStateMachine = new ProtocolInitializationStateMachine(peripheral,
        requiredCharacteristics);
  }

  @Test
  public void onConnectStateEvent_tagNegotiation() {
    // Cases 1 - Hello command send
    // Arrange
    AtomicReference<InitializationState> initializationState = new AtomicReference<>();
    protocolInitializationStateMachine
        .getState().onNext(initializationState::set);
    // Act
    protocolInitializationStateMachine.startNegotiation();
    shadowOf(getMainLooper()).idleFor(Duration.ofSeconds(2));
    // Assert
    assertThat(initializationState.get().equals(InitializationState.ofHelloSent())).isTrue();

    // Cases 2 - Begin command send
    // Arrange
    FakeConnectState helloCommandResponse = getHelloCommandResponse();
    // Act
    protocolInitializationStateMachine.onStateEvent(helloCommandResponse);
    // Assert
    assertThat(initializationState.get().equals(InitializationState.ofBeginSent())).isTrue();

    // Cases 3 - GetDeviceInfo command send
    // Arrange
    JacquardManagerInitialization.initJacquardManager();
    JacquardManager.getInstance()
        .init(SdkConfig
            .of(/* clientId= */"aa-aa-aa-aa", /* apiKey= */"api key", /* cloudEndpointUrl= */null));
    FakeConnectState tagInitializedConnectState = getBeginCommandResponse();
    DataProvider.create(getVendors());
    // Act
    protocolInitializationStateMachine.onStateEvent(tagInitializedConnectState);
    // Assert
    assertThat(initializationState.get().equals(InitializationState.ofComponentInfoSent()))
        .isTrue();

    // Cases 4 - Tag initialized
    // Arrange
    List<FakeConnectState> connectStates = getDeviceInfoCommandResponse();
    DataProvider.create(getVendors());
    // Act
    for (FakeConnectState connectState : connectStates) {
      protocolInitializationStateMachine.onStateEvent(connectState);
    }
    // Assert
    assertThat(initializationState.get().getType().equals(InitializationState.Type.TAG_INITIALIZED))
        .isTrue();

    // Cases 5 - State machine is already terminal
    // Act
    protocolInitializationStateMachine.startNegotiation();
    // Assert
    assertThat(initializationState.get().getType().equals(InitializationState.Type.TAG_INITIALIZED))
        .isTrue();
  }

  @Test
  public void onConnectStateEvent_failedResponse() {
    // Arrange
    AtomicReference<InitializationState> initializationState = new AtomicReference<>();
    protocolInitializationStateMachine
        .getState().onNext(initializationState::set);
    protocolInitializationStateMachine.startNegotiation();
    shadowOf(getMainLooper()).idleFor(Duration.ofSeconds(2));
    FakeConnectState failureResponse = getFailureResponse();
    // Act
    protocolInitializationStateMachine.onStateEvent(failureResponse);
    // Assert
    assertThat(initializationState.get().equals(InitializationState.ofHelloSent())).isTrue();
  }

  private static List<byte[]> getResponseByteArray(Response response) {
    List<byte[]> fragments = new Fragmenter("commandFragmenter",
        ProtocolSpec.VERSION_2.getMtuSize()).fragmentData(response.toByteArray());
    return fragments;
  }

  private static Map<String, Vendor> getVendors() {
    List<Capability> capabilities = new ArrayList<>();
    capabilities.add(Product.Capability.GESTURE);
    capabilities.add(Product.Capability.LED);
    List<Product> products = new ArrayList<>();
    products.add(Product.of("00-00-00-02", "Product 2", "jq_image", capabilities));
    Map<String, Vendor> vendors = new HashMap<>();
    vendors.put("00-00-00-01", Vendor.of("00-00-00-01", "Vendor 1", products));
    return vendors;
  }

  private CharacteristicUpdate getCharacteristicUpdate(byte[] commandResponse) {
    FakeBluetoothGattCharacteristic bluetoothGattCharacteristic = new FakeBluetoothGattCharacteristic(
        RESPONSE_UUID, /* properties= */ 0, /* permissions= */ 0);
    bluetoothGattCharacteristic.setValue(commandResponse);
    return CharacteristicUpdate
        .of(peripheral, bluetoothGattCharacteristic);
  }

  private FakeConnectState getHelloCommandResponse() {
    FakeConnectState connectState = new FakeConnectState();
    connectState.setType(ConnectState.Type.CHARACTERISTIC_UPDATED);
    HelloResponse helloResponse = HelloResponse.newBuilder().setModel("UJT").setProductId(675014560)
        .setProtocolMax(2).setProtocolMin(2).setVendor("Google Inc.").setVendorId(293089480)
        .build();
    Response response = Response
        .newBuilder()
        .setComponentId(0)
        .setId(1)
        .setStatus(Status.STATUS_OK)
        .setExtension(HelloResponse.hello, helloResponse)
        .build();
    connectState
        .setCharacteristicUpdate(getCharacteristicUpdate(getResponseByteArray(response).get(0)));
    return connectState;
  }

  private List<FakeConnectState> getDeviceInfoCommandResponse() {
    DeviceInfoResponse deviceInfoResponse = DeviceInfoResponse.newBuilder()
        .setFirmwareMajor(0).setFirmwareMinor(0).setFirmwarePoint(0).setModel("UJT")
        .setVendorId(1).setUuid(FakeComponent.UUID).setProductId(2).setRevision(0).setVendor("")
        .setBootloaderPoint(0).setBootloaderMinor(0).setBootloaderMajor(0).build();
    Response response = Response
        .newBuilder()
        .setComponentId(0)
        .setId(3)
        .setStatus(Status.STATUS_OK)
        .setExtension(DeviceInfoResponse.deviceInfo, deviceInfoResponse)
        .build();
    List<byte[]> responseByteArray = getResponseByteArray(response);
    List<FakeConnectState> connectStates = new ArrayList<>();
    for (byte[] bytes : responseByteArray){
      FakeConnectState connectState = new FakeConnectState();
      connectState.setType(ConnectState.Type.CHARACTERISTIC_UPDATED);
      connectState.setCharacteristicUpdate(getCharacteristicUpdate(bytes));
      connectStates.add(connectState);
    }
    return connectStates;
  }

  private FakeConnectState getBeginCommandResponse() {
    FakeConnectState connectState = new FakeConnectState();
    connectState.setType(ConnectState.Type.CHARACTERISTIC_UPDATED);
    BeginResponse beginResponse = BeginResponse.newBuilder().build();
    Response response = Response
        .newBuilder()
        .setComponentId(0)
        .setId(2)
        .setStatus(Status.STATUS_OK)
        .setExtension(BeginResponse.begin, beginResponse)
        .build();
    connectState
        .setCharacteristicUpdate(getCharacteristicUpdate(getResponseByteArray(response).get(0)));
    return connectState;
  }

  private FakeConnectState getFailureResponse() {
    FakeConnectState connectState = new FakeConnectState();
    connectState.setType(ConnectState.Type.CHARACTERISTIC_UPDATED);
    Response response = Response.newBuilder()
        .setId(1)
        .setComponentId(1)
        .setStatus(Status.ERROR_APP_UNKNOWN)
        .build();
    connectState
        .setCharacteristicUpdate(getCharacteristicUpdate(getResponseByteArray(response).get(0)));
    return connectState;
  }
}
