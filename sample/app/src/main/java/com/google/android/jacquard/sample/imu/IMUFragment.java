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
 */

package com.google.android.jacquard.sample.imu;

import static com.google.android.jacquard.sdk.imu.ImuModule.SESSION_FILE_EXTENSION;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sample.ViewModelFactory;
import com.google.android.jacquard.sample.dialog.DefaultDialog;
import com.google.android.jacquard.sample.dialog.DefaultDialog.DefaultDialogBuilder;
import com.google.android.jacquard.sample.dialog.DefaultDialog.DefaultDialogButtonClickListener;
import com.google.android.jacquard.sample.imu.ImuSessionListAdapter.Action;
import com.google.android.jacquard.sample.imu.db.JqSessionInfo;
import com.google.android.jacquard.sample.utilities.Util;
import com.google.android.jacquard.sdk.dfu.FirmwareUpdateState;
import com.google.android.jacquard.sdk.imu.InitState;
import com.google.android.jacquard.sdk.imu.InitState.Type;
import com.google.android.jacquard.sdk.imu.exception.DCException;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionMode;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionStatus;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

/**
 * Fragment for IMU apis.
 */
public class IMUFragment extends Fragment implements OnClickListener {

  private static final String TAG = IMUFragment.class.getSimpleName();
  private Button startSession, stopSession;
  private LinearLayout progressLayout;
  private ImuViewModel imuViewModel;
  private RecyclerView sessionListView;
  private ImuSessionListAdapter imuSessionListAdapter;
  private ImageButton overflowButton;
  private LinearLayout pastSessionSection;
  private Signal<Pair<Action, JqSessionInfo>> adapterActionSignal;
  private Subscription downloadSubscription;
  private final List<Subscription> subscriptions = new ArrayList<>();
  private Observer<List<JqSessionInfo>> sessionListObserver;
  private TextView progressMessage;
  private TextView sessionTimer;
  private Timer currentSessionTimer;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_imu, container, false);
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    imuViewModel =
        new ViewModelProvider(
            requireActivity(),
            new ViewModelFactory(requireActivity().getApplication(), getNavController()))
            .get(ImuViewModel.class);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    initToolbar();
    progressLayout = view.findViewById(R.id.progress);
    progressMessage = view.findViewById(R.id.progress_message);
    pastSessionSection = view.findViewById(R.id.past_session_page_section);
    setUpButtons(view);
    showProgress();
    subscriptions.add(imuViewModel.init()
        .tap(state -> updateProgressMessage(getInitStateMessage(state)))
        .filter(state -> state.isType(Type.INITIALIZED))
        .flatMap(ignore -> {
          setUpSessionsList(getView());
          return imuViewModel.getDataCollectionStatus();
        }).first()
        .observe(dcstatus -> populateUi(dcstatus),
            error -> {
              if (error != null) {
                onFatalError(
                    error instanceof TimeoutException ? getString(R.string.tag_disconnected)
                        : error.getMessage());
              }
            }));
  }

  private void populateUi(DataCollectionStatus dcstatus) {
    PrintLogger.d(TAG, "Module init done in fragment # " + dcstatus);
    imuViewModel.getDataCollectionMode().observe(dcMode -> {
      PrintLogger.d(TAG, "DataCollection Mode # " + dcMode);
      if (dcMode == null || DataCollectionMode.DATA_COLLECTION_MODE_STORE
          .equals(dcMode) || !dcstatus.equals(DataCollectionStatus.DATA_COLLECTION_LOGGING)) {
        updateCurrentSession(dcstatus);
        updateSessionButtons(dcstatus);
        updateMenus();
        if (!dcstatus.equals(DataCollectionStatus.DATA_COLLECTION_LOGGING)) {
          observeUjtSessionList();
        } else {
          hideProgress();
          populateLocalSessionList();
        }
      } else {
        onFatalError("Ujt is collecting IMU Samples in " + dcMode);
      }
    }, error -> {
      if (error != null) {
        onFatalError(
            error instanceof TimeoutException ? getString(R.string.tag_disconnected)
                : error.getMessage());
      }
    });
  }

  private void observeUjtSessionList() {
    imuViewModel.getUjtSessionList().observe(
        totalSessions ->
            imuViewModel.getSessionsList().observeForever(getSessionListObserver()),
        error -> {
          if (error != null) {
            hideProgress();
            populateLocalSessionList();
          }
        });
  }

  @Override
  public void onDestroyView() {
    hideProgress();
    if (sessionListObserver != null) {
      imuViewModel.getSessionsList().removeObserver(sessionListObserver);
    }
    for (Subscription subscription : subscriptions) {
      if (subscription != null) {
        subscription.unsubscribe();
      }
    }
    stopCurrentSessionTimer();
    subscriptions.clear();
    imuViewModel.destroy();
    super.onDestroyView();
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.overflow: {
        PrintLogger.d(TAG, "Overflow Clicked #");
        getBottomSheetDialog().show();
        break;
      }
      case R.id.start_imu_session: {
        PrintLogger.d(TAG, "Start Clicked #");
        startImuSession();
        break;
      }
      case R.id.stop_imu_session: {
        PrintLogger.d(TAG, "Stop Session Clicked #");
        stopImuSession();
        break;
      }
    }
  }

  private String getInitStateMessage(InitState state) {
    switch (state.getType()) {
      case INIT:
        return getString(R.string.imu_init_state_message);
      case CHECK_FOR_UPDATES:
        return getString(R.string.imu_check_updates_message);
      case ACTIVATE:
        return getString(R.string.imu_activating_message);
      case MODULE_DFU:
        return getDfuStatusMessage(state.moduleDfu());
      case TAG_DFU:
        return getDfuStatusMessage(state.tagDfu());
      case INITIALIZED:
        return getString(R.string.imu_initialize_complete_message);
    }
    return ""; // should never come here
  }

  private String getDfuStatusMessage(FirmwareUpdateState dfuState) {
    switch (dfuState.getType()) {
      case IDLE:
        return getString(R.string.imu_dfu_idle_message);
      case PREPARING_TO_TRANSFER:
        return getString(R.string.imu_dfu_ready_to_transfer_message);
      case TRANSFER_PROGRESS:
        return getString(R.string.imu_dfu_progress_message, dfuState.transferProgress());
      case TRANSFERRED:
        return getString(R.string.imu_dfu_transfer_complete_message);
      case EXECUTING:
        return getString(R.string.imu_dfu_applying_updates_message);
      case COMPLETED:
        return getString(R.string.imu_dfu_updates_done_message);
      case ERROR:
        return getString(R.string.imu_dfu_error_message);
    }
    return "";
  }

  private void updateCurrentSession() {
    imuViewModel.getDataCollectionStatus().onNext(this::updateCurrentSession);
  }

  private void updateCurrentSession(DataCollectionStatus status) {
    View currentSessionView = requireView().findViewById(R.id.current_session_page_section);
    if (!status.equals(DataCollectionStatus.DATA_COLLECTION_LOGGING)) {
      stopCurrentSessionTimer();
      currentSessionView.setVisibility(View.GONE);
      return;
    }
    currentSessionView.setVisibility(View.VISIBLE);
    TextView sessionId = currentSessionView.findViewById(R.id.session_id);
    sessionTimer = currentSessionView.findViewById(R.id.session_timer);
    imuViewModel.getCurrentSessionId().onNext(session -> {
      PrintLogger.d(TAG, "Current Session Id # " + session);
      sessionId.setText(session);
      currentSessionTimer = new Timer();
      currentSessionTimer.scheduleAtFixedRate(new TimerTask() {
        @Override
        public void run() {
          sessionTimer.setText(DateUtils.formatElapsedTime(
              System.currentTimeMillis() / 1000 - Long.parseLong(session)));
        }
      }, 0, 1000);
    });
  }

  private void stopCurrentSessionTimer() {
    if (currentSessionTimer != null) {
      currentSessionTimer.cancel();
    }
    if (sessionTimer != null) {
      sessionTimer.setText("");
    }
  }

  private void stopImuSession() {
    currentSessionTimer.cancel();
    showProgress();
    imuViewModel.stopSession().observe(stopped -> {
      PrintLogger.d(TAG, "Stop session successful ? " + stopped);
      if (stopped) {
        imuViewModel.getSessionsList().observeForever(getSessionListObserver());
        updateCurrentSession();
        populateLocalSessionList();
        stopSession.setVisibility(View.GONE);
        startSession.setVisibility(View.VISIBLE);
        updateMenus();
      }
    }, this::handleError);
  }

  private void startImuSession() {
    showProgress();
    imuViewModel.startSession().observe(id -> {
      hideProgress();
      PrintLogger.d(TAG, String.format("IMU Session started. session id : %s", id));
      if (!TextUtils.isEmpty(id)) {
        updateCurrentSession();
        startSession.setVisibility(View.GONE);
        stopSession.setVisibility(View.VISIBLE);
      }
    }, error -> {
      hideProgress();
      if (error instanceof DCException) {
        DataCollectionStatus status = ((DCException) error).getDataCollectionStatus();
        if (status.equals(DataCollectionStatus.DATA_COLLECTION_LOW_BATTERY) || status
            .equals(DataCollectionStatus.DATA_COLLECTION_LOW_STORAGE)) {
          int message = getErrorMessage(status);
          int title = getErrorTitle(status);
          getErrorDialog(title, message).show(getParentFragmentManager(), /* tag= */ null);
          return;
        }
      }
      handleError(error);
    });
  }

  private void updateSessionButtons(DataCollectionStatus dcstatus) {
    if (dcstatus.equals(DataCollectionStatus.DATA_COLLECTION_LOGGING)) {
      startSession.setVisibility(View.GONE);
      stopSession.setVisibility(View.VISIBLE);
    } else {
      stopSession.setVisibility(View.GONE);
      startSession.setVisibility(View.VISIBLE);
    }
  }

  private void setUpButtons(@NonNull View view) {
    startSession = view.findViewById(R.id.start_imu_session);
    stopSession = view.findViewById(R.id.stop_imu_session);
    overflowButton = view.findViewById(R.id.overflow);
    startSession.setOnClickListener(this);
    stopSession.setOnClickListener(this);
    overflowButton.setOnClickListener(this);
  }

  private void setUpSessionsList(@NonNull View view) {
    DividerItemDecoration divider =
        new DividerItemDecoration(getContext(),
            DividerItemDecoration.VERTICAL);

    divider.setDrawable(ContextCompat.getDrawable(getContext(),
        R.drawable.list_view_divider));

    sessionListView = view.findViewById(R.id.session_list);
    imuSessionListAdapter = new ImuSessionListAdapter(new ArrayList<>(),
        imuViewModel.getDownloadDirectory());
    sessionListView.setAdapter(imuSessionListAdapter);
    LinearLayoutManager recyclerLayoutManager = new LinearLayoutManager(getContext());
    sessionListView.setLayoutManager(recyclerLayoutManager);
    sessionListView.addItemDecoration(divider);
    adapterActionSignal = imuSessionListAdapter.getUserActionSignal();
    adapterActionSignal.onNext(this::handleUserClick);
  }

  private void handleUserClick(Pair<Action, JqSessionInfo> event) {
    switch (event.first) {
      case DOWNLOAD: {
        downloadSessionData(event);
      }
      break;
      case SHARE:
        shareImuSamples(
            getSessionDataFilePath(event));
        break;
      case DELETE:
        erase(event);
        break;
      case VIEW:
        viewImuSamples(getSessionDataFilePath(event));
        break;
    }
  }

  private String getSessionDataFilePath(Pair<Action, JqSessionInfo> event) {
    return imuViewModel.getDownloadDirectory() + "/" + event.second.imuSessionId
        + SESSION_FILE_EXTENSION;
  }

  private void downloadSessionData(Pair<Action, JqSessionInfo> event) {
    final DefaultDialog downloadDialog = getDownloadDialog(new DefaultDialogButtonClickListener() {
      @Override
      public void buttonClick() {
        PrintLogger.d(TAG, "Download Cancelled # ");
        downloadSubscription.unsubscribe();
      }
    });
    downloadDialog.show(getParentFragmentManager(), null);
    downloadSubscription = imuViewModel.downloadSession(JqSessionInfo.map(event.second))
        .observe(progress -> {
          downloadDialog.updateProgress(progress.first);
          if (progress.first == 100) {
            showDownloadSnackbar(event.second.imuSessionId);
            downloadDialog.dismiss();
            // Force invalidate
            sessionListView.setAdapter(imuSessionListAdapter);
          }
        }, error -> {
          if (error != null) {
            handleError(error);
          }
          downloadDialog.dismiss();
          if (downloadSubscription != null) {
            downloadSubscription.unsubscribe();
          }
        });
    subscriptions.add(downloadSubscription);
  }

  private void erase(Pair<Action, JqSessionInfo> event) {
    final DefaultDialog eraseConfirmation = getEraseConfirmationDialog(
        new DefaultDialogButtonClickListener() {
          @Override
          public void buttonClick() {
            showProgress();
            imuViewModel.erase(event.second).observe(erased -> {
              hideProgress();
              if (erased) {
                imuSessionListAdapter.removeSession(JqSessionInfo.map(event.second));
                updateMenus();
              }
            }, IMUFragment.this::handleError);
          }
        });
    eraseConfirmation.show(getParentFragmentManager(), null);
  }

  private void updateMenus() {
    overflowButton
        .setVisibility(imuSessionListAdapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
    pastSessionSection
        .setVisibility(imuSessionListAdapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
  }

  private DefaultDialog getErrorDialog(int title, int message) {
    return new DefaultDialogBuilder()
        .setCancellable(false)
        .setShowPositiveButton(true)
        .setPositiveButtonTitleId(R.string.got_it)
        .setShowSubtitle(true)
        .setTitle(title)
        .setSubtitle(message).build();
  }

  private DefaultDialog getDownloadDialog(
      DefaultDialogButtonClickListener listener) {
    return new DefaultDialogBuilder()
        .setCancellable(false)
        .setShowProgress(true)
        .setShowPositiveButton(true)
        .setPositiveButtonClick(listener)
        .setPositiveButtonTitleId(R.string.cancel)
        .setShowSubtitle(true)
        .setTitle(R.string.imu_session_download_title)
        .setSubtitle(R.string.imu_download_session_subtitle).build();
  }

  private DefaultDialog getEraseConfirmationDialog(
      DefaultDialogButtonClickListener listener) {
    return new DefaultDialogBuilder()
        .setCancellable(false)
        .setShowPositiveButton(true)
        .setPositiveButtonClick(listener)
        .setPositiveButtonTitleId(R.string.imu_delete)
        .setShowNegativeButton(true)
        .setNegativeButtonClick(null)
        .setNegativeButtonTitle(R.string.cancel)
        .setShowSubtitle(true)
        .setTitle(R.string.imu_delete_session_title)
        .setSubtitle(R.string.imu_delete_session_subtitle).build();
  }

  private DefaultDialog getEraseAllConfirmationDialog(
      DefaultDialogButtonClickListener listener) {
    return new DefaultDialogBuilder()
        .setCancellable(false)
        .setShowPositiveButton(true)
        .setPositiveButtonClick(listener)
        .setPositiveButtonTitleId(R.string.imu_delete)
        .setShowNegativeButton(true)
        .setNegativeButtonClick(null)
        .setNegativeButtonTitle(R.string.cancel)
        .setShowSubtitle(true)
        .setTitle(R.string.imu_delete_all_sessions_title)
        .setSubtitle(R.string.imu_delete_all_session_subtitle).build();
  }

  private void populateLocalSessionList() {
    File[] downloaded = imuViewModel.getDownloadDirectory().listFiles();
    if (downloaded == null) {
      return;
    }
    imuViewModel.getSessionsList().observe(requireActivity(), list -> {
      for (File session : downloaded) {
        JqSessionInfo info = filter(list,
            session.getName().substring(0, session.getName().lastIndexOf('.')));
        if (info != null) {
          imuSessionListAdapter.addSession(info);
        }
      }
      updateMenus();
    });
  }

  private JqSessionInfo filter(List<JqSessionInfo> list, String sessionId) {
    for (JqSessionInfo info : list) {
      if (TextUtils.equals(info.imuSessionId, sessionId) && TextUtils
          .equals(info.tagSerialNumber, imuViewModel.getTagSerialNumber())) {
        return info;
      }
    }
    return null;
  }

  private Observer<List<JqSessionInfo>> getSessionListObserver() {
    if (sessionListObserver == null) {
      sessionListObserver = sessions -> {
        PrintLogger.d(TAG, "In Fragment # Sessions DB updated # " + sessions.size());
        imuSessionListAdapter.clear();
        for (JqSessionInfo info : sessions) {
          if (TextUtils.equals(info.tagSerialNumber, imuViewModel.getTagSerialNumber())) {
            PrintLogger.d(TAG, "Session from db # " + info);
            imuSessionListAdapter.addSession(info);
          }
        }
        hideProgress();
        updateMenus();
      };
    }
    return sessionListObserver;
  }

  private void shareImuSamples(String filePath) {
    File loggerFile = new File(filePath);
    Uri fileUri = FileProvider
        .getUriForFile(requireContext(), requireContext().getPackageName() + ".provider",
            loggerFile);
    Intent sendIntent = new Intent();
    sendIntent.setAction(Intent.ACTION_SEND);
    sendIntent.setType("*/*");
    sendIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.imu_share_samples));
    sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
    sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    startActivity(Intent.createChooser(sendIntent, getString(R.string.imu_share_samples)));
  }

  private BottomSheetDialog getBottomSheetDialog() {
    BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
    dialog.setContentView(R.layout.imu_bottom_sheet);
    dialog.setCanceledOnTouchOutside(true);
    TextView deleteAll = dialog.findViewById(R.id.delete_all_sessions);
    TextView cancel = dialog.findViewById(R.id.cancel);
    deleteAll.setOnClickListener(v -> {
      PrintLogger.d(TAG, "Erase All clicked..");
      dialog.cancel();
      getEraseAllConfirmationDialog(new DefaultDialogButtonClickListener() {
        @Override
        public void buttonClick() {
          showProgress();
          imuViewModel.eraseAll().observe(erased -> {
            hideProgress();
            if (erased) {
              imuSessionListAdapter.clear();
              updateMenus();
            }
          }, IMUFragment.this::handleError);
        }
      }).show(getParentFragmentManager(), null);
    });
    cancel.setOnClickListener(v -> {
      PrintLogger.d(TAG, "Cancel clicked..");
      dialog.cancel();
    });
    return dialog;
  }

  private void onFatalError(String message) {
    Util.showSnackBar(getView(), message);
    subscriptions.add(Signal.from(1).delay(1000).onNext(ignore -> imuViewModel.backKeyPressed()));
  }

  private void handleError(Throwable error) {
    if (error != null) {
      hideProgress();
      error.printStackTrace();
      String message;
      if (error instanceof DCException) {
        message = getString(getErrorMessage(((DCException) error).getDataCollectionStatus()));
      } else {
        message = error.getMessage();
      }
      Util.showSnackBar(requireView(), message);
    }
  }

  private int getErrorMessage(DataCollectionStatus status) {
    switch (status) {
      case DATA_COLLECTION_LOW_BATTERY:
        return R.string.imu_low_battery_message;
      case DATA_COLLECTION_LOW_STORAGE:
        return R.string.imu_low_storage_message;
      default:
        return R.string.imu_start_session_generic_error;
    }
  }

  private int getErrorTitle(DataCollectionStatus status) {
    switch (status) {
      case DATA_COLLECTION_LOW_BATTERY:
        return R.string.imu_low_battery_title;
      case DATA_COLLECTION_LOW_STORAGE:
        return R.string.imu_low_storage_title;
      default:
        return 0;
    }
  }

  private void initToolbar() {
    Toolbar toolbar = getView().findViewById(R.id.toolbar);
    toolbar.setNavigationOnClickListener(v -> imuViewModel.backKeyPressed());
  }

  private void showProgress() {
    if (isAdded()) {
      updateProgressMessage(getString(R.string.please_wait));
      changeStatusBarColor(R.color.progress_overlay);
      progressLayout.setVisibility(View.VISIBLE);
    }
  }

  private void updateProgressMessage(String message) {
    progressMessage.setText(message);
  }

  private void hideProgress() {
    PrintLogger.d(TAG, "Hide Progress # ");
    if (isAdded()) {
      changeStatusBarColor(R.color.white);
      progressLayout.setVisibility(View.GONE);
    }
  }

  private void showDownloadSnackbar(String sessionId) {
    Util.showSnackBar(getView(), getString(R.string.imu_session_download_complete),
        getString(R.string.imu_session_download_download_action),
        v -> viewImuSamples(imuViewModel.getDownloadDirectory() + "/" + sessionId
            + SESSION_FILE_EXTENSION));
  }

  private void changeStatusBarColor(@ColorRes int color) {
    requireActivity().getWindow().setStatusBarColor(
        ContextCompat.getColor(requireContext(), color));
  }

  private void viewImuSamples(String path) {
    getNavController().navigate(IMUFragmentDirections
        .actionImuFragmentToImusamplesListFragment(path));
  }

  private NavController getNavController() {
    return NavHostFragment.findNavController(this);
  }
}
