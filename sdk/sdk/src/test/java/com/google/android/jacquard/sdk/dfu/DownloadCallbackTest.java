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

import static com.google.common.truth.Truth.assertThat;

import android.os.Build.VERSION_CODES;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.jacquard.sdk.dfu.DownloadManagerImpl.DownloadCallback;
import com.google.android.jacquard.sdk.dfu.model.FileDescriptor;
import com.google.android.jacquard.sdk.rx.Signal;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public class DownloadCallbackTest {

  @Test
  public void givenValueToOnResponse_whenResponseNull_thenError() {

    // Assign
    CountDownLatch latch = new CountDownLatch(1);
    final String message = "Response unavailable.";
    DownloadCallback downloadCallback = new DownloadCallback();
    Signal<FileDescriptor> signal = downloadCallback.getSignal();

    // Act
    downloadCallback.onResponse(null, null);

    // Assert
    signal.onError(error -> {
      latch.countDown();
      assertThat(error.getMessage()).isEqualTo(message);
    });

    try {
      latch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void givenValueToOnResponse_whenResponseBodyNull_thenError() {

    // Assign
    CountDownLatch latch = new CountDownLatch(1);
    final String message = "Response body unavailable.";
    DownloadCallback downloadCallback = new DownloadCallback();
    Signal<FileDescriptor> signal = downloadCallback.getSignal();

    Response.Builder builder = new Response.Builder();
    builder.request(new Request.Builder().url("https://hostname.com/").build());
    builder.protocol(Protocol.HTTP_1_0);
    builder.message("This is message");
    builder.body(null);
    builder.code(200);

    // Act
    downloadCallback.onResponse(/* call= */ null, builder.build());

    // Assert
    signal.onError(error -> {
      latch.countDown();
      assertThat(error.getMessage()).isEqualTo(message);
    });

    try {
      latch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void givenValueToOnResponse_whenResponseNotSuccessful_thenError() {

    // Assign
    CountDownLatch latch = new CountDownLatch(1);
    final String message = "Download failed.";
    DownloadCallback downloadCallback = new DownloadCallback();
    Signal<FileDescriptor> signal = downloadCallback.getSignal();

    Response.Builder builder = new Response.Builder();
    builder.request(new Request.Builder().url("https://hostname.com/").build());
    builder.protocol(Protocol.HTTP_1_0);
    builder.message("This is message");
    builder.body(ResponseBody.create(/* contentType= */null, /* content= */""));
    builder.code(0);

    // Act
    downloadCallback.onResponse(/* call= */ null, builder.build());

    // Assert
    signal.onError(error -> {
      latch.countDown();
      assertThat(error.getMessage()).isEqualTo(message);
    });

    try {
      latch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void givenValueToOnResponse_whenResponseCorrect_thenFileDescriptorReturned() {

    // Assign
    CountDownLatch latch = new CountDownLatch(1);

    DownloadCallback downloadCallback = new DownloadCallback();
    Signal<FileDescriptor> signal = downloadCallback.getSignal();

    Response.Builder builder = new Response.Builder();
    builder.request(new Request.Builder().url("https://hostname.com/").build());
    builder.protocol(Protocol.HTTP_1_0);
    builder.message("This is message");
    builder.body(ResponseBody.create(/* contentType= */null, /* content= */""));
    builder.code(200);

    // Assert
    signal.onNext(fileDescr -> {
      latch.countDown();
      assertThat(fileDescr).isNotNull();
    });

    // Act
    downloadCallback.onResponse(/* call= */ null, builder.build());

    try {
      latch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void givenValueToOnFailure_whenException_thenError() {

    // Assign
    CountDownLatch latch = new CountDownLatch(1);
    final String message = "This is test exception";
    DownloadCallback downloadCallback = new DownloadCallback();
    Signal<FileDescriptor> signal = downloadCallback.getSignal();

    // Act
    downloadCallback.onFailure(null, new IOException(message));

    // Assert
    signal.onError(error -> {
      latch.countDown();
      assertThat(error.getMessage()).isEqualTo(message);
    });

    try {
      latch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
