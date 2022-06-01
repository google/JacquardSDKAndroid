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
package com.google.android.jacquard.sdk.dfu;

import static android.os.Looper.getMainLooper;
import static com.google.android.jacquard.sdk.dfu.DfuUtil.SEPARATOR;
import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.Build.VERSION_CODES;
import androidx.core.util.Pair;
import androidx.test.core.app.ApplicationProvider;
import com.google.android.jacquard.sdk.CommonContextStub;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.rx.Signal;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link DfuUtil}
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public final class DfuUtilTest {

  private String dfuDirectory;

  @Before
  public void setup() {
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    dfuDirectory = ApplicationProvider.getApplicationContext().getFilesDir().getAbsolutePath();
  }

  @Test
  public void crc16_successful() {
    // Assign
    byte[] message = "Hello".getBytes();
    // Act
    int response = DfuUtil.crc16(message, message.length);
    // Assert
    assertThat(response).isEqualTo(/* expected= */52182);
  }

  @Test
  public void crc16_zero() {
    // Assign
    byte[] message = "".getBytes();
    // Act
    int response = DfuUtil.crc16(message, message.length);
    // Assert
    assertThat(response).isEqualTo(/* expected= */0);
  }

  @Test
  public void getFileSize_fileNotExists() {
    // Act
    long response = DfuUtil.getFileSize(dfuDirectory, getDfuInfo());
    // Assert
    assertThat(response).isEqualTo(/* expected= */0);
  }

  @Test
  public void getFileSize() throws IOException {
    // Assign
    String fileText = "Hello";
    DFUInfo dfuInfo = getDfuInfo();
    createFile(dfuInfo, fileText);
    // Act
    long response = DfuUtil.getFileSize(dfuDirectory, dfuInfo);
    // Assert
    assertThat(response).isEqualTo(fileText.length());
  }

  @Test
  public void getAbsolutePath() throws IOException {
    // Assign
    String fileText = "Hello";
    DFUInfo dfuInfo = getDfuInfo();
    createFile(dfuInfo, fileText);
    // Act
    String response = DfuUtil.getFirmwareFilePath(dfuDirectory, dfuInfo);
    // Assert
    assertThat(response).isEqualTo(
        dfuDirectory + File.separator + dfuInfo.vendorId() + SEPARATOR + dfuInfo.productId()
            + SEPARATOR + dfuInfo.moduleId() + SEPARATOR + dfuInfo.version().toString());
  }

  private void createFile(DFUInfo dfuInfo, String text) throws IOException {
    File file = new File(DfuUtil.getFirmwareFilePath(dfuDirectory, dfuInfo));
    file.createNewFile();
    FileWriter myWriter = new FileWriter(file);
    myWriter.write(text);
    myWriter.close();
  }

  private static DFUInfo getDfuInfo() {
    String version = "000006000";
    String dfuStatus = "optional";
    URI downloadUrl = null;
    try {
      downloadUrl = new URI("https://storage.googleapis.com/");
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
    String vendorId = "42-f9-a1-e3";
    String productId = "73-d0-58-c3";
    String moduleId = "a9-46-5b-d3";
    return DFUInfo.create(version, dfuStatus, downloadUrl, vendorId, productId, moduleId);
  }

  @Test
  public void givenFileToInputStreamToFile_whenCreateNewFile_thenCorrect()
      throws IOException, InterruptedException {

    // Assign
    CountDownLatch latch = new CountDownLatch(1);
    Context context = Robolectric.setupService(CommonContextStub.class);
    byte[] outBytes = "abcdefghijklmnopqrstuvwxyz".getBytes(StandardCharsets.UTF_8);
    PrintLogger.initialize(context);
    InputStream in = new ByteArrayInputStream(outBytes);
    File file = new File(/* pathname= */context.getFilesDir() + "/fakeFile");
    Signal<String> signal = Signal.create();

    // Assert
    signal.onNext(bool -> {
      latch.countDown();
      assertThat(file.length()).isEqualTo(outBytes.length);
    });


    // Act
    DfuUtil.inputStreamToFile(in, file, in.available()).forward(signal);
    latch.await(/* timeout= */5, TimeUnit.SECONDS);
  }

  @Test
  public void givenNonExistentFilePath_whenReturnEmptyPair_thenCorrect() throws FileNotFoundException {
    // Assign
    Context context = Robolectric.setupService(CommonContextStub.class);
    File file = new File(/* pathname= */context.getFilesDir() + "/FileNoExists");

    // Act
    Pair<InputStream, Long> pair = DfuUtil.getFileInputStream(file.getPath());


    // Assert
    assertThat(pair.first).isNull();
    assertThat(pair.second).isEqualTo(/* expected= */0);
  }

  @Test
  public void givenCorrectFilePath_whenReturnCorrectPair_thenCorrect() throws IOException {
    // Assign
    Context context = Robolectric.setupService(CommonContextStub.class);
    PrintLogger.initialize(context);

    File file = new File(/* pathname= */context.getFilesDir() + "/FileExists");
    file.createNewFile();
    byte[] outBytes = "WriteThisForTest".getBytes(StandardCharsets.UTF_8);
    InputStream in = new ByteArrayInputStream(outBytes);
    DfuUtil.inputStreamToFile(in, file, in.available()).consume();
    shadowOf(getMainLooper()).idleFor(Duration.ofSeconds(1));

    // Act
    Pair<InputStream, Long> pair = DfuUtil.getFileInputStream(file.getPath());


    // Assert
    assertThat(pair.first).isNotNull();
    assertThat(pair.second).isEqualTo(file.length());
  }

  @Test
  public void givenCorrectFilePath_whenFileLengthDoesNotMatch_thenError() throws IOException {
    // Assign
    Context context = Robolectric.setupService(CommonContextStub.class);
    PrintLogger.initialize(context);

    File file = new File(/* pathname= */context.getFilesDir() + "/FileExists");
    file.createNewFile();
    byte[] outBytes = "WriteThisForTest".getBytes(StandardCharsets.UTF_8);
    InputStream in = new ByteArrayInputStream(outBytes);
    AtomicReference<Throwable> atomicReference = new AtomicReference<>();
    Signal<String> signal = Signal.create();
    signal.onError(atomicReference::set);

    // Act
    DfuUtil.inputStreamToFile(in, file, 100L).forward(signal);
    shadowOf(getMainLooper()).idleFor(Duration.ofSeconds(1));

    // Assert
    assertThat(atomicReference.get().getMessage())
        .isEqualTo("Cached file length does not match with ContentLength");
  }
}
