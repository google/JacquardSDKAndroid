/*
 *
 *
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

package com.google.android.jacquard.sample.places

import android.view.View
import android.view.View.OnClickListener
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.places.GestureAdapter.ItemClickListener
import com.google.android.jacquard.sample.places.GestureItem.Companion.create
import com.google.android.jacquard.sample.utilities.AbilityConstants
import com.google.android.jacquard.sample.utilities.RecipeManager
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.model.Gesture.GestureType
import com.google.android.jacquard.sdk.rx.Signal
import com.google.auto.value.AutoOneOf


class PlacesConfigViewModel(
    private val navController: NavController, private val recipeManager: RecipeManager
) : ViewModel(), ItemClickListener, OnClickListener {

    companion object {
        private val TAG = PlacesConfigViewModel::class.java.simpleName
    }

    private var gestureItem: GestureItem? = null
    val stateSignal: Signal<State> = Signal.create()

    override fun onItemClick(gestureItem: GestureItem) {
        PrintLogger.d(TAG, "onItemClick() :: gesture item selected.")
        this@PlacesConfigViewModel.gestureItem = gestureItem
    }

    /** Emits a list of all supported gestures.  */
    fun getGestures(): List<GestureItem> {
        val recipe = recipeManager.getRecipeForAbility(AbilityConstants.PLACES_ABILITY)
        val assignedGestureId: Int = recipe?.gestureId ?: -1
        return mutableListOf(
            create(
                GestureType.BRUSH_IN.id == assignedGestureId,
                GestureType.BRUSH_IN.id,
                GestureType.BRUSH_IN.description
            ),
            create(
                GestureType.BRUSH_OUT.id == assignedGestureId,
                GestureType.BRUSH_OUT.id,
                GestureType.BRUSH_OUT.description
            ),
            create(
                GestureType.DOUBLE_TAP.id == assignedGestureId,
                GestureType.DOUBLE_TAP.id,
                GestureType.DOUBLE_TAP.description
            )
        )
    }

    /**
     * Handles assign ability button click.
     */
    fun assignAbility() {
        PrintLogger.d(TAG, "assignAbility() :: assigning places ability.")
        val recipe = recipeManager.getRecipeForAbility(AbilityConstants.PLACES_ABILITY)
        if (recipe != null) {
            PrintLogger.d(TAG, "Removing older recipe.")
            recipeManager.removeRecipe(recipe.gestureId)
        }
        recipeManager.addRecipe(AbilityConstants.PLACES_ABILITY, gestureItem!!.id)
        navController.navigate(
            PlacesConfigFragmentDirections
                .actionPlacesConfigFragmentToPlacesListFragment(gestureItem!!.id)
        )
        gestureItem = null
    }

    override fun onClick(v: View) {
        if (v.id == R.id.assignButton) {
            if (gestureItem == null) {
                stateSignal.next(State.ofError("Please select gesture to assign."))
                return
            }
            PrintLogger.d(TAG, "onClick() :: assigning places ability.")
            stateSignal.next(State.ofAssigned())
        }
    }

    /**
     * Handles the back arrow click in the toolbar.
     */
    fun backArrowClick() {
        navController.popBackStack()
    }

    @AutoOneOf(State.Type::class)
    abstract class State {
        abstract val type: Type

        abstract fun assigned()
        abstract fun error(): String
        enum class Type {
            ASSIGNED, ERROR
        }

        companion object {
            fun ofAssigned(): State {
                return AutoOneOf_PlacesConfigViewModel_State.assigned()
            }

            fun ofError(errorMsg: String): State {
                return AutoOneOf_PlacesConfigViewModel_State.error(errorMsg)
            }
        }
    }
}