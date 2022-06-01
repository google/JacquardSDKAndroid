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
import com.google.android.jacquard.sdk.rx.Signal;

/** Interface declare utility methods related to downloading. */
interface DownloadManager {

  /** Skeleton utility method to download file from url. */
  Signal<FileDescriptor> download(String downloadUrl, String apiKey);
}
