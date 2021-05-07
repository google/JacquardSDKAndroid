/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.jacquard.sample.renametag;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.jacquard.sample.ConnectivityManager.Events;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sample.ViewModelFactory;
import com.google.android.jacquard.sample.utilities.Util;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import java.util.ArrayList;
import java.util.List;

/** A fragment for renaming the tag. */
public class RenameTagFragment extends Fragment {

  private static final String TAG = RenameTagFragment.class.getSimpleName();

  private final List<Subscription> subscriptions = new ArrayList<>();

  private RenameTagViewModel viewModel;
  private EditText tagNameEdt;
  private TextView tagNameTv;
  private View renameEditLayout, tagNameLayout;
  private View parentView;
  private final OnGlobalLayoutListener globalLayoutListener = new OnGlobalLayoutListener() {

    private final int defaultKeyboardHeightDP = 100;
    private final int EstimatedKeyboardDP =
        defaultKeyboardHeightDP + (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP ? 48
            : 0);
    private final Rect rect = new Rect();
    private boolean alreadyOpen;

    @Override
    public void onGlobalLayout() {
      int estimatedKeyboardHeight = (int) TypedValue
          .applyDimension(TypedValue.COMPLEX_UNIT_DIP, EstimatedKeyboardDP,
              parentView.getResources().getDisplayMetrics());
      parentView.getWindowVisibleDisplayFrame(rect);
      int heightDiff = parentView.getRootView().getHeight() - (rect.bottom - rect.top);
      boolean isShown = heightDiff >= estimatedKeyboardHeight;

      if (isShown == alreadyOpen) {
        return;
      }
      alreadyOpen = isShown;
      onKeyBoardVisibilityChanged(isShown);
    }
  };

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel = new ViewModelProvider(
        requireActivity(),
        new ViewModelFactory(requireActivity().getApplication(), getNavController()))
        .get(RenameTagViewModel.class);
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_rename_tag, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    initToolbar(view);
    initViews(view);
    setKeyboardVisibility(view);
    viewModel.initCurrentTag();
    initData();
    subscribeEvents();
  }

  @Override
  public void onDestroyView() {
    parentView.getViewTreeObserver().removeOnGlobalLayoutListener(globalLayoutListener);
    for (Signal.Subscription subscription : subscriptions) {
      subscription.unsubscribe();
    }
    subscriptions.clear();
    super.onDestroyView();
  }



  private void initViews(View view) {
    tagNameEdt = view.findViewById(R.id.tag_name_edt);
    tagNameTv = view.findViewById(R.id.tag_name);
    renameEditLayout = view.findViewById(R.id.rename_edit_layout);
    tagNameLayout = view.findViewById(R.id.tag_name_layout);
    view.findViewById(R.id.clear_icon).setOnClickListener(v -> tagNameEdt.setText(""));
    view.findViewById(R.id.rename_tag).setOnClickListener(v -> {
      tagNameEdt.setText(viewModel.getTagName());
      renameEditLayout.setVisibility(View.VISIBLE);
      tagNameLayout.setVisibility(View.GONE);
      tagNameEdt.requestFocus();
      InputMethodManager imm = (InputMethodManager) getActivity()
          .getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.showSoftInput(tagNameEdt, InputMethodManager.SHOW_IMPLICIT);
    });
    tagNameEdt.setOnEditorActionListener(
        (textView, actionId, keyEvent) -> {
          if (actionId == EditorInfo.IME_ACTION_DONE) {
            if (TextUtils.isEmpty(tagNameEdt.getText().toString())) {
              return true;
            }
            Util.hideSoftKeyboard(getActivity());
            tagNameLayout.setVisibility(View.VISIBLE);
            renameEditLayout.setVisibility(View.GONE);
            subscriptions.add(viewModel
                .updateTagName(tagNameEdt.getText().toString())
                .observe(updatedTagName -> {
                  tagNameTv.setText(updatedTagName);
                  Util.showSnackBar(getView(), updatedTagName + " name updated successfully");
                  viewModel.updateKnownDevices(updatedTagName);
                }, error -> {
                  if (error == null) {
                    return;
                  }
                  PrintLogger.e(TAG, error.getMessage());
                  showSnackbar(error.getMessage());
                }));
            return true;
          }
          return false;
        });
  }

  private void initData() {
    tagNameTv.setText(viewModel.getTagName());
  }

  private void onKeyBoardVisibilityChanged(boolean visible) {
    if (!visible) {
      renameEditLayout.setVisibility(View.GONE);
      tagNameLayout.setVisibility(View.VISIBLE);
    }
  }

  private void setKeyboardVisibility(View view) {
    parentView = view.getRootView();
    parentView.getViewTreeObserver().addOnGlobalLayoutListener(globalLayoutListener);
  }

  private NavController getNavController() {
    return NavHostFragment.findNavController(this);
  }

  private void initToolbar(View root) {
    Toolbar toolbar = root.findViewById(R.id.toolbar);
    toolbar.setNavigationOnClickListener(v -> {
      Util.hideSoftKeyboard(getActivity());
      viewModel.backArrowClick();
    });
  }

  private void showSnackbar(String message) {
    Util.showSnackBar(getView(), message);
  }

  private void subscribeEvents() {
    subscriptions.add(viewModel.getConnectivityEvents().onNext(this::onEvents));
  }

  private void onEvents(Events events) {
    switch (events) {
      case TAG_DISCONNECTED:
        showSnackbar(getString(R.string.tag_not_connected));
        break;
      case TAG_DETACHED:
        showSnackbar(getString(R.string.gear_detached));
        break;
    }
  }
}
