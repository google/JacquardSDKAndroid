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
package com.google.android.jacquard.sample.tagmanager;

import static com.google.android.jacquard.sample.scan.AdapterItem.Type.TAG;

import android.annotation.SuppressLint;
import android.content.IntentSender;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.google.android.jacquard.sample.KnownTag;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sample.scan.AdapterItem;
import com.google.android.jacquard.sample.tagmanager.TagManagerAdapter.AdapterItemViewHolder;
import com.google.android.jacquard.sdk.rx.Fn;
import com.google.android.jacquard.sdk.rx.Signal;

/**
 * A adapter for tag manager fragment {@link TagManagerFragment}.
 */
public class TagManagerAdapter extends ListAdapter<AdapterItem, AdapterItemViewHolder> {

  /**
   * Callback for click events.
   */
  interface ItemClickListener {

    /**
     * Callback for the tag has been clicked.
     *
     * @param tag known tag
     */
    void onItemClick(KnownTag tag);

    /**
     * Callback for the tag has been selected.
     *
     * @param tag known tag
     */
    void onTagSelect(KnownTag tag, Fn<IntentSender, Signal<Boolean>> senderHandler);
  }

  private final TagManagerViewModel viewModel;
  private final Fn<IntentSender, Signal<Boolean>> senderHandler;
  private static TagViewHolder selectedTagViewHolder;

  public TagManagerAdapter(final @NonNull Fn<IntentSender, Signal<Boolean>> senderHandler, TagManagerViewModel viewModel) {
    super(new DiffCallback());
    this.viewModel = viewModel;
    this.senderHandler = senderHandler;
  }

  @Override
  public AdapterItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    AdapterItem.Type type = AdapterItem.Type.values()[viewType];
    switch (type) {
      case TAG:
        return getTagViewHolder(parent);
    }
    throw new IllegalStateException("Unknown tag type: " + type);
  }

  @Override
  public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
    super.onDetachedFromRecyclerView(recyclerView);
    selectedTagViewHolder = null;
  }

  @Override
  public void onBindViewHolder(AdapterItemViewHolder holder, int position) {
    holder.bind(getItem(position), senderHandler);
  }

  @Override
  public int getItemViewType(int position) {
    return getItem(position).getType().ordinal();
  }

  private TagViewHolder getTagViewHolder(ViewGroup parent) {
    View view = getView(parent, R.layout.scan_list_tag);
    return new TagViewHolder(view, viewModel);
  }

  private static View getView(ViewGroup parent, @LayoutRes int layoutResId) {
    LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
    return layoutInflater.inflate(layoutResId, parent, /* attachToRoot= */false);
  }

  static class DiffCallback extends DiffUtil.ItemCallback<AdapterItem> {

    @Override
    public boolean areItemsTheSame(@NonNull AdapterItem oldItem,
        @NonNull AdapterItem newItem) {
      AdapterItem.Type oldItemType = oldItem.getType();
      if (oldItemType == TAG && newItem.getType() == TAG) {
        return oldItem.tag().identifier().equals(newItem.tag().identifier());
      }
      return false;
    }

    @SuppressLint("DiffUtilEquals")
    @Override
    public boolean areContentsTheSame(@NonNull AdapterItem oldItem,
        @NonNull AdapterItem newItem) {
      return oldItem.equals(newItem);
    }
  }

  abstract static class AdapterItemViewHolder extends ViewHolder {

    public AdapterItemViewHolder(@NonNull View itemView) {
      super(itemView);
    }

    abstract void bind(AdapterItem item, Fn<IntentSender, Signal<Boolean>> senderHandler);
  }

  static class TagViewHolder extends AdapterItemViewHolder {

    private final TextView displayName;
    private final TextView address;
    private final ImageView tagSelection;
    private final TagManagerViewModel viewModel;
    private final View layout;

    public TagViewHolder(View itemView, TagManagerViewModel viewModel) {
      super(itemView);
      displayName = itemView.findViewById(R.id.tag_name);
      address = itemView.findViewById(R.id.tag_identifier);
      tagSelection = itemView.findViewById(R.id.tag_selected_icon);
      layout = itemView.findViewById(R.id.scan_item_layout);
      this.viewModel = viewModel;
    }

    @Override
    void bind(AdapterItem item, Fn<IntentSender, Signal<Boolean>> senderHandler) {
      KnownTag tag = item.tag();
      displayName.setText(tag.displayName());
      address.setText(tag.pairingSerialNumber());
      setCurrentTagAsSelected(tag);
      tagSelection.setOnClickListener(v -> onTagSelect(senderHandler, tag, v));
      itemView.setOnClickListener(v -> {
        viewModel.onItemClick(tag);
      });
    }

    private void onTagSelect(Fn<IntentSender, Signal<Boolean>> senderHandler, KnownTag tag,
        View v) {
      if (selectedTagViewHolder != null) {
        selectedTagViewHolder.layout.setSelected(false);
      }
      selectedTagViewHolder = TagViewHolder.this;
      layout.setSelected(true);
      viewModel.onTagSelect(tag, senderHandler);
    }

    private void setCurrentTagAsSelected(KnownTag tag) {
      if (viewModel.isCurrentTag(tag)) {
        selectedTagViewHolder = TagViewHolder.this;
        layout.setSelected(true);
      }
    }
  }

}
