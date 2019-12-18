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

import android.view.MotionEvent
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.widget.RecyclerView
import com.ibashkimi.screenrecorder.data.Recording

class RecordingDetails(private val adapterPosition: Int, val recording: Recording) :
    ItemDetailsLookup.ItemDetails<Recording>() {
    override fun getSelectionKey(): Recording? {
        return recording
    }

    override fun getPosition(): Int {
        return adapterPosition
    }
}

class RecordingDetailsLookup(private val recyclerView: RecyclerView) :
    ItemDetailsLookup<Recording>() {
    override fun getItemDetails(e: MotionEvent): ItemDetails<Recording>? {
        return recyclerView.findChildViewUnder(e.x, e.y)?.let {
            (recyclerView.getChildViewHolder(it) as? RecordingViewHolder)?.getItemDetails()
        }
    }
}

class RecordingKeyProvider(var adapter: RecordingAdapter) :
    ItemKeyProvider<Recording>(SCOPE_CACHED) {
    override fun getKey(position: Int): Recording? {
        return adapter.data[position]
    }

    override fun getPosition(key: Recording): Int {
        return adapter.data.indexOf(key)
    }
}