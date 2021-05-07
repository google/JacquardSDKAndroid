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
import android.content.SharedPreferences;
import android.os.Build.VERSION_CODES;
import android.util.Pair;
import androidx.test.core.app.ApplicationProvider;
import com.google.android.jacquard.sdk.CommonContextStub;
import com.google.android.jacquard.sdk.dfu.DFUInfo.UpgradeStatus;
import com.google.android.jacquard.sdk.dfu.model.FileDescriptor;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.common.base.Optional;
import com.google.gson.Gson;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public class CacheRepositoryImplTest {

  private final String version = "001001001";
  private final String productId = "pid";
  private final String moduleId = "mid";
  private final String vendorId = "vid";
  private final String dfuStatus = "optional";
  private final String tagSerialNumber = "aa-aa-aa-aa";
  private final URI downloadUrl = URI.create("http://fake/url") ;

  private Context context;

  @Before
  public void setup() {
    context = Robolectric.setupService(CommonContextStub.class);
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
  }

  @Test
  public void givenDFUInfo_whenFileExist_thenCorrect() throws IOException, InterruptedException {

    // Assign
    Context context = Robolectric.setupService(CommonContextStub.class);

    byte[] outBytes = "abcdefghijklmnopqrstuvwxyz".getBytes(StandardCharsets.UTF_8);
    InputStream in = new ByteArrayInputStream(outBytes);
    File file = new File(/* pathname= */context.getFilesDir() + File.separator + "DfuImages");
    DfuUtil.inputStreamToFile(in, file);

    DFUInfo dfuInfo = DFUInfo
        .create(version, dfuStatus, downloadUrl, vendorId, productId, moduleId);
    CacheRepository cacheRepository = new CacheRepositoryImpl(context);
    getFirmwareFile(cacheRepository, context).createNewFile();

    // Act
    boolean hasDescriptor = cacheRepository.hasDescriptor(dfuInfo);

    // Assert
    assertThat(hasDescriptor).isTrue();
  }

  @Test
  public void givenDFUInfoAndFileDescriptor_whenCacheDescriptorCorrect_thenCorrect()
      throws IOException {

    // Assign
    CacheRepository cacheRepository = new CacheRepositoryImpl(context);
    DFUInfo dfuInfo = DFUInfo
        .create(version, dfuStatus, downloadUrl, vendorId, productId, moduleId);
    Pair<InputStream, Long> pair = createStream(context,
        cacheRepository.descriptorFileName(dfuInfo));

    // Act
    cacheRepository
        .cacheDescriptor(dfuInfo, FileDescriptor.create(pair.first, pair.second));

    // Assert
    File file = getFirmwareFile(cacheRepository, context);
    assertThat(file.exists()).isTrue();
  }

  @Test
  public void givenDFUInfo_whenFileNotExists_thenDescriptorAbsent() throws IOException {

    // Assign
    DFUInfo dfuInfo = DFUInfo
        .create(version, dfuStatus, downloadUrl, vendorId, productId, moduleId);

    CacheRepository cacheRepository = new CacheRepositoryImpl(context);

    // Act
    Optional<FileDescriptor> fileDescriptorOptional = cacheRepository.getDescriptor(dfuInfo);

    // Assert
    assertThat(fileDescriptorOptional.isPresent()).isFalse();
  }

  @Test
  public void givenDFUInfo_whenFileNotFoundException_thenDescriptorAbsent() throws IOException {

    // Assign
    DFUInfo dfuInfo = DFUInfo
        .create(version, dfuStatus, downloadUrl, vendorId, productId, moduleId);

    CacheRepository cacheRepository = new CacheRepositoryImpl(context);

    // Act
    Optional<FileDescriptor> fileDescriptorOptional = cacheRepository.getDescriptor(dfuInfo);

    // Assert
    assertThat(fileDescriptorOptional.isPresent()).isFalse();
  }

  @Test
  public void givenDFUInfo_whenFileInpustreamFound_thenDescriptorPresent() throws IOException {

    // Assign
    CacheRepository cacheRepository = new CacheRepositoryImpl(context);
    DFUInfo dfuInfo = DFUInfo
        .create(version, dfuStatus, downloadUrl, vendorId, productId, moduleId);
    Pair<InputStream, Long> pair = createStream(context,
        cacheRepository.descriptorFileName(dfuInfo));

    // Act
    Optional<FileDescriptor> fileDescriptorOptional = cacheRepository.getDescriptor(dfuInfo);

    // Assert
    assertThat(fileDescriptorOptional.isPresent()).isTrue();
    assertThat(fileDescriptorOptional.get().totalSize()).isEqualTo(pair.second);
  }

  @Test
  public void givenDFUInfoToCacheUpdateInformation_whenSharedPreferenceSaved_thenCorrect() {

    // Assign
    SharedPreferences sharedPreferences = context.getSharedPreferences("JacquardPublicSdk", Context.MODE_PRIVATE);
    CacheRepository cacheRepository = new CacheRepositoryImpl(context);
    DFUInfo dfuInfo = DFUInfo
        .create(version, dfuStatus, downloadUrl, vendorId, productId, moduleId);
    final long expectedTimeInMillis = Calendar.getInstance().getTimeInMillis();

    // Act
    cacheRepository.cacheUpdateInformation(dfuInfo, vendorId, productId, moduleId, tagSerialNumber,
        expectedTimeInMillis);


    // Assert
    String resultDfuInfo = sharedPreferences
        .getString(cacheRepository.imageInfoKey(vendorId, productId, moduleId, tagSerialNumber),
            "");
    long resultTimeInMillis = sharedPreferences.getLong(
        cacheRepository.imageInfoKey(vendorId, productId, moduleId, tagSerialNumber) + "_time", 0L);
    String dfuJson = new Gson().toJson(dfuInfo);

    assertThat(resultDfuInfo).isEqualTo(dfuJson);
    assertThat(resultTimeInMillis).isEqualTo(expectedTimeInMillis);
  }

  @Test
  public void givenDFUInfoTOGetUpdateInformation_whenJsonEmpty_dfuAbsent() {

    // Assign
    CacheRepository cacheRepository = new CacheRepositoryImpl(context);

    // Act
    Optional<DFUInfo> optional = cacheRepository
        .getUpdateInformation(vendorId, productId, moduleId, version);

    // Assert
    assertThat(optional.isPresent()).isFalse();
  }

  @Test
  public void givenDFUInfoTOGetUpdateInformation_whenJsonExist_dfuAbsent() {

    // Assign
    final long expectedTimeInMillis = Calendar.getInstance().getTimeInMillis();
    CacheRepository cacheRepository = new CacheRepositoryImpl(context);

    SharedPreferences sharedPreferences = context
        .getSharedPreferences("JacquardPublicSdk", Context.MODE_PRIVATE);

    DFUInfo dfuInfo = DFUInfo
        .create(version, dfuStatus, downloadUrl, vendorId, productId, moduleId);

    sharedPreferences.edit()
        .putString(cacheRepository.imageInfoKey(vendorId, productId, moduleId, tagSerialNumber),
            new Gson().toJson(dfuInfo)).apply();
    sharedPreferences.edit().putLong(
        cacheRepository.imageInfoKey(vendorId, productId, moduleId, tagSerialNumber) + "_time",
        expectedTimeInMillis)
        .apply();

    // Act
    Optional<DFUInfo> optional = cacheRepository.getUpdateInformation(vendorId, productId,
        moduleId, tagSerialNumber);

     // Assert
    assertThat(optional.isPresent()).isTrue();

    DFUInfo exptectedInfo = optional.get();
    assertThat(exptectedInfo.version().toZeroString()).isEqualTo(version);
    assertThat(exptectedInfo.dfuStatus()).isEqualTo(UpgradeStatus.OPTIONAL);
    assertThat(exptectedInfo.downloadUrl()).isEqualTo(downloadUrl);
    assertThat(exptectedInfo.vendorId()).isEqualTo(vendorId);
    assertThat(exptectedInfo.productId()).isEqualTo(productId);
    assertThat(exptectedInfo.moduleId()).isEqualTo(moduleId);

  }

  private File getFirmwareFile(CacheRepository cacheRepository ,Context context) {
    String filePath = context.getFilesDir() + File.separator + "DfuImages";
    File dir = new File(filePath);
    if (!dir.exists()) {
      dir.mkdirs();
    }

    DFUInfo dfuInfo = DFUInfo
        .create(version, dfuStatus, downloadUrl, vendorId, productId, moduleId);
    return new File(filePath,
        cacheRepository.descriptorFileName(dfuInfo));
  }

  private static Pair<InputStream, Long> createStream(Context context, String fileName) throws IOException {
    /* create File to write */
    File dir = new File(/* pathname= */context.getFilesDir() + File.separator + "DfuImages");
    if (!dir.exists()) {
      dir.mkdirs();
    }

    byte[] outBytes = "WriteThisForTest".getBytes(StandardCharsets.UTF_8);
    InputStream in = new ByteArrayInputStream(outBytes);
    File file = new File(dir, fileName);
    file.createNewFile();
    DfuUtil.inputStreamToFile(in, file);

    Pair<InputStream, Long> pair = Pair.create(in, file.length());
    return pair;
  }
}
