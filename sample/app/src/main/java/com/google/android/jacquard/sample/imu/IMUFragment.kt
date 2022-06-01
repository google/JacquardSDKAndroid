/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sample.imu

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.dialog.DefaultDialog
import com.google.android.jacquard.sample.dialog.DefaultDialog.DefaultDialogBuilder
import com.google.android.jacquard.sample.dialog.DefaultDialog.DefaultDialogButtonClickListener
import com.google.android.jacquard.sample.fragment.extensions.changeStatusBarColor
import com.google.android.jacquard.sample.fragment.extensions.getViewModel
import com.google.android.jacquard.sample.fragment.extensions.showSnackBar
import com.google.android.jacquard.sample.imu.db.JqSessionInfo
import com.google.android.jacquard.sdk.dfu.FirmwareUpdateState
import com.google.android.jacquard.sdk.imu.ImuModule
import com.google.android.jacquard.sdk.imu.exception.DCException
import com.google.android.jacquard.sdk.lm.InitState
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.rx.Signal
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionMode
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionStatus
import java.io.File
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeoutException

class IMUFragment: Fragment(), View.OnClickListener {

  companion object {
    private val TAG = IMUFragment::class.java.simpleName
  }

  private val imuViewModel by lazy { getViewModel<ImuViewModel>() }
  private val imuSessionListAdapter by lazy { ImuSessionListAdapter(imuViewModel.getDownloadDirectory()) }
  private val sessionsObserver : Observer<List<JqSessionInfo>> by lazy { getSessionListObserver() }
  private val subscriptions = mutableListOf<Signal.Subscription>()
  private var currentSessionTimer: Timer? = null
  private var downloadSubscription: Signal.Subscription? = null

  private lateinit var progressLayout: LinearLayout
  private lateinit var startSession: Button
  private lateinit var stopSession: Button
  private lateinit var overflowButton: ImageButton
  private lateinit var pastSessionSection: LinearLayout
  private lateinit var sessionListView: RecyclerView
  private lateinit var progressMessage: TextView

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_imu, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()
    initToolbar()
    setupButtons()
    showProgress()
    observeDataCollection()
  }

  override fun onDestroyView() {
    hideProgress()
    imuViewModel.getSessionsList().removeObserver(sessionsObserver)
    imuSessionListAdapter.clear()
    for (subscription in subscriptions) {
      subscription.unsubscribe()
    }
    stopCurrentSessionTimer()
    subscriptions.clear()
    imuViewModel.destroy()
    super.onDestroyView()
  }

  override fun onClick(v: View) {
    when(v.id) {
      R.id.overflow -> {
        PrintLogger.d(TAG, "Overflow Clicked #")
        getBottomSheetDialog().show()
      }
      R.id.start_imu_session -> {
        PrintLogger.d(TAG, "Start Clicked #")
        startImuSession()
      }
      R.id.stop_imu_session -> {
        PrintLogger.d(TAG, "Stop Session Clicked #")
        stopImuSession()
      }
    }
  }
  
  private fun setupButtons() {
    startSession.setOnClickListener(this)
    stopSession.setOnClickListener(this)
    overflowButton.setOnClickListener(this)
  }

  private fun initToolbar() {
    requireView().findViewById<Toolbar>(R.id.toolbar)
      .setNavigationOnClickListener { imuViewModel.backKeyPressed() }
  }

  private fun initViews() {
    startSession = requireView().findViewById(R.id.start_imu_session)
    stopSession = requireView().findViewById(R.id.stop_imu_session)
    overflowButton = requireView().findViewById(R.id.overflow)
    progressLayout = requireView().findViewById(R.id.progress)
    pastSessionSection = requireView().findViewById(R.id.past_session_page_section)
    sessionListView = requireView().findViewById(R.id.session_list)
    progressMessage = requireView().findViewById(R.id.progress_message)
  }

  private fun updateProgressMessage(message: String) {
    progressMessage.text = message
  }

  private fun clearSessionTimerText() {
    requireView()
      .findViewById<View>(R.id.current_session_page_section)
      .findViewById<TextView>(R.id.session_timer)
      .setText(R.string.empty)
  }

  private fun observeDataCollection() {
    subscriptions.add(
      imuViewModel.init()
        .tap { state -> updateProgressMessage(getInitStateMessage(state)) }
        .filter { state -> state.isType(InitState.Type.INITIALIZED) }
        .flatMap {
          setUpSessionsList()
          return@flatMap imuViewModel.getDataCollectionStatus()
        }
        .first().observe(
          { dcStatus -> populateUi(dcStatus) }, { error ->
            error?.let { onFatalError(getErrorMessage(it)) }
          }
        )
    )
  }

  private fun getInitStateMessage(state: InitState): String {
    return when (state.type) {
      InitState.Type.INIT -> getString(R.string.imu_init_state_message)
      InitState.Type.CHECK_FOR_UPDATES -> getString(R.string.imu_check_updates_message)
      InitState.Type.ACTIVATE -> getString(R.string.imu_activating_message)
      InitState.Type.MODULE_DFU -> getDfuStatusMessage(state.moduleDfu())
      InitState.Type.TAG_DFU -> getDfuStatusMessage(state.tagDfu())
      InitState.Type.INITIALIZED -> getString(R.string.imu_initialize_complete_message)
    }
  }

  private fun getDfuStatusMessage(dfuState: FirmwareUpdateState): String {
    return when (dfuState.type) {
      FirmwareUpdateState.Type.IDLE -> getString(R.string.imu_dfu_idle_message)
      FirmwareUpdateState.Type.PREPARING_TO_TRANSFER -> getString(R.string.imu_dfu_ready_to_transfer_message)
      FirmwareUpdateState.Type.TRANSFER_PROGRESS -> getString(
        R.string.imu_dfu_progress_message,
        dfuState.transferProgress()
      )
      FirmwareUpdateState.Type.TRANSFERRED -> getString(R.string.imu_dfu_transfer_complete_message)
      FirmwareUpdateState.Type.EXECUTING -> getString(R.string.imu_dfu_applying_updates_message)
      FirmwareUpdateState.Type.COMPLETED -> getString(R.string.imu_dfu_updates_done_message)
      FirmwareUpdateState.Type.STOPPED -> getString(R.string.imu_dfu_updates_cancelled_message)
      FirmwareUpdateState.Type.ERROR -> getString(R.string.imu_dfu_error_message)
    }
  }

  private fun setUpSessionsList() {
    ContextCompat.getDrawable(requireContext(), R.drawable.list_view_divider)
      ?.let {
        val divider = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        divider.setDrawable(it)

        sessionListView.layoutManager = LinearLayoutManager(requireContext())
        sessionListView.addItemDecoration(divider)
        sessionListView.adapter = imuSessionListAdapter
        subscriptions.add(imuSessionListAdapter.actionSignal.onNext { pair -> handleUserClick(pair) })
      }
  }

  private fun onFatalError(message: String) {
    showSnackBar(message)
    subscriptions.add(Signal.from(1).delay(1000).onNext { imuViewModel.backKeyPressed() })
  }

  private fun populateUi(dcStatus: DataCollectionStatus) {
    PrintLogger.d(
      TAG,
      "Module init done in fragment # $dcStatus"
    )
    imuViewModel.getDataCollectionMode().observe(
      { dcMode ->
        PrintLogger.d(
          TAG,
          "DataCollection Mode # $dcMode"
        )

        if (dcMode == null ||
          DataCollectionMode.DATA_COLLECTION_MODE_STORE == dcMode ||
          dcStatus != DataCollectionStatus.DATA_COLLECTION_LOGGING) {

          updateCurrentSession(dcStatus)
          updateSessionButtons(dcStatus)
          updateMenus()

          if (dcStatus != DataCollectionStatus.DATA_COLLECTION_LOGGING) {
            observeUjtSessionList()
          } else {
            hideProgress()
            populateLocalSessionList()
          }
        } else {
          onFatalError("Ujt is collecting IMU Samples in $dcMode")
        }
      }, { error ->
        error?.let { onFatalError(getErrorMessage(it)) }
      }
    )
  }

  private fun updateCurrentSession() {
    imuViewModel.getDataCollectionStatus().onNext { status: DataCollectionStatus ->
      this.updateCurrentSession(
        status
      )
    }
  }

  private fun updateCurrentSession(status: DataCollectionStatus) {
    val currentSessionView = requireView().findViewById<View>(R.id.current_session_page_section)
    if (status != DataCollectionStatus.DATA_COLLECTION_LOGGING) {
      stopCurrentSessionTimer()
      currentSessionView.visibility = View.GONE
      return
    }
    currentSessionView.visibility = View.VISIBLE
    val sessionId = currentSessionView.findViewById<TextView>(R.id.session_id)
    imuViewModel.getCurrentSessionId().onNext { session ->
      PrintLogger.d(
        TAG,
        "Current Session Id # $session"
      )
      sessionId.text = session
      currentSessionTimer = Timer()
      currentSessionTimer!!.scheduleAtFixedRate(object : TimerTask() {
        override fun run() {
          currentSessionView.findViewById<TextView>(R.id.session_timer).text =
            DateUtils.formatElapsedTime(
              System.currentTimeMillis() / 1000 - session.toLong()
            )
        }
      }, 0L, 1000L)
    }
  }

  private fun updateSessionButtons(status: DataCollectionStatus) {
    if (status == DataCollectionStatus.DATA_COLLECTION_LOGGING) {
      startSession.visibility = View.GONE
      stopSession.visibility = View.VISIBLE
    } else {
      stopSession.visibility = View.GONE
      startSession.visibility = View.VISIBLE
    }
  }

  private fun observeUjtSessionList() {
    imuViewModel.getUjtSessionList().observe(
      {
        imuViewModel.getSessionsList().observeForever(sessionsObserver)
      } ,
      { error ->
        error?.let {
          hideProgress()
          populateLocalSessionList()
        }
      }
    )
  }

  private fun getSessionListObserver(): Observer<List<JqSessionInfo>> {
    return Observer { sessions: List<JqSessionInfo> ->
      PrintLogger.d(TAG,
                    "In Fragment # Sessions DB updated # ${sessions.size}"
      )
      imuSessionListAdapter.clear()
      for (info in sessions) {
        if (TextUtils.equals(info.tagSerialNumber, imuViewModel.getTagSerialNumber())) {
          PrintLogger.d(TAG,
                        "Session from db # $info"
          )
          imuSessionListAdapter.addSession(info)
        }
      }
      hideProgress()
      updateMenus()
    }
  }

  private fun hideProgress() {
    PrintLogger.d(TAG, "Hide Progress # $isAdded")
    if (isAdded) {
      changeStatusBarColor(R.color.white)
      progressLayout.visibility = View.GONE
    }
  }

  private fun showProgress() {
    PrintLogger.d(TAG, "Show Progress # $isAdded")
    if (isAdded) {
      updateProgressMessage(getString(R.string.please_wait))
      changeStatusBarColor(R.color.progress_overlay)
      progressLayout.visibility = View.VISIBLE
    }
  }

  private fun populateLocalSessionList() {
    imuViewModel.getDownloadDirectory().listFiles()?.let { downloaded ->
      imuViewModel.getSessionsList().observe(requireActivity()) { sessionList ->
        for (session in downloaded) {
          filter(
            sessionList, session.name.substring(0, session.name.lastIndexOf('.'))
          )?.let {
            imuSessionListAdapter.addSession(it)
          }
        }
        updateMenus()
      }
    }
  }

  private fun filter(list: List<JqSessionInfo>, sessionId: String): JqSessionInfo? {
    for (info in list) {
      if (TextUtils.equals(info.imuSessionId, sessionId)
        && TextUtils.equals(info.tagSerialNumber, imuViewModel.getTagSerialNumber())
      ) {
        return info
      }
    }
    return null
  }

  private fun stopCurrentSessionTimer() {
    currentSessionTimer?.cancel()
    clearSessionTimerText()
  }

  private fun getErrorMessage(error: Throwable): String {
    return if (error is TimeoutException) getString(R.string.tag_disconnected) else error.message
      ?: "Unknown Error"
  }

  private fun getErrorMessage(status: DataCollectionStatus): Int {
    return when (status) {
      DataCollectionStatus.DATA_COLLECTION_LOW_BATTERY -> R.string.imu_low_battery_message
      DataCollectionStatus.DATA_COLLECTION_LOW_STORAGE -> R.string.imu_low_storage_message
      else -> R.string.imu_start_session_generic_error
    }
  }

  private fun handleUserClick(event: Pair<Action, JqSessionInfo>) {
    when (event.first) {
      Action.DOWNLOAD -> downloadSessionData(event)
      Action.SHARE -> shareImuSamples(getSessionDataFilePath(event))
      Action.DELETE -> erase(event)
      Action.VIEW -> imuViewModel.viewImuSamples(getSessionDataFilePath(event))
    }
  }

  private fun getSessionDataFilePath(event: Pair<Action, JqSessionInfo>): String {
    return "${imuViewModel.getDownloadDirectory()}/${event.second.imuSessionId}${ImuModule.SESSION_FILE_EXTENSION}"
  }

  private fun erase(event: Pair<Action, JqSessionInfo>) {
    val eraseConfirmation = getEraseConfirmationDialog {
      showProgress()
      imuViewModel.erase(event.second).observe(
        { erased ->
          hideProgress()
          if (erased) {
            imuSessionListAdapter.removeSession(event.second)
            updateMenus()
          }
        },
        { error ->
          error?.let { handleError(it) }
        })
    }
    eraseConfirmation.show(parentFragmentManager, null)
  }

  private fun updateMenus() {
    PrintLogger.d(TAG, "session Item count ${imuSessionListAdapter.itemCount}")

    overflowButton.visibility =
      if (imuSessionListAdapter.itemCount == 0) View.GONE else View.VISIBLE

    pastSessionSection.visibility =
      if (imuSessionListAdapter.itemCount == 0) View.GONE else View.VISIBLE
  }

  private fun shareImuSamples(filePath: String) {
    val loggerFile = File(filePath)
    val fileUri = FileProvider.getUriForFile(
      requireContext(),
      /* authority= */ requireContext().packageName + ".provider",
      loggerFile
    )

    Intent().apply {
      action = Intent.ACTION_SEND
      type = "*/*"
      putExtra(Intent.EXTRA_TITLE, getString(R.string.imu_share_samples))
      putExtra(Intent.EXTRA_STREAM, fileUri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      startActivity(Intent.createChooser(this, getString(R.string.imu_share_samples)))
    }
  }

  private fun downloadSessionData(event: Pair<Action, JqSessionInfo>) {
    var downloadDialog: DefaultDialog? = null
    downloadDialog = getDownloadDialog {
      PrintLogger.d(TAG, "Download Cancelled # ")
      downloadSubscription?.unsubscribe()
      downloadDialog?.dialog?.dismiss()
      stopDataCollectionSession()
    }
    downloadDialog.show(parentFragmentManager, null)
    downloadSubscription = imuViewModel
      .downloadSession(JqSessionInfo.map(event.second))
      .observe({ progress ->
                 downloadDialog.updateProgress(progress.first)
                 if (progress.first == 100) {
                   showDownloadSnackBar(event.second.imuSessionId)
                   downloadDialog.dismiss()
                   // Force invalidate
                   sessionListView.adapter = imuSessionListAdapter
                 }
               },
               { error ->
                  error?.let {
                    handleError(it)
                  }
                 downloadDialog.dismiss()
                 downloadSubscription?.unsubscribe()
                 stopDataCollectionSession()
               })
    subscriptions.add(downloadSubscription!!)
  }

  private fun stopDataCollectionSession() {
    showProgress()
    imuViewModel.stopDataCollectionSession()
      .tapError { handleError(it) }
      .onNext { hideProgress() }
  }

  private fun handleError(error: Throwable) {
    hideProgress()
    error.printStackTrace()
    showSnackBar(
      if (error is DCException) getString(getErrorMessage(error.dataCollectionStatus)) else error?.message
        ?: "Unknown Error"
    )
  }

  private fun showDownloadSnackBar(sessionId: String) {
    showSnackBar(
      getString(R.string.imu_session_download_complete),
      getString(R.string.imu_session_download_download_action)
    ) { imuViewModel.viewImuSamples("${imuViewModel.getDownloadDirectory()}/$sessionId${ImuModule.SESSION_FILE_EXTENSION}") }
  }

  private fun getDownloadDialog(listener: () -> Unit): DefaultDialog {
    return DefaultDialogBuilder()
      .setCancellable(false)
      .setShowProgress(true)
      .setShowPositiveButton(true)
      .setPositiveButtonClick(object : DefaultDialogButtonClickListener(){
        override fun buttonClick() {
          listener.invoke()
        }
      })
      .setPositiveButtonTitleId(R.string.cancel)
      .setShowSubtitle(true)
      .setTitle(R.string.imu_session_download_title)
      .setSubtitle(R.string.imu_download_session_subtitle)
      .build()
  }

  private fun getEraseConfirmationDialog(listener: () -> Unit): DefaultDialog {
    return DefaultDialogBuilder()
      .setCancellable(false)
      .setShowPositiveButton(true)
      .setPositiveButtonTitleId(R.string.imu_delete)
      .setShowNegativeButton(true)
      .setNegativeButtonClick(null)
      .setNegativeButtonTitle(R.string.cancel)
      .setShowSubtitle(true)
      .setTitle(R.string.imu_delete_session_title)
      .setSubtitle(R.string.imu_delete_session_subtitle)
      .setPositiveButtonClick(object : DefaultDialogButtonClickListener() {
        override fun buttonClick() {
          listener.invoke()
        }
      })
      .build()
  }

  private fun getBottomSheetDialog(): BottomSheetDialog {
    val dialog = BottomSheetDialog(requireContext())
    dialog.setContentView(R.layout.imu_bottom_sheet)
    dialog.setCanceledOnTouchOutside(true)
    val deleteAll = dialog.findViewById<TextView>(R.id.delete_all_sessions)
    val cancel = dialog.findViewById<TextView>(R.id.cancel)
    deleteAll!!.setOnClickListener {
      PrintLogger.d(TAG, "Erase All clicked..")
      dialog.cancel()
      getEraseAllConfirmationDialog {
        showProgress()
        imuViewModel.eraseAll().observe(
          { erased ->
            hideProgress()
            if (erased) {
              imuSessionListAdapter.clear()
              updateMenus()
            }
          },
          { error ->
            error?.let { handleError(it) }
          })
      }.show(parentFragmentManager, /* tag= */ null)
    }
    cancel!!.setOnClickListener {
      PrintLogger.d(TAG, "Cancel clicked..")
      dialog.cancel()
    }
    return dialog
  }

  private fun getEraseAllConfirmationDialog(listener: () -> Unit): DefaultDialog {
    return DefaultDialogBuilder()
      .setCancellable(false)
      .setShowPositiveButton(true)
      .setPositiveButtonTitleId(R.string.imu_delete)
      .setShowNegativeButton(true)
      .setNegativeButtonClick(null)
      .setNegativeButtonTitle(R.string.cancel)
      .setShowSubtitle(true)
      .setTitle(R.string.imu_delete_all_sessions_title)
      .setSubtitle(R.string.imu_delete_all_session_subtitle)
      .setPositiveButtonClick(object : DefaultDialogButtonClickListener() {
        override fun buttonClick() {
          listener.invoke()
        }
      })
      .build()
  }

  private fun getErrorTitle(status: DataCollectionStatus): Int {
    return when (status) {
      DataCollectionStatus.DATA_COLLECTION_LOW_BATTERY -> R.string.imu_low_battery_title
      DataCollectionStatus.DATA_COLLECTION_LOW_STORAGE -> R.string.imu_low_storage_title
      else -> 0
    }
  }

  private fun getErrorDialog(title: Int, message: Int): DefaultDialog {
    return DefaultDialogBuilder()
      .setCancellable(false)
      .setShowPositiveButton(true)
      .setPositiveButtonTitleId(R.string.got_it)
      .setShowSubtitle(true)
      .setTitle(title)
      .setSubtitle(message)
      .build()
  }

  private fun stopImuSession() {
    currentSessionTimer?.cancel()
    showProgress()
    imuViewModel.stopSession().observe(
      { isStopped ->
        PrintLogger.d(TAG, "Stop session successful ? $isStopped")
        if (isStopped) {
          imuViewModel.getSessionsList().observeForever(sessionsObserver)
          updateCurrentSession()
          populateLocalSessionList()
          stopSession.visibility = View.GONE
          startSession.visibility = View.VISIBLE
          updateMenus()
        }
      },
      { error -> error?.let { handleError(it) } }
    )
  }

  private fun startImuSession() {
    showProgress()
    imuViewModel.startSession().observe(
      { id ->
        hideProgress()
        PrintLogger.d(TAG, String.format("IMU Session started. session id : $id"))
        if (!TextUtils.isEmpty(id)) {
          updateCurrentSession()
          startSession.visibility = View.GONE
          stopSession.visibility = View.VISIBLE
        }
      },
      { error ->
        hideProgress()
        error?.let {
          if (it !is DCException) {
            handleError(it)
            return@observe
          }

          val status = it.dataCollectionStatus
          if (status == DataCollectionStatus.DATA_COLLECTION_LOW_BATTERY ||
            status == DataCollectionStatus.DATA_COLLECTION_LOW_STORAGE
          ) {
            val message = getErrorMessage(status)
            val title: Int = getErrorTitle(status)
            getErrorDialog(title, message).show(parentFragmentManager, /* tag= */ null)
          } else {
            handleError(it)
          }
        }
      }
    )
  }
}