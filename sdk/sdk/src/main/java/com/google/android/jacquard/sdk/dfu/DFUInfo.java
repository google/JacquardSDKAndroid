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
package com.google.android.jacquard.sdk.dfu;

import static com.google.android.jacquard.sdk.dfu.DFUInfo.UpgradeStatus.NOT_AVAILABLE;

import android.text.TextUtils;
import com.google.android.jacquard.sdk.model.Revision;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import java.net.URI;
import java.net.URISyntaxException;
import javax.annotation.Nullable;

/** Represents Image information. */
@AutoValue
public abstract class DFUInfo {

  public abstract Revision version();

  public abstract UpgradeStatus dfuStatus();

  abstract URI downloadUrl();

  public abstract String vendorId();

  public abstract String productId();

  @Nullable
  public abstract String moduleId();

  public static DFUInfo create(String version,
      String dfuStatus,
      URI downloadUrl,
      String vendorId,
      String productId,
      String moduleId) {

    return new AutoValue_DFUInfo(Revision.fromZeroString(version), getUpgradeStatus(dfuStatus),
        downloadUrl, vendorId,
        productId,
        moduleId);
  }

  /**
   * Create DFUInfo from remote response {@link RemoteDfuInfo}.
   *
   * @param remoteDfuInfo {@link RemoteDfuInfo}
   * @return <code>DFUInfo</code>
   */
  public static DFUInfo create(RemoteDfuInfo remoteDfuInfo) {
    try {
      return DFUInfo.create(remoteDfuInfo.getVersion(),
          remoteDfuInfo.getDfuStatus(),
          new URI(Strings.nullToEmpty(remoteDfuInfo.getDownloadUrl())),
          remoteDfuInfo.getVid(),
          remoteDfuInfo.getPid(),
          remoteDfuInfo.getMid());
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /** Returns true if vid/pid matches. */
  public boolean isApplicableTo(String vendorId, String productId) {
    return TextUtils.equals(vendorId, vendorId()) && TextUtils.equals(productId, productId());
  }

  private static UpgradeStatus getUpgradeStatus(String dfuStatus) {
    try {
      return Enum.valueOf(UpgradeStatus.class, dfuStatus.toUpperCase());
    } catch (IllegalArgumentException ex) {
      return NOT_AVAILABLE;
    }
  }

  /** Set of dfu upgrade status. */
  public enum UpgradeStatus {
    NOT_AVAILABLE,
    OPTIONAL,
    MANDATORY
  }

  public static TypeAdapter<DFUInfo> typeAdapter(Gson gson) {
    return new AutoValue_DFUInfo.GsonTypeAdapter(gson);
  }
}
