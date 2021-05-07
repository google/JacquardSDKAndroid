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

import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionTrialData;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionTrialList;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionTrialListNotification;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionTrialSensorData;

/**
 * Dummy {@link DataCollectionTrialListNotification} class.
 */
public class FakeDCTrialListNotification {

  /**
   * Dummy {@link DataCollectionTrialList}
   */
  public static DataCollectionTrialList getDCTrialList() {
    return DataCollectionTrialList.newBuilder()
        .setProductId("product")
        .setCampaignId("campaign")
        .setSessionId("session")
        .setTrialId("1627344030")
        .addTrialData(DataCollectionTrialData.newBuilder().addSensorData(
            DataCollectionTrialSensorData.newBuilder().setFsize(2900)
                .setSensorId(0).setCrc16(111))
            .setSubjectId("subject").build())
        .build();
  }

  /**
   * Dummy {@link DataCollectionTrialListNotification}.
   */
  public static DataCollectionTrialListNotification getDCTrialListNotification() {
    return DataCollectionTrialListNotification.newBuilder()
        .setTrialIndex(0)
        .setTotalTrials(1)
        .setTrial(getDCTrialList())
        .build();
  }
}
