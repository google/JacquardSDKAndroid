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

import com.google.android.jacquard.sdk.dfu.DownloadManagerImpl.DownloadCallback;
import com.google.android.jacquard.sdk.dfu.model.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import okhttp3.Call;
import okhttp3.Response;

/**
 * Fake implementation of {@link okhttp3.Callback}. We need to use this class in UnitTests
 * instead of {@link DownloadCallback} because downloadUrl keeps expiring because of which, we
 * cannot Test actual implementation of DownloadCallback.
 */
class FakeDownloadCallback extends DownloadCallback {

  private final String message;
  private final boolean success;
  private FileDescriptor fileDescriptor;

  public FakeDownloadCallback(String message, boolean success) {
    this.message = message;
    this.success = success;
  }

  @Override
  public void onFailure(Call call, IOException e) {
    signal.error(e);
  }

  @Override
  public void onResponse(Call call, Response response) {
    if (!success) {
      signal.error(new IOException(message));
      return;
    }

    FileDescriptor empty = FileDescriptor.create(new InputStream() {
      @Override
      public int read() throws IOException {
        return 0;
      }
    }, 0);

    signal.next(fileDescriptor == null ? empty : fileDescriptor);
  }

  public FakeDownloadCallback setFileDescriptor(FileDescriptor fileDescriptor) {
    this.fileDescriptor = fileDescriptor;
    return this;
  }
}
