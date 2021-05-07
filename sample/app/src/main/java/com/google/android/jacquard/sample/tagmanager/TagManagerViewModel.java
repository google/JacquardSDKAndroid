package com.google.android.jacquard.sample.tagmanager;

import androidx.lifecycle.ViewModel;
import androidx.navigation.NavController;
import com.google.android.jacquard.sample.ConnectivityManager;
import com.google.android.jacquard.sample.KnownTag;
import com.google.android.jacquard.sample.Preferences;
import com.google.android.jacquard.sample.scan.AdapterItem;
import com.google.android.jacquard.sample.tagmanager.TagManagerAdapter.ItemClickListener;
import com.google.android.jacquard.sample.tagmanager.TagManagerFragmentDirections.ActionTagManagerFragmentToScanFragment;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.auto.value.AutoOneOf;
import java.util.ArrayList;
import java.util.List;

/**
 * A viewmodel for fragment {@link TagManagerFragment}.
 */
public class TagManagerViewModel extends ViewModel implements ItemClickListener {

  private static final String TAG = TagManagerViewModel.class.getSimpleName();

  @AutoOneOf(State.Type.class)
  public static abstract class State {

    public enum Type {
      ACTIVE
    }

    public abstract Type getType();

    public abstract String active();

    public static State ofActive(String serialNumber) {
      return AutoOneOf_TagManagerViewModel_State.active(serialNumber);
    }
  }

  public final Signal<State> stateSignal = Signal.create();
  private final NavController navController;
  private final Preferences preferences;
  private final ConnectivityManager connectivityManager;

  public TagManagerViewModel(
      ConnectivityManager connectivityManager,
      Preferences preferences,
      NavController navController) {
    this.connectivityManager = connectivityManager;
    this.navController = navController;
    this.preferences = preferences;
  }

  @Override
  public void onItemClick(KnownTag tag) {
    PrintLogger.d(TAG, "Launching Tag details screen.");
    navController
        .navigate(TagManagerFragmentDirections.actionTagManagerFragmentToTagDetailsFragment(tag));
  }

  @Override
  public void onTagSelect(KnownTag tag) {
    PrintLogger.d(TAG, "onTagSelect tag: " + tag.identifier());
    connectivityManager.connect(tag.identifier());
    preferences.putCurrentDevice(tag);
    stateSignal.next(State.ofActive(tag.pairingSerialNumber()));
  }

  /**
   * Returns true if {@code tag} is current tag.
   *
   * @param tag current tag
   */
  public boolean isCurrentTag(KnownTag tag) {
    if (preferences.getCurrentTag() == null) {
      return false;
    }
    return tag.identifier().equals(preferences.getCurrentTag().identifier());
  }

  /**
   * Returns collection of previously known tags.
   */
  public List<AdapterItem> getKnownTagsSection() {
    List<AdapterItem> knownTagSection = new ArrayList<>();
    List<KnownTag> knownTags = preferences.getKnownTags();
    for (KnownTag knownTag : knownTags) {
      knownTagSection.add(AdapterItem.ofTag(knownTag));
    }
    return knownTagSection;
  }

  /**
   * Handles back arrow in toolbar.
   */
  public void backArrowClick() {
    navController.popBackStack();
  }

  /**
   * Initiates devices scanning.
   */
  public void initiateScan() {
    ActionTagManagerFragmentToScanFragment action = TagManagerFragmentDirections
        .actionTagManagerFragmentToScanFragment();
    action.setIsUserOnboarded(true);
    navController.navigate(action);
  }
}
