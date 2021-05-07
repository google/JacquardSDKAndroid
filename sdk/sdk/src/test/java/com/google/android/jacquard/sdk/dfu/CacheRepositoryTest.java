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

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.Build.VERSION_CODES;
import com.google.android.jacquard.sdk.CommonContextStub;
import java.io.File;
import java.net.URI;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public class CacheRepositoryTest {

  private final String version = "001001001";
  private final String productId = "pid";
  private final String moduleId = "mid";
  private final String vendorId = "vid";
  private final String dfuStatus = "optional";
  private final String tagSerialNumber = "aa-aa-aa-aa";
  private final URI downloadUrl = URI.create("http://fake/url") ;
  private final String separator = "_";

  @Test
  public void givenAllDataToCreateKeyForUpdateInformation_whenKeyCreated_thenCorrect() {

    // Assign
    Context context = Robolectric.setupService(CommonContextStub.class);
    CacheRepository cacheRepository = new CacheRepositoryImpl(context);
    StringBuilder builder = new StringBuilder();
    String expected = builder.append(vendorId)
        .append(separator)
        .append(productId)
        .append(separator)
        .append(moduleId)
        .append(separator)
        .append(version).toString();
    DFUInfo dfuInfo = DFUInfo
        .create(version, dfuStatus, downloadUrl, vendorId, productId, moduleId);

    // Act
    String key = cacheRepository.createKeyForUpdateInformation(dfuInfo);

    // Assert
    assertThat(key).isEqualTo(expected);
  }

  @Test
  public void givenParentDirToCreateParentDir_whenValidPath_thenCreateFile() {

    // Assign
    Context context = Robolectric.setupService(CommonContextStub.class);
    CacheRepository cacheRepository = new CacheRepositoryImpl(context);
    String directoryPath = context.getFilesDir() + File.separator + "DfuImages";

    // Act
    String responseDirectoryPath = cacheRepository.createParentDir(directoryPath);

    // Assert
    assertThat(responseDirectoryPath).isEqualTo(directoryPath);
  }

  @Test
  public void givenValidDataToDescriptorFileName_whenFileNameCreated_thenCorrect() {

    // Assign
    Context context = Robolectric.setupService(CommonContextStub.class);
    CacheRepository cacheRepository = new CacheRepositoryImpl(context);
    DFUInfo dfuInfo = DFUInfo
        .create(version, dfuStatus, downloadUrl, vendorId, productId, moduleId);
    String expected = "FILEDESCRIPTOR_" + cacheRepository
        .createKeyForUpdateInformation(dfuInfo);

    // Act
    String fileName = cacheRepository.descriptorFileName(dfuInfo);

    // Assert
    assertThat(fileName).isEqualTo(expected);
  }


  @Test
  public void givenValidDataToImageInfoKey_whenImageKeyCreated_thenCorrect() {

    // Assign
    Context context = Robolectric.setupService(CommonContextStub.class);
    CacheRepository cacheRepository = new CacheRepositoryImpl(context);
    DFUInfo dfuInfo = DFUInfo
        .create(version, dfuStatus, downloadUrl, vendorId, productId, moduleId);
    String expected =
        "IMAGEINFO_" + vendorId + "_" + productId + "_" + tagSerialNumber + "_" + moduleId;

    // Act
    String fileName = cacheRepository.imageInfoKey(vendorId, productId, moduleId, tagSerialNumber);

    // Assert
    assertThat(fileName).isEqualTo(expected);
  }

}
