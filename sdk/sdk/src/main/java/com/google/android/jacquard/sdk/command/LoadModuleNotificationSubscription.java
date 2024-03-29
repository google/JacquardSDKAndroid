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
import com.google.android.jacquard.sdk.model.Module;
import com.google.android.jacquard.sdk.util.JQUtils;

/** This subscription class gives you a ability to get subscribed to load module notifications. */
public class LoadModuleNotificationSubscription
    implements NotificationSubscription<Module> {

  InnerLoadModuleNotificationSubscription subscription = new InnerLoadModuleNotificationSubscription();

  @Nullable
  @Override
  public Module extract(byte[] packet) {
    return subscription.extract(JQUtils.getNotification(packet));
  }
}
