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

import com.google.android.jacquard.sdk.dfu.model.FileDescriptor;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import java.io.IOException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Responsible to download {@link FileDescriptor} from {@link DFUInfo}. */
class DownloadManagerImpl implements DownloadManager {

  private static final String TAG = DownloadManagerImpl.class.getSimpleName();

  /**
   * Download the file using url. This method uses OkHttpClient to download the file.
   *
   * @param downloadUrl Url to consume to download file.
   * @param apiKey ApiKey to validate the client.
   * @return Signal<FileDescriptor> with byte stream of file.
   */
  @Override
  public Signal<FileDescriptor> download(String downloadUrl, String apiKey) {
    DownloadCallback downloadCallback = new DownloadCallback();
    return download(downloadCallback, downloadUrl, apiKey);
  }

  Signal<FileDescriptor> download(DownloadCallback downloadCallback, @Nonnull String downloadUrl,
      String apiKey) {
    return Signal.create(signal -> {
      OkHttpClient client = new OkHttpClient();
      Request request =
          new Request.Builder()
              .url(downloadUrl)
              .build();
      PrintLogger.d(TAG, "starting download!");
      client.newCall(request).enqueue(downloadCallback);
      downloadCallback.getSignal().forward(signal);
      return new Subscription();
    });
  }

  static class DownloadCallback implements Callback {

    protected final Signal<FileDescriptor> signal = Signal.create();

    /**
     * Exposes the Signal to observe, which delivers downloaded data.
     *
     * @return the {@link Signal<FileDescriptor>} which contains InputStream of downloaded file.
     */
    public Signal<FileDescriptor> getSignal() {
      return signal;
    }

    @Override
    public void onFailure(@Nullable Call call, IOException e) {
      signal.error(e);
    }

    @Override
    public void onResponse(Call call, @Nullable Response response) {
      if (response == null) {
        signal.error(new IOException("Response unavailable."));
        return;
      }

      if (!response.isSuccessful()) {
        signal.error(new IOException("Download failed."));
        return;
      }

      ResponseBody body = response.body();
      if (body == null) {
        signal.error(new IOException("Response body unavailable."));
        return;
      }

      signal.next(FileDescriptor.create(body.byteStream(), body.contentLength()));
      signal.complete();
      body.close();
    }
  }
}
