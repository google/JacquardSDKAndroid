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

package com.google.android.jacquard.sdk.command;

import androidx.annotation.Nullable;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.model.Product;
import com.google.android.jacquard.sdk.model.Product.Capability;
import com.google.android.jacquard.sdk.model.Revision;
import com.google.android.jacquard.sdk.model.Vendor;
import com.google.atap.jacquard.protocol.JacquardProtocol.DeviceInfoResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Fake implementation for {@link Component} class.
 */
public class FakeComponent extends Component {

  public final static String VID = "74-a8-ce-54";
  public final static String PID = "8a-66-50-f4";
  public static final String UUID = "0-07-9A16FLHBN005JM-1910";
  public static final String SERIAL_NUMBER = "9A16FLHBN005JM";
  public static final String GEAR_SERIAL_NUMBER = "Gear_9A16FLHBN005JM";

  @Override
  public int componentId() {
    return 0;
  }

  @Override
  public Vendor vendor() {
    return null;
  }

  @Override
  public Product product() {
    return null;
  }


  @Override
  public List<Capability> gearCapabilities() {
    return null;
  }

  @Nullable
  @Override
  public Revision version() {
    return null;
  }

  @Nullable
  @Override
  public String serialNumber() {
    return null;
  }

  /**
   * Returns Tag component.
   */
  public static Component getTagComponent() {
    List<Capability> capabilities = new ArrayList<>();
    capabilities.add(Capability.LED);
    Product product = Product.of(PID, "Product 1", "jq_image",
        capabilities);
    List<Product> products = new ArrayList<>();
    products.add(product);
    Vendor vendor = Vendor.of(VID, "Vendor 1", products);
    return Component
        .of(Component.TAG_ID, vendor, product, product.capabilities(), Revision.create(0, 0, 0),
            SERIAL_NUMBER);
  }

  /**
   * Returns Tag component.
   */
  public static Component getGearComponent(int componentId) {
    List<Capability> capabilities = new ArrayList<>();
    capabilities.add(Capability.LED);
    Product product = Product.of(PID, "Product 1", "jq_image", capabilities);
    List<Product> products = new ArrayList<>();
    products.add(product);
    Vendor vendor = Vendor.of(VID, "Vendor 1", products);
    return Component
        .of(componentId, vendor, product, product.capabilities(),/* revision= */
            Revision.create(0, 0, 0), /* serialNumber= */ GEAR_SERIAL_NUMBER);
  }
}
