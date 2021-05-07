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

import static com.google.android.jacquard.sdk.imu.ImuModule.SESSION_FILE_EXTENSION;

import android.text.format.DateFormat;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sample.imu.db.JqSessionInfo;
import com.google.android.jacquard.sdk.imu.model.ImuSessionInfo;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.rx.Signal;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for Imu Session List.
 */
public class ImuSessionListAdapter extends RecyclerView.Adapter<ImuSessionListAdapter.ViewHolder> {

  private static final String TAG = ImuSessionListAdapter.class.getSimpleName();

  /**
   * User actions on IMU Session List Item.
   */
  public enum Action {
    DOWNLOAD,
    SHARE,
    DELETE,
    VIEW
  }

  private Signal<Pair<Action, JqSessionInfo>> actionSignal = Signal.create();

  private final List<JqSessionInfo> sessionList = new ArrayList<>();

  private String downloadDirectory;

  public ImuSessionListAdapter(@NonNull List<JqSessionInfo> sessionList, File downloadDirectory) {
    this.sessionList.addAll(sessionList);
    this.downloadDirectory = downloadDirectory.getAbsolutePath();
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.imu_session_list_item, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    JqSessionInfo data = sessionList.get(position);
    long timestamp = Long.parseLong(data.imuSessionId) * 1000;
    String date = DateFormat.format("MMM dd, yyyy HH:mm:ss", timestamp).toString();
    holder.sessionId.setText(data.imuSessionId);
    holder.sessionStartTime.setText(date);
    File session = new File(downloadDirectory, data.imuSessionId + SESSION_FILE_EXTENSION);
    PrintLogger.d(TAG,
        "bind data # sessionId # " + data.imuSessionId + ", downloaded # " + session.length()
            + ", total size # " + data.imuSize);
    boolean isSessionDownloaded = session.exists() && session.length() == data.imuSize;
    holder.download.setVisibility(isSessionDownloaded ? View.GONE : View.VISIBLE);
    holder.share.setVisibility(isSessionDownloaded ? View.VISIBLE : View.GONE);
    holder.viewData.setVisibility(isSessionDownloaded ? View.VISIBLE : View.GONE);
  }

  @Override
  public int getItemCount() {
    return sessionList.size();
  }

  public void addSession(@NonNull JqSessionInfo data) {
    if (!sessionList.contains(data)) {
      sessionList.add(data);
      notifyDataSetChanged();
    }
  }

  public Signal<Pair<Action, JqSessionInfo>> getUserActionSignal() {
    return actionSignal;
  }

  public void addSessions(@NonNull List<JqSessionInfo> sessionList) {
    for (JqSessionInfo info : sessionList) {
      if (!this.sessionList.contains(info)) {
        this.sessionList.add(info);
      }
    }
    notifyDataSetChanged();
  }

  public void removeSession(ImuSessionInfo info) {
    this.sessionList.remove(info);
    notifyDataSetChanged();
  }

  public void clear() {
    sessionList.clear();
    notifyDataSetChanged();
  }

  public JqSessionInfo getSelectedTrialData(int position) {
    return sessionList.get(position);
  }

  /**
   * Viewholder class.
   */
  public class ViewHolder extends RecyclerView.ViewHolder {

    TextView sessionId, sessionStartTime;
    ImageButton download, share, delete, viewData;

    public ViewHolder(@NonNull View view) {
      super(view);
      sessionId = view.findViewById(R.id.session_id);
      sessionStartTime = view.findViewById(R.id.session_start_time);
      download = view.findViewById(R.id.download);
      download.setOnClickListener(v -> {
        PrintLogger.d(TAG, "Clicked on download # " + getAdapterPosition());
        JqSessionInfo sessionInfo = getSelectedTrialData(getAdapterPosition());
        actionSignal.next(Pair.create(Action.DOWNLOAD, sessionInfo));
      });
      share = view.findViewById(R.id.share);
      share.setOnClickListener(v -> {
        PrintLogger.d(TAG, "Clicked on share # " + getAdapterPosition());
        JqSessionInfo sessionInfo = getSelectedTrialData(getAdapterPosition());
        actionSignal.next(Pair.create(Action.SHARE, sessionInfo));
      });
      delete = view.findViewById(R.id.delete);
      delete.setOnClickListener(v -> {
        PrintLogger.d(TAG, "Clicked on delete # " + getAdapterPosition());
        JqSessionInfo sessionInfo = getSelectedTrialData(getAdapterPosition());
        actionSignal.next(Pair.create(Action.DELETE, sessionInfo));
      });
      viewData = view.findViewById(R.id.view_data);
      viewData.setOnClickListener(v -> {
        PrintLogger.d(TAG, "Clicked on viewData # " + getAdapterPosition());
        JqSessionInfo sessionInfo = getSelectedTrialData(getAdapterPosition());
        actionSignal.next(Pair.create(Action.VIEW, sessionInfo));
      });
    }
  }
}
