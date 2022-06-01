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

package com.google.android.jacquard.sdk.model;

import com.google.android.jacquard.sdk.util.StringUtils;
import com.google.atap.jacquard.protocol.JacquardProtocol.ModuleDescriptor;

/**
 * Fake {@link com.google.android.jacquard.sdk.imu.ImuModule}
 */
public class FakeImuModule {

  private static final String IMU_VENDOR_ID = "11-78-30-c8";
  private static final String IMU_PRODUCT_ID = "ef-3e-5b-88";
  private static final String IMU_MODULE_ID = "3d-0b-e7-53";
  private static StringUtils stringUtils = StringUtils.getInstance();

  public static ModuleDescriptor getModuleDescriptor(boolean isActive) {
    return ModuleDescriptor.newBuilder().setVendorId(stringUtils.hexStringToInteger(IMU_VENDOR_ID))
        .setProductId(stringUtils.hexStringToInteger(IMU_PRODUCT_ID))
        .setModuleId(stringUtils.hexStringToInteger(IMU_MODULE_ID))
        .setIsEnabled(isActive)
        .setName("IMU Loadable Module")
        .build();
  }

  public static Module getImuModule() {
    return Module.create(
        stringUtils.hexStringToInteger(IMU_VENDOR_ID),
        stringUtils.hexStringToInteger(IMU_PRODUCT_ID),
        stringUtils.hexStringToInteger(IMU_MODULE_ID));
  }
}
