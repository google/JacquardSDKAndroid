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

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.*
import com.google.android.gms.maps.GoogleMap.OnMapClickListener
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.jacquard.sample.R
import java.text.SimpleDateFormat
import java.util.*

class PlacesListAdapter(
    private var placesItemList: List<PlacesItem>, private var itemClick: ItemClickListener
) : RecyclerView.Adapter<PlacesListAdapter.AbstractViewHolder>() {


    /**
     * Callback for click events.
     */
    interface ItemClickListener {
        fun onMoreOptionClick(placesItem: PlacesItem)
        fun onMapClick(placeItem: PlacesItem)
    }

    abstract class AbstractViewHolder(
        protected var context: Context, itemView: View,
        protected var itemClick: ItemClickListener
    ) : RecyclerView.ViewHolder(itemView) {
         val dateFormat = SimpleDateFormat(
            "hh:mm aa",
            Locale.getDefault()
        )
        var map: GoogleMap? = null
        abstract fun populate(model: PlacesItem)

    }

    private class SectionViewHolder(
        context: Context, itemView: View,
        itemClick: ItemClickListener
    ) :
        AbstractViewHolder(
            context, itemView,
            itemClick
        ) {
        private val textTitle: TextView = itemView.findViewById(R.id.sectionTxt)
        override fun populate(model: PlacesItem) {
            textTitle.text = model.title
        }
    }

    private class PlacesItemViewHolder(
        context: Context, itemView: View,
        itemClick: ItemClickListener
    ) :
        AbstractViewHolder(
            context, itemView,
            itemClick
        ),
        OnMapReadyCallback, OnMapClickListener {
        private val textTitle: TextView = itemView.findViewById(R.id.place_name)
        private val textSubTitle: TextView = itemView.findViewById(R.id.place_address)
        private val txtTime: TextView = itemView.findViewById(R.id.place_time)
        private val mapView: MapView = itemView.findViewById(R.id.places_map_view)
        private val moreOptions: ImageView = itemView.findViewById(R.id.item_more_options)
        override fun populate(model: PlacesItem) {
            textTitle.text = model.title
            textSubTitle.text = model.subTitle
            txtTime.text = dateFormat.format(Date(model.time))
            mapView.tag = model
            this.moreOptions.setOnClickListener {
                itemClick.onMoreOptionClick(
                    model
                )
            }
            setMapLocation()
        }

        override fun onMapReady(googleMap: GoogleMap) {
            MapsInitializer.initialize(context)
            map = googleMap
            map!!.setOnMapClickListener(this)
            setMapLocation()
        }

        override fun onMapClick(latLng: LatLng) {
            val placesItem = mapView.tag as PlacesItem
            itemClick.onMapClick(placesItem)
        }

        private fun setMapLocation() {
            map?.let { newMap ->
                val (_, latLng) = mapView.tag as PlacesItem
                newMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13f))
                newMap.addMarker(MarkerOptions().position(latLng))
                // Set the map type back to normal.
                newMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            }
        }

        init {
            mapView.onCreate(null)
            mapView.getMapAsync(this)
        }
    }

    /**
     * Creating ViewHolder for recyclerview in [PlacesListFragment].
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AbstractViewHolder {
        return if (PlacesItem.Type.values()[viewType] === PlacesItem.Type.PLACES_ITEM) {
            PlacesItemViewHolder(
                parent.context, LayoutInflater.from(parent.context)
                    .inflate(
                        R.layout.places_list_item,  /* root= */
                        parent,  /* attachToRoot= */
                        false
                    ),
                itemClick
            )
        } else SectionViewHolder(
            parent.context, LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.places_section_item,  /* root= */
                    parent,  /* attachToRoot= */
                    false
                ),
            itemClick
        )
    }


    /**
     * Binding ViewHolder for recyclerview in [PlacesListFragment].
     */
    override fun onBindViewHolder(holder: AbstractViewHolder, position: Int) {
        holder.populate(placesItemList[position])
    }

    /**
     * Returns the number of items.
     */
    override fun getItemCount(): Int {
        return placesItemList.size
    }

    /**
     * Returns the type of View.
     */
    override fun getItemViewType(position: Int): Int {
        return placesItemList[position].type.ordinal
    }
}