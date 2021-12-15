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

package com.google.android.jacquard.sdk.dfu;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build.VERSION_CODES;
import androidx.test.core.app.ApplicationProvider;
import com.google.android.jacquard.sdk.JacquardManagerInitialization;
import com.google.android.jacquard.sdk.command.DeviceInfo;
import com.google.android.jacquard.sdk.command.FakeComponent;
import com.google.android.jacquard.sdk.datastore.DataProvider;
import com.google.android.jacquard.sdk.initialization.FakeTransportImpl;
import com.google.android.jacquard.sdk.initialization.FakeTransportState;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.model.FakePeripheral;
import com.google.android.jacquard.sdk.model.Product;
import com.google.android.jacquard.sdk.model.Product.Capability;
import com.google.android.jacquard.sdk.model.ProtocolSpec;
import com.google.android.jacquard.sdk.model.Vendor;
import com.google.android.jacquard.sdk.pairing.RequiredCharacteristics;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTagImpl;
import com.google.android.jacquard.sdk.util.FakeFragmenter;
import com.google.atap.jacquard.protocol.JacquardProtocol.DFUStatusResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.DeviceInfoResponse;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Unit tests for {@link FirmwareImageWriterStateMachine} */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public final class FirmwareImageWriterStateMachineTest {

  private final static String VENDOR_ID = "74-a8-ce-54";
  private final static String FILE_TEXT = "test";
  private final static String PRODUCT_ID = "8a-66-50-f4";
  private FirmwareImageWriterStateMachine firmwareImageWriter;
  private ByteArrayInputStream inputStream;

  @Before
  public void setup() {
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    DataProvider.create(getVendors());
    JacquardManagerInitialization.initJacquardManager();
    firmwareImageWriter = new FirmwareImageWriterStateMachine(VENDOR_ID, PRODUCT_ID,
        Component.TAG_ID);
    inputStream = new ByteArrayInputStream(FILE_TEXT.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  public void uploadBinary_fileNotFoundException() {
    // Assign
    ConnectedJacquardTagImpl connectedJacquardTag = new ConnectedJacquardTagImpl(getTransport(),
        getDeviceInfo());
    AtomicReference<FirmwareImageWriterState> response = new AtomicReference<>();
    firmwareImageWriter.getState().onNext(state -> {
      response.set(state);
    });
    // Act
    firmwareImageWriter.uploadBinary(connectedJacquardTag, null);
    // Assert
    assertThat(response.get().getType()).isEqualTo(FirmwareImageWriterState.Type.ERROR);
  }

  @Test
  public void uploadBinary_stateTerminal() {
    // Assign
    ConnectedJacquardTagImpl connectedJacquardTag = new ConnectedJacquardTagImpl(getTransport(),
        getDeviceInfo());
    AtomicReference<FirmwareImageWriterState> response = new AtomicReference<>();
    firmwareImageWriter.getState().onNext(state -> {
      response.set(state);
    });
    firmwareImageWriter.uploadBinary(connectedJacquardTag, null);
    // Act
    firmwareImageWriter.uploadBinary(connectedJacquardTag, inputStream);
    // Assert
    assertThat(response.get().getType()).isEqualTo(FirmwareImageWriterState.Type.ERROR);
  }

  @Test
  public void uploadBinary_checkingStatus_preparingForWrite_writing_complete() {
    // Assign
    FakeTransportImpl fakeTransport = getTransport();
    ConnectedJacquardTagImpl connectedJacquardTag = new ConnectedJacquardTagImpl(fakeTransport,
        getDeviceInfo());
    List<FirmwareImageWriterState> firmwareImageWriterStates = new ArrayList<>();
    firmwareImageWriter.getState().onNext(state -> {
      firmwareImageWriterStates.add(state);
    });
    fakeTransport.setDFUStatusResponse(getDfuStatusResponse(0, 0, 0, 0));
    // Act
    firmwareImageWriter.uploadBinary(connectedJacquardTag, inputStream);
    // Assert
    assertThat(firmwareImageWriterStates.get(0).getType())
        .isEqualTo(FirmwareImageWriterState.Type.CHECKING_STATUS);
    assertThat(firmwareImageWriterStates.get(1).getType())
        .isEqualTo(FirmwareImageWriterState.Type.PREPARING_FOR_WRITE);
    assertThat(firmwareImageWriterStates.get(2).writing().offset()).isEqualTo(0);
    assertThat(firmwareImageWriterStates.get(3).writing().offset()).isEqualTo(FILE_TEXT.length());
    assertThat(firmwareImageWriterStates.get(4).getType())
        .isEqualTo(FirmwareImageWriterState.Type.COMPLETE);
    assertThat(firmwareImageWriterStates.size()).isEqualTo(5);
  }

  @Test
  public void uploadBinary_checkingStatus_writing_complete() {
    // Assign
    FakeTransportImpl fakeTransport = getTransport();
    ConnectedJacquardTagImpl connectedJacquardTag = new ConnectedJacquardTagImpl(fakeTransport,
        getDeviceInfo());
    List<FirmwareImageWriterState> firmwareImageWriterStates = new ArrayList<>();
    firmwareImageWriter.getState().onNext(state -> {
      firmwareImageWriterStates.add(state);
    });
    fakeTransport.setDFUStatusResponse(getDfuStatusResponse(63646, 2, 39686, 4));
    // Act
    firmwareImageWriter.uploadBinary(connectedJacquardTag, inputStream);
    // Assert
    assertThat(firmwareImageWriterStates.get(0).getType())
        .isEqualTo(FirmwareImageWriterState.Type.CHECKING_STATUS);
    assertThat(firmwareImageWriterStates.get(1).getType())
        .isEqualTo(FirmwareImageWriterState.Type.WRITING);
    assertThat(firmwareImageWriterStates.get(2).writing().offset()).isEqualTo(FILE_TEXT.length());
    assertThat(firmwareImageWriterStates.get(3).getType())
        .isEqualTo(FirmwareImageWriterState.Type.COMPLETE);
    assertThat(firmwareImageWriterStates.size()).isEqualTo(4);
  }

  @Test
  public void uploadBinary_checkingStatus_complete() {
    // Assign
    FakeTransportImpl fakeTransport = getTransport();
    ConnectedJacquardTagImpl connectedJacquardTag = new ConnectedJacquardTagImpl(fakeTransport,
        getDeviceInfo());
    List<FirmwareImageWriterState> firmwareImageWriterStates = new ArrayList<>();
    firmwareImageWriter.getState().onNext(state -> {
      firmwareImageWriterStates.add(state);
    });
    fakeTransport.setDFUStatusResponse(getDfuStatusResponse(39686, 4, 39686, 4));
    // Act
    firmwareImageWriter.uploadBinary(connectedJacquardTag, inputStream);
    // Assert
    assertThat(firmwareImageWriterStates.get(0).getType())
        .isEqualTo(FirmwareImageWriterState.Type.CHECKING_STATUS);
    assertThat(firmwareImageWriterStates.get(1).writing().done()).isTrue();
    assertThat(firmwareImageWriterStates.get(2).getType())
        .isEqualTo(FirmwareImageWriterState.Type.COMPLETE);
    assertThat(firmwareImageWriterStates.size()).isEqualTo(3);
  }

  @Test
  public void uploadBinary_checkingStatusError() {
    // Assign
    FakeTransportImpl fakeTransport = getTransport();
    ConnectedJacquardTagImpl connectedJacquardTag = new ConnectedJacquardTagImpl(fakeTransport,
        getDeviceInfo());
    List<FirmwareImageWriterState> firmwareImageWriterStates = new ArrayList<>();
    firmwareImageWriter.getState().onNext(state -> {
      firmwareImageWriterStates.add(state);
    });
    fakeTransport.throwErrorDfuStatus();
    // Act
    firmwareImageWriter.uploadBinary(connectedJacquardTag, inputStream);
    // Assert
    assertThat(firmwareImageWriterStates.get(0).getType())
        .isEqualTo(FirmwareImageWriterState.Type.CHECKING_STATUS);
    assertThat(firmwareImageWriterStates.get(1).getType())
        .isEqualTo(FirmwareImageWriterState.Type.ERROR);
    assertThat(firmwareImageWriterStates.size()).isEqualTo(2);
  }

  @Test
  public void uploadBinary_preparingForWriteError() {
    // Assign
    FakeTransportImpl fakeTransport = getTransport();
    ConnectedJacquardTagImpl connectedJacquardTag = new ConnectedJacquardTagImpl(fakeTransport,
        getDeviceInfo());
    List<FirmwareImageWriterState> firmwareImageWriterStates = new ArrayList<>();
    firmwareImageWriter.getState().onNext(state -> {
      firmwareImageWriterStates.add(state);
    });
    fakeTransport.throwErrorDfuPrepare();
    // Act
    firmwareImageWriter.uploadBinary(connectedJacquardTag, inputStream);
    // Assert
    assertThat(firmwareImageWriterStates.get(0).getType())
        .isEqualTo(FirmwareImageWriterState.Type.CHECKING_STATUS);
    assertThat(firmwareImageWriterStates.get(1).getType())
        .isEqualTo(FirmwareImageWriterState.Type.PREPARING_FOR_WRITE);
    assertThat(firmwareImageWriterStates.get(2).getType())
        .isEqualTo(FirmwareImageWriterState.Type.ERROR);
    assertThat(firmwareImageWriterStates.size()).isEqualTo(3);
  }

  @Test
  public void uploadBinary_writingError() {
    // Assign
    FakeTransportImpl fakeTransport = getTransport();
    ConnectedJacquardTagImpl connectedJacquardTag = new ConnectedJacquardTagImpl(fakeTransport,
        getDeviceInfo());
    List<FirmwareImageWriterState> firmwareImageWriterStates = new ArrayList<>();
    firmwareImageWriter.getState().onNext(state -> {
      firmwareImageWriterStates.add(state);
    });
    fakeTransport.throwErrorDfuWrite();
    // Act
    firmwareImageWriter.uploadBinary(connectedJacquardTag, inputStream);
    // Assert
    assertThat(firmwareImageWriterStates.get(0).getType())
        .isEqualTo(FirmwareImageWriterState.Type.CHECKING_STATUS);
    assertThat(firmwareImageWriterStates.get(1).getType())
        .isEqualTo(FirmwareImageWriterState.Type.PREPARING_FOR_WRITE);
    assertThat(firmwareImageWriterStates.get(2).writing().offset()).isEqualTo(0);
    assertThat(firmwareImageWriterStates.get(3).getType())
        .isEqualTo(FirmwareImageWriterState.Type.ERROR);
    assertThat(firmwareImageWriterStates.size()).isEqualTo(4);
  }

  private static FakeTransportImpl getTransport() {
    FakePeripheral peripheral = new FakePeripheral(/* bleQueue= */ null);
    FakeFragmenter commandFragmenter = new FakeFragmenter("commandFragmenter",
        ProtocolSpec.VERSION_2.getMtuSize());
    FakeFragmenter notificationFragmenter = new FakeFragmenter("notificationFragmenter",
        ProtocolSpec.VERSION_2.getMtuSize());
    FakeFragmenter dataFragmenter = new FakeFragmenter("dataFragmenter",
        ProtocolSpec.VERSION_2.getMtuSize());
    FakeTransportState transportState = new FakeTransportState(commandFragmenter,
        notificationFragmenter, dataFragmenter);
    RequiredCharacteristics requiredCharacteristics = new RequiredCharacteristics();
    FakeTransportImpl transport = new FakeTransportImpl(peripheral, requiredCharacteristics,
        transportState);
    return transport;
  }

  private static DeviceInfo getDeviceInfo() {
    return DeviceInfo.ofTag(
        DeviceInfoResponse.newBuilder().setGearId("").setSkuId("").setVendorId(1957219924)
            .setProductId(-1973006092).setUuid(FakeComponent.UUID).setVendor("").setRevision(0)
            .setModel("").setMlVersion("").setBootloaderMajor(0).setBootloaderMinor(0)
            .setFirmwarePoint(0).setFirmwareMinor(0).setFirmwareMajor(0).build());
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

  private DFUStatusResponse getDfuStatusResponse(int currentCrc, int currentSize, int finalCrc,
      int finalSize) {
    return DFUStatusResponse.newBuilder().setFinalSize(finalSize)
        .setFinalCrc(finalCrc).setCurrentSize(currentSize).setComponent(Component.TAG_ID)
        .setCurrentCrc(currentCrc).build();
  }
}
