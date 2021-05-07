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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import com.google.android.jacquard.sample.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.List;

/**
 * Fragment for showing bottom sheet dialog.
 */
public class BottomSheetDialogFragment extends DialogFragment {

  public static BottomSheetDialogFragment newInstance() {
    return new BottomSheetDialogFragment();
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Dialog dialog = new BottomSheetDialog(requireContext(), getTheme());
    dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    return dialog;
  }

  @Override
  public int getTheme() {
    return R.style.BaseBottomSheetDialog;
  }

  @SuppressLint("RestrictedApi")
  @Override
  public void setupDialog(Dialog dialog, int style) {
    super.setupDialog(dialog, style);
    dialog.setContentView(createDialogView(dialog, getArguments()));
  }

  /**
   * Creates dialog view.
   *
   * @param dialog    Dialog view.
   * @param arguments Arguments to be set.
   * @return Dialog view.
   */
  private View createDialogView(Dialog dialog, Bundle arguments) {
    View containerView =
        LayoutInflater.from(requireContext()).inflate(R.layout.container_bottom_sheet, null, false);
    LinearLayout bottomSheetDialog = containerView.findViewById(R.id.bottom_sheet_dialog);
    if (arguments != null) {
      List<BottomSheetItem> sheetItems = arguments.getParcelableArrayList("items");
      if (sheetItems != null) {
        for (int i = 0; i < sheetItems.size(); i++) {
          if (i == sheetItems.size() - 1) {
            bottomSheetDialog.addView(
                addItemToContainer(dialog.getContext(), sheetItems.get(i), true));
          } else {
            bottomSheetDialog.addView(
                addItemToContainer(dialog.getContext(), sheetItems.get(i), false));
          }
        }
      }
    }
    return bottomSheetDialog;
  }

  /**
   * Adding item to the bottom sheet container
   *
   * @param context    Context from fragment
   * @param sheetItem  JQBottomSheetDialogItem to be added
   * @param isLastItem true if item is last one else false
   * @return View to be added in bottom sheet dialog
   */
  private View addItemToContainer(Context context, final BottomSheetItem sheetItem,
      boolean isLastItem) {
    View itemView =
        LayoutInflater.from(requireContext()).inflate(R.layout.item_bottom_sheet, null, false);
    TextView lblBottomSheetItem = itemView.findViewById(R.id.lbl_bottom_sheet_item);
    if (isLastItem) {
      lblBottomSheetItem.setTextColor(ContextCompat.getColor(context, R.color.grey_600));
    }
    lblBottomSheetItem.setText(sheetItem.getName());
    lblBottomSheetItem.setTag(sheetItem.getId());

    lblBottomSheetItem.setOnClickListener(
        v -> {
          sheetItem.getBottomSheetMenuListener().onMenuItemClicked((Integer) v.getTag());
          dismiss();
        });
    return lblBottomSheetItem;
  }

  /**
   * listener for bottom sheet dialog item click
   */
  public interface BottomSheetMenuListener {

    void onMenuItemClicked(int id);
  }
}
