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

package com.google.android.jacquard.sdk.util;

import android.util.Patterns;
import android.webkit.URLUtil;
import androidx.annotation.NonNull;
import com.google.android.jacquard.sdk.JqExtensionRegistry;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.atap.jacquard.protocol.JacquardProtocol.Notification;
import java.net.MalformedURLException;
import java.net.URL;

/** Class for utility apis. */
public class JQUtils {

  private static final String TAG = JQUtils.class.getSimpleName();

  /**
   * Returns true if input strig is valid url. False otherwise.
   *
   * @param endpointUrl
   */
  public static boolean isValidUrl(@NonNull String endpointUrl) {
    try {
      URL url = new URL(endpointUrl);
      return URLUtil.isValidUrl(endpointUrl) && Patterns.WEB_URL.matcher(endpointUrl).matches();
    } catch (MalformedURLException ignored) {
      // Ignore
    }
    return false;
  }

  /** Returns Notification object of provided byte data. */
  public static Notification getNotification(byte[] packet) {
    try {
      Notification notification = Notification.parseFrom(packet, JqExtensionRegistry.instance);
      PrintLogger.d(
          TAG,
          "Notification Received # Domain # "
              + notification.getDomain()
              + " # OpCode # "
              + notification.getOpcode());
      PrintLogger.d(TAG, "Notification Received # " + notification);
      return notification;
    } catch (Exception e) {
      PrintLogger.e(TAG, "Failed to deserialize notification", e);
      return null;
    }
  }
}
