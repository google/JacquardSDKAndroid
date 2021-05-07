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

import static org.junit.Assert.assertEquals;

import android.os.Build.VERSION_CODES;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.model.Product;
import com.google.android.jacquard.sdk.model.Vendor;
import com.google.android.jacquard.sdk.util.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link DataProvider}
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public final class DataProviderTest {
  @Test
  public void testGetComponent() {
    // Assign
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    List<Product.Capability> capabilities = new ArrayList<>();
    capabilities.add(Product.Capability.GESTURE);
    capabilities.add(Product.Capability.LED);
    List<Product> products = new ArrayList<>();
    products.add(Product.of("2", "Product 2","jq_image", capabilities));
    Map<String, Vendor> vendors = new HashMap<>();
    vendors.put("1", Vendor.of("1", "Vendor 1", products));
    DataProvider.create(vendors, StringUtils.getInstance());
    // Act
    Component component = DataProvider.getDataProvider()
        .getComponent(1, 1, 2,/* version= */null,/* serialNumber= */null);
    // Assert
    assertEquals(component.componentId(), 1);
    assertEquals(component.vendor().id(), "1");
    assertEquals(component.product().id(), "2");
    assertEquals(component.gearCapabilities().size(), 2);
    assertEquals(component.gearCapabilities().get(0).name(), "GESTURE");
  }
}
