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

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ibashkimi.screenrecorder.R
import com.ibashkimi.screenrecorder.data.Recording


abstract class RecordingListFragment : Fragment() {

    private lateinit var messageView: TextView

    private lateinit var recordingsAdapter: RecordingAdapter

    protected lateinit var selectionTracker: SelectionTracker<Recording>

    protected val viewModel: RecordingsViewModel by viewModels()

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(layoutRes, container, false)

        messageView = root.findViewById(R.id.message_no_video)
        messageView.visibility = View.GONE

        root.findViewById<RecyclerView>(R.id.videos_list).apply {
            val linearLayoutManager = LinearLayoutManager(context)
            layoutManager = linearLayoutManager
            recordingsAdapter = RecordingAdapter()
            adapter = recordingsAdapter
            selectionTracker = SelectionTracker.Builder(
                "recording-selection-id",
                this,
                RecordingKeyProvider(recordingsAdapter),
                RecordingDetailsLookup(this),
                StorageStrategy.createParcelableStorage(Recording::class.java)
            )
                .withOnItemActivatedListener { item, _ ->
                    onRecordingClick(item.selectionKey!!)
                    return@withOnItemActivatedListener true
                }
                .build()
            savedInstanceState?.let { selectionTracker.onRestoreInstanceState(it) }
            recordingsAdapter.selectionTracker = selectionTracker
        }

        viewModel.recordings.observe(viewLifecycleOwner, Observer {
            onDataLoaded(it)
        })

        return root
    }

    abstract val layoutRes: Int

    protected open fun onRecordingClick(recording: Recording) {
        val intent = Intent()
        intent.setAction(Intent.ACTION_VIEW)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .setDataAndType(
                recording.uri,
                requireContext().contentResolver.getType(recording.uri)
            )
        startActivity(intent)
    }

    protected fun onDataLoaded(data: List<Recording>) {
        messageView.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
        recordingsAdapter.updateData(data)
    }

    protected fun rename(recording: Recording, newName: String) {
        viewModel.rename(recording, newName)
    }

    protected fun delete(recording: Recording) {
        viewModel.deleteRecording(recording)
    }

    protected fun delete(recordings: List<Recording>) {
        viewModel.deleteRecordings(recordings)
    }
}
