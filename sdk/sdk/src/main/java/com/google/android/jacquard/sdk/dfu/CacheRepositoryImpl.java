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
package com.google.android.jacquard.sdk.dfu;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Pair;
import com.google.android.jacquard.sdk.dfu.model.FileDescriptor;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.util.SdkTypeAdapterFactory;
import com.google.common.base.Optional;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Calendar;

/** Implementation of {@link CacheRepository}. */
final class CacheRepositoryImpl implements CacheRepository{

  private static final String TAG = CacheRepositoryImpl.class.getSimpleName();
  private static final String DIRECTORY_NAME = "DfuImages";
  private static final String PREF_NAME = "JacquardPublicSdk";

  private final SharedPreferences sharedPreferences;
  private final String cacheParentDirectoryPath;

  CacheRepositoryImpl(Context context) {
    sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    cacheParentDirectoryPath = context.getFilesDir() + File.separator + DIRECTORY_NAME;
  }

  /**
   * Checks if descriptor exists in internal memory for specified {@link DFUInfo}.
   *
   * @param dfuInfo {@link DFUInfo} to check Firmware file.
   * @return <code>true</code> is file exists.
   */
  @Override
  public boolean hasDescriptor(DFUInfo dfuInfo) {
    return new File(cacheParentDirectoryPath, descriptorFileName(dfuInfo)).exists();
  }

  /**
   * Stores the Firmware Image to the internal memory.
   *
   * @param dfuInfo {@link DFUInfo} to create unique file name.
   * @param descriptor {@link FileDescriptor} to read InputStream to store.
   * @return <code>true</code> if file written successfully.
   */
  @Override
  public Signal<Boolean> cacheDescriptor(DFUInfo dfuInfo, FileDescriptor descriptor) {
    return DfuUtil.inputStreamToFile(descriptor.inputStream(),
        new File(createParentDir(cacheParentDirectoryPath), descriptorFileName(dfuInfo)),
        descriptor.totalSize());
  }

  /**
   * Delete the Firmware Image from internal memory.
   *
   * @param dfuInfo {@link DFUInfo} to create unique file name.
   * @return <code>true</code> if file deleted successfully.
   */
  @Override
  public boolean removeCacheDescription(DFUInfo dfuInfo) {
    File deleteFile = new File(createParentDir(cacheParentDirectoryPath), descriptorFileName(dfuInfo));
    if (deleteFile.exists()) {
      return deleteFile.delete();
    }

    return false;
  }

  /**
   * Returns FileDescriptor from internal memory.
   *
   * @param dfuInfo {@link DFUInfo} to create a file name.
   * @return {@link Optional<FileDescriptor>}
   */
  @Override
  public Optional<FileDescriptor> getDescriptor(DFUInfo dfuInfo) {

    try {
      Pair<InputStream, Long> pair = DfuUtil
          .getFileInputStream(/* path= */
              createParentDir(cacheParentDirectoryPath) + File.separator +
                  descriptorFileName(dfuInfo));
      if (pair.first == null) {
        return Optional.absent();
      }

      return Optional.of(FileDescriptor.create(pair.first, pair.second));
    } catch (FileNotFoundException e) {
      PrintLogger.e(TAG, "Error Message: " + e.getMessage());
      return Optional.absent();
    }
  }

  /**
   * Stored DFUInfo to persistent memory.
   *
   * @param dfuInfo <code>DFUInfo</code> Object to be saved in SharedPreference.
   * @param vid <code>String</code> vendor id.
   * @param pid <code>String</code> product id.
   * @param mid <code>String</code> module id.
   * @param tagSerialNUmber <code>String</code> Tag serial number.
   */
  @Override
  public void cacheUpdateInformation(DFUInfo dfuInfo,
      String vid, String pid, String mid, String tagSerialNUmber,
      long timeInMilliseconds) {
    sharedPreferences.edit()
        .putString(imageInfoKey(vid, pid, mid, tagSerialNUmber),
            new Gson().toJson(dfuInfo))
        .apply();
    sharedPreferences.edit()
        .putLong(imageInfoKey(vid, pid, mid, tagSerialNUmber) + "_time", timeInMilliseconds)
        .apply();
  }

  /**
   * Read {@link DFUInfo} from shared preference for provided parameters.
   *
   * @param vid <code>String</code> vendor id.
   * @param pid <code>String</code> product id.
   * @param mid <code>String</code> module id.
   * @param tagSerialNumber <code>String</code> Tag serial number.
   * @return {@link Optional<DFUInfo>}
   */
  @Override
  public Optional<DFUInfo> getUpdateInformation(String vid,
      String pid,
      String mid,
      String tagSerialNumber) {

    String jsonDfuInfo = sharedPreferences
        .getString(imageInfoKey(vid, pid, mid, tagSerialNumber), /* defValue= */"");

    if (TextUtils.isEmpty(jsonDfuInfo)) {
      return Optional.absent();
    }

    if (isCachePeriodInvalidated(sharedPreferences
        .getLong(imageInfoKey(vid, pid, mid, tagSerialNumber) + "_time", 0L))) {
      return Optional.absent();
    }

    return Optional
        .of(SdkTypeAdapterFactory.gson().fromJson(jsonDfuInfo,
            DFUInfo.class));
  }

  /**
   * @param vid <code>String</code> vendor id.
   * @param pid <code>String</code> product id.
   * @param mid <code>String</code> module id.
   * @param tagSerialNumber <code>String</code> tag serial number.
   */
  @Override
  public void removeUpdateInformation(String vid,
      String pid,
      String mid,
      String tagSerialNumber) {
    sharedPreferences.edit().remove(imageInfoKey(vid, pid, mid, tagSerialNumber))
        .remove(imageInfoKey(vid, pid, mid, tagSerialNumber) + "_time").apply();
  }

  private boolean isCachePeriodInvalidated(long timeOfCache) {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.HOUR, -INVALIDATION_PERIOD_IN_HOUR);
    return timeOfCache <= calendar.getTimeInMillis();
  }
}
