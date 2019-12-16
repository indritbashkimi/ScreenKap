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
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ibashkimi.screenrecorder.data.Recording
import com.ibashkimi.screenrecorder.databinding.ItemRecordingBinding
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow


class RecordingAdapter(items: List<Recording> = emptyList()) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val data: ArrayList<Recording> = ArrayList<Recording>(items.size).apply { addAll(items) }

    lateinit var selectionTracker: SelectionTracker<Recording>

    fun updateData(newData: List<Recording>) {
        data.clear()
        data.addAll(newData)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemRecordingBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)
        return RecordingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val recording = data[position]
        val recordingViewHolder = holder as RecordingViewHolder
        recordingViewHolder.bind(data[position], position, selectionTracker.isSelected(recording))
    }

    override fun getItemCount() = data.size
}

class RecordingViewHolder(private val binding: ItemRecordingBinding) : RecyclerView.ViewHolder(binding.root) {

    var recording: Recording? = null
    var pos: Int = -1

    fun bind(recording: Recording, position: Int, isSelected: Boolean) {
        this.recording = recording
        this.pos = position
        binding.apply {
            foreground.isVisible = isSelected
            thumbnail.load(recording.uri)
            title.text = recording.title
            duration.text = toTime(recording.duration.toLong())
            modified.text = DateUtils.getRelativeTimeSpanString(recording.modified)
            size.text = getFileSize(recording.size)
        }
    }

    fun getItemDetails(): ItemDetailsLookup.ItemDetails<Recording> = RecordingDetails(pos, recording!!)

    private fun ImageView.load(uri: Uri) {
        clipToOutline = true
        Glide.with(this)
                .asBitmap()
                .centerCrop()
                .load(uri)
                .into(this)
    }

    private fun getFileSize(size: Long): String {
        if (size <= 0)
            return "0"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
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
