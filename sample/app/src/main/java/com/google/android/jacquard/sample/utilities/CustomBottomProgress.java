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

public class CustomBottomProgress extends FrameLayout {

  private ProgressBar progressBar;
  private TextView textProgress;

  public CustomBottomProgress(@NonNull Context context) {
    this(context, null);
  }

  public CustomBottomProgress(@NonNull Context context,
      @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public CustomBottomProgress(@NonNull Context context, @Nullable AttributeSet attrs,
      int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initView();
  }

  private void initView() {
    View view = inflate(getContext(), R.layout.item_dfu_progress, this);
    progressBar = view.findViewById(R.id.progressBar);
    textProgress = view.findViewById(R.id.txtDownloadingPercentage);
  }

  public void setProgress(int progress) {
    progressBar.setProgress(progress);
    textProgress.setText(String.format("%d %s", progress, "%"));
  }
}
