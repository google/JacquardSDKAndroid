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

import androidx.annotation.Nullable;
import com.google.android.jacquard.sdk.datastore.DataProvider;
import com.google.android.jacquard.sdk.model.GearState;
import com.google.atap.jacquard.protocol.JacquardProtocol.AttachedNotification;
import com.google.atap.jacquard.protocol.JacquardProtocol.Notification;
import com.google.protobuf.GeneratedMessageLite.GeneratedExtension;

/**
 * Use to subscribe to gear attach/detach notification.
 *
 * <p>When the gear is attached of detached a {@link GearState} will be emitted from {@link
 * com.google.android.jacquard.sdk.tag.ConnectedJacquardTag#subscribe(NotificationSubscription)}
 */
class InnerAttachedNotificationSubscription
    extends InnerNotification<GearState, AttachedNotification> {

  private final DataProvider dataProvider;

  public InnerAttachedNotificationSubscription() {
    dataProvider = DataProvider.getDataProvider();
  }

  @Override
  GeneratedExtension<Notification, AttachedNotification> getExtensionType() {
    return AttachedNotification.attached;
  }

  @Nullable
  @Override
  GearState getNotification(AttachedNotification notification) {

    if (!notification.getAttachState()) {
      return GearState.ofDetached();
    }

    return GearState.ofAttached(dataProvider
        .getGearComponent(notification.getComponentId(), notification.getVendorId(),
            notification.getProductId(), /* version= */ null,/* serialNumber= */ null));
  }
}
