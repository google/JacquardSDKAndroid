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

package com.google.android.jacquard.sdk.connection;

import static com.google.android.jacquard.sdk.util.BluetoothSig.RESPONSE_UUID;
import static com.google.common.truth.Truth.assertThat;

import android.Manifest.permission;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build.VERSION_CODES;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.jacquard.sdk.ConnectState.Type;
import com.google.android.jacquard.sdk.FakeConnectState;
import com.google.android.jacquard.sdk.JacquardManager;
import com.google.android.jacquard.sdk.JacquardManagerInitialization;
import com.google.android.jacquard.sdk.datastore.DataProvider;
import com.google.android.jacquard.sdk.initialization.FakeProtocolInitializationStateMachine;
import com.google.android.jacquard.sdk.initialization.InitializationState;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.CharacteristicUpdate;
import com.google.android.jacquard.sdk.model.FakePeripheral;
import com.google.android.jacquard.sdk.model.JacquardError;
import com.google.android.jacquard.sdk.model.Product;
import com.google.android.jacquard.sdk.model.Product.Capability;
import com.google.android.jacquard.sdk.model.SdkConfig;
import com.google.android.jacquard.sdk.model.Vendor;
import com.google.android.jacquard.sdk.pairing.FakeTagPairingStateMachine;
import com.google.android.jacquard.sdk.pairing.RequiredCharacteristics;
import com.google.android.jacquard.sdk.pairing.TagPairingState;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import com.google.android.jacquard.sdk.tag.JacquardTagFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link TagConnectionStateMachine}.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public final class TagConnectionStateMachineTest {

  private static final int TOTAL_STEPS = 14;
  private final static String VENDOR_ID = "74-a8-ce-54";
  private final static String PRODUCT_ID = "8a-66-50-f4";

  private final FakeTagPairingStateMachine tagPairingStateMachine = new FakeTagPairingStateMachine();
  private final FakeProtocolInitializationStateMachine protocolInitializationStateMachine = new FakeProtocolInitializationStateMachine();
  private final FakePeripheral peripheral = new FakePeripheral(/* bleQueue= */ null);

  private TagConnectionStateMachine connectionStateMachine;
  private ConnectionState connectionState;
  private FakeConnectState connectState = new FakeConnectState();

  @Before
  public void setup() {
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    JacquardManagerInitialization.initJacquardManager();
    DataProvider.create(getVendors());
    connectionStateMachine = new TagConnectionStateMachine(/* device= */null, this::doConnect);
    connectionStateMachine.getState().onNext(state -> connectionState = state);
  }

  @Test
  public void getState_emitsState() {
    // Assert
    assertThat(connectionState.getType())
        .isEqualTo(ConnectionState.ofPreparingToConnect().getType());
  }

  @Test
  public void connect_tagPairingStateDisconnected_emitsPairingToConnect() {
    // Arrange
    connectState.setType(Type.DISCONNECTED);
    connectState.setPeripheral(peripheral);
    // Act
    connectionStateMachine.connect();
    // Assert
    assertThat(connectionState.getType())
        .isEqualTo(ConnectionState.ofPreparingToConnect().getType());
  }

  @Test
  public void connect_tagPairingStateBluetoothConnected_emitsConnectingState() {
    // Arrange
    connectState.setType(Type.CONNECTED);
    connectState.setPeripheral(peripheral);
    // Act
    connectionStateMachine.connect();
    // Assert
    assertThat(connectionState.getType())
        .isEqualTo(ConnectionState.ofConnecting().getType());
  }

  @Test
  public void connect_tagPairingStateServiceDiscovered_emitsConnectingState() {
    // Arrange
    onTagPairingStateConnected();
    connectionStateMachine.connect(tagPairingStateMachine);
    connectState.setType(Type.SERVICES_DISCOVERED);
    // Act
    connectionStateMachine.connect();
    // Assert
    assertThat(connectionState.getType())
        .isEqualTo(ConnectionState.ofConnecting().getType());
  }

  @Test
  public void connect_tagPairingStateCharacteristicsUpdated_emitsInitializingState() {
    // Arrange
    onTagPairingStateServiceDiscovered();
    connectionStateMachine.connect(tagPairingStateMachine);
    onTagPairingStateCharacteristicUpdated();
    // Act
    connectionStateMachine.connect(tagPairingStateMachine);
    // Assert
    assertThat(connectionState.getType())
        .isEqualTo(ConnectionState.ofInitializing().getType());
  }

  @Test
  public void connect_tagPairingStateError_emitsDisconnectState() {
    // Arrange
    connectState.setType(Type.FAILED_TO_CONNECT);
    connectState.setPeripheral(peripheral);
    tagPairingStateMachine
        .setTagPairingState(TagPairingState.ofError(JacquardError.ofInternalError()));
    // Act
    connectionStateMachine.connect(tagPairingStateMachine);
    // Assert
    assertThat(connectionState.getType())
        .isEqualTo(ConnectionState.Type.DISCONNECTED);
  }

  @Test
  public void initializeConnection_protocolInitializationTagInitialized_emitsPreparingToConnect() {
    // Arrange
    JacquardManagerInitialization.initJacquardManager();
    onTagPairingStateServiceDiscovered();
    connectionStateMachine.connect(tagPairingStateMachine);
    onTagPairingStateCharacteristicUpdated();
    connectionStateMachine.connect(tagPairingStateMachine);
    ConnectedJacquardTag tag = JacquardTagFactory.createConnectedJacquardTag();
    protocolInitializationStateMachine.setInitializationState(
        InitializationState.ofTagInitialized(tag));
    connectionStateMachine.initializeConnection(protocolInitializationStateMachine);
    onTagPairingStateDisconnected();
    //Act
    connectionStateMachine.connect(tagPairingStateMachine);
    // Assert
    assertThat(connectionState.getType())
        .isEqualTo(ConnectionState.Type.CONNECTED);
  }

  @Test
  public void initializeConnection_protocolInitializationTagInitialized_emitsConnectedState() {
    // Arrange
    JacquardManagerInitialization.initJacquardManager();
    final String CLIENT_ID = "aa-aa-aa-aa";
    final String API_KEY = "api key";
    JacquardManagerInitialization.initJacquardManager();
    JacquardManager.getInstance()
        .init(SdkConfig.of(CLIENT_ID, API_KEY, /* cloudEndpointUrl= */ null));
    onTagPairingStateServiceDiscovered();
    connectionStateMachine.connect(tagPairingStateMachine);
    onTagPairingStateCharacteristicUpdated();
    connectionStateMachine.connect(tagPairingStateMachine);
    ConnectedJacquardTag tag = JacquardTagFactory.createConnectedJacquardTag();
    protocolInitializationStateMachine.setInitializationState(
        InitializationState.ofTagInitialized(tag));
    // Act
    connectionStateMachine.initializeConnection(protocolInitializationStateMachine);
    // Assert
    assertThat(connectionState.getType())
        .isEqualTo(ConnectionState.Type.CONNECTED);
  }

  @Test
  public void initializeConnection_protocolInitializationTagInitialized_emitsDisconnectedState() {
    // Arrange
    onTagPairingStateServiceDiscovered();
    connectionStateMachine.connect(tagPairingStateMachine);
    onTagPairingStateCharacteristicUpdated();
    connectionStateMachine.connect(tagPairingStateMachine);
    protocolInitializationStateMachine.setInitializationState(
        InitializationState.ofError(new Throwable()));
    // Act
    connectionStateMachine.initializeConnection(protocolInitializationStateMachine);
    // Assert
    assertThat(connectionState.getType())
        .isEqualTo(ConnectionState.Type.DISCONNECTED);
  }

  @Test
  public void destroy_unsubscribeSubscriptions() {
    //Act
    connectionStateMachine.destroy();
    // Assert
    assertThat(connectionState.getType())
        .isEqualTo(ConnectionState.Type.PREPARING_TO_CONNECT);
  }

  @Test
  public void initializeConnection_protocolInitializationError_emitsConfiguringState() {
    // Arrange
    protocolInitializationStateMachine.setInitializationState(
        InitializationState.ofError(new Throwable()));
    // Act
    connectionStateMachine.initializeConnection(protocolInitializationStateMachine);
    // Assert
    assertThat(connectionState.getType())
        .isEqualTo(ConnectionState.Type.DISCONNECTED);
  }

  @Test
  public void onConnectStateEvent() {
    // Arrange
    connectState.setPeripheral(peripheral);
    // Act
    connectionStateMachine.onStateEvent(connectState.getDisConnectedState());
    // Assert
    assertThat(connectionState.getType())
        .isEqualTo(ConnectionState.ofPreparingToConnect().getType());
  }

  private void doConnect(BluetoothDevice device) {
    connectionStateMachine.onStateEvent(connectState);
  }

  private void onTagPairingStateDisconnected() {
    connectState.setType(Type.DISCONNECTED);
    connectState.setPeripheral(peripheral);
    tagPairingStateMachine
        .setTagPairingState(TagPairingState.ofDisconnected());
  }

  private void onTagPairingStateConnected() {
    connectState.setType(Type.CONNECTED);
    connectState.setPeripheral(peripheral);
    tagPairingStateMachine.setTagPairingState(TagPairingState.ofBluetoothConnected());
  }

  private void onTagPairingStateServiceDiscovered() {
    connectState.setType(Type.SERVICES_DISCOVERED);
    connectState.setPeripheral(peripheral);
    tagPairingStateMachine.setTagPairingState(TagPairingState.ofAwaitingNotificationUpdates());
  }

  private void onTagPairingStateCharacteristicUpdated() {
    FakeBluetoothGattCharacteristic bluetoothGattCharacteristic = new FakeBluetoothGattCharacteristic(
        RESPONSE_UUID, /* properties= */0, /* permissions= */ 0);
    CharacteristicUpdate characteristicUpdate = CharacteristicUpdate
        .of(peripheral, bluetoothGattCharacteristic);
    connectState.setType(Type.CHARACTERISTIC_UPDATED);
    connectState.setCharacteristicUpdate(characteristicUpdate);
    tagPairingStateMachine.setTagPairingState(TagPairingState
        .ofTagPaired(peripheral, new RequiredCharacteristics()));
  }

  /**
   * A fake implementation of {@link BluetoothGattCharacteristic}.
   */
  private static class FakeBluetoothGattCharacteristic extends BluetoothGattCharacteristic {

    private final UUID uuid;
    private byte[] value;

    /**
     * Create a new BluetoothGattCharacteristic.
     * <p>Requires {@link permission#BLUETOOTH} permission.
     *
     * @param uuid        The UUID for this characteristic
     * @param properties  Properties of this characteristic
     * @param permissions Permissions for this characteristic
     */
    public FakeBluetoothGattCharacteristic(UUID uuid, int properties, int permissions) {
      super(uuid, properties, permissions);
      this.uuid = uuid;
    }

    @Override
    public UUID getUuid() {
      return uuid;
    }

    @Override
    public byte[] getValue() {
      return value;
    }

    @Override
    public boolean setValue(byte[] value) {
      this.value = value;
      return true;
    }
  }

  private static Map<String, Vendor> getVendors() {
    List<Capability> capabilities = new ArrayList<>();
    capabilities.add(Product.Capability.GESTURE);
    capabilities.add(Product.Capability.LED);
    List<Product> products = new ArrayList<>();
    products.add(Product.of(PRODUCT_ID, "Product", "jq_image", capabilities));
    Map<String, Vendor> vendors = new HashMap<>();
    vendors.put(VENDOR_ID, Vendor.of(VENDOR_ID, "Vendor", products));
    return vendors;
  }
}
