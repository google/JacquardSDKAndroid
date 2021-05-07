/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.google.android.jacquard.sample.dialog;

import android.app.Activity;
import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import com.google.android.jacquard.sample.MainActivity;
import com.google.android.jacquard.sample.dialog.DefaultDialog.DefaultDialogBuilder;
import com.google.android.jacquard.sample.dialog.DefaultDialog.DefaultDialogButtonClickListener;
import java.util.ArrayList;

/**
 * Utility class for dialogs.
 */
public class DialogUtils {

  /**
   * Shows bottom sheet dialog.
   */
  public static void showBottomOptions(
      ArrayList<BottomSheetItem> bottomSheetItems, Activity activity) {
    BottomSheetDialogFragment bottomSheetDialogFragment = BottomSheetDialogFragment.newInstance();
    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList("items", bottomSheetItems);
    bottomSheetDialogFragment.setArguments(bundle);
    bottomSheetDialogFragment.show(((MainActivity) activity).getSupportFragmentManager(),
        bottomSheetDialogFragment.getTag());
  }

  /**
   * Shows confirmation dialog.
   */
  public static void showDialog(int title, int subtitle, int positiveButton, int negativeButton,
      DefaultDialogButtonClickListener clickListener, FragmentManager fragmentManager) {
    new DefaultDialogBuilder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setPositiveButtonTitleId(positiveButton)
        .setNegativeButtonTitle(negativeButton)
        .setShowNegativeButton(true)
        .setShowPositiveButton(true)
        .setCancellable(false)
        .setShowSubtitle(true)
        .setShowProgress(false)
        .setPositiveButtonClick(clickListener).build()
        .show(fragmentManager, /* tag= */null);
  }
}
