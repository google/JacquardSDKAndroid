/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.jacquard.sdk.remote;

import android.content.res.Resources;
import com.google.android.jacquard.sdk.datastore.ComponentMetaData;
import com.google.android.jacquard.sdk.model.Revision;
import com.google.android.jacquard.sdk.rx.Signal;
import java.util.List;

/**
 * Fake implementation of {@link LocalRemoteFunctionImpl}.
 */
public class FakeLocalRemoteFunction extends LocalRemoteFunctionImpl {

  /**
   * Creates a new instance of LocalRemoteFunctionImpl
   *
   * @param resources an Android Resources used to get access the resources folder.
   */
  public FakeLocalRemoteFunction(Resources resources) {
    super(resources);
  }

  @Override
  public Signal<ComponentMetaData> getComponents() {
    return getComponentMetaData(getClass().getClassLoader().getResourceAsStream("metadata.json"));
  }

  @Override
  public Signal<List<Revision>> getBadFirmwareVersions() {
    return getBadFirmwareVersions(
        getClass().getClassLoader().getResourceAsStream("badfirmwareversion.json"));
  }
}
