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

package com.google.android.jacquard.sample.scan;

import static com.google.android.jacquard.sample.scan.AdapterItem.Type.SECTION_HEADER;
import static com.google.android.jacquard.sample.scan.AdapterItem.Type.TAG;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.google.android.jacquard.sample.KnownTag;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sample.scan.ScanAdapter.AdapterItemViewHolder;

/** Adapter for displaying {@link AdapterItem}. */
public class ScanAdapter extends ListAdapter<AdapterItem, AdapterItemViewHolder> {

  private AdapterItem previousSelectedItem;
  private final ItemClickListener itemClickListener;
  public ScanAdapter(ItemClickListener itemClickListener) {
    super(new DiffCallback());
    this.itemClickListener = itemClickListener;
  }

  private static View getView(ViewGroup parent, @LayoutRes int layoutResId) {
    LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
    return layoutInflater.inflate(layoutResId, parent, false);
  }

  @NonNull
  @Override
  public AdapterItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    AdapterItem.Type type = AdapterItem.Type.values()[viewType];
    switch (type) {
      case SECTION_HEADER:
        return getSectionViewHolder(parent);
      case TAG:
        return getTagViewHolder(parent);
    }
    throw new IllegalStateException("Unknown tag type: " + type);
  }

  private SectionHeaderViewHolder getSectionViewHolder(ViewGroup parent) {
    View view = getView(parent, R.layout.scan_list_section_header);
    return new SectionHeaderViewHolder(view);
  }

  private TagViewHolder getTagViewHolder(ViewGroup parent) {
    View view = getView(parent, R.layout.scan_list_tag);
    return new TagViewHolder(view, itemClickListener);
  }

  @Override
  public void onBindViewHolder(@NonNull AdapterItemViewHolder holder, int position) {
    holder.bind(getItem(position));
  }

  @Override
  public int getItemViewType(int position) {
    return getItem(position).getType().ordinal();
  }

  /** Callback for click events. */
  interface ItemClickListener {

    /** Callback for the a tag has been selected. */
    void onItemClick(KnownTag tag);
  }

  static class DiffCallback extends DiffUtil.ItemCallback<AdapterItem> {

    @Override
    public boolean areItemsTheSame(@NonNull AdapterItem oldItem,
        @NonNull AdapterItem newItem) {
      AdapterItem.Type oldItemType = oldItem.getType();
      AdapterItem.Type newItemType = newItem.getType();

      if (oldItemType == TAG && newItem.getType() == TAG) {
        return oldItem.tag().identifier().equals(newItem.tag().identifier());
      }

      if (oldItemType == SECTION_HEADER && newItemType == SECTION_HEADER) {
        return oldItem.sectionHeader().equals(newItem.sectionHeader());
      }
      return false;
    }

    @SuppressLint("DiffUtilEquals")
    @Override
    public boolean areContentsTheSame(@NonNull AdapterItem oldItem, @NonNull AdapterItem newItem) {
      return oldItem.equals(newItem);
    }
  }

  static class SectionHeaderViewHolder extends AdapterItemViewHolder {

    private final TextView headerTitle;

    public SectionHeaderViewHolder(@NonNull View itemView) {
      super(itemView);
      headerTitle = itemView.findViewById(R.id.header_title);
    }

    @Override
    void bind(AdapterItem item) {
      headerTitle.setText(item.sectionHeader());
    }
  }

  abstract static class AdapterItemViewHolder extends ViewHolder {

    public AdapterItemViewHolder(@NonNull View itemView) {
      super(itemView);
    }

    abstract void bind(AdapterItem item);
  }

  class TagViewHolder extends AdapterItemViewHolder {

    private final TextView displayName;
    private final TextView address;
    private final ItemClickListener itemClickListener;
    private final View layout;

    public TagViewHolder(View itemView, ItemClickListener itemClickListener) {
      super(itemView);
      displayName = itemView.findViewById(R.id.tag_name);
      address = itemView.findViewById(R.id.tag_identifier);
      layout = itemView.findViewById(R.id.scan_item_layout);
      this.itemClickListener = itemClickListener;
    }

    @Override
    void bind(AdapterItem item) {
      KnownTag tag = item.tag();
      displayName.setText(tag.displayName());
      address.setText(tag.pairingSerialNumber());
      layout.setSelected(item.isItemSelected);
      itemView.setOnClickListener(
          v -> {
            AdapterItem adapterItem = getItem(getAdapterPosition());
            if (adapterItem.isItemSelected) {
              return;
            }
            adapterItem.isItemSelected = true;
            if (previousSelectedItem != null) {
              previousSelectedItem.isItemSelected = false;
            }
            previousSelectedItem = adapterItem;
            itemClickListener.onItemClick(tag);
          });
    }
  }
}
