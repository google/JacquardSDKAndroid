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
 */

package com.google.android.jacquard.sdk.tag;

import static com.google.android.jacquard.sdk.command.FakeComponent.UUID;

import com.google.android.jacquard.sdk.command.DeviceInfo;
import com.google.android.jacquard.sdk.imu.FakeDfuManager;
import com.google.android.jacquard.sdk.initialization.FakeTransportImpl;
import com.google.android.jacquard.sdk.initialization.FakeTransportState;
import com.google.android.jacquard.sdk.model.FakePeripheral;
import com.google.android.jacquard.sdk.model.ProtocolSpec;
import com.google.android.jacquard.sdk.pairing.RequiredCharacteristics;
import com.google.android.jacquard.sdk.util.FakeFragmenter;
import com.google.atap.jacquard.protocol.JacquardProtocol.DeviceInfoResponse;

/**
 * Factory class to create fake {@link ConnectedJacquardTag}.
 */
public class JacquardTagFactory {

  private static final int VID = 1957219924;
  private static final int PID = -1973006092;
  private static final FakeFragmenter commandFragmenter = new FakeFragmenter("commandFragmenter",
      ProtocolSpec.VERSION_2.getMtuSize());
  private static final FakeFragmenter notificationFragmenter = new FakeFragmenter(
      "notificationFragmenter",
      ProtocolSpec.VERSION_2.getMtuSize());
  private static final FakeFragmenter dataFragmenter = new FakeFragmenter("dataFragmenter",
      ProtocolSpec.VERSION_2.getMtuSize());
  private final FakeTransportState transportState = new FakeTransportState(commandFragmenter,
      notificationFragmenter, dataFragmenter);

  private final FakePeripheral peripheral = new FakePeripheral(null);
  private final RequiredCharacteristics requiredCharacteristics = new RequiredCharacteristics(
      null, null, null, null, /* rawCharacteristic= */null);
  private static final JacquardTagFactory instance = new JacquardTagFactory();

  private ConnectedJacquardTagImpl tag;
  private FakeTransportImpl transport;

  public static ConnectedJacquardTag createConnectedJacquardTag() {
    return createConnectedJacquardTag(true);
  }

  public static ConnectedJacquardTag createConnectedJacquardTag(boolean isModulePresent) {
    return instance.createFakeConnectedTag(isModulePresent);
  }

  private ConnectedJacquardTag createFakeConnectedTag(boolean isModulePresent) {
    if (tag == null) {
      transport = new FakeTransportImpl(peripheral, requiredCharacteristics,
          transportState);
      tag = new ConnectedJacquardTagImpl(transport, getDeviceInfo(), new FakeDfuManager());
    }
    transport.setModulePresent(isModulePresent);
    return tag;
  }

  private static DeviceInfo getDeviceInfo() {
    return DeviceInfo.ofTag(
        DeviceInfoResponse.newBuilder().setGearId("").setSkuId("").setVendorId(VID)
            .setProductId(PID).setUuid(UUID).setVendor("").setRevision(0).setModel("")
            .setMlVersion("").setBootloaderMajor(0).setBootloaderMinor(0)
            .setFirmwarePoint(0).setFirmwareMinor(0).setFirmwareMajor(0)
            .build());
  }

  private JacquardTagFactory() {
  }
}
