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

import com.google.android.jacquard.sdk.model.Gesture;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataChannelNotification;
import com.google.atap.jacquard.protocol.JacquardProtocol.Notification;
import com.google.protobuf.GeneratedMessageLite.GeneratedExtension;

/**
 * Use to subscribe to gesture notifications from the tag.
 * <p>
 *  When a gesture is performed on the gear a {@link Gesture} will be emitted from
 * {@link com.google.android.jacquard.sdk.tag.ConnectedJacquardTag#subscribe(NotificationSubscription)}
 */
public class InnerGestureNotificationSubscription extends InnerNotification<Gesture,
  DataChannelNotification> {

  @Override
  public GeneratedExtension<Notification,DataChannelNotification> getExtensionType() {
    return DataChannelNotification.data;
  }

  @Override
  public Gesture getNotification(DataChannelNotification dataChannelNotification) {
    if (!dataChannelNotification.hasInferenceData()) {
      return null;
    }
    return Gesture.of(dataChannelNotification.getInferenceData());
  }
}
