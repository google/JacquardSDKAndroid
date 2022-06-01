/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sample.imu

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.util.Pair
import androidx.recyclerview.widget.RecyclerView
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.imu.db.JqSessionInfo
import com.google.android.jacquard.sdk.imu.ImuModule
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.rx.Signal
import java.io.File

/** User actions on IMU Session List Item. */
enum class Action {
    DOWNLOAD,
    SHARE,
    DELETE,
    VIEW
}

/** View holder for [ImuSessionListAdapter]. */
class SessionListViewHolder(
    view: View,
    private val actionSignal: Signal<Pair<Action, JqSessionInfo>>,
    private val sessionList: List<JqSessionInfo>
) :
    RecyclerView.ViewHolder(view) {

    companion object {
        private val TAG = SessionListViewHolder::class.java.simpleName
    }

    val sessionId: TextView = view.findViewById(R.id.session_id)
    val sessionStartTime: TextView = view.findViewById(R.id.session_start_time)

    val download: ImageButton = view.findViewById(R.id.download)
    val share: ImageButton = view.findViewById(R.id.share)
    val delete: ImageButton = view.findViewById(R.id.delete)
    val viewData: ImageButton = view.findViewById(R.id.view_data)

    private val sendData: (action: Action) -> Unit = { action ->
        sessionList[position].let {
            PrintLogger.d(
                TAG,
                "Clicked on $action at# $adapterPosition"
            )
            actionSignal.next(
                Pair(
                    action,
                    it
                )
            )
        }
    }

    init {
        download.setOnClickListener { sendData(Action.DOWNLOAD) }
        share.setOnClickListener { sendData(Action.SHARE) }
        delete.setOnClickListener { sendData(Action.DELETE) }
        viewData.setOnClickListener { sendData(Action.VIEW) }
    }

}

/** Recyclerview adapter used in fragment [IMUFragment]. */
class ImuSessionListAdapter(downloadDirectory: File) :
    RecyclerView.Adapter<SessionListViewHolder>() {

    companion object {
        private val TAG = ImuSessionListAdapter::class.simpleName
    }

    val actionSignal: Signal<Pair<Action, JqSessionInfo>> =
        Signal.create()

    private val sessionList = mutableListOf<JqSessionInfo>()
    private val downloadDirectory: String = downloadDirectory.absolutePath

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionListViewHolder {
        return LayoutInflater.from(parent.context)
            .inflate(R.layout.imu_session_list_item, parent, false)
            .let { SessionListViewHolder(it, actionSignal, sessionList) }
    }

    override fun onBindViewHolder(holder: SessionListViewHolder, position: Int) {
        val data = sessionList[position]
        holder.sessionId.text = data.imuSessionId
        holder.sessionStartTime.text =
            DateFormat.format(/* inFormat= */"MMM dd, yyyy HH:mm:ss",
                data.imuSessionId.toLong() * 1000
            )
                .toString()
        val session =
            File(downloadDirectory, data.imuSessionId + ImuModule.SESSION_FILE_EXTENSION)
        PrintLogger.d(
            TAG,
            "bind data # sessionId # ${data.imuSessionId}, downloaded # ${session.length()} , total size # ${data.imuSize}"
        )

        val isSessionDownloaded = session.exists() && session.length() == data.imuSize.toLong()
        holder.download.visibility = if (isSessionDownloaded) View.GONE else View.VISIBLE
        holder.share.visibility = if (isSessionDownloaded) View.VISIBLE else View.GONE
        holder.viewData.visibility = if (isSessionDownloaded) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int = sessionList.size

    fun addSession(data: JqSessionInfo) {
        if (!sessionList.contains(data)) {
            sessionList.add(data)
            notifyDataSetChanged()
        }
    }

    fun removeSession(data: JqSessionInfo) {
        sessionList.remove(data)
        notifyDataSetChanged()
    }

    fun clear() {
        sessionList.clear()
        notifyDataSetChanged()
    }
}