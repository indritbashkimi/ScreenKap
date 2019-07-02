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
import androidx.lifecycle.*
import com.ibashkimi.screenrecorder.data.*
import com.ibashkimi.screenrecorder.services.RecorderState
import com.ibashkimi.screenrecorder.settings.PreferenceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class RecordingsViewModel(app: Application) : AndroidViewModel(app) {

    private val context: Context = getApplication()

    private var dataManager: DataManager? = null

    private val preferences = PreferenceHelper(context)

    private val uriObserver = ContentChangeObserver {
        refresh()
    }

    val recordings: MutableLiveData<List<Recording>>

    val recorderState = RecorderState.state

    init {
        val sortOptions = preferences.createPreferenceChangedLiveData(
                PreferenceHelper.KEY_ORDER_BY,
                PreferenceHelper.KEY_SORT_BY
        )
        val saveLocation = Transformations.distinctUntilChanged(preferences.saveLocationLiveData)
        recordings = MediatorLiveData<List<Recording>>().apply {
            addSource(sortOptions) {
                this.value?.let { data ->
                    postValue(processData(data))
                }
            }
            addSource(saveLocation) {
                preferences.saveLocation?.let {
                    setNewDataManager(it)
                    refresh()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        dataManager?.unregisterUriChangeObserver(uriObserver)
    }

    private fun setNewDataManager(saveUri: PreferenceHelper.SaveUri) {
        android.util.Log.d("RecordingsViewModel", "setNewDataManager called")
        dataManager?.unregisterUriChangeObserver(uriObserver)
        val source = when (saveUri.type) {
            PreferenceHelper.UriType.MEDIA_STORE -> MediaStoreDataSource(context, saveUri.uri)
            PreferenceHelper.UriType.SAF -> SAFDataSource(context, saveUri.uri)
        }
        dataManager = DataManager(source).apply {
            registerUriChangeObserver(uriObserver)
        }
    }

    fun refresh() {
        dataManager?.let {
            viewModelScope.launch(Dispatchers.IO) {
                recordings.postValue(processData(it.fetchRecordings()))
            }
        }
    }

    fun rename(recording: Recording, newName: String) {
        dataManager?.rename(recording, newName)
    }

    fun deleteRecording(recording: Recording) {
        dataManager?.delete(recording)
    }

    fun deleteRecordings(recordings: List<Recording>) {
        dataManager?.delete(recordings.map { it.uri })
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
}