/*
 * Copyright (C) 2019 Indrit Bashkimi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibashkimi.screenrecorder.recordings

import android.net.Uri
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.ibashkimi.screenrecorder.R
import com.ibashkimi.screenrecorder.data.Recording
import java.text.DecimalFormat


class RecordingAdapter(items: List<Recording> = emptyList()) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val data: ArrayList<Recording> = ArrayList<Recording>(items.size).apply { addAll(items) }

    lateinit var selectionTracker: SelectionTracker<Recording>

    fun updateData(newData: List<Recording>) {
        data.clear()
        data.addAll(newData)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.item_recording, viewGroup, false)
        return RecordingViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val recording = data[position]
        val recordingViewHolder = holder as RecordingViewHolder
        recordingViewHolder.bind(data[position], position, selectionTracker.isSelected(recording))
    }

    override fun getItemCount() = data.size
}

class RecordingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val thumbnail: ImageView = view.findViewById(R.id.thumbnail)
    private val title: TextView = view.findViewById(R.id.title)
    private val duration: TextView = view.findViewById(R.id.duration)
    private val modified: TextView = view.findViewById(R.id.modified)
    private val size: TextView = view.findViewById(R.id.size)
    private val foreground: View = view.findViewById(R.id.foreground)

    var recording: Recording? = null
    var pos: Int = -1

    fun bind(recording: Recording, position: Int, isSelected: Boolean) {
        this.recording = recording
        this.pos = position
        this.foreground.isVisible = isSelected

        loadThumbnail(thumbnail, recording.uri)

        title.text = recording.title
        duration.text = toTime(recording.duration.toLong())
        modified.text = DateUtils.getRelativeTimeSpanString(recording.modified * 1000)
        size.text = getFileSize(recording.size)
    }

    fun getItemDetails(): ItemDetailsLookup.ItemDetails<Recording> = RecordingDetails(pos, recording!!)

    private fun loadThumbnail(imageView: ImageView, uri: Uri) {
        Glide.with(imageView)
                .asBitmap()
                .apply(RequestOptions().frame(-1))
                .centerCrop()
                .load(uri)
                .into(imageView)
    }

    private fun getFileSize(size: Long): String {
        if (size <= 0)
            return "0"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
    }

    private fun toTime(millis: Long): String {
        /*if (Build.VERSION.SDK_INT >= 26) {
            return LocalTime.ofSecondOfDay(millis / 1000).toString()
        }*/
        val hours: Long = (millis / (1000 * 60 * 60))
        val minutes = (millis % (1000 * 60 * 60) / (1000 * 60))
        val seconds = (millis % (1000 * 60 * 60) % (1000 * 60) / 1000)
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

}
