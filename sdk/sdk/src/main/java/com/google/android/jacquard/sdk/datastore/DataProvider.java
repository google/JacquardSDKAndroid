package com.google.android.jacquard.sdk.datastore;
/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.google.android.jacquard.sdk.command.DeviceInfo;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.model.Product;
import com.google.android.jacquard.sdk.model.Revision;
import com.google.android.jacquard.sdk.model.Vendor;
import com.google.android.jacquard.sdk.util.StringUtils;
import java.util.Map;

/**
 * Stores and provides all meta data from cloud json
 */
public final class DataProvider {
  private static final String TAG = DataProvider.class.getSimpleName();
  private static DataProvider dataProvider;
  private static StringUtils stringUtils;

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
   * @param stringUtilsInstance stringUtils {@link StringUtils} class instance.
   */
  public static void create(Map<String, Vendor> vendors, StringUtils stringUtilsInstance) {
    dataProvider = new DataProvider(vendors);
    stringUtils = stringUtilsInstance;
  }

  /**
   * Returns the Component object correspond to vendorId, productId.
   *
   * @param componentId int component id.
   * @param vendorId int vendor id.
   * @param productId int product id.
   * @param version Revision object.
   * @param serialNumber String device serial number.
   * @return Corresponding Component object.
   */
  public Component getComponent(int componentId, int vendorId, int productId, Revision version,
      String serialNumber) {
    Vendor vendor = getVendor(stringUtils.integerToHexString(vendorId));
    PrintLogger.d(TAG,
        "Vendor Id int >> " + vendorId + " hex >> " + stringUtils.integerToHexString(vendorId));
    Product product = getProduct(stringUtils.integerToHexString(productId), vendor);
    PrintLogger.d(TAG,
        "Product Id int >> " + productId + " hex >> " + stringUtils.integerToHexString(productId));
    return Component
        .of(componentId, vendor, product, product.capabilities(), version, serialNumber);
  }

  /**
   * Returns the Component object correspond to componentId, deviceInfo.
   *
   * @param componentId int component id.
   * @param deviceInfo DeviceInfo object.
   * @return Corresponding Component object.
   */
  public Component getComponent(int componentId, DeviceInfo deviceInfo) {
    return getComponent(componentId, deviceInfo.vendorId(), deviceInfo.productId(),
        deviceInfo.version(), deviceInfo.serialNumber());
  }

  private DataProvider(Map<String, Vendor> vendors) {
    this.vendors = vendors;
  }

  private final Map<String, Vendor> vendors;

  private Vendor getVendor(String vendorId) {
    return vendors.containsKey(vendorId) ?
        vendors.get(vendorId) :
        vendors.get(0);
  }

  private Product getProduct(String productId, Vendor vendor) {
    for (Product product : vendor.products()) {
      if (productId.equals(product.id())) {
        return product;
      }
    }
    return vendor.products().get(0);
  }
}
