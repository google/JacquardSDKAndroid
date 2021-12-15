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
package com.google.android.jacquard.sdk.datastore;

import static com.google.android.jacquard.sdk.command.FakeComponent.PID;
import static com.google.android.jacquard.sdk.command.FakeComponent.UUID;
import static com.google.android.jacquard.sdk.command.FakeComponent.VID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.os.Build.VERSION_CODES;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.jacquard.sdk.command.DeviceInfo;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.model.Product;
import com.google.android.jacquard.sdk.model.Vendor;
import com.google.android.jacquard.sdk.util.StringUtils;
import com.google.atap.jacquard.protocol.JacquardProtocol.DeviceInfoResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link DataProvider}
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public final class DataProviderTest {

  @Before
  public void setup() {
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    List<Product.Capability> capabilities = new ArrayList<>();
    capabilities.add(Product.Capability.GESTURE);
    capabilities.add(Product.Capability.LED);
    List<Product> products = new ArrayList<>();
    products.add(Product.of("00-00-00-02", "Product 2","jq_image", capabilities));
    Map<String, Vendor> vendors = new HashMap<>();
    vendors.put("00-00-00-01", Vendor.of("00-00-00-01", "Vendor 1", products));
    DataProvider.create(vendors);
  }
  
  @Test
  public void testGetComponent() {
    // Act
    Component component = DataProvider.getDataProvider()
        .getGearComponent(1, 1, 2,/* version= */null,/* serialNumber= */null);
    // Assert
    assertEquals(component.componentId(), 1);
    assertEquals(component.vendor().id(), "00-00-00-01");
    assertEquals(component.product().id(), "00-00-00-02");
    assertEquals(component.gearCapabilities().size(), 2);
    assertEquals(component.gearCapabilities().get(0).name(), "GESTURE");
  }

  @Test
  public void testTagComponent() {
    // Act
    Component component = DataProvider.getDataProvider().getTagComponent(getDeviceInfo());
    // Assert
    assertEquals(component.componentId(), Component.TAG_ID);
    assertEquals(component.vendor().id(), VID);
    assertEquals(component.product().id(), PID);
    assertEquals(component.gearCapabilities().size(), 1);
    assertEquals(component.gearCapabilities().get(0).name(), "LED");
  }

  @Test
  public void testGetUnknownComponent() {
    // Act
    Component component = DataProvider.getDataProvider()
        .getGearComponent(1, 10, 20,/* version= */null,/* serialNumber= */null);
    // Assert
    assertNull(component);
  }

  private static DeviceInfo getDeviceInfo() {
    StringUtils stringUtils = StringUtils.getInstance();
    return DeviceInfo.ofTag(DeviceInfoResponse.newBuilder().setGearId("").setSkuId("")
        .setVendorId(stringUtils.hexStringToInteger(VID))
        .setProductId(stringUtils.hexStringToInteger(PID)).setUuid(UUID).setVendor("")
        .setRevision(0).setModel("").setMlVersion("").setBootloaderMajor(0).setBootloaderMinor(0)
        .setFirmwarePoint(0).setFirmwareMinor(0).setFirmwareMajor(0).build());
  }
}
