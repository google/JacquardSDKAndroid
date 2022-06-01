package com.google.android.jacquard.sample.tagmanager

import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.google.android.jacquard.sample.ConnectivityManager
import com.google.android.jacquard.sample.KnownTag
import com.google.android.jacquard.sample.Preferences
import com.google.android.jacquard.sample.scan.AdapterItem
import com.google.android.jacquard.sdk.dfu.DFUInfo
import com.google.android.jacquard.sdk.dfu.DfuManager
import com.google.android.jacquard.sdk.dfu.FirmwareUpdateState
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.model.Component
import com.google.android.jacquard.sdk.model.JacquardError
import com.google.android.jacquard.sdk.rx.Signal
import com.google.auto.value.AutoOneOf

/**
 * A viewModel for fragment [TagManagerFragment].
 */
class TagManagerViewModel(
    private val connectivityManager: ConnectivityManager,
    private val preferences: Preferences,
    private val navController: NavController
) : ViewModel(), TagManagerAdapter.ItemClickListener {

    companion object {
        private val TAG = TagManagerViewModel::class.java.simpleName
    }

    @AutoOneOf(State.Type::class)
    abstract class State {
        enum class Type {
            ACTIVE,
            IDLE,
            NO_UPDATE,
            CHECK_FIRMWARE,
            FIRMWARE_STATE,
            UPDATE_COMPLETE,
            CONNECTED,
            DISCONNECTED,
        }

        abstract fun getType(): Type
        abstract fun active(): String
        abstract fun idle()
        abstract fun noUpdate()
        abstract fun checkFirmware()
        abstract fun firmwareState(): FirmwareUpdateState
        abstract fun updateComplete()
        abstract fun connected()
        abstract fun disconnected(): JacquardError

        companion object {
            fun ofActive(serialNumber: String): State {
                return AutoOneOf_TagManagerViewModel_State.active(serialNumber)
            }

            fun ofIdle(): State {
                return AutoOneOf_TagManagerViewModel_State.idle()
            }

            fun ofNoUpdate(): State {
                return AutoOneOf_TagManagerViewModel_State.noUpdate()
            }

            fun ofCheckFirmware(): State {
                return AutoOneOf_TagManagerViewModel_State.checkFirmware()
            }

            fun ofFirmwareState(fwState: FirmwareUpdateState): State {
                return AutoOneOf_TagManagerViewModel_State.firmwareState(fwState)
            }

            fun ofUpdateComplete(): State {
                return AutoOneOf_TagManagerViewModel_State.updateComplete()
            }

            fun ofConnected(): State {
                return AutoOneOf_TagManagerViewModel_State.connected()
            }

            fun ofDisconnected(error: JacquardError): State {
                return AutoOneOf_TagManagerViewModel_State.disconnected(error)
            }
        }
    }

    val stateSignal: Signal<State> = Signal.create()
    val fmUpdateStates = mutableMapOf<String, Signal<State>>()
    val isFirmwareUpdateInProgress
        get() = tagsFwUpdateProgressCount != 0
    private var tagsFwUpdateProgressCount = 0
    private val subscriptionConnectedTags = mutableListOf<Signal.Subscription>()

    override fun onItemClick(tag: KnownTag) {
        PrintLogger.d(TAG, "Launching Tag details screen.")
        navController
            .navigate(TagManagerFragmentDirections.actionTagManagerFragmentToTagDetailsFragment(tag))
    }

    override fun onTagSelect(tag: KnownTag) {
        PrintLogger.d(TAG, "onTagSelect tag: ${tag.address()}")
        preferences.putCurrentDevice(tag)
        stateSignal.next(State.ofActive(tag.pairingSerialNumber))
    }

    /**
     * Returns true if `tag` is current tag.
     *
     * @param tag current tag
     */
    fun isCurrentTag(tag: KnownTag): Boolean {
        return preferences.currentTag?.let {
            tag.address() == it.address()
        } ?: false
    }

    /**
     * Returns collection of previously known tags.
     */
    fun getKnownTagsSection(): List<AdapterItem> {
        val knownTagSection: MutableList<AdapterItem> = mutableListOf()
        for (knownTag in preferences.knownTags) {
            knownTagSection.add(AdapterItem.ofTag(knownTag))
            fmUpdateStates[knownTag.address()] = Signal.create()
        }
        return knownTagSection
    }

    /**
     * Handles back arrow in toolbar.
     */
    fun backArrowClick() {
        navController.popBackStack()
    }

    /**
     * Initiates devices scanning.
     */
    fun initiateScan() {
        val action = TagManagerFragmentDirections.actionTagManagerFragmentToScanFragment()
        action.isUserOnboarded = true
        navController.navigate(action)
    }

    override fun onCleared() {
        unSubscribeTagDFU()
    }

    fun updateAllTags() {
        unSubscribeTagDFU()
        tagsFwUpdateProgressCount = preferences.knownTags.size
        for (knownTag in preferences.knownTags) {
            subscriptionConnectedTags.add(connectivityManager.getConnectedTag(knownTag.address())
              .flatMap { tag ->
                  checkDFU(tag.dfuManager(), tag.components, tag.address())
                      .flatMap { listDFU -> applyUpdates(tag.dfuManager(), listDFU, tag.address()) }
              }.tapError { exception ->

                    if (exception is JacquardError && exception.type == JacquardError.Type.BLUETOOTH_CONNECTION_ERROR
                    ) {
                        // Handle disconnected UI, when the Tag is disconnected before CheckUpdate process.
                        PrintLogger.d(TAG, "Tag disconnected $exception")
                        emitTagNotConnected()
                    } else {
                        // Handle Error, when the Tag throws error while checkUpdate process.
                        // Error while applyUpdate will be wrapped in FirmwareUpdateState.ofError and sent to onNext.
                        PrintLogger.d(TAG, "checkUpdate Error $exception")
                        emitUpdateError()
                    }
                    fmUpdateStates[knownTag.address()]!!.next(
                        State.ofFirmwareState(
                            FirmwareUpdateState.ofError(exception)
                        )
                    )
                }.onNext { fwState ->
                    when (fwState.type) {
                        FirmwareUpdateState.Type.TRANSFER_PROGRESS,
                        FirmwareUpdateState.Type.EXECUTING -> {
                            fmUpdateStates[knownTag.address()]!!.next(
                                State.ofFirmwareState(
                                    fwState
                                )
                            )
                        }
                        FirmwareUpdateState.Type.STOPPED -> {
                            PrintLogger.d(TAG, "STOPPED")
                            fmUpdateStates[knownTag.address()]!!.next(State.ofFirmwareState(fwState))
                            emitUpdateStopped()
                        }
                        FirmwareUpdateState.Type.COMPLETED -> {
                            PrintLogger.d(TAG, "COMPLETED")
                            fmUpdateStates[knownTag.address()]!!.next(
                                State.ofFirmwareState(
                                    fwState
                                )
                            )
                            emitUpdateComplete()
                        }

                        FirmwareUpdateState.Type.ERROR -> {
                            PrintLogger.d(TAG, "ERROR ${fwState.error()}")
                            fmUpdateStates[knownTag.address()]!!.next(
                                State.ofFirmwareState(
                                    fwState
                                )
                            )
                            emitUpdateError()
                        }
                        else -> { /* Rest of the states are ignored. */
                        }
                    }
                })
        }
    }

    fun doneClick() {
        for (knownTag in preferences.knownTags) {
            fmUpdateStates[knownTag.address()]!!.next(State.ofIdle())
        }
    }

    private fun checkDFU(
        dfuManager: DfuManager,
        listComponent: List<Component>,
        address: String
    ): Signal<List<DFUInfo>> {
        return Signal.create { signal ->
            fmUpdateStates[address]!!.next(State.ofCheckFirmware())
            val listDFU = mutableListOf<DFUInfo>()
            val subscription = dfuManager.checkFirmware(
                listComponent,
                false
            ).flatMap { fwList ->
                listDFU.addAll(fwList)
                dfuManager.checkModuleUpdate(false)
            }.tapError { signal.error(it) }
                .onNext { mlList ->
                    listDFU.addAll(mlList)
                    signal.next(listDFU)
                }

            object : Signal.Subscription() {
                override fun onUnsubscribe() {
                    subscription.unsubscribe()
                }
            }
        }
    }

    private fun applyUpdates(dfuManager: DfuManager, listDFU: List<DFUInfo>, address: String): Signal<FirmwareUpdateState> {
        if (listDFU.isEmpty() || !isUpdateAvailable(listDFU)) {
            fmUpdateStates[address]!!.next(State.ofNoUpdate())
            emitNoUpdate()
            return Signal.empty()
        }

        return dfuManager.applyUpdates(
            listDFU,
            true
        )
    }

    private fun isUpdateAvailable(listDFU: List<DFUInfo>): Boolean {
        return listDFU.any { it.dfuStatus() != DFUInfo.UpgradeStatus.NOT_AVAILABLE }
    }

    private fun unSubscribeTagDFU() {
        for (subscription in subscriptionConnectedTags) {
            subscription.unsubscribe()
        }
    }

    fun stopUpdate() {
        for (knownTag in preferences.knownTags) {
            subscriptionConnectedTags.add(
                connectivityManager.getConnectedTag(knownTag.address()).onNext { tag ->
                    tag.dfuManager().stop()
                })
        }
    }

    private fun emitNoUpdate() {
        --tagsFwUpdateProgressCount
        checkIfProgressCompleteAndSendUpdateComplete()
    }

    private fun emitUpdateError() {
        --tagsFwUpdateProgressCount
        checkIfProgressCompleteAndSendUpdateComplete()
    }

    private fun emitUpdateComplete() {
        --tagsFwUpdateProgressCount
        checkIfProgressCompleteAndSendUpdateComplete()
    }

    private fun emitTagNotConnected() {
        --tagsFwUpdateProgressCount
        checkIfProgressCompleteAndSendUpdateComplete()
    }

    private fun emitUpdateStopped() {
        --tagsFwUpdateProgressCount
        checkIfProgressCompleteAndSendUpdateComplete(true)
    }

    private fun checkIfProgressCompleteAndSendUpdateComplete(isStoppedCalled:Boolean = false) {
        if (!isFirmwareUpdateInProgress && !isStoppedCalled) {
            // This will reset stop button to Done
            stateSignal.next(State.ofUpdateComplete())
        }
    }
}