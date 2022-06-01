/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sdk.command;

import com.google.android.jacquard.sdk.model.Revision;
import com.google.atap.jacquard.protocol.JacquardProtocol.DeviceInfoResponse;
import com.google.auto.value.AutoValue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Data class for holding the device info. */
@AutoValue
public abstract class DeviceInfo {

  /**
   * Gear id of the device.
   */
  public abstract String gearId();

  /**
   * Vendor id of the device.
   */
  public abstract int vendorId();

  /**
   * Product id of the device.
   */
  public abstract int productId();

  /**
   * Sku id of the device.
   */
  public abstract String skuId();

  /**
   * Vendor name of the device.
   */
  public abstract String vendor();

  /**
   * Model name of the device.
   */
  public abstract String model();

  /**
   * ML firmware version of the device .
   */
  public abstract String mlVersion();

  /**
   * Bootloader major version of the device .
   */
  public abstract int bootloaderMajor();

  /**
   * Bootloader minor version of the device .
   */
  public abstract int bootloaderMinor();

  /**
   * Firmware version of the device .
   */
  public abstract Revision version();

  /**
   * Manufacturer serial number.
   */
  public abstract String serialNumber();

  /**
   * Device unique id.
   */
  public abstract String uuid();

  /**
   * Device revision.
   */
  public abstract int revision();

  /**
   * Bootloader point version of the device .
   */
  public abstract int bootloaderPoint();

  static DeviceInfo ofGear(DeviceInfoResponse deviceInfoResponse) {
    return buildDeviceInfo(deviceInfoResponse, deviceInfoResponse.getUuid());
  }

  public static DeviceInfo ofTag(DeviceInfoResponse deviceInfoResponse) {
    return buildDeviceInfo(deviceInfoResponse,
        extractUJTSerialNumber(deviceInfoResponse.getUuid()));
  }

  private static final Pattern MFG_ID_PATTERN = Pattern.compile("^\\w+-\\w+-(\\w+)-\\w+$");

  private static DeviceInfo buildDeviceInfo(DeviceInfoResponse deviceInfoResponse,
      String serialNumber) {
    return new AutoValue_DeviceInfo(
        deviceInfoResponse.getGearId(),
        deviceInfoResponse.getVendorId(),
        deviceInfoResponse.getProductId(),
        deviceInfoResponse.getSkuId(),
        deviceInfoResponse.getVendor(),
        deviceInfoResponse.getModel(),
        deviceInfoResponse.getMlVersion(),
        deviceInfoResponse.getBootloaderMajor(),
        deviceInfoResponse.getBootloaderMinor(),
        Revision
            .create(deviceInfoResponse.getFirmwareMajor(), deviceInfoResponse.getFirmwareMinor(),
                deviceInfoResponse.getFirmwarePoint()),
        serialNumber,
        deviceInfoResponse.getUuid(),
        deviceInfoResponse.getRevision(),
        deviceInfoResponse.getBootloaderPoint());
  }

  private static String extractUJTSerialNumber(String uuid) {
    Matcher m = MFG_ID_PATTERN.matcher(uuid);
    return m.find() ? m.group(1) : uuid;
  }
}
