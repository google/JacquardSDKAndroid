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
package com.google.android.jacquard.sdk.remote;

import android.content.res.Resources;
import com.google.android.jacquard.sdk.R;
import com.google.android.jacquard.sdk.datastore.ComponentMetaData;
import com.google.android.jacquard.sdk.model.Revision;
import com.google.android.jacquard.sdk.model.Vendor;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.util.SdkTypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Responsible for loading MetaData from local Json file. */
class LocalRemoteFunctionImpl implements RemoteFunction {

  private final LocalParser<String, InputStream> localParser;
  private final Resources resources;

  public LocalRemoteFunctionImpl(Resources resources) {
    this.localParser = new LocalJsonParserImpl();
    this.resources = resources;
  }

  @Override
  public Signal<ComponentMetaData> getComponents() {
    return getComponentMetaData(resources.openRawResource(R.raw.metadata));
  }

  @Override
  public Signal<List<Revision>> getBadFirmwareVersions() {
    return getBadFirmwareVersions(resources.openRawResource(R.raw.badfirmwareversion));
  }

  Signal<List<Revision>> getBadFirmwareVersions(InputStream inputStream) {
    return Signal.just(
        SdkTypeAdapterFactory.gson()
            .fromJson(
                localParser.parse(inputStream), new TypeToken<List<Revision>>() {}.getType()));
  }

  /**
   * Returns the ComponentMetaData signal
   *
   * @param inputStream - InputStream of metadata json file to create ComponentMetaData object.
   */
  Signal<ComponentMetaData> getComponentMetaData(InputStream inputStream) {
    String jsonString = localParser.parse(inputStream);
    MetaDataModel model = SdkTypeAdapterFactory.runtimeGsonTypeAdapterFactory()
        .fromJson(jsonString, MetaDataModel.class);
    Map<String, Vendor> vendorMap = getVendorsMap(model);
    return Signal.just(ComponentMetaData.of(vendorMap));
  }

  private Map<String, Vendor> getVendorsMap(MetaDataModel model) {
    List<Vendor> listVendor = model.vendors();
    Map<String, Vendor> map = new HashMap<>();
    for (Vendor vendor : listVendor) {
      map.put(vendor.id(), vendor);
    }
    return map;
  }
}
