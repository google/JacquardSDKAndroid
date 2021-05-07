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

package com.google.android.jacquard.sample.firmwareupdate;

import static com.google.android.jacquard.sdk.dfu.FirmwareUpdateState.Type.COMPLETED;
import static com.google.android.jacquard.sdk.dfu.FirmwareUpdateState.Type.ERROR;
import static com.google.android.jacquard.sdk.dfu.FirmwareUpdateState.Type.TRANSFERRED;
import static com.google.android.jacquard.sdk.dfu.FirmwareUpdateState.Type.TRANSFER_PROGRESS;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sample.ViewModelFactory;
import com.google.android.jacquard.sample.dialog.DefaultDialog;
import com.google.android.jacquard.sample.dialog.DefaultDialog.DefaultDialogBuilder;
import com.google.android.jacquard.sample.dialog.DefaultDialog.DefaultDialogButtonClickListener;
import com.google.android.jacquard.sample.firmwareupdate.FirmwareUpdateViewModel.State;
import com.google.android.jacquard.sdk.dfu.DFUInfo;
import com.google.android.jacquard.sdk.dfu.DFUInfo.UpgradeStatus;
import com.google.android.jacquard.sdk.dfu.execption.InsufficientBatteryException;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.GearState;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import java.util.ArrayList;
import java.util.List;

/** A fragment for tag and gear firmware updated. */
public class FirmwareUpdateFragment extends Fragment {

  private static final String TAG = FirmwareUpdateFragment.class.getSimpleName();

  private final List<Subscription> dfuSubscriptionList = new ArrayList<>();
  private FirmwareUpdateViewModel viewModel;
  private View progressBarHolder;
  private Subscription navSubscription;
  private TextView productVersionTv,tagVersionTv;
  private Subscription gearNotificationSub;
  private SwitchCompat autoUpdateSw, forceUpdateSw;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel = new ViewModelProvider(
        requireActivity(),
        new ViewModelFactory(requireActivity().getApplication(), getNavController()))
        .get(FirmwareUpdateViewModel.class);
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_firmware_update, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    initView(view);
    navSubscription = viewModel.stateSignal.onNext(this::onNavigation);
    viewModel.init();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    unsubscribe();
  }

  private void onNavigation(State state) {
    switch (state.getType()) {
      case CONNECTED:
        onConnected(state.connected());
        break;
      case DISCONNECTED:
      case ERROR:
        onDisconnected();
    }
  }

  private void onConnected(ConnectedJacquardTag tag) {
    tagVersionTv.setText(tag.tagComponent().version().toString());
    gearNotificationSub = viewModel.getGearNotifications(tag).onNext(notification -> {
      if (notification.gear().getType() == GearState.Type.ATTACHED) {
        productVersionTv.setText(tag.gearComponent().version().toString());
      } else {
        productVersionTv.setText(R.string.not_available);
      }
    });
  }

  private void onDisconnected() {
    if (gearNotificationSub != null) {
      gearNotificationSub.unsubscribe();
    }
    tagVersionTv.setText(R.string.not_available);
    productVersionTv.setText(R.string.not_available);
  }

  private void initView(View view) {
    initToolbar(view);
    progressBarHolder = view.findViewById(R.id.progress_bar_holder);
    productVersionTv = view.findViewById(R.id.product_version_tv);
    tagVersionTv = view.findViewById(R.id.tag_version_tv);
    autoUpdateSw = view.findViewById(R.id.auto_update_sw);
    forceUpdateSw = view.findViewById(R.id.force_update_sw);
    handleBackPress(view);
    view.findViewById(R.id.check_for_firmware_btn).setOnClickListener(v -> checkFirmware());
  }

  private void handleBackPress(View view) {
    view.setFocusableInTouchMode(true);
    view.requestFocus();
    view.setOnKeyListener((v, keyCode, event) ->
        keyCode == KeyEvent.KEYCODE_BACK && progressBarHolder.getVisibility() == View.VISIBLE);
  }

  private void initToolbar(View root) {
    Toolbar toolbar = root.findViewById(R.id.toolbar);
    toolbar.setNavigationOnClickListener(v -> viewModel.backArrowClick());
  }

  private NavController getNavController() {
    return NavHostFragment.findNavController(this);
  }

  private void checkFirmware() {
    PrintLogger.d(TAG, "checkFirmware");
    showProgressLoader(R.string.checking_for_updates_progress);
    dfuSubscriptionList.add(viewModel.checkFirmware(forceUpdateSw.isChecked())
        .tapError(error -> {
          PrintLogger.e(TAG, "Fragment checkFirmware() : " + error.getMessage());
          hideProgressLoader();
          handleError(error);
        })
        .onNext(listDfu -> {
          hideProgressLoader();
          if (noUpdateAvailable(listDfu)) {
            showNoDFUDialog();
            return;
          }

          if (hasAtleastOneMandatory(listDfu)) {
            showMandatoryDialog();
            return;
          }

          showOptionalDialog();
        }));
  }

  private void applyFirmware() {
    PrintLogger.d(TAG, "applyFirmware");
    DefaultDialog defaultDialog = showApplyFirmwareProgress();
    dfuSubscriptionList.add(viewModel.applyFirmware(autoUpdateSw.isChecked()).
        tapError(error -> {
          PrintLogger.e(TAG, "applyFirmware error: " + error.getMessage());
          defaultDialog.dismiss();
          showErrorDialog(error);
        })
        .onNext(firmwareUpdateState -> {
          PrintLogger.d(TAG, "applyFirmware firmwareUpdateState = " + firmwareUpdateState);
          if (firmwareUpdateState.getType().equals(ERROR)) {
            defaultDialog.dismiss();
            showErrorDialog(firmwareUpdateState.error());
            return;
          }
          if (firmwareUpdateState.getType().equals(TRANSFER_PROGRESS)) {
            defaultDialog.updateProgress(firmwareUpdateState.transferProgress());
          } else if (firmwareUpdateState.getType().equals(TRANSFERRED)) {
            defaultDialog.dismiss();
            if (autoUpdateSw.isChecked()) {
              showProgressLoader(R.string.execute_updates_progress);
            } else {
              showAlmostReadyDialog();
            }
          } else if (firmwareUpdateState.getType().equals(COMPLETED)) {
            hideProgressLoader();
            showUpdateCompleteDialog();
          }
        }));
  }

  private void showErrorDialog(Throwable error) {
    hideProgressLoader();
    handleError(error);
    dfuUnsubscribe();
  }

  private void executeFirmware() {
    PrintLogger.d(TAG, "executeFirmware");
    showProgressLoader(R.string.execute_updates_progress);
    dfuSubscriptionList.add(viewModel.executeFirmware().observe(firmwareUpdateState -> {
          PrintLogger.d(TAG, "executeFirmware firmwareUpdateState: " + firmwareUpdateState);
          if (firmwareUpdateState.getType().equals(ERROR)) {
            showErrorDialog(firmwareUpdateState.error());
            return;
          }
          if (firmwareUpdateState.getType().equals(COMPLETED)) {
            hideProgressLoader();
            showUpdateCompleteDialog();
          }
        },
        error -> showErrorDialog(error)));
  }

  private boolean noUpdateAvailable(List<DFUInfo> listDfu) {
    if (listDfu == null || listDfu.isEmpty()) {
      return true;
    }

    for (DFUInfo dfuInfo : listDfu) {
      if (!dfuInfo.dfuStatus().equals(UpgradeStatus.NOT_AVAILABLE)) {
        return false;
      }
    }

    return true;
  }

  private boolean hasAtleastOneMandatory(List<DFUInfo> list) {
    for (DFUInfo dfuInfo : list) {
      if (dfuInfo.dfuStatus().equals(UpgradeStatus.MANDATORY)) {
        return true;
      }
    }
    return false;
  }

  private void unsubscribe() {
    dfuUnsubscribe();
    if (gearNotificationSub != null) {
      gearNotificationSub.unsubscribe();
    }
    if (navSubscription != null) {
      navSubscription.unsubscribe();
    }
  }

  private void dfuUnsubscribe(){
    for (Subscription subscription : dfuSubscriptionList) {
      subscription.unsubscribe();
    }
    dfuSubscriptionList.clear();
  }

  private void showMandatoryDialog() {
    PrintLogger.d(TAG, "showMandatoryDialog");
    new DefaultDialogBuilder()
        .setTitle(R.string.optional_update_dialog_title)
        .setSubtitle(R.string.optional_update_dialog_subtitle)
        .setPositiveButtonTitleId(R.string.optional_update_dialog_positive_btn)
        .setShowNegativeButton(false)
        .setShowPositiveButton(true)
        .setCancellable(true)
        .setShowSubtitle(true)
        .setShowProgress(false)
        .setPositiveButtonClick(new DefaultDialogButtonClickListener() {

          @Override
          public void buttonClick() {
            applyFirmware();
          }
        }).build().show(getParentFragmentManager(), /* tag= */null);
  }

  private void showOptionalDialog() {
    PrintLogger.d(TAG, "showOptionalDialog");
    new DefaultDialogBuilder()
        .setTitle(R.string.optional_update_dialog_title)
        .setSubtitle(R.string.optional_update_dialog_subtitle)
        .setPositiveButtonTitleId(R.string.optional_update_dialog_positive_btn)
        .setNegativeButtonTitle(R.string.optional_update_dialog_negative_btn)
        .setShowNegativeButton(true)
        .setShowPositiveButton(true)
        .setCancellable(true)
        .setShowSubtitle(true)
        .setShowProgress(false)
        .setPositiveButtonClick(new DefaultDialogButtonClickListener() {

          @Override
          public void buttonClick() {
            applyFirmware();
          }
        }).build().show(getParentFragmentManager(), /* tag= */null);
  }

  private void showNoDFUDialog() {
    PrintLogger.d(TAG, "showNoDFUDialog");
    new DefaultDialogBuilder()
        .setTitle(R.string.no_update_available_title)
        .setPositiveButtonTitleId(R.string.no_update_available_positive_btn)
        .setShowNegativeButton(false)
        .setShowPositiveButton(true)
        .setCancellable(true)
        .setShowSubtitle(false)
        .setShowProgress(false)
        .build().show(getParentFragmentManager(), /* tag= */null);
  }

  private DefaultDialog showApplyFirmwareProgress() {
    PrintLogger.d(TAG, "showApplyFirmwareProgress");
    DefaultDialog defaultDialog = new DefaultDialogBuilder()
        .setTitle(R.string.apply_update_dialog_title)
        .setSubtitle(R.string.apply_update_dialog_subtitle)
        .setShowNegativeButton(false)
        .setShowPositiveButton(false)
        .setCancellable(false)
        .setShowSubtitle(true)
        .setShowProgress(true)
        .build();

    defaultDialog.show(getParentFragmentManager(), /* tag= */null);
    return defaultDialog;
  }

  private void showAlmostReadyDialog() {
    PrintLogger.d(TAG, "showAlmostReadyDialog");
    new DefaultDialogBuilder()
        .setTitle(R.string.almost_ready_title)
        .setSubtitle(R.string.almost_ready_subtitle)
        .setPositiveButtonTitleId(R.string.almost_ready_update_dialog_positive_btn)
        .setShowNegativeButton(false)
        .setShowPositiveButton(true)
        .setCancellable(true)
        .setShowSubtitle(true)
        .setShowProgress(false)
        .setPositiveButtonClick(new DefaultDialogButtonClickListener() {

          @Override
          public void buttonClick() {
            executeFirmware();

          }
        }).build().show(getParentFragmentManager(), /* tag= */null);
  }

  private void showUpdateCompleteDialog() {
    PrintLogger.d(TAG, "showUpdateCompleteDialog");
    new DefaultDialogBuilder()
        .setTitle(R.string.update_complete_dialog_title)
        .setSubtitle(R.string.update_complete_dialog_subtitle)
        .setPositiveButtonTitleId(R.string.update_complete_dialog_positive_btn)
        .setShowNegativeButton(false)
        .setShowPositiveButton(true)
        .setCancellable(true)
        .setShowSubtitle(true)
        .setShowProgress(false)
        .build().show(getParentFragmentManager(), /* tag= */null);
  }

  private void showErrorDialog(int title, int subTitle, int positiveBtn) {
    PrintLogger.d(TAG, "showErrorDialog");
    new DefaultDialogBuilder()
        .setTitle(title)
        .setSubtitle(subTitle)
        .setPositiveButtonTitleId(positiveBtn)
        .setShowNegativeButton(false)
        .setShowPositiveButton(true)
        .setCancellable(true)
        .setShowSubtitle(true)
        .setShowProgress(false)
        .build().show(getParentFragmentManager(), /* tag= */null);
  }

  private void showProgressLoader(int loaderMessageId) {
    PrintLogger.d(TAG, "showProgressLoader");
    progressBarHolder.setVisibility(View.VISIBLE);
    ((TextView) progressBarHolder.findViewById(R.id.progress_message)).setText(loaderMessageId);
  }

  private void hideProgressLoader() {
    PrintLogger.d(TAG, "hideProgressLoader");
    progressBarHolder.setVisibility(View.GONE);
  }

  private void handleError(Throwable error) {
    PrintLogger.d(TAG, "handleError: " + error);
    if (error instanceof InsufficientBatteryException) {
      showErrorDialog(R.string.dfu_error_battery_title,
          R.string.dfu_error_battery_subtitle,
          R.string.dfu_error_battery_positive_btn);
      return;
    }
    showErrorDialog(R.string.dfu_error_generic_title,
        R.string.dfu_error_generic_subtitle,
        R.string.dfu_error_generic_positive_btn);
  }
}