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
package com.google.android.jacquard.sdk;

import android.Manifest.permission;
import android.content.Context;
import android.content.IntentSender;
import androidx.annotation.RequiresPermission;
import com.google.android.jacquard.sdk.connection.ConnectionState;
import com.google.android.jacquard.sdk.model.SdkConfig;
import com.google.android.jacquard.sdk.rx.Fn;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.tag.AdvertisedJacquardTag;

/**
 * Interface for scanning for and connecting to Jacquard Tags. <br/> DO NOT forget to call {@link
 * JacquardManager#init(SdkConfig)} before calling any apis.
 */
public interface JacquardManager {

  /** Returns the application context */
  Context getApplicationContext();

  /**
   * Starts scanning for Jacquard Tags.
   * When a tag is found it is emitted on the Main Thread.
   * @return a {@link Signal} with found jacquard tags as a {@link AdvertisedJacquardTag}.
   */
  @RequiresPermission(permission.BLUETOOTH)
  Signal<AdvertisedJacquardTag> startScanning();

  /**
   * Connects to the provided address.
   *
   * @param address bluetooth address to connect to.
   * @return a {@link Signal} emitting the {@link ConnectionState}.
   */
  @RequiresPermission(permission.BLUETOOTH)
  Signal<ConnectionState> connect(Context activityContext, String address,
      Fn<IntentSender, Signal<Boolean>> senderHandler);

  /**
   * Releases all allocation resources.
   */
  void destroy();

  /**
   * Releases all observables held for the tag.
   *
   * @param address mac address of the tag.
   */
  void forget(String address);

  /**
   * Returns a shared instance of JacquardManager.
   */
  static JacquardManager getInstance() {
    return JacquardManagerImpl.instance;
  }

  /**
   * Initialized the sdk with provided SdkConfig object.
   *
   * @param config a {@link SdkConfig} to initialize the sdk.
   */
  void init(SdkConfig config);

  Signal<ConnectionState> getConnectionStateSignal(String address);

  MemoryCache getMemoryCache();

  SdkConfig getSdkConfig();
}
