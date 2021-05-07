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
package com.google.android.jacquard.sdk.rx;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import java.util.concurrent.Executor;

/**
 * Provides executors for signal operators.
 */
public final class Executors {

  private Executors() {
    // Utility class.
  }

  /**
   * Posts executions to the main thread.
   */
  public static Executor mainThreadExecutor() {
    return MAIN_THREAD_EXECUTOR;
  }

  /**
   * Immediately calls {@link Runnable#run()} on the current thread.
   */
  public static Executor directExecutor() {
    return DIRECT_EXECUTOR;
  }

  private static final Executor MAIN_THREAD_EXECUTOR =
      new Executor() {
        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
          handler.post(command);
        }
      };

  private static final Executor DIRECT_EXECUTOR = Runnable::run;
}