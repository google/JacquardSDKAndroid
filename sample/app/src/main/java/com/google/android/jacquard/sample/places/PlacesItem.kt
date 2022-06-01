package com.google.android.jacquard.sample.places

import com.google.android.gms.maps.model.LatLng

/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Model to hold data for places item.
 */
data class PlacesItem(
    var id: Int,
    var latLng: LatLng,
    var title: String,
    var subTitle: String,
    var time: Long,
    var type: Type
) {
    /**
     * Enum type for [PlacesItem].
     */
    enum class Type {
        SECTION, PLACES_ITEM
    }
}