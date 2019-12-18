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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.ibashkimi.screenrecorder.data.DataManager
import com.ibashkimi.screenrecorder.data.MediaStoreDataSource
import com.ibashkimi.screenrecorder.data.Recording
import com.ibashkimi.screenrecorder.data.SAFDataSource
import com.ibashkimi.screenrecorder.services.RecorderState
import com.ibashkimi.screenrecorder.services.SaveUri
import com.ibashkimi.screenrecorder.services.UriType
import com.ibashkimi.screenrecorder.settings.PreferenceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn


@UseExperimental(ExperimentalCoroutinesApi::class)
class RecordingsViewModel(app: Application) : AndroidViewModel(app) {

    private val context: Context = getApplication()

    private var dataManager: DataManager? = null

    val recorderState = RecorderState.state

    val recordings: LiveData<List<Recording>>

    init {
        val preferences = PreferenceHelper(context)
        recordings = preferences.saveLocationFlow.flatMapLatest {
            dataManager = createDataManager(it)
            dataManager?.recordings() ?: emptyFlow()
        }.flowOn(Dispatchers.IO).combine(preferences.sortOrderOptionsFlow) { recordings, options ->
            processData(recordings, options)
        }.flowOn(Dispatchers.Default).asLiveData()
    }

    private fun createDataManager(saveUri: SaveUri?) = when (saveUri?.type) {
        UriType.MEDIA_STORE -> DataManager(MediaStoreDataSource(context, saveUri.uri))
        UriType.SAF -> DataManager(SAFDataSource(context, saveUri.uri))
        else -> null
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

    private fun processData(recordings: List<Recording>, options: PreferenceHelper.SortOrderOptions): List<Recording> {
        return recordings.filter { !it.isPending }.run {
            when (options.sortBy) {
                PreferenceHelper.SortBy.NAME -> sortedBy { it.title }
                PreferenceHelper.SortBy.DATE -> sortedBy { it.modified }
                PreferenceHelper.SortBy.DURATION -> sortedBy { it.duration }
                PreferenceHelper.SortBy.SIZE -> sortedBy { it.size }
            }.run {
                when (options.orderBy) {
                    PreferenceHelper.OrderBy.ASCENDING -> this
                    PreferenceHelper.OrderBy.DESCENDING -> reversed()
                }
            }
        }
    }
}