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
package com.google.android.jacquard.sdk.dfu;

import static android.os.Looper.getMainLooper;
import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.Build.VERSION_CODES;
import com.google.android.jacquard.sdk.CommonContextStub;
import com.google.android.jacquard.sdk.JacquardManager;
import com.google.android.jacquard.sdk.JacquardManagerInitialization;
import com.google.android.jacquard.sdk.dfu.DFUChecker.CheckUpdateParams;
import com.google.android.jacquard.sdk.dfu.DFUInfo.UpgradeStatus;
import com.google.android.jacquard.sdk.dfu.model.FileDescriptor;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.SdkConfig;
import com.google.android.jacquard.sdk.remote.FakeLocalRemoteFunction;
import com.google.android.jacquard.sdk.remote.RemoteFunctionInitialization;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.common.base.Optional;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public class DFUCheckerTest {

  final String tagVersion = "001053000";
  private final String version = "001053000";
  private final String dfuStatus = "optional";
  private final URI downloadUrl = new URI("https://fake.url/");
  private final String vendorId = "42-f9-a1-e3";
  private final String productId = "73-d0-58-c3";
  private final String moduleId = "a9-46-5b-d3";
  private final String tagSerialNumber = "000006000";

  private Context context;
  private CheckUpdateParams.Builder builder;

  public DFUCheckerTest() throws URISyntaxException {
  }

  @Before
  public void setup() {
    builder = CheckUpdateParams.builder();
    builder.vendorId(vendorId);
    builder.productId(productId);
    builder.moduleId(moduleId);
    builder.componentSerialNumber(tagSerialNumber);
    builder.componentVersion(version);
    builder.tagVersion(tagVersion);
    context = Robolectric.setupService(CommonContextStub.class);
    PrintLogger.initialize(context);
    RemoteFunctionInitialization.initRemoteFunction(
        new FakeLocalRemoteFunction(context.getResources()));
    JacquardManagerInitialization.initJacquardManager();
  }

  @Test
  public void givenCheckUpdate_whenAllParametersCorrect_thenDFUDownloadCorrect()
      throws IOException, InterruptedException {

    // Assign
    RemoteDfuInfo remoteDfuInfo = RemoteDfuInfo.create(version,
        dfuStatus, downloadUrl.toString(), vendorId, productId, moduleId);

    //Create file.
    byte[] outBytes = "abcdefghijklmnopqrstuvwxyz".getBytes(StandardCharsets.UTF_8);
    InputStream in = new ByteArrayInputStream(outBytes);
    File file = new File(/* pathname= */context.getFilesDir() + "/fakeFile");
    DfuUtil.inputStreamToFile(in, file, in.available()).consume();

    DFUChecker dfuChecker = new DummyDFUChecker<>(remoteDfuInfo, true)
        .createDownloadManager(file);
    CacheRepository cacheRepository = new CacheRepositoryImpl(context);
    Signal<List<DFUInfo>> signal = Signal.create();

    // Assert
    signal.observe(dfuInfos -> {
          for (DFUInfo dfu : dfuInfos) {
            Optional<DFUInfo> optional = cacheRepository.getUpdateInformation(vendorId,
                productId, moduleId, version);
            Optional<FileDescriptor> fileDescriptorOptional = cacheRepository.getDescriptor(dfu);

            assertThat(dfu.downloadUrl()).isNotNull();
            assertThat(dfu.productId()).isEqualTo(productId);
            assertThat(dfu.vendorId()).isEqualTo(vendorId);
            assertThat(optional.isPresent()).isTrue();
            assertThat(fileDescriptorOptional.isPresent()).isTrue();
          }
        },
        error -> assertThat(error).isNull());

    // Act
    dfuChecker.checkUpdate(Collections.singletonList(builder.build()), /* forceUpdate= */true)
        .forward(signal);
    shadowOf(getMainLooper()).idleFor(Duration.ofSeconds(3));
  }

  @Test
  public void givenCheckUpdate_whenModuleIdEmptyAndGearIdNotEmpty_thenDFUDownloadCorrect()
      throws InterruptedException, IOException {
    // Assign
    CountDownLatch latch = new CountDownLatch(1);

    //Create file.
    byte[] outBytes = "abcdefghijklmnopqrstuvwxyz".getBytes(StandardCharsets.UTF_8);
    InputStream in = new ByteArrayInputStream(outBytes);
    File file = new File(/* pathname= */context.getFilesDir() + "/fakeFile");
    DfuUtil.inputStreamToFile(in, file, in.available()).consume();

    RemoteDfuInfo remoteDfuInfo = RemoteDfuInfo.create(version,
        dfuStatus, downloadUrl.toString(), vendorId, productId, "");
    DFUChecker dfuChecker = new DummyDFUChecker<>(remoteDfuInfo, true)
        .createDownloadManager(file);
    Signal<List<DFUInfo>> signal = Signal.create();
    builder.moduleId("");

    // Assert
    signal.onNext(dfuInfos -> {
      latch.countDown();
      for (DFUInfo dfu : dfuInfos) {
        assertThat(dfu.downloadUrl()).isNotNull();
        assertThat(dfu.productId()).isEqualTo(productId);
        assertThat(dfu.vendorId()).isEqualTo(vendorId);
      }
    });

    // Act
    dfuChecker.checkUpdate(Collections.singletonList(builder.build()), /* forceUpdate= */true).forward(signal);
    latch.await(/* timeout=  */5, TimeUnit.SECONDS);
  }

  @Test
  public void givenCheckUpdate_whenModuleIdEmptyAndGearIdEmpty_thenDFUDownloadCorrect()
      throws InterruptedException, IOException {
    // Assign
    CountDownLatch latch = new CountDownLatch(1);

    //Create file.
    byte[] outBytes = "abcdefghijklmnopqrstuvwxyz".getBytes(StandardCharsets.UTF_8);
    InputStream in = new ByteArrayInputStream(outBytes);
    File file = new File(/* pathname= */context.getFilesDir() + "/fakeFile");
    DfuUtil.inputStreamToFile(in, file, in.available()).consume();

    RemoteDfuInfo remoteDfuInfo = RemoteDfuInfo.create(version,
        dfuStatus, downloadUrl.toString(), vendorId, productId, "");

    DFUChecker dfuChecker = new DummyDFUChecker<>(remoteDfuInfo, true)
        .createDownloadManager(file);
    Signal<List<DFUInfo>> signal = Signal.create();
    builder.moduleId("");

    // Assert
    signal.onNext(dfuInfos -> {
      latch.countDown();
      for (DFUInfo dfu : dfuInfos) {
        assertThat(dfu.downloadUrl()).isNotNull();
        assertThat(dfu.productId()).isEqualTo(productId);
        assertThat(dfu.vendorId()).isEqualTo(vendorId);
      }
    });

    // Act
    dfuChecker.checkUpdate(Collections.singletonList(builder.build()), /* forceUpdate= */true)
        .forward(signal);
    latch.await(/* timeout=  */5, TimeUnit.SECONDS);
  }

  @Test
  public void givenCheckUpdate_whenUJTAlreadyUpdated_thenDFUNotAvailable()
      throws FileNotFoundException, InterruptedException {

    // Assign
    final int expected = 0;

    RemoteDfuInfo remoteDfuInfo = RemoteDfuInfo.create(version,
        "NOT_AVAILABLE", downloadUrl.toString(), vendorId, productId, moduleId);

    DFUChecker dfuChecker = new DummyDFUChecker<>(remoteDfuInfo, true);
    Signal<List<DFUInfo>> signal = Signal.create();
    AtomicReference<Integer> availableDfu = new AtomicReference<>(/* initialValue= */0);
    signal.observe(dfuInfos -> {
          for (DFUInfo dfu : dfuInfos) {
            if (!dfu.dfuStatus().equals(UpgradeStatus.NOT_AVAILABLE)) {
              availableDfu.updateAndGet(value -> value + 1);
            }
          }
        },
        error -> assertThat(error).isNull());

    // Act
    dfuChecker.checkUpdate(Collections.singletonList(builder.build()), /* forceUpdate= */true)
        .forward(signal);

    // Assert
    assertThat(availableDfu.get()).isEqualTo(expected);
  }

  @Test
  public void givenCheckUpdateNoForceUpdate_whenCachedDFUExist_thenReturnDFU()
      throws IOException {

    // Assign
    AtomicReference<List<DFUInfo>> atomicReference = new AtomicReference<>();
    final String differentVersion = "001054000";
    RemoteDfuInfo remoteDfuInfo = RemoteDfuInfo.create(differentVersion,
        UpgradeStatus.OPTIONAL.toString(), downloadUrl.toString(), vendorId, productId,
        moduleId);
    DFUInfo dfuInfo = DFUInfo.create(remoteDfuInfo);
    builder.componentVersion(version);

    // Create Cache
    CacheRepository cacheRepository = new CacheRepositoryImpl(context);
    cacheRepository.cacheUpdateInformation(dfuInfo,
        vendorId, productId, moduleId, version, Calendar.getInstance().getTimeInMillis());

    //Create file.
    byte[] outBytes = "abcdefghijklmnopqrstuvwxyz".getBytes(StandardCharsets.UTF_8);
    InputStream in = new ByteArrayInputStream(outBytes);
    File file = new File(/* pathname= */
        cacheRepository.createParentDir(context.getFilesDir() + File.separator + "DfuImages")
            + File.separator +
            cacheRepository.descriptorFileName(dfuInfo));
    DfuUtil.inputStreamToFile(in, file, in.available()).consume();
    cacheRepository.cacheDescriptor(dfuInfo, FileDescriptor.create(in, file.length()));

    // DFU Checker
    DFUChecker dfuChecker = new DummyDFUChecker<>(remoteDfuInfo, true).createDownloadManager(file);
    Signal<List<DFUInfo>> signal = Signal.create();
    signal.onNext(atomicReference::set);

    // Act
    dfuChecker.checkUpdate(Collections.singletonList(builder.build()), /* forceUpdate= */false)
        .forward(signal);

    // Assert
    assertThat(atomicReference.get().get(0)).isEqualTo(dfuInfo);
  }

  @Test
  public void givenCheckUpdate_whenApiKeyNotExists_thenThrowError() {

    // Assign
    JacquardManager jacquardManager = JacquardManager.getInstance();
    jacquardManager.init(SdkConfig.of("ClientId", "", /* cloudEndpointUrl= */ null));

    AtomicReference<Throwable> atomicReference = new AtomicReference<>();
    RemoteDfuInfo remoteDfuInfo = RemoteDfuInfo.create(version,
        UpgradeStatus.NOT_AVAILABLE.toString(), downloadUrl.toString(), vendorId, productId, moduleId);
    DFUChecker dfuChecker = new DummyDFUChecker<>(remoteDfuInfo, true);
    Signal<List<DFUInfo>> signal = Signal.create();
    signal.onError(atomicReference::set);

    // Act
    dfuChecker.checkUpdate(Collections.singletonList(builder.build()), /* forceUpdate= */true)
        .forward(signal);

    // Assert
    assertThat(atomicReference.get()).isInstanceOf(IllegalArgumentException.class);
    assertThat(atomicReference.get().getMessage()).isEqualTo(
        "Need to initialise SdkConfig with required data by calling JacquardManager#init");
  }

  @Test
  public void givenCheckUpdate_whenClientIdNotExists_thenThrowError() {

    // Assign
    JacquardManager jacquardManager = JacquardManager.getInstance();
    jacquardManager.init(SdkConfig.of("", "ApiKey", /* cloudEndpointUrl= */ null));

    AtomicReference<Throwable> atomicReference = new AtomicReference<>();
    RemoteDfuInfo remoteDfuInfo = RemoteDfuInfo.create(version,
        UpgradeStatus.NOT_AVAILABLE.toString(), downloadUrl.toString(), vendorId, productId, moduleId);
    DFUChecker dfuChecker = new DummyDFUChecker<>(remoteDfuInfo, true);
    Signal<List<DFUInfo>> signal = Signal.create();
    signal.onError(atomicReference::set);

    // Act
    dfuChecker.checkUpdate(Collections.singletonList(builder.build()), /* forceUpdate= */true)
        .forward(signal);

    // Assert
    assertThat(atomicReference.get()).isInstanceOf(IllegalArgumentException.class);
    assertThat(atomicReference.get().getMessage()).isEqualTo(
        "Need to initialise SdkConfig with required data by calling JacquardManager#init");
  }
}
