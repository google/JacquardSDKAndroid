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

package com.google.android.jacquard.sample.imu.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.android.jacquard.sdk.imu.Sensors;
import com.google.android.jacquard.sdk.imu.model.ImuSessionInfo;
import java.util.Objects;

@Entity(tableName = "imu_sessions")
public class JqSessionInfo {

  @PrimaryKey
  @NonNull
  public String imuSessionId;

  public String dcSessionId;

  public String campaignId;

  public String subjectId;

  public int sensor;

  public String productId;

  public int imuSize;

  @NonNull
  public String tagSerialNumber;

  @Nullable
  public String path;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JqSessionInfo that = (JqSessionInfo) o;
    return imuSessionId.equals(that.imuSessionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(imuSessionId);
  }

  @Override
  public String toString() {
    return "JqSessionInfo{" +
        "imuSessionId='" + imuSessionId + '\'' +
        ", dcSessionId='" + dcSessionId + '\'' +
        ", campaignId='" + campaignId + '\'' +
        ", subjectId='" + subjectId + '\'' +
        ", sensor=" + sensor +
        ", productId='" + productId + '\'' +
        ", imuSize=" + imuSize +
        ", tagSerialNumber='" + tagSerialNumber + '\'' +
        ", path='" + path + '\'' +
        '}';
  }

  public static JqSessionInfo map(ImuSessionInfo info, String tagSerialNumber) {
    JqSessionInfo sessionInfo = new JqSessionInfo();
    sessionInfo.campaignId = info.campaignId();
    sessionInfo.dcSessionId = info.dcSessionId();
    sessionInfo.imuSessionId = info.imuSessionId();
    sessionInfo.productId = info.productId();
    sessionInfo.subjectId = info.subjectId();
    sessionInfo.imuSize = info.imuSize();
    sessionInfo.sensor = info.sensor().id();
    sessionInfo.tagSerialNumber = tagSerialNumber;
    return sessionInfo;
  }

  public static ImuSessionInfo map(JqSessionInfo info) {
    return ImuSessionInfo.builder()
        .productId(info.productId)
        .subjectId(info.subjectId)
        .campaignId(info.campaignId)
        .imuSessionId(info.imuSessionId)
        .dcSessionId(info.dcSessionId)
        .imuSize(info.imuSize)
        .sensor(Sensors.forId(info.sensor))
        .build();
  }
}
