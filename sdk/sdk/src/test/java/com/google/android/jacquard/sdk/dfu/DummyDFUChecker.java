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

import java.io.File;
import java.io.FileNotFoundException;

public class DummyDFUChecker<T> extends DFUChecker{


  private boolean success;
  private T body;
  private Throwable error;


  DummyDFUChecker(T body, boolean success) {
   super();
   this.body = body;
   this.success = success;
    createCloudManager();
  }

  DummyDFUChecker(Throwable error, boolean success) {
    super();
    this.error = error;
    this.success = success;
    createCloudManager();
  }

  private void createCloudManager() {
    cloudManager = new FakeCloudManager<T>()
        .setSuccess(success)
        .setBody(body)
        .setError(error);
  }

  DummyDFUChecker createDownloadManager(File file) throws FileNotFoundException {
    downloadManager = new FakeDownloadManager(file, true);
    return this;
  }
}
