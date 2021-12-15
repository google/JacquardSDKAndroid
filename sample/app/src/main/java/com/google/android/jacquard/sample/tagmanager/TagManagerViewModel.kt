package com.google.android.jacquard.sample.tagmanager

import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.google.android.jacquard.sample.ConnectivityManager
import com.google.android.jacquard.sample.KnownTag
import com.google.android.jacquard.sample.Preferences
import com.google.android.jacquard.sample.scan.AdapterItem
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.rx.Fn
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
      ACTIVE
    }

    abstract fun getType(): Type
    abstract fun active(): String

    companion object {
      fun ofActive(serialNumber: String): State {
        return AutoOneOf_TagManagerViewModel_State.active(serialNumber)
      }
    }
  }

  val stateSignal: Signal<State> = Signal.create()

  override fun onItemClick(tag: KnownTag) {
    PrintLogger.d(TAG, "Launching Tag details screen.")
    navController
      .navigate(TagManagerFragmentDirections.actionTagManagerFragmentToTagDetailsFragment(tag))
  }

  override fun onTagSelect(tag: KnownTag, senderHandler: Fn<IntentSender, Signal<Boolean>>) {
    PrintLogger.d(TAG, "onTagSelect tag: ${tag.address()}")
    connectivityManager.connect(null, tag.address(), senderHandler)
    preferences.putCurrentDevice(tag)
    stateSignal.next(State.ofActive(tag.pairingSerialNumber()))
  }

  /**
   * Returns true if `tag` is current tag.
   *
   * @param tag current tag
   */
  fun isCurrentTag(tag: KnownTag): Boolean {
    return if (preferences.currentTag == null) false
    else tag.address() == preferences.currentTag.address()
  }

  /**
   * Returns collection of previously known tags.
   */
  fun getKnownTagsSection(): List<AdapterItem> {
    val knownTagSection: MutableList<AdapterItem> = mutableListOf()
    for (knownTag in preferences.knownTags) {
      knownTagSection.add(AdapterItem.ofTag(knownTag))
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
}