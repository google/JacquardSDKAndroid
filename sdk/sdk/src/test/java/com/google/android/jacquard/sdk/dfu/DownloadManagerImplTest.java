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

import android.os.Build.VERSION_CODES;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.jacquard.sdk.dfu.model.FileDescriptor;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.rx.Signal;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public class DownloadManagerImplTest {

  final String apiKey = "Fake api key";
  final String downloadUrl = "https://fake.com/";

  @Test
  public void givenCorrectUrlApiKey_whenFileDownloadedCorrect_thenCorrect()
      throws InterruptedException {
    // Assign
    CountDownLatch latch = new CountDownLatch(1);
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    DownloadManagerImpl downloadManager = new DownloadManagerImpl();
    AtomicReference<InputStream> inputStreamAtomicReference = new AtomicReference<>();
    Signal<FileDescriptor> signal = Signal.create();
    signal.onNext(fileDesc -> {
      inputStreamAtomicReference.set(fileDesc.inputStream());
      latch.countDown();
    });
    // Act
    downloadManager.download(new FakeDownloadCallback(/* message= */
            "Fake Response Error.",/* success= */true),
        downloadUrl, apiKey).forward(signal);
    latch.await(/* timeout= */20, TimeUnit.SECONDS);
    // Assert
    assertThat(inputStreamAtomicReference.get()).isNotNull();
  }

  @Test
  public void givenWrongUrlApiKey_whenNotFileDownloaded_thenCorrect() throws InterruptedException {
    // Assign
    CountDownLatch latch = new CountDownLatch(1);
    DownloadManagerImpl downloadManager = new DownloadManagerImpl();
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    AtomicReference<Throwable> throwableAtomicReference = new AtomicReference<>();
    Signal<FileDescriptor> signal = Signal.create();
    signal.onError(error -> {
      throwableAtomicReference.set(error);
      latch.countDown();
    });
    // Act
    downloadManager.download(new FakeDownloadCallback(/* message= */
            "Fake Response Error.", /* success= */false),
        downloadUrl, "").forward(signal);
    latch.await(/* timeout= */20, TimeUnit.SECONDS);
    // Assert
    assertThat(throwableAtomicReference.get().getMessage()).isEqualTo("Fake Response Error.");
  }
}
