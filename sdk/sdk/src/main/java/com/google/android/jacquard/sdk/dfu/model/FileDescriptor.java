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

package com.google.android.jacquard.sdk.dfu.model;

import com.google.auto.value.AutoValue;
import java.io.InputStream;

/** POJO class Represents File downloaded from cloud. */
@AutoValue
public abstract class FileDescriptor {

  /** This method returns the inputStream from the response body. */
  public abstract InputStream inputStream();

  /** Total size of the downloaded file in bytes. */
  public abstract long totalSize();

  /**
   * Creates new instance of {@link FileDescriptor}.
   *
   * @param inputStream InputStream of file.
   * @param length Total size of a file.
   * @return New {@link FileDescriptor}.
   */
  public static FileDescriptor create(InputStream inputStream, long length) {
    return new AutoValue_FileDescriptor(inputStream, length);
  }
}
