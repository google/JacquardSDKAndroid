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

import android.text.TextUtils;
import androidx.annotation.Nullable;
import com.google.android.jacquard.sdk.util.StringUtils;
import com.google.atap.jacquard.protocol.JacquardProtocol.ModuleDescriptor;
import com.google.auto.value.AutoValue;

/**
 * Model class for loadable module.
 */
@AutoValue
public abstract class Module {

  @Nullable
  public abstract String name();

  public abstract String vendorId();

  public abstract String productId();

  public abstract String moduleId();

  @Nullable
  public abstract Revision version();

  public abstract boolean isEnabled();

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof Module)) {
      return false;
    }
    Module other = (Module) obj;
    return TextUtils.equals(vendorId(), other.vendorId()) &&
        TextUtils.equals(productId(), other.productId()) &&
        TextUtils.equals(moduleId(), other.moduleId());
  }

  public static Module create(int vid, int pid, int mid) {
    return create(null, vid, pid, mid, Revision.create(0, 0, 0), false);
  }

  public static Module create(ModuleDescriptor descriptor) {
    return create(descriptor.getName(), descriptor.getVendorId(), descriptor.getProductId(),
        descriptor.getModuleId(),
        Revision.create(descriptor.getVerMajor(), descriptor.getVerMinor(),
            descriptor.getVerPoint()), descriptor.getIsEnabled());
  }

  private static Module create(String name, int vid, int pid, int mid,
      Revision version, boolean isEnabled) {
    StringUtils utils = StringUtils.getInstance();
    return new AutoValue_Module(name, utils.integerToHexString(vid),
        utils.integerToHexString(pid), utils.integerToHexString(mid), version, isEnabled);
  }
}
