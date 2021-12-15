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

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.google.android.jacquard.sample.dialog.DFUUtil;
import com.google.android.jacquard.sample.dialog.DefaultDialog;
import com.google.android.jacquard.sample.dialog.DefaultDialog.DefaultDialogButtonClickListener;
import com.google.android.jacquard.sample.firmwareupdate.FirmwareUpdateViewModel.State;
import com.google.android.jacquard.sample.utilities.CustomBottomProgress;
import com.google.android.jacquard.sdk.dfu.DFUInfo;
import com.google.android.jacquard.sdk.dfu.DFUInfo.UpgradeStatus;
import com.google.android.jacquard.sdk.dfu.FirmwareUpdateState;
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
  private Subscription stateSubscription;
  private FirmwareUpdateViewModel viewModel;
  private View progressBarHolder;
  private Subscription navSubscription;
  private TextView productVersionTv,tagVersionTv;
  private Subscription gearNotificationSub;
  private SwitchCompat autoUpdateSw, forceUpdateSw;
  private Button checkForFirmwareBtn;
  private CustomBottomProgress viewDownloadProgress;
  private DefaultDialog defaultDialog;
  private ApplyFirmwareListener applyFirmwareListener;

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
    fwUpdateStateListener();
  }

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);
    applyFirmwareListener = (ApplyFirmwareListener) context;
  }

  @Override
  public void onResume() {
    super.onResume();
    fwUpdateStateListener();
  }

  @Override
  public void onPause() {
    unSubscribeState();
    super.onPause();
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
    viewDownloadProgress = view.findViewById(R.id.includeLayout);
    checkForFirmwareBtn = view.findViewById(R.id.check_for_firmware_btn);
    handleBackPress(view);
    checkForFirmwareBtn.setOnClickListener(v -> checkFirmware());
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
    defaultDialog = showApplyFirmwareProgress();
    dfuSubscriptionList.add(viewModel.applyFirmware(autoUpdateSw.isChecked()).consume());
    applyFirmwareListener.applyFirmwareInitiated();
    fwUpdateStateListener();
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
            DFUUtil.showUpdateCompleteDialog(getParentFragmentManager());
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
    DFUUtil.showMandatoryDialog(getParentFragmentManager(),
        new DefaultDialogButtonClickListener() {
          @Override
          public void buttonClick() {
            applyFirmware();
          }
        });
  }

  private void showOptionalDialog() {
    PrintLogger.d(TAG, "showOptionalDialog");
    DFUUtil.showOptionalDialog(getParentFragmentManager(),
        new DefaultDialogButtonClickListener() {
          @Override
          public void buttonClick() {
            applyFirmware();
          }
        });
  }

  private void showNoDFUDialog() {
    PrintLogger.d(TAG, "showNoDFUDialog");
    DFUUtil.showNoDFUDialog(getParentFragmentManager());
  }

  private DefaultDialog showApplyFirmwareProgress() {
    PrintLogger.d(TAG, "showApplyFirmwareProgress");
    return DFUUtil.showApplyFirmwareProgress(getParentFragmentManager(),
        new DefaultDialogButtonClickListener() {
          @Override
          public void buttonClick() {
            PrintLogger.d(TAG, "Update progress ok clicked.");
            showBottomDownloadProgress(0);
          }
        });
  }

  private void showAlmostReadyDialog() {
    PrintLogger.d(TAG, "showAlmostReadyDialog");
    if (viewModel.isAlmostReadyShown()) {
      return;
    }

    viewModel.saveAlmostReadyShown();
    DFUUtil.showAlmostReadyDialog(getParentFragmentManager(),
        new DefaultDialogButtonClickListener() {
          @Override
          public void buttonClick() {
            executeFirmware();
          }
        });
  }

  private void showErrorDialog(int title, int subTitle, int positiveBtn) {
    PrintLogger.d(TAG, "showErrorDialog");
    DFUUtil.showErrorDialog(title, subTitle, positiveBtn, getParentFragmentManager());
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

  private void enable() {
    autoUpdateSw.setEnabled(true);
    forceUpdateSw.setEnabled(true);
    checkForFirmwareBtn.setEnabled(true);
  }

  private void disable() {
    autoUpdateSw.setEnabled(false);
    forceUpdateSw.setEnabled(false);
    checkForFirmwareBtn.setEnabled(false);
  }

  private void hideBottomDownloadProgress() {
    if (viewDownloadProgress.getVisibility() != View.VISIBLE) {
      return;
    }
    viewDownloadProgress.setVisibility(View.GONE);
    enable();
  }

  private void showBottomDownloadProgress(int progress) {
    viewDownloadProgress.setVisibility(View.VISIBLE);
    viewDownloadProgress.setProgress(progress);
    disable();
  }

  private void hideDefaultDialog(DefaultDialog defaultDialog) {
    if (defaultDialog == null || !defaultDialog.isAdded()) {
      return;
    }
    defaultDialog.dismiss();
  }

  private void firmwareUpdatePositiveUI(FirmwareUpdateState firmwareUpdateState) {
    PrintLogger.d(TAG, "applyFirmware firmwareUpdateState = " + firmwareUpdateState);
    switch (firmwareUpdateState.getType()) {
      case ERROR:
        firmwareUpdateNegativeUI(firmwareUpdateState.error());
        break;
      case TRANSFER_PROGRESS:
        autoUpdateSw.setChecked(viewModel.isAutoUpdateChecked());
        if (defaultDialog != null && defaultDialog.isAdded()) {
          defaultDialog.updateProgress(firmwareUpdateState.transferProgress());
        } else {
          showBottomDownloadProgress(firmwareUpdateState.transferProgress());
        }
        break;
      case TRANSFERRED:
        hideDefaultDialog(defaultDialog);
        hideBottomDownloadProgress();

        if (autoUpdateSw.isChecked()) {
          showProgressLoader(R.string.execute_updates_progress);
        } else {
          showAlmostReadyDialog();
        }
        break;
      case COMPLETED:
        hideProgressLoader();
        DFUUtil.showUpdateCompleteDialog(getParentFragmentManager());
        break;
      default:
        break;
    }
  }

  private void firmwareUpdateNegativeUI(Throwable error) {
    PrintLogger.e(TAG, "applyFirmware error: " + error.getMessage());
    hideDefaultDialog(defaultDialog);
    hideBottomDownloadProgress();
    showErrorDialog(error);
  }

  private void unSubscribeState() {
    if (stateSubscription != null) {
      stateSubscription.unsubscribe();
      stateSubscription = null;
    }
  }

  private void fwUpdateStateListener() {
    PrintLogger.d(TAG, "Register FirmwareUpdateState Signal: " + viewModel.getState().hashCode());
    unSubscribeState();
    stateSubscription = viewModel.getState().tapError(error -> {
      PrintLogger.d(TAG, "State Error: " + error);
      firmwareUpdateNegativeUI(error);
    }).onNext(firmwareUpdateState -> {
      PrintLogger
          .d(TAG, "isResumed: " + isResumed() + " firmwareUpdateState: " + firmwareUpdateState);
      firmwareUpdatePositiveUI(firmwareUpdateState);
    });
  }
}