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

package com.google.android.jacquard.sdk.imu.model;

import com.google.android.jacquard.sdk.imu.Sensors;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionTrialList;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionTrialListNotification;
import com.google.auto.value.AutoValue;
import java.util.Objects;

/**
 * Model class for Imu Session info.
 */
@AutoValue
public abstract class ImuSessionInfo {

  public abstract String imuSessionId();

  public abstract String dcSessionId();

  public abstract String campaignId();

  public abstract String subjectId();

  public abstract Sensors sensor();

  public abstract String productId();

  public abstract int imuSize();

  public static ImuSessionInfo of(DataCollectionTrialListNotification notification) {
    DataCollectionTrialList trial = notification.getTrial();
    return builder().imuSessionId(trial.getTrialId()).dcSessionId(trial.getSessionId())
        .campaignId(trial.getCampaignId()).subjectId(trial.getTrialData(0).getSubjectId())
        .productId(trial.getProductId())
        .sensor(Sensors.forId(trial.getTrialData(0).getSensorData(0).getSensorId()))
        .imuSize(trial.getTrialData(0).getSensorData(0).getFsize()).build();
  }

  public static ImuSessionInfo of(String sessionId, int sessionSize) {
    return builder().imuSessionId(sessionId).dcSessionId("jqSession")
        .campaignId("jqCampaign").subjectId("jqSubject")
        .productId("jqProduct")
        .sensor(Sensors.IMU)
        .imuSize(sessionSize).build();
  }

  public static Builder builder() {
    return new AutoValue_ImuSessionInfo.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder imuSessionId(String imusessionId);

    public abstract Builder dcSessionId(String dcSessionId);

    public abstract Builder campaignId(String campaignId);

    public abstract Builder subjectId(String subjectId);

    public abstract Builder sensor(Sensors sensor);

    public abstract Builder productId(String productId);

    public abstract Builder imuSize(int size);

    public abstract ImuSessionInfo build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ImuSessionInfo that = (ImuSessionInfo) o;
    return imuSessionId().equals(that.imuSessionId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(imuSessionId());
  }

  @Override
  public String toString() {
    return
        imuSessionId() +
            ", file size: " + imuSize() +
            " bytes. }";
  }
}
