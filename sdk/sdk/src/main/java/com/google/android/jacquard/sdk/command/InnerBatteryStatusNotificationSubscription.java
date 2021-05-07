package com.google.android.jacquard.sdk.command;
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

import com.google.atap.jacquard.protocol.JacquardProtocol.BatteryStatusNotification;
import com.google.atap.jacquard.protocol.JacquardProtocol.Notification;
import com.google.protobuf.GeneratedMessageLite.GeneratedExtension;

/**
 * Use to subscribe to battery status notifications.
 * <p>
 *  The tag will send a notification when the battery status changes and will be emitted from
 * {@link com.google.android.jacquard.sdk.tag.ConnectedJacquardTag#subscribe(NotificationSubscription)}
 */
public class InnerBatteryStatusNotificationSubscription extends
  InnerNotification<BatteryStatus, BatteryStatusNotification> {

  @Override
  public GeneratedExtension<Notification, BatteryStatusNotification> getExtensionType() {
    return BatteryStatusNotification.batteryStatusNotification;
  }

  @Override
  public BatteryStatus getNotification(BatteryStatusNotification batteryStatusNotification) {
    return BatteryStatus.of(batteryStatusNotification);
  }
}
