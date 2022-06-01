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

import androidx.annotation.Nullable;
import com.google.protobuf.GeneratedMessageLite;
import com.google.protobuf.GeneratedMessageLite.GeneratedExtension;

/** Interface for subscribing to notifications from the tag. */
public abstract class InnerNotification<Type, Extension> {

  protected abstract <ProtoNotification extends GeneratedMessageLite.ExtendableMessage>
      GeneratedExtension<ProtoNotification, Extension> getExtensionType();

  /**
   * Extracts the payload from the notification. Returns null of the notification is not the
   * expected notification.
   */
  @Nullable
  public <ProtoNotification extends GeneratedMessageLite.ExtendableMessage> Type extract(
      @Nullable ProtoNotification notification) {
    if (notification == null) {
      return null;
    }
    GeneratedExtension<ProtoNotification, Extension> extensionType = getExtensionType();
    if (!notification.hasExtension(extensionType)) {
      return null;
    }
    Extension extension = (Extension) notification.getExtension(extensionType);
    return getNotification(extension);
  }

  @Nullable
  protected abstract Type getNotification(Extension extension);
}
