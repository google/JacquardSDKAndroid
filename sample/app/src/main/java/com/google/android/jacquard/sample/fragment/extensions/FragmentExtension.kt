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

package com.google.android.jacquard.sample.fragment.extensions

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.ViewModelFactory

inline fun <reified T : ViewModel> Fragment.getViewModel(): T {
  return ViewModelProvider(
    requireActivity(),
    ViewModelFactory(
      requireActivity().application,
      Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
    )
  ).get(T::class.java)
}


inline fun <reified T : ViewModel> AppCompatActivity.activityViewModels(): T {
  val navHostFragment =
    supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
  return ViewModelProvider(
    this,
    ViewModelFactory(
      application,
      navHostFragment.navController
    )
  ).get(T::class.java)
}