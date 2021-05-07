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

import android.text.TextUtils;
import com.google.android.jacquard.sdk.dfu.model.FileDescriptor;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.common.base.Optional;
import java.io.File;
import javax.annotation.Nonnull;


/** Interface skeleton for Caching mechanism. */
interface CacheRepository {

  String PREPEND_IMAGEINFO = "IMAGEINFO";
  String PREPEND_FILE_DESCRIPTOR = "FILEDESCRIPTOR_";
  int INVALIDATION_PERIOD_IN_HOUR = 12;
  /**
   * Checks if descriptor exists in internal memory for specified {@link DFUInfo}.
   *
   * @param dfuInfo {@link DFUInfo} to check Firmware file.
   * @return <code>true</code> is file exists.
   */
  boolean hasDescriptor(DFUInfo dfuInfo);

  /**
   * Stores the Firmware Image to the internal memory.
   *
   * @param dfuInfo {@link DFUInfo} to create unique file name.
   * @param descriptor {@link FileDescriptor} to read InputStream to store.
   * @return <code>true</code> if file written successfully.
   */
  Signal<Boolean> cacheDescriptor(DFUInfo dfuInfo, FileDescriptor descriptor);

  /**
   * Delete the Firmware Image from internal memory.
   *
   * @param dfuInfo {@link DFUInfo} to create unique file name.
   * @return <code>true</code> if file deleted successfully.
   */
  boolean removeCacheDescription(DFUInfo dfuInfo);

  /**
   * Returns FileDescriptor from internal memory.
   *
   * @param dfuInfo {@link DFUInfo} to create a file name.
   * @return {@link Optional<FileDescriptor>}
   */
  Optional<FileDescriptor> getDescriptor(DFUInfo dfuInfo);

  /**
   * Stored DFUInfo to persistent memory.
   *
   * @param dfuInfo <code>DFUInfo</code> Object to be saved in SharedPreference.
   * @param vid <code>String</code> vendor id.
   * @param pid <code>String</code> product id.
   * @param mid <code>String</code> module id.
   * @param tagSerialNUmber <code>String</code> Tag serial number.
   */
  void cacheUpdateInformation(DFUInfo dfuInfo, String vid, String pid, String mid,
      String tagSerialNUmber, long timeInMilliseconds);

  /**
   * Read {@link DFUInfo} from persistent memory for provided parameters.
   *
   * @param vid <code>String</code> vendor id.
   * @param pid <code>String</code> product id.
   * @param mid <code>String</code> module id.
   * @param tagSerialNumber <code>String</code> tag serial number.
   * @return {@link Optional<DFUInfo>}
   */
  Optional<DFUInfo> getUpdateInformation(String vid,
      String pid,
      String mid,
      String tagSerialNumber);

  /**
   * @param vid <code>String</code> vendor id.
   * @param pid <code>String</code> product id.
   * @param mid <code>String</code> module id.
   * @param tagSerialNumber <code>String</code> tag serial number.
   */
  void removeUpdateInformation(String vid,
      String pid,
      String mid,
      String tagSerialNumber);

  /**
   * Create Unique key with all parameters along with separator.
   *
   * @param dfuInfo {@link DFUInfo}.
   * @return String Unique key.
   */
  default String createKeyForUpdateInformation(DFUInfo dfuInfo) {
    String separator = "_";
    StringBuilder builder = new StringBuilder();
    builder.append(dfuInfo.vendorId())
        .append(separator)
        .append(dfuInfo.productId())
        .append(separator);

    if (!TextUtils.isEmpty(dfuInfo.moduleId())) {
      return builder.append(dfuInfo.moduleId())
          .append(separator)
          .append(dfuInfo.version().toZeroString()).toString();
    }
    return builder
        .append(dfuInfo.version().toZeroString())
        .toString();
  }

  /**
   * Create Directory for provided path if not exist.
   *
   * @param cacheParentDirectoryPath <code>String</code> path to create Directory.
   * @return <code>String</code> Directory absolute path.
   */
  default String createParentDir(@Nonnull String cacheParentDirectoryPath) {
    File cacheParent = new File(cacheParentDirectoryPath);
    if (!cacheParent.exists()) {
      cacheParent.mkdirs();
    }
    return cacheParent.getAbsolutePath();
  }

  /**
   * Creates the file name by combining provided data.
   *
   * @param dfuInfo {@link DFUInfo}.
   * @return <code>String</code> File name.
   */
  default String descriptorFileName(DFUInfo dfuInfo) {
    return PREPEND_FILE_DESCRIPTOR + createKeyForUpdateInformation(dfuInfo);
  }

  /**
   * Unique key for specific combination of Component info.
   *
   * @param vid <code>String</code> vendor id.
   * @param pid <code>String</code> product id.
   * @param mid <code>String</code> module id.
   * @param tagSerialNUmber <code>String</code> tag serial number.
   * @return <code>String</code> Image key.
   */
  default String imageInfoKey(String vid, String pid, String mid, String tagSerialNUmber) {
    String separator = "_";
    StringBuilder builder = new StringBuilder();
    builder.append(PREPEND_IMAGEINFO)
        .append(separator)
        .append(vid)
        .append(separator)
        .append(pid)
        .append(separator)
        .append(tagSerialNUmber);

    if (!TextUtils.isEmpty(mid)) {
      builder
          .append(separator)
          .append(mid);
    }
    return builder.toString();
  }

}
