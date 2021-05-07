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

package com.google.android.jacquard.sample;

import android.app.Application;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.SdkConfig;

public class SampleApplication extends Application {

  private static final String TAG = SampleApplication.class.getSimpleName();
  private ResourceLocator resourceLocator;

  @Override
  public void onCreate() {
    super.onCreate();
    resourceLocator = new ResourceLocator(this);
    resourceLocator.getConnectivityManager()
        .init(SdkConfig.of(getPackageName(), BuildConfig.API_KEY));
    PrintLogger.d(TAG,
        "App created # " + getString(R.string.app_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
  }

  @Override
  public void onTerminate() {
    resourceLocator.getConnectivityManager().destroy();
    super.onTerminate();
  }

  public ResourceLocator getResourceLocator() {
    return resourceLocator;
  }
}
