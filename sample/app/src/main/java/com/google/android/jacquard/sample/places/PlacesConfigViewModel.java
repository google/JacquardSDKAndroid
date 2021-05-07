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

package com.google.android.jacquard.sample.places;

import static com.google.android.jacquard.sdk.model.Gesture.GestureType.BRUSH_IN;
import static com.google.android.jacquard.sdk.model.Gesture.GestureType.BRUSH_OUT;
import static com.google.android.jacquard.sdk.model.Gesture.GestureType.DOUBLE_TAP;

import android.view.View;
import android.view.View.OnClickListener;
import androidx.lifecycle.ViewModel;
import androidx.navigation.NavController;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sample.places.GestureAdapter.ItemClickListener;
import com.google.android.jacquard.sample.utilities.AbilityConstants;
import com.google.android.jacquard.sample.utilities.Recipe;
import com.google.android.jacquard.sample.utilities.RecipeManager;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.auto.value.AutoOneOf;
import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for Places ability screen.
 */
public class PlacesConfigViewModel extends ViewModel implements ItemClickListener, OnClickListener {

  private static final String TAG = PlacesConfigViewModel.class.getSimpleName();
  public final Signal<State> stateSignal = Signal.create();
  private final NavController navController;
  private final RecipeManager recipeManager;
  private final List<Subscription> subscriptions = new ArrayList<>();
  private GestureItem gestureItem;

  public PlacesConfigViewModel(NavController navController, RecipeManager recipeManager) {
    this.navController = navController;
    this.recipeManager = recipeManager;
  }

  /** Emits a list of all supported gestures. */
  public List<GestureItem> getGestures() {
    Recipe recipe = recipeManager.getRecipeForAbility(AbilityConstants.PLACES_ABILITY);
    final int assignedGestureId = recipe != null ? recipe.gestureId(): -1;
    return new ArrayList<GestureItem>() {
      {
        add(GestureItem.create(BRUSH_IN.getId() == assignedGestureId,
            BRUSH_IN.getId(), BRUSH_IN.getDescription()));
        add(GestureItem.create(BRUSH_OUT.getId() == assignedGestureId,
            BRUSH_OUT.getId(), BRUSH_OUT.getDescription()));
        add(GestureItem.create(DOUBLE_TAP.getId() == assignedGestureId,
            DOUBLE_TAP.getId(), DOUBLE_TAP.getDescription()));
      }
    };
  }

  /**
   * Handles the back arrow click in the toolbar.
   */
  public void backArrowClick() {
    navController.popBackStack();
  }

  /**
   * Handles assign ability button click.
   */
  public void assignAbility() {
    PrintLogger.d(TAG, "assignAbility() :: assigning places ability.");
    Recipe recipe = recipeManager.getRecipeForAbility(AbilityConstants.PLACES_ABILITY);
    if (recipe != null) {
      PrintLogger.d(TAG, "Removing older recipe.");
      recipeManager.removeRecipe(recipe.gestureId());
    }
    recipeManager.addRecipe(AbilityConstants.PLACES_ABILITY, gestureItem.id);
    navController.navigate(
        PlacesConfigFragmentDirections
            .actionPlacesConfigFragmentToPlacesListFragment(gestureItem.id));
    gestureItem = null;
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    for (Subscription subscription : subscriptions) {
      subscription.unsubscribe();
    }
    subscriptions.clear();
  }

  @Override
  public void onItemClick(GestureItem gestureItem) {
    PrintLogger.d(TAG, "onItemClick() :: gesture item selected.");
    this.gestureItem = gestureItem;
  }

  @Override
  public void onClick(View v) {
    if (v.getId() == R.id.assignButton) {
      if (gestureItem == null) {
        stateSignal.next(State.ofError("Please select gesture to assign."));
        return;
      }
      PrintLogger.d(TAG, "onClick() :: assigning places ability.");
      stateSignal.next(State.ofAssigned());
    }
  }

  @AutoOneOf(PlacesConfigViewModel.State.Type.class)
  public abstract static class State {

    public static PlacesConfigViewModel.State ofAssigned() {
      return AutoOneOf_PlacesConfigViewModel_State.assigned();
    }

    public static PlacesConfigViewModel.State ofError(String errorMsg) {
      return AutoOneOf_PlacesConfigViewModel_State.error(errorMsg);
    }

    public abstract State.Type getType();

    abstract void assigned();

    abstract String error();

    public enum Type {
      ASSIGNED,
      ERROR
    }
  }
}
