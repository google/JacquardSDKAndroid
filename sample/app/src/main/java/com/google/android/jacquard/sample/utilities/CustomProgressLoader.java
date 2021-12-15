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
package com.google.android.jacquard.sample.utilities;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.jacquard.sample.R;

public class CustomProgressLoader extends FrameLayout {

  private ProgressBar progressLoader;
  private TextView textProgressMessage;

  public CustomProgressLoader(@NonNull Context context) {
    this(context, null, 0, 0);
  }

  public CustomProgressLoader(@NonNull Context context,
      @Nullable AttributeSet attrs) {
    this(context, attrs, 0, 0);
  }

  public CustomProgressLoader(@NonNull Context context, @Nullable AttributeSet attrs,
      int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  public CustomProgressLoader(@NonNull Context context, @Nullable AttributeSet attrs,
      int defStyleAttr,
      int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    initView();
  }

  private void initView() {
    View view = inflate(getContext(), R.layout.custom_progress_bar_holder, this);
    progressLoader = view.findViewById(R.id.progress_bar);
    textProgressMessage = view.findViewById(R.id.progress_message);
  }
}
