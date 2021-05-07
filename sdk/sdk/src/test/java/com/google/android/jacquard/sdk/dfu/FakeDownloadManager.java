package com.google.android.jacquard.sdk.dfu;

import com.google.android.jacquard.sdk.dfu.model.FileDescriptor;
import com.google.android.jacquard.sdk.rx.Signal;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

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
public class FakeDownloadManager extends DownloadManagerImpl{

  private boolean success;
  private FileDescriptor fileDescriptor;

  FakeDownloadManager(File file, boolean success) throws FileNotFoundException {
    this.success = success;
    this.fileDescriptor = FileDescriptor.create(new FileInputStream(file), file.length());
  }

  @Override
  public Signal<FileDescriptor> download(String downloadUrl, String apiKey) {
    if (success) {
      return Signal.just(fileDescriptor);
    }

    return Signal.empty(new Exception("Fake exception"));
  }
}
