package com.google.android.jacquard.sdk.command;
/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.google.android.jacquard.sdk.model.TouchData;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataChannelNotification;
import com.google.atap.jacquard.protocol.JacquardProtocol.Notification;
import com.google.protobuf.GeneratedMessageLite.GeneratedExtension;

/**
 * Use to subscribe to continuous touch data from the tag.
 *
 * <p>When a the gear is touch {@link TouchData} will be emitted from {@link
 * com.google.android.jacquard.sdk.tag.ConnectedJacquardTag#subscribe(NotificationSubscription)}
 */
public class InnerContinuousTouchNotificationSubscription
    extends InnerNotification<TouchData, DataChannelNotification> {

  @Override
  protected GeneratedExtension<Notification, DataChannelNotification> getExtensionType() {
    return DataChannelNotification.data;
  }

  @Override
  protected TouchData getNotification(DataChannelNotification dataChannelNotification) {
    return TouchData.of(dataChannelNotification.getTouchData());
  }
}
