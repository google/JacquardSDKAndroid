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
import static com.google.android.jacquard.sdk.util.BluetoothSig.NOTIFY_UUID;
import static com.google.android.jacquard.sdk.util.BluetoothSig.RESPONSE_UUID;
import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.Manifest.permission;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build.VERSION_CODES;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.CharacteristicUpdate;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.model.FakePeripheral;
import com.google.android.jacquard.sdk.model.Peripheral;
import com.google.android.jacquard.sdk.model.ProtocolSpec;
import com.google.android.jacquard.sdk.pairing.RequiredCharacteristics;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.android.jacquard.sdk.util.FakeFragmenter;
import com.google.atap.jacquard.protocol.JacquardProtocol;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;
import com.google.atap.jacquard.protocol.JacquardProtocol.Status;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link Transport}
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public final class TransportTest {

  private static final String IDENTIFIER = "C2:04:1C:6F:02:BA";
  private static final String PERIPGERAL_NAME = "Fake-Jacquard Tag";
  private static final int RETRIES = 2;

  private final FakeFragmenter commandFragmenter = new FakeFragmenter("commandFragmenter",
      ProtocolSpec.VERSION_2.getMtuSize());
  private final FakeFragmenter notificationFragmenter = new FakeFragmenter("notificationFragmenter",
      ProtocolSpec.VERSION_2.getMtuSize());
  FakeFragmenter dataFragmenter = new FakeFragmenter("dataFragmenter",
      ProtocolSpec.VERSION_2.getMtuSize());
  private final FakeTransportState transportState = new FakeTransportState(commandFragmenter,
      notificationFragmenter, dataFragmenter);

  private final FakePeripheral peripheral = new FakePeripheral(null);
  private final RequiredCharacteristics requiredCharacteristics = new RequiredCharacteristics(
      null, null, null, null, /* rawCharacteristic= */null);

  private JacquardProtocol.Notification notification;
  private CharacteristicUpdate characteristic;
  private Transport transport;

  @Before
  public void setUp() {
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    transport = new TransportImpl(peripheral, requiredCharacteristics, transportState);
    transport.getNotificationSignal().onNext(n -> notification = n);
  }

  @Test
  public void getPeripheralIdentifier_returnsIdentifier() {
    // Act
    String identifier = transport.getPeripheralIdentifier();
    // Assert
    assertThat(identifier).isEqualTo(IDENTIFIER);
  }

  @Test
  public void getPeripheralName_returnsTagName() {
    // Act
    String peripheralName = transport.getDefaultDisplayName();
    // Assert
    assertThat(peripheralName).isEqualTo(PERIPGERAL_NAME);
  }

  @Test
  public void enqueue_sendsRequest() {
    // Act
    transport.enqueue(createRequest(), Peripheral.WriteType.WITH_RESPONSE, RETRIES).consume();
    // Assert
    assertThat(transport.getPendingRequestSize()).isEqualTo(1);
  }

  @Test
  public void enqueue_retriesSendsRequest() {
    // Arrange
    transportState.commandFragmenter.setThrowsException(/* throwsException= */true);
    AtomicReference<Throwable> throwableAtomicReference = new AtomicReference<>();
    // Act
    transport.enqueue(createRequest(), Peripheral.WriteType.WITH_RESPONSE, RETRIES)
        .tapError(throwableAtomicReference::set).consume();
    // Assert
    assertThat(transport.getPendingRequestSize()).isEqualTo(0);
    assertThat(throwableAtomicReference.get()).isInstanceOf(Exception.class);
  }

  @Test
  public void characteristicUpdated_responseUuid_returnsSuccessResponseResult() {
    // Arrange
    FakeBluetoothGattCharacteristic bluetoothGattCharacteristic = new FakeBluetoothGattCharacteristic(
        RESPONSE_UUID, /* properties= */0, /* permissions= */ 0);
    CharacteristicUpdate characteristicUpdate = CharacteristicUpdate
        .of(peripheral, bluetoothGattCharacteristic);
    byte[] data = getResponseByteData(/* requestId= */1);
    bluetoothGattCharacteristic.setValue(data);
    AtomicReference<Response> responseAtomicReference = new AtomicReference<>();
    transport.enqueue(createRequest(), Peripheral.WriteType.WITH_RESPONSE, RETRIES)
        .onNext(responseAtomicReference::set);
    // Act
    transport.characteristicUpdated(characteristicUpdate);
    // Assert
    assertThat(responseAtomicReference.get().getStatus()).isEqualTo(Status.STATUS_OK);
  }

  @Test
  public void characteristicUpdated_responseUuid_returnsTimeOutException() {
    // Arrange
    FakeBluetoothGattCharacteristic bluetoothGattCharacteristic = new FakeBluetoothGattCharacteristic(
        RESPONSE_UUID,  /* properties= */ 0,  /* permissions= */  0);
    CharacteristicUpdate characteristicUpdate = CharacteristicUpdate
        .of(peripheral, bluetoothGattCharacteristic);
    byte[] data = getResponseByteData(/* requestId= */0);
    bluetoothGattCharacteristic.setValue(data);
    AtomicReference<Throwable> responseAtomicReference = new AtomicReference<>();
    transport.enqueue(createRequest(), Peripheral.WriteType.WITH_RESPONSE, /* retries=*/0)
        .onError(responseAtomicReference::set);
    // Act
    transport.characteristicUpdated(characteristicUpdate);
    shadowOf(getMainLooper()).idleFor(Duration.ofSeconds(10));
    // Assert
    assertThat(responseAtomicReference.get()).isInstanceOf(TimeoutException.class);
  }

  @Test
  public void characteristicUpdated_responseUuid_retry_returnsTimeOutException() {
    // Arrange
    FakeBluetoothGattCharacteristic bluetoothGattCharacteristic = new FakeBluetoothGattCharacteristic(
        RESPONSE_UUID,  /* properties= */ 0,  /* permissions= */  0);
    CharacteristicUpdate characteristicUpdate = CharacteristicUpdate
        .of(peripheral, bluetoothGattCharacteristic);
    byte[] data = getResponseByteData(/* requestId= */0);
    bluetoothGattCharacteristic.setValue(data);
    AtomicReference<Throwable> responseAtomicReference = new AtomicReference<>();
    transport.enqueue(createRequest(), Peripheral.WriteType.WITH_RESPONSE, /* retries=*/1)
        .onError(responseAtomicReference::set);
    // Act
    transport.characteristicUpdated(characteristicUpdate);
    shadowOf(getMainLooper()).idleFor(Duration.ofSeconds(20));
    // Assert
    assertThat(responseAtomicReference.get()).isInstanceOf(TimeoutException.class);
  }

  @Test
  public void characteristicUpdated_invalidProtocolBufferException_returnsTimeOutException() {
    // Arrange
    FakeBluetoothGattCharacteristic bluetoothGattCharacteristic = new FakeBluetoothGattCharacteristic(
        RESPONSE_UUID,  /* properties= */ 0,  /* permissions= */  0);
    CharacteristicUpdate characteristicUpdate = CharacteristicUpdate
        .of(peripheral, bluetoothGattCharacteristic);
    // Invalid response byte data
    byte[] data = new byte[]{-64, 6, 8, 0, 16, 29, 24, 0};
    bluetoothGattCharacteristic.setValue(data);
    AtomicReference<Throwable> responseAtomicReference = new AtomicReference<>();
    transport.enqueue(createRequest(), Peripheral.WriteType.WITH_RESPONSE, /* retries=*/0)
        .onError(responseAtomicReference::set);
    // Act
    transport.characteristicUpdated(characteristicUpdate);
    shadowOf(getMainLooper()).idleFor(Duration.ofSeconds(10));
    // Assert
    assertThat(responseAtomicReference.get()).isInstanceOf(TimeoutException.class);
  }

  private CharacteristicUpdate getCharacteristicUpdate(byte[] commandResponse) {
    FakeBluetoothGattCharacteristic bluetoothGattCharacteristic = new FakeBluetoothGattCharacteristic(
        RESPONSE_UUID, /* properties= */ 0, /* permissions= */ 0);
    bluetoothGattCharacteristic.setValue(commandResponse);
    return CharacteristicUpdate
        .of(peripheral, bluetoothGattCharacteristic);
  }

  @Test
  public void characteristicUpdated_characteristicsDataEmpty_notHandlesResponseResult() {
    // Arrange
    FakeBluetoothGattCharacteristic bluetoothGattCharacteristic = new FakeBluetoothGattCharacteristic(
        RESPONSE_UUID, /* properties= */0, /* permissions= */ 0);
    bluetoothGattCharacteristic.setValue(new byte[0]);
    CharacteristicUpdate characteristicUpdate = CharacteristicUpdate
        .of(peripheral, bluetoothGattCharacteristic);
    AtomicReference<Response> responseAtomicReference = new AtomicReference<>();
    transport.enqueue(createRequest(), Peripheral.WriteType.WITH_RESPONSE, RETRIES)
        .onNext(responseAtomicReference::set);
    // Act
    transport.characteristicUpdated(characteristicUpdate);
    // Assert
    assertThat(responseAtomicReference.get()).isNull();
  }

  @Test
  public void characteristicUpdated_notifyUuid_emitsNotification() {
    // Arrange
    FakeBluetoothGattCharacteristic bluetoothGattCharacteristic = new FakeBluetoothGattCharacteristic(
        NOTIFY_UUID, /* properties= */0, /* permissions= */ 0);
    CharacteristicUpdate characteristicUpdate = CharacteristicUpdate
        .of(peripheral, bluetoothGattCharacteristic);
    byte[] data = getCharacteristicsDataWhenNotifyUuid();
    bluetoothGattCharacteristic.setValue(data);
    // Act
    transport.characteristicUpdated(characteristicUpdate);
    // Assert
    assertThat(notification.hasComponentId()).isTrue();
  }

  @Test
  public void characteristicUpdated_characteristicsDataEmpty_notEmitsNotification() {
    // Arrange
    FakeBluetoothGattCharacteristic bluetoothGattCharacteristic = new FakeBluetoothGattCharacteristic(
        NOTIFY_UUID, /* properties= */0, /* permissions= */ 0);
    CharacteristicUpdate characteristicUpdate = CharacteristicUpdate
        .of(peripheral, bluetoothGattCharacteristic);
    bluetoothGattCharacteristic.setValue(new byte[0]);
    // Act
    transport.characteristicUpdated(characteristicUpdate);
    // Assert
    assertThat(notification).isNull();
  }

  @Test
  public void characteristicUpdated_decodeFragmentPacketNull_notEmitsNotification() {
    // Arrange
    FakeBluetoothGattCharacteristic bluetoothGattCharacteristic = new FakeBluetoothGattCharacteristic(
        NOTIFY_UUID, /* properties= */0, /* permissions= */ 0);
    CharacteristicUpdate characteristicUpdate = CharacteristicUpdate
        .of(peripheral, bluetoothGattCharacteristic);
    bluetoothGattCharacteristic.setValue(new byte[100]);
//    transportState.setDataNull(/* isDataNull= */ true);
    transportState.notificationFragmenter.setDecodeFragmentNull(/* isDataNull= */true);
    // Act
    transport.characteristicUpdated(characteristicUpdate);
    // Assert
    assertThat(notification).isNull();
  }

  @Test
  public void valueWritten_emitsCharacteristicUpdates() {
    // Arrange
    FakeBluetoothGattCharacteristic bluetoothGattCharacteristic = new FakeBluetoothGattCharacteristic(
        NOTIFY_UUID, /* properties= */0, /* permissions= */ 0);
    CharacteristicUpdate characteristicUpdate = CharacteristicUpdate
        .of(peripheral, bluetoothGattCharacteristic);
    transport.getValueWrittenSignal().onNext(c -> characteristic = c);
    // Act
    transport.valueWritten(characteristicUpdate);
    // Assert
    assertThat(characteristic.characteristic().getUuid()).isEqualTo(NOTIFY_UUID);
  }

  @Test
  public void fetchSignalStrength_emitsRSSIValueUpdated() {
    // Arrange
    List<Integer> rssiValueList = new ArrayList<>();
    transport.fetchRSSIValue().onNext(rssiValueList::add);
    // Act
    transport.onRSSIValueUpdated(-50);
    transport.onRSSIValueUpdated(-45);
    transport.onRSSIValueUpdated(-60);
    // Assert
    assertThat(rssiValueList.size()).isEqualTo(3);
    assertThat(rssiValueList.get(0)).isEqualTo(-50);
    assertThat(rssiValueList.get(1)).isEqualTo(-45);
    assertThat(rssiValueList.get(2)).isEqualTo(-60);
  }

  @Test
  public void fetchSignalStrength_stopRSSIValue() {
    // Arrange
    List<Integer> rssiValueList = new ArrayList<>();
    transport.fetchRSSIValue().onNext(rssiValueList::add);
    transport.onRSSIValueUpdated(-50);
    transport.onRSSIValueUpdated(-45);
    // Act
    transport.stopRSSIValue();
    transport.onRSSIValueUpdated(-60);
    // Assert
    assertThat(rssiValueList.size()).isEqualTo(2);
    assertThat(rssiValueList.get(0)).isEqualTo(-50);
    assertThat(rssiValueList.get(1)).isEqualTo(-45);
  }

  @Test
  public void fetchSignalStrength_emitsRSSIValueUpdated_unsubscribe() {
    // Arrange
    List<Integer> rssiValueList = new ArrayList<>();
    Subscription subscription = transport.fetchRSSIValue().onNext(rssiValueList::add);
    transport.onRSSIValueUpdated(-50);
    transport.onRSSIValueUpdated(-45);
    // Act
    subscription.unsubscribe();
    transport.onRSSIValueUpdated(-60);
    // Assert
    assertThat(rssiValueList.size()).isEqualTo(2);
    assertThat(rssiValueList.get(0)).isEqualTo(-50);
    assertThat(rssiValueList.get(1)).isEqualTo(-45);
  }

  private byte[] getResponseByteData(int requestId) {
    Response response = Response.newBuilder().setComponentId(Component.TAG_ID).setId(requestId)
        .setStatus(Status.STATUS_OK).build();
    return transportState.commandFragmenter.fragmentData(response.toByteArray()).get(0);
  }

  private static byte[] getCharacteristicsDataWhenNotifyUuid() {
    return new byte[]{-64, 6, 8, 0, 16, 29, 24, 0};
  }

  private static JacquardProtocol.Request createRequest() {
    return JacquardProtocol.Request
        .newBuilder()
        .setComponentId(0)
        .setId(0)
        .setDomain(JacquardProtocol.Domain.BASE)
        .setOpcode(JacquardProtocol.Opcode.LED_PATTERN)
        .setComponentId(Component.TAG_ID).build();
  }

  /**
   * A fake implementation of {@link BluetoothGattCharacteristic}.
   */
  public static class FakeBluetoothGattCharacteristic extends BluetoothGattCharacteristic {

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
}
