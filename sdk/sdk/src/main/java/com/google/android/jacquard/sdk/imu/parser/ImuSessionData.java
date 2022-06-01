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

package com.google.android.jacquard.sdk.imu.parser;

import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionActionHeader;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionMetadata;
import com.google.atap.jacquard.protocol.JacquardProtocol.ImuConfiguration;
import com.google.protos.atap.jacquard.core.Jacquard.ImuSample;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

/**
 * Model class for parsed IMU raw sample binary file.
 */
public class ImuSessionData {

  /**
   * Model class for ImuSample collection.
   */
  public static class ImuSampleCollection {

    private DataCollectionActionHeader actionHeader;
    private final List<ImuSample> imuSamples = new ArrayList<>();

    /**
     * Returns Immutable list of {@link ImuSample} of this collection.
     */
    public List<ImuSample> getImuSamples() {
      return ImmutableList.copyOf(imuSamples);
    }

    /**
     * Returns action id.
     */
    public int getActionId() {
      return actionHeader.getActionId();
    }

    public boolean isError() {
      return actionHeader.getIsError();
    }
  }

  private DataCollectionMetadata metadata;
  private ImuConfiguration imuConfig;
  private ImuSampleCollection currentImuSample;
  private final List<ImuSampleCollection> imuSampleCollections = new ArrayList<>();

  /**
   * Returns immutable list of {@link ImuSampleCollection}.
   */
  public List<ImuSampleCollection> getImuSampleCollections() {
    return ImmutableList.copyOf(imuSampleCollections);
  }

  /**
   * Returns {@link DataCollectionMetadata} associated.
   */
  public DataCollectionMetadata getDataCollectionMetadata() {
    return metadata;
  }

  /**
   * Returns Imu Session id.
   */
  public String getImuSessionId() {
    return metadata.getTrialId();
  }

  /**
   * Returns {@link ImuConfiguration} used for this IMU Session.
   */
  public ImuConfiguration getImuConfig() {
    return imuConfig;
  }

  void setMetadata(DataCollectionMetadata metadata) {
    this.metadata = metadata;
  }

  void setActionHeader(DataCollectionActionHeader actionHeader) {
    currentImuSample = new ImuSampleCollection();
    currentImuSample.actionHeader = actionHeader;
    imuSampleCollections.add(currentImuSample);
  }

  void updateActionHeaderResult(boolean isError) {
    if (currentImuSample != null && currentImuSample.actionHeader != null) {
      DataCollectionActionHeader updatedHeader = DataCollectionActionHeader
          .newBuilder(currentImuSample.actionHeader)
          .setIsError(isError).build();
      currentImuSample.actionHeader = updatedHeader;
    }
  }

  void addSample(ImuSample sample) {
    if (currentImuSample != null) {
      currentImuSample.imuSamples.add(sample);
    }
  }

  void setImuConfig(ImuConfiguration imuConfig) {
    this.imuConfig = imuConfig;
  }
}
