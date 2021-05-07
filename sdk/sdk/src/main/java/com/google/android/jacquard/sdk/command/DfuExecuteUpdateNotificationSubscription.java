/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sdk.command;

import androidx.annotation.Nullable;
import com.google.atap.jacquard.protocol.JacquardProtocol.DFUExecuteUpdateNotification;
import com.google.atap.jacquard.protocol.JacquardProtocol.Notification;

/**
 * Use to subscribe for dfu execute update notifications from the gear.
 * <p>
 * When a dfuExecuteCommand executed for gear a {@link DFUExecuteUpdateNotification} will be emitted
 * from {@link com.google.android.jacquard.sdk.tag.ConnectedJacquardTag#subscribe(NotificationSubscription)}
 */
public class DfuExecuteUpdateNotificationSubscription implements
    NotificationSubscription<DFUExecuteUpdateNotification> {

  private final InnerDfuExecuteUpdateNotificationSubscription notificationSubscription =
      new InnerDfuExecuteUpdateNotificationSubscription();

  @Nullable
  @Override
  public DFUExecuteUpdateNotification extract(Notification notification) {
    return notificationSubscription.extract(notification);
  }
}
