package com.google.android.jacquard.sdk.datastore;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.jacquard.sdk.command.DeviceInfo;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.model.Product;
import com.google.android.jacquard.sdk.model.Product.Capability;
import com.google.android.jacquard.sdk.model.Revision;
import com.google.android.jacquard.sdk.model.Vendor;
import com.google.android.jacquard.sdk.util.StringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Stores and provides all meta data from cloud json
 */
public final class DataProvider {
  private static DataProvider dataProvider;
  private final StringUtils stringUtils = StringUtils.getInstance();
  private final Map<String, Vendor> vendors;

  /**
   * Returns existing instance of {@link DataProvider}.<br/> Do not forget to call {@link
   * DataProvider#create(Map)} before calling this api.
   *
   * @return {@link DataProvider}
   */
  public static DataProvider getDataProvider() {
    if (dataProvider == null) {
      throw new IllegalStateException("Store is not created. Please call create() first");
    }
    return dataProvider;
  }

  /**
   * Creates an Object of {@link DataProvider} with all the data.
   *
   * @param vendors All the vendors Mapped with its unique id.
   */
  public static void create(Map<String, Vendor> vendors) {
    dataProvider = new DataProvider(vendors);
  }

  /**
   * Returns the gear Component object correspond to vendorId, productId.
   *
   * @param componentId  int component id.
   * @param vendorId     int vendor id.
   * @param productId    int product id.
   * @param version      Revision object.
   * @param serialNumber String device serial number.
   * @return Corresponding {@link Component} or null if no matching component is found.
   */
  @Nullable
  public Component getGearComponent(int componentId, int vendorId, int productId, Revision version,
      String serialNumber) {
    Vendor vendor = getVendor(stringUtils.integerToHexString(vendorId));
    if (vendor == null) {
      return null;
    }
    Product product = getProduct(stringUtils.integerToHexString(productId), vendor);
    if (product == null) {
      return null;
    }
    return Component
        .of(componentId, vendor, product, product.capabilities(), version, serialNumber);
  }

  /**
   * Returns the tag Component object.
   *
   * @param deviceInfo DeviceInfo object.
   */
  public Component getTagComponent(@NonNull DeviceInfo deviceInfo) {
    List<Capability> capabilities = new ArrayList<>();
    capabilities.add(Capability.LED);
    Product product = Product
        .of(stringUtils.integerToHexString(deviceInfo.productId()), "Jacquard Tag", "JQTag",
            capabilities);
    List<Product> products = new ArrayList<>();
    products.add(product);
    Vendor vendor = Vendor
        .of(stringUtils.integerToHexString(deviceInfo.vendorId()), deviceInfo.vendor(), products);
    return Component.of(Component.TAG_ID, vendor, product, capabilities, deviceInfo.version(),
        deviceInfo.serialNumber());
  }

  private DataProvider(Map<String, Vendor> vendors) {
    this.vendors = vendors;
  }

  @Nullable
  private Vendor getVendor(String vendorId) {
    return vendors.get(vendorId);
  }

  @Nullable
  private Product getProduct(String productId, Vendor vendor) {
    for (Product product : vendor.products()) {
      if (productId.equals(product.id())) {
        return product;
      }
    }
    return null;
  }
}
