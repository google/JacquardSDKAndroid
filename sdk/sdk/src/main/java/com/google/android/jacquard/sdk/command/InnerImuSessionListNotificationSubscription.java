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

package com.google.android.jacquard.sdk.command;

import androidx.annotation.Nullable;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionTrialListNotification;
import com.google.atap.jacquard.protocol.JacquardProtocol.Notification;
import com.google.protobuf.GeneratedMessageLite.GeneratedExtension;

/** Notification subscription class to parse the notification. */
class InnerImuSessionListNotificationSubscription
    extends InnerNotification<
        DataCollectionTrialListNotification, DataCollectionTrialListNotification> {

  @Override
  protected GeneratedExtension<Notification, DataCollectionTrialListNotification> getExtensionType() {
    return DataCollectionTrialListNotification.trialList;
  }

  @Nullable
  @Override
  protected DataCollectionTrialListNotification getNotification(
      DataCollectionTrialListNotification notification) {
    return notification;
  }
}
