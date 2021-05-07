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
package com.google.android.jacquard.sample.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.google.android.jacquard.sample.R;

/** Generic Dialog to show messages. */
public class DefaultDialog extends DialogFragment implements OnClickListener{
  private static final String EXTRA_SHOW_POSITIVE = "EXTRA_SHOW_POSITIVE";
  private static final String EXTRA_SHOW_NEGATIVE = "EXTRA_SHOW_NEGATIVE";
  private static final String EXTRA_SHOW_SUBTITLE = "EXTRA_SHOW_SUBTITLE";
  private static final String EXTRA_SHOW_PROGRESS = "EXTRA_SHOW_PROGRESS";

  private static final String EXTRA_POSITIVE_BUTTON_TEXT = "EXTRA_POSITIVE_BUTTON_TEXT";
  private static final String EXTRA_NEGATIVE_BUTTON_TEXT = "EXTRA_NEGATIVE_BUTTON_TEXT";
  private static final String EXTRA_SUBTITLE_TEXT = "EXTRA_SUBTITLE_TEXT";
  private static final String EXTRA_SUBTITLE_TEXT_STR = "EXTRA_SUBTITLE_TEXT_STR";
  private static final String EXTRA_TITLE_TEXT = "EXTRA_TITLE_TEXT";
  private static final String EXTRA_IS_CANCELABLE = "EXTRA_IS_CANCELABLE";

  private static final String EXTRA_POSITIVE_BUTTON_CLICK = "EXTRA_POSITIVE_BUTTON_CLICK";
  private static final String EXTRA_NEGATIVE_BUTTON_CLICK = "EXTRA_NEGATIVE_BUTTON_CLICK";

  private DefaultDialogButtonClickListener postivieButtonClick;
  private DefaultDialogButtonClickListener negativeButtonClick;
  private ProgressBar progressBar;
  private TextView progressPercentage;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return init(
        inflater.inflate(R.layout.fragment_custom_dialog, container, /* attachRoot= */false));
  }

  @Override
  public void onResume() {
    super.onResume();
    overrideDialogWidth(getDialog());
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    Dialog dialog = super.onCreateDialog(savedInstanceState);
    dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
    return dialog;
  }

  /**
   * Populates the percentage for apply firmware progress.
   *
   * @param percentage <code>int</code>.
   */
  public void updateProgress(int percentage) {
    if (progressBar == null) {
      return;
    }

    progressBar.setProgress(percentage);
    progressPercentage.setText(String.format("%s %%", percentage));
  }

  @Override
  public void onClick(View v) {
    if (v.getId() == R.id.dialogPositiveButton) {
      dismiss();
      if (postivieButtonClick != null) {
        postivieButtonClick.buttonClick();
      }
    }

    if (v.getId() == R.id.dialogNegativeButton) {
      dismiss();
      if (negativeButtonClick != null) {
        negativeButtonClick.buttonClick();
      }
    }
  }

  private void overrideDialogWidth(Dialog dialog) {
    WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
    params.width = LayoutParams.MATCH_PARENT;
    params.height = LayoutParams.WRAP_CONTENT;
    dialog.getWindow().setAttributes(params);
  }

  private View init(View parentView) {
    TextView textViewTitle = parentView.findViewById(R.id.page_title);
    TextView textViewSubTitle = parentView.findViewById(R.id.subTitleTxt);
    textViewSubTitle.setMovementMethod(LinkMovementMethod.getInstance());
    Button positiveBtn = parentView.findViewById(R.id.dialogPositiveButton);
    Button negativeBtn = parentView.findViewById(R.id.dialogNegativeButton);
    View progressLayout = parentView.findViewById(R.id.progressLayout);
    progressBar = parentView.findViewById(R.id.progressBar);
    progressPercentage = parentView.findViewById(R.id.txtProgressPercentage);
    Bundle bundle = getArguments();

    if (bundle == null) {
      return parentView;
    }

    boolean showSubTitle = bundle.getBoolean(EXTRA_SHOW_SUBTITLE, true);
    boolean showProgress = bundle.getBoolean(EXTRA_SHOW_PROGRESS, true);
    boolean showPositive = bundle.getBoolean(EXTRA_SHOW_POSITIVE, true);
    boolean showNegative = bundle.getBoolean(EXTRA_SHOW_NEGATIVE, true);
    boolean isCancelable = bundle.getBoolean(EXTRA_IS_CANCELABLE, true);
    int titleId = bundle.getInt(EXTRA_TITLE_TEXT, 0);
    int subTitleId = bundle.getInt(EXTRA_SUBTITLE_TEXT, 0);
    String subTitleStr = bundle.getString(EXTRA_SUBTITLE_TEXT_STR, getString(R.string.empty));
    int positiveText = bundle.getInt(EXTRA_POSITIVE_BUTTON_TEXT, 0);
    int negativeText = bundle.getInt(EXTRA_NEGATIVE_BUTTON_TEXT, 0);
    postivieButtonClick = bundle.getParcelable(EXTRA_POSITIVE_BUTTON_CLICK);
    negativeButtonClick = bundle.getParcelable(EXTRA_NEGATIVE_BUTTON_CLICK);

    setCancelable(isCancelable);
    textViewTitle.setText(titleId);
    if (subTitleId == R.string.empty){
      textViewSubTitle.setText(subTitleStr);
    } else {
      textViewSubTitle.setText(subTitleId);
    }
    positiveBtn.setText(positiveText);
    negativeBtn.setText(negativeText);
    positiveBtn.setOnClickListener(this);
    negativeBtn.setOnClickListener(this);

    if (!showSubTitle) {
      textViewSubTitle.setVisibility(View.GONE);
    }
    if (!showProgress) {
      progressLayout.setVisibility(View.GONE);
    }
    if (!showPositive) {
      positiveBtn.setVisibility(View.GONE);
    }
    if (!showNegative) {
      negativeBtn.setVisibility(View.GONE);
    }
    return parentView;
  }

  /** Builder for {@link DefaultDialog}. */
  public static class DefaultDialogBuilder {
    private boolean showNegativeButton = false;
    private boolean showPositiveButton = false;
    private boolean showSubtitle = false;
    private boolean showProgress = false;
    private boolean isCancellable = true;

    private int positiveButtonTitleId = R.string.optional_update_dialog_positive_btn;
    private int negativeButtonTitle = R.string.optional_update_dialog_negative_btn;
    private int subtitle = R.string.empty;
    private int title = R.string.optional_update_dialog_title;
    private String subtitleStr;

    private DefaultDialogButtonClickListener positiveButtonClick;
    private DefaultDialogButtonClickListener negativeButtonClick;

    public DefaultDialogBuilder setShowNegativeButton(boolean showNegativeButton) {
      this.showNegativeButton = showNegativeButton;
      return this;
    }

    public DefaultDialogBuilder setShowPositiveButton(boolean showPositiveButton) {
      this.showPositiveButton = showPositiveButton;
      return this;
    }

    public DefaultDialogBuilder setShowSubtitle(boolean showSubtitle) {
      this.showSubtitle = showSubtitle;
      return this;
    }

    public DefaultDialogBuilder setShowProgress(boolean showProgress) {
      this.showProgress = showProgress;
      return this;
    }

    public DefaultDialogBuilder setPositiveButtonTitleId(int positiveButtonTitleId) {
      this.positiveButtonTitleId = positiveButtonTitleId;
      return this;
    }

    public DefaultDialogBuilder setNegativeButtonTitle(int negativeButtonTitle) {
      this.negativeButtonTitle = negativeButtonTitle;
      return this;
    }

    public DefaultDialogBuilder setSubtitle(int subtitle) {
      this.subtitle = subtitle;
      return this;
    }

    public DefaultDialogBuilder setSubtitle(String subtitleStr) {
      this.subtitleStr = subtitleStr;
      return this;
    }

    public DefaultDialogBuilder setTitle(int title) {
      this.title = title;
      return this;
    }

    public DefaultDialogBuilder setCancellable(boolean cancellable) {
      isCancellable = cancellable;
      return this;
    }

    public DefaultDialogBuilder setPositiveButtonClick(DefaultDialogButtonClickListener positiveButtonClick) {
      this.positiveButtonClick = positiveButtonClick;
      return this;
    }

    public DefaultDialogBuilder setNegativeButtonClick(DefaultDialogButtonClickListener negativeButtonClick) {
      this.negativeButtonClick = negativeButtonClick;
      return this;
    }

    /**
     * Builds {@link DefaultDialog} alonth with required parameters.
     *
     * @return <code> DefaultDialog </code>
     */
    public DefaultDialog build() {
      Bundle bundle = new Bundle();
      bundle.putBoolean(EXTRA_SHOW_POSITIVE, showPositiveButton);
      bundle.putBoolean(EXTRA_SHOW_NEGATIVE, showNegativeButton);
      bundle.putBoolean(EXTRA_SHOW_SUBTITLE, showSubtitle);
      bundle.putBoolean(EXTRA_SHOW_PROGRESS, showProgress);
      bundle.putBoolean(EXTRA_IS_CANCELABLE, isCancellable);
      bundle.putInt(EXTRA_POSITIVE_BUTTON_TEXT, positiveButtonTitleId);
      bundle.putInt(EXTRA_NEGATIVE_BUTTON_TEXT, negativeButtonTitle);
      bundle.putInt(EXTRA_SUBTITLE_TEXT, subtitle);
      bundle.putString(EXTRA_SUBTITLE_TEXT_STR, subtitleStr);
      bundle.putInt(EXTRA_TITLE_TEXT, title);
      bundle.putParcelable(EXTRA_POSITIVE_BUTTON_CLICK, positiveButtonClick);
      bundle.putParcelable(EXTRA_NEGATIVE_BUTTON_CLICK, negativeButtonClick);
      DefaultDialog dialog = new DefaultDialog();
      dialog.setArguments(bundle);
      return dialog;
    }
  }

  /** Listener for positive and negative buttons of {@link DefaultDialog}. */
  public abstract static class DefaultDialogButtonClickListener implements Parcelable {


    public DefaultDialogButtonClickListener(){}

    public abstract void buttonClick();

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    @Override
    public int describeContents() {
      return 0;
    }
  }
}
