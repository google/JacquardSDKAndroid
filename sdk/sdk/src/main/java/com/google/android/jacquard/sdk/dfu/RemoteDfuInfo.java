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

import androidx.annotation.VisibleForTesting;
import com.google.gson.annotations.SerializedName;

/** Response class for the Retrofit. */
final class RemoteDfuInfo {

  @SerializedName("vid")
  private String vid;
  @SerializedName("pid")
  private String pid;
  @SerializedName("mid")
  private String mid;
  @SerializedName("version")
  private String version;
  @SerializedName("dfuStatus")
  private String dfuStatus;
  @SerializedName("downloadUrl")
  private String downloadUrl;

  public String getVid() {
    return vid;
  }

  public String getPid() {
    return pid;
  }

  public String getMid() {
    return mid;
  }

  public String getVersion() {
    return version;
  }

  public String getDfuStatus() {
    return dfuStatus;
  }

  public String getDownloadUrl() {
    return downloadUrl;
  }

  @VisibleForTesting
  static RemoteDfuInfo create(String version,
      String dfuStatus,
      String downloadUrl,
      String vendorId,
      String productId,
      String moduleId) {
    RemoteDfuInfo remoteDfuInfo = new RemoteDfuInfo();
    remoteDfuInfo.vid = vendorId;
    remoteDfuInfo.pid = productId;
    remoteDfuInfo.mid = moduleId;
    remoteDfuInfo.version = version;
    remoteDfuInfo.dfuStatus = dfuStatus;
    remoteDfuInfo.downloadUrl = downloadUrl;
    return remoteDfuInfo;
  }
}
