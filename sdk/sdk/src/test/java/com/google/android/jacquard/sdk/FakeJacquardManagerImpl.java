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

package com.google.android.jacquard.sdk;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import com.google.android.jacquard.sdk.connection.ConnectionState;
import com.google.android.jacquard.sdk.datastore.DataProvider;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.Product;
import com.google.android.jacquard.sdk.model.Product.Capability;
import com.google.android.jacquard.sdk.model.SdkConfig;
import com.google.android.jacquard.sdk.model.Vendor;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.tag.JacquardTagFactory;
import com.google.android.jacquard.sdk.util.Lists;
import com.google.android.jacquard.sdk.util.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fake implementation for {@link JacquardManager}
 */
public final class FakeJacquardManagerImpl extends JacquardManagerImpl {

  private final static String CLIENT_ID = "aa-aa-aa-aa";
  private final static String API_KEY = "api key";
  private final static String VENDOR_ID = "74-a8-ce-54";
  private final static String PRODUCT_ID = "8a-66-50-f4";
  private boolean isModulePresent, isModuleActive;

  private ConnectionState state;
  private boolean sendConnectedTwiceForExecute;

  /**
   * Constructs a new JacquardManagerImpl.
   *
   * @param context context providing access to the {@link BluetoothAdapter}.
   */
  public FakeJacquardManagerImpl(Context context) {
    this(context, true, false);
  }

  /**
   * Constructs a new JacquardManagerImpl.
   *
   * @param context         context providing access to the {@link BluetoothAdapter}.
   * @param isModulePresent If Loadable module is present
   */
  public FakeJacquardManagerImpl(Context context, boolean isModulePresent, boolean isModuleActive) {
    super(context);
    init(context);
    this.isModulePresent = isModulePresent;
    this.isModuleActive = isModuleActive;
  }

  @Override
  public Signal<ConnectionState> getConnectionStateSignal(String address) {
    if (state == null) {
      return Signal
          .from(ConnectionState
              .ofConnected(JacquardTagFactory.createConnectedJacquardTag(isModulePresent, isModuleActive)));
    } else {
      return sendConnectedTwiceForExecute ? Signal.from(Lists.unmodifiableListOf(state, state))
          : Signal.from(state);
    }
  }

  public void setState(ConnectionState state) {
    this.state = state;
  }

  public void setSendConnectedTwiceForExecute(boolean sendConnectedTwiceForExecute) {
    this.sendConnectedTwiceForExecute = sendConnectedTwiceForExecute;
  }

  private void init(Context context) {
    PrintLogger.initialize(context);
    DataProvider.create(getVendors());
    JacquardManagerInitialization.initJacquardManager();
    JacquardManager.getInstance()
        .init(SdkConfig.of(CLIENT_ID, API_KEY, /* cloudEndpointUrl= */ null));
  }

  private Map<String, Vendor> getVendors() {
    List<Capability> capabilities = new ArrayList<>();
    capabilities.add(Product.Capability.GESTURE);
    capabilities.add(Product.Capability.LED);
    List<Product> products = new ArrayList<>();
    products.add(Product.of(PRODUCT_ID, "Product", "jq_image", capabilities));
    Map<String, Vendor> vendors = new HashMap<>();
    vendors.put(VENDOR_ID, Vendor.of(VENDOR_ID, "Vendor", products));
    return vendors;
  }
}
