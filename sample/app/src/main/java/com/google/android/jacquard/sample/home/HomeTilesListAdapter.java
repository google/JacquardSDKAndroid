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

package com.google.android.jacquard.sample.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.DiffUtil.ItemCallback;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sample.home.HomeTilesListAdapter.AbstractViewHolder;

/** Adapter for recyclerview in {@link HomeFragment}. */
public class HomeTilesListAdapter extends ListAdapter<HomeTileModel, AbstractViewHolder> {

  private static final DiffUtil.ItemCallback<HomeTileModel> DIFF_CALLBACK =
      new ItemCallback<HomeTileModel>() {
        @Override
        public boolean areItemsTheSame(
            @NonNull HomeTileModel oldItem, @NonNull HomeTileModel newItem) {
          return oldItem.title() == newItem.title();
        }

        @Override
        public boolean areContentsTheSame(
            @NonNull HomeTileModel oldItem, @NonNull HomeTileModel newItem) {
          return oldItem.title() == newItem.title()
              && oldItem.type().equals(newItem.type())
              && oldItem.enabled() == newItem.enabled();
        }
      };
  private final Context context;
  private final ItemClickListener<HomeTileModel> itemClick;

  protected HomeTilesListAdapter(Context context, ItemClickListener<HomeTileModel> itemClick) {
    super(DIFF_CALLBACK);
    this.context = context;
    this.itemClick = itemClick;
  }

  /** Creating ViewHolder for recyclerview in {@link HomeFragment}. */
  @NonNull
  @Override
  public AbstractViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    switch (HomeTileModel.Type.values()[viewType]) {
      case TILE_API:
        return new TileTypeApiViewHolder(
            context,
            LayoutInflater.from(context)
                .inflate(
                    R.layout.item_product_tile_api, /* root= */ null, /* attachToRoot= */ false),
            itemClick,
            R.drawable.shape_tile_disabled,
            R.drawable.shape_tile_enabled,
            R.color.home_tile_semi_transparent_black,
            R.color.black);
      case TILE_SAMPLE_USE_CASE:
        return new TileTypeSampleUseCaseViewHolder(
            context,
            LayoutInflater.from(context)
                .inflate(
                    R.layout.item_product_tile_sample_use_case,
                    /* root= */ null,
                    /* attachToRoot= */ false),
            itemClick,
            R.drawable.shape_tile_disabled,
            R.drawable.shape_tile_enabled,
            R.color.home_tile_semi_transparent_black,
            R.color.black);
    }
    return new SectionViewHolder(
        context,
        LayoutInflater.from(context)
            .inflate(R.layout.item_product_section, /* root= */ null, /* attachToRoot= */ false),
        itemClick);
  }

  /** Binding ViewHolder for recyclerview in {@link HomeFragment}. */
  @Override
  public void onBindViewHolder(@NonNull AbstractViewHolder holder, int position) {
    holder.populate(getCurrentList().get(position));
  }

  /** Returns the number of items. */
  @Override
  public int getItemCount() {
    return getCurrentList().size();
  }

  /** Returns the type of View. */
  @Override
  public int getItemViewType(int position) {
    return getCurrentList().get(position).type().ordinal();
  }

  abstract static class AbstractViewHolder extends ViewHolder {

    protected ItemClickListener<HomeTileModel> itemClick;
    protected int drawableDisabled;
    protected int drawableEnabled;
    protected int textColorEnabled;
    protected int textColorDisabled;
    protected Context context;

    public AbstractViewHolder(
        @NonNull Context context,
        @NonNull View itemView,
        ItemClickListener<HomeTileModel> itemClick,
        int drawableDisabled,
        int drawableEnabled,
        int textColorDisabled,
        int textColorEnabled) {
      super(itemView);
      this.context = context;
      this.itemClick = itemClick;
      this.drawableDisabled = drawableDisabled;
      this.drawableEnabled = drawableEnabled;
      this.textColorEnabled = textColorEnabled;
      this.textColorDisabled = textColorDisabled;
    }

    abstract void populate(HomeTileModel model);
  }

  private static class TileTypeApiViewHolder extends AbstractViewHolder {

    private final TextView textTitle;
    private final TextView textSubTitle;

    TileTypeApiViewHolder(
        @NonNull Context context,
        @NonNull View itemView,
        ItemClickListener<HomeTileModel> itemClick,
        int drawableDisabled,
        int drawableEnabled,
        int textColorDisabled,
        int textColorEnabled) {
      super(
          context,
          itemView,
          itemClick,
          drawableDisabled,
          drawableEnabled,
          textColorDisabled,
          textColorEnabled);
      textTitle = itemView.findViewById(R.id.tileTitle);
      textSubTitle = itemView.findViewById(R.id.tileSubTitle);
    }

    @Override
    void populate(HomeTileModel model) {
      textTitle.setText(context.getString(model.title()));
      textSubTitle.setText(context.getString(model.subTitle()));
      if (!model.enabled()) {
        itemView.setBackgroundResource(drawableDisabled);
        textTitle.setTextColor(context.getColor(textColorDisabled));
        textSubTitle.setTextColor(context.getColor(textColorDisabled));
        return;
      }
      textTitle.setTextColor(context.getColor(textColorEnabled));
      textSubTitle.setTextColor(context.getColor(textColorEnabled));
      itemView.setBackgroundResource(drawableEnabled);
      itemView.setOnClickListener((v) -> itemClick.itemClick(model));
    }
  }

  private static class TileTypeSampleUseCaseViewHolder extends AbstractViewHolder {

    private final TextView textTitle;

    TileTypeSampleUseCaseViewHolder(
        @NonNull Context context,
        @NonNull View itemView,
        ItemClickListener<HomeTileModel> itemClick,
        int drawableDisabled,
        int drawableEnabled,
        int textColorDisabled,
        int textColorEnabled) {
      super(
          context,
          itemView,
          itemClick,
          drawableDisabled,
          drawableEnabled,
          textColorDisabled,
          textColorEnabled);
      textTitle = itemView.findViewById(R.id.tileTitle);
    }

    @Override
    void populate(HomeTileModel model) {
      textTitle.setText(context.getString(model.title()));
      if (!model.enabled()) {
        itemView.setBackgroundResource(drawableDisabled);
        textTitle.setTextColor(context.getColor(textColorDisabled));
        return;
      }
      textTitle.setTextColor(context.getColor(textColorEnabled));
      itemView.setBackgroundResource(drawableEnabled);
      itemView.setOnClickListener((v) -> itemClick.itemClick(model));
    }
  }

  private static class SectionViewHolder extends AbstractViewHolder {

    private final TextView textTitle;

    public SectionViewHolder(
        @NonNull Context context,
        @NonNull View itemView,
        ItemClickListener<HomeTileModel> itemClick) {
      super(
          context,
          itemView,
          itemClick,
          /* drawableDisabled= */ 0,
          /* drawableEnabled= */ 0,
          /* textColorDisabled= */ 0,
          /* textColorEnabled= */ 0);
      textTitle = itemView.findViewById(R.id.tileTitle);
    }

    void populate(HomeTileModel model) {
      textTitle.setText(context.getString(model.title()));
      if (!model.enabled()) {
        return;
      }
      itemView.setOnClickListener((v) -> itemClick.itemClick(model));
    }
  }
}
