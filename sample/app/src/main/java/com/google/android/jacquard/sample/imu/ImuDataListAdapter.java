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
 */

package com.google.android.jacquard.sample.imu;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.jacquard.sample.R;
import com.google.atap.jacquard.protocol.JacquardProtocol.ImuSample;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter class for imu samples list.
 */
public class ImuDataListAdapter extends RecyclerView.Adapter<ImuDataListAdapter.ViewHolder> {

  private final List<ImuSample> imuSampleList = new ArrayList<>();

  public ImuDataListAdapter(@NonNull List<ImuSample> imuSampleList) {
    this.imuSampleList.addAll(imuSampleList);
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.imu_sample_list_item, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    ImuSample imu = imuSampleList.get(position);
    holder.imuText
        .setText("Accx: " + imu.getAccX() + ", AccY: " + imu.getAccY() + ", AccZ: " + imu.getAccZ()
            + "\nGyroPitch: " + imu.getGyroPitch() + ", GyroRoll: " + imu.getGyroRoll()
            + ", GyroYaw: " + imu.getGyroYaw());
  }

  public void add(@NonNull List<ImuSample> imuSampleList) {
    this.imuSampleList.addAll(imuSampleList);
    notifyDataSetChanged();
  }

  @Override
  public int getItemCount() {
    return imuSampleList.size();
  }

  /**
   * View holder class.
   */
  class ViewHolder extends RecyclerView.ViewHolder {

    TextView imuText;

    public ViewHolder(@NonNull View view) {
      super(view);
      imuText = view.findViewById(R.id.imu_sample_textview);
    }
  }
}
