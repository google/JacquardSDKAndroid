/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.jacquard.sample.places;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sample.places.PlacesItem.Type;
import com.google.android.jacquard.sample.places.PlacesListAdapter.AbstractViewHolder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for recyclerview in {@link PlacesListFragment}.
 */
public class PlacesListAdapter extends RecyclerView.Adapter<AbstractViewHolder> {

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("hh:mm aa",
      Locale.getDefault());
  private final List<PlacesItem> placesItemList;
  private final ItemClickListener itemClick;

  protected PlacesListAdapter(List<PlacesItem> placesItemList, ItemClickListener itemClick) {
    this.itemClick = itemClick;
    this.placesItemList = placesItemList;
  }

  /**
   * Creating ViewHolder for recyclerview in {@link PlacesListFragment}.
   */
  @NonNull
  @Override
  public AbstractViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    if (Type.values()[viewType] == Type.PLACES_ITEM) {
      return new PlacesItemViewHolder(parent.getContext(), LayoutInflater.from(parent.getContext())
          .inflate(R.layout.places_list_item, /* root= */ parent, /* attachToRoot= */ false),
          itemClick);
    }
    return new SectionViewHolder(parent.getContext(), LayoutInflater.from(parent.getContext())
        .inflate(R.layout.places_section_item, /* root= */ parent, /* attachToRoot= */ false),
        itemClick);
  }

  /**
   * Binding ViewHolder for recyclerview in {@link PlacesListFragment}.
   */
  @Override
  public void onBindViewHolder(@NonNull AbstractViewHolder holder, int position) {
    holder.populate(placesItemList.get(position));
  }

  /**
   * Returns the number of items.
   */
  @Override
  public int getItemCount() {
    return placesItemList.size();
  }

  /**
   * Returns the type of View.
   */
  @Override
  public int getItemViewType(int position) {
    return placesItemList.get(position).type().ordinal();
  }

  /**
   * Callback for click events.
   */
  interface ItemClickListener {

    void onMoreOptionClick(PlacesItem placesItem);

    void onMapClick(PlacesItem placeItem);
  }

  abstract static class AbstractViewHolder extends ViewHolder {

    protected ItemClickListener itemClick;
    protected Context context;
    protected GoogleMap map;
    ;

    public AbstractViewHolder(@NonNull Context context, @NonNull View itemView,
        ItemClickListener itemClick) {
      super(itemView);
      this.context = context;
      this.itemClick = itemClick;
    }

    abstract void populate(PlacesItem model);
  }

  private static class PlacesItemViewHolder extends AbstractViewHolder implements
      OnMapReadyCallback, OnMapClickListener {

    private final TextView textTitle, textSubTitle, txtTime;
    private final MapView mapView;
    private final ImageView moreOptions;

    PlacesItemViewHolder(@NonNull Context context, @NonNull View itemView,
        ItemClickListener itemClick) {
      super(context, itemView, itemClick);
      textTitle = itemView.findViewById(R.id.place_name);
      textSubTitle = itemView.findViewById(R.id.place_address);
      txtTime = itemView.findViewById(R.id.place_time);
      mapView = itemView.findViewById(R.id.places_map_view);
      moreOptions = itemView.findViewById(R.id.item_more_options);
      if (mapView != null) {
        mapView.onCreate(null);
        mapView.getMapAsync(this);
      }
    }

    @Override
    void populate(PlacesItem model) {
      textTitle.setText(model.title());
      textSubTitle.setText(model.subTitle());
      txtTime.setText(DATE_FORMAT.format(new Date(model.time())));
      mapView.setTag(model);
      moreOptions.setOnClickListener((v) -> itemClick.onMoreOptionClick(model));
      setMapLocation();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
      MapsInitializer.initialize(context);
      map = googleMap;
      map.setOnMapClickListener(this);
      setMapLocation();
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
      PlacesItem placesItem = (PlacesItem) mapView.getTag();
      itemClick.onMapClick(placesItem);
    }

    private void setMapLocation() {
      if (map == null) {
        return;
      }
      PlacesItem placesItem = (PlacesItem) mapView.getTag();
      map.moveCamera(CameraUpdateFactory.newLatLngZoom(placesItem.latLng(), 13f));
      map.addMarker(new MarkerOptions().position(placesItem.latLng()));

      // Set the map type back to normal.
      map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    }
  }

  private static class SectionViewHolder extends AbstractViewHolder {

    private final TextView textTitle;

    public SectionViewHolder(@NonNull Context context, @NonNull View itemView,
        ItemClickListener itemClick) {
      super(context, itemView, itemClick);
      textTitle = itemView.findViewById(R.id.sectionTxt);
    }

    void populate(PlacesItem model) {
      textTitle.setText(model.title());
    }
  }

}
