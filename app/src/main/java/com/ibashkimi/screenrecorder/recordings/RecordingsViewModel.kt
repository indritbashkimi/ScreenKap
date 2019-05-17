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

import android.app.Application
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ibashkimi.screenrecorder.data.DataManager
import com.ibashkimi.screenrecorder.data.Recording
import com.ibashkimi.screenrecorder.services.RecorderState
import com.ibashkimi.screenrecorder.settings.PreferenceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class RecordingsViewModel(app: Application) : AndroidViewModel(app) {

    private val context: Context = getApplication()

    private val dataManager = DataManager(getApplication())

    private val preferences = PreferenceHelper(context)

    private val uriObserver = MyContentObserver()

    val recordings: MutableLiveData<List<Recording>>

    val recorderState = RecorderState.state

    init {
        val sortOptions = preferences.createPreferenceChangedLiveData(
                PreferenceHelper.KEY_ORDER_BY,
                PreferenceHelper.KEY_SORT_BY
        )
        recordings = MediatorLiveData<List<Recording>>().apply {
            addSource(sortOptions) {
                this.value?.let { data ->
                    postValue(processData(data))
                }
            }
        }
        refresh()
        dataManager.registerUriChangeObserver(uriObserver)
    }

    override fun onCleared() {
        super.onCleared()
        dataManager.unregisterUriChangeObserver(uriObserver)
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            recordings.postValue(processData(dataManager.fetchRecordings()))
        }
    }

    fun rename(recording: Recording, newName: String) {
        dataManager.rename(recording, newName)
    }

    fun deleteRecording(recording: Recording) {
        dataManager.delete(recording)
    }

    fun deleteRecordings(recordings: List<Recording>) {
        dataManager.delete(recordings.map { it.uri })
    }

    private fun processData(recordings: List<Recording>): List<Recording> {
        return recordings.filter { !it.isPending }.run {
            when (preferences.sortBy) {
                PreferenceHelper.SortBy.NAME -> sortedBy { it.title }
                PreferenceHelper.SortBy.DATE -> sortedBy { it.modified }
                PreferenceHelper.SortBy.DURATION -> sortedBy { it.duration }
                PreferenceHelper.SortBy.SIZE -> sortedBy { it.size }
            }.run {
                when (preferences.orderBy) {
                    PreferenceHelper.OrderBy.ASCENDING -> this
                    PreferenceHelper.OrderBy.DESCENDING -> reversed()
                }
            }
        }
    }

    inner class MyContentObserver(handler: Handler = Handler()) : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            onChange(selfChange, null)
        }

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            refresh()
        }

        override fun deliverSelfNotifications() = true
    }
}