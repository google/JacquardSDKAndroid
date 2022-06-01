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
package com.google.android.jacquard.sdk.model;

import com.google.atap.jacquard.protocol.JacquardProtocol.DeviceInfoResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.auto.value.AutoValue;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Information describing a component. */
@AutoValue
public abstract class InternalComponent implements Comparable<InternalComponent> {

  // match and extract serial numbers from the mfg id, example: 0-07-YMDDVVVPPSSSSS-1902
  private static final Pattern MFG_ID_PATTERN = Pattern.compile("^\\w+-\\w+-(\\w+)-\\w+$");

  // need to exclude componentId from equals()
  private int componentId;

  public final int componentId() {
    return componentId;
  }

  public abstract Domain domain();

  public abstract int vendorId();

  public abstract int productId();

  public abstract String serialNumber();

  public abstract String gearId();

  public abstract String vendorName();

  public abstract String modelName();

  public abstract String sku();

  public abstract Revision revision();

  public abstract int hardwareVersion();

  public abstract Revision bootLoaderRevision();

  public String vendor() {
    return formatId(vendorId(), vendorName());
  }

  public String product() {
    return formatId(productId(), modelName());
  }

  private static String formatId(int id, String dflt) {
    return (id == 0
        ? dflt
        : String.format(
            Locale.US,
            "%02x-%02x-%02x-%02x",
            (id >> 24) & 0xff,
            (id >> 16) & 0xff,
            (id >> 8) & 0xff,
            id & 0xff))
        .split("\\s+")[0];
  }

  public boolean isTag() {
    return componentId() == 0;
  }

  public static InternalComponent of(int componentId, DeviceInfoResponse deviceInfoResponse,
      Domain domain) {

    Revision firmwareRevision = Revision
        .create(deviceInfoResponse.getFirmwareMajor(), deviceInfoResponse.getFirmwareMinor(),
            deviceInfoResponse.getFirmwarePoint());

    Revision bootloaderRevision = Revision.create(deviceInfoResponse.getBootloaderMajor(),
        deviceInfoResponse.getBootloaderMinor(), deviceInfoResponse.getBootloaderPoint());

    return create(
        componentId,
        domain,
        deviceInfoResponse.getVendorId(),
        deviceInfoResponse.getProductId(),
        componentId != 0
            ? deviceInfoResponse.getUuid()
            : extractUJTSerialNumber(deviceInfoResponse.getUuid()),
        deviceInfoResponse.getGearId(),
        deviceInfoResponse.getVendor(),
        deviceInfoResponse.getModel(),
        deviceInfoResponse.getSkuId(),
        firmwareRevision,
        deviceInfoResponse.getRevision(),
        bootloaderRevision);
  }

  private static InternalComponent create(
      int componentId,
      Domain domain,
      int vendorId,
      int productId,
      String serialNumber,
      String gearId,
      String vendor,
      String model,
      String sku,
      Revision revision,
      int hardwareVersion,
      Revision bootLoaderRevision) {
    InternalComponent c =
        new AutoValue_InternalComponent(
            domain,
            vendorId,
            productId,
            serialNumber,
            gearId,
            vendor,
            model,
            sku,
            revision,
            hardwareVersion,
            bootLoaderRevision);
    c.componentId = componentId;
    return c;
  }

  @Override
  public int compareTo(InternalComponent o) {
    int r = vendorName().compareTo(o.vendorName());
    r = r != 0 ? r : modelName().compareTo(o.modelName());
    r = r != 0 ? r : revision().compareTo(o.revision());
    r = r != 0 ? r : serialNumber().compareTo(o.serialNumber());
    return r;
  }

  private static String extractUJTSerialNumber(String uuid) {
    Matcher m = MFG_ID_PATTERN.matcher(uuid);
    return m.find() ? m.group(1) : uuid;
  }
}