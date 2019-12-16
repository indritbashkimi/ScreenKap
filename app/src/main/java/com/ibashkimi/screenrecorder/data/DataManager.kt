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

package com.ibashkimi.screenrecorder.data

import android.content.ContentValues
import android.net.Uri
import kotlinx.coroutines.flow.Flow


class DataManager(private val source: DataSource) {

    fun create(folderUri: Uri, name: String, mimeType: String, extra: ContentValues?): Uri? {
        return source.create(folderUri, name, mimeType, extra)
    }

    fun delete(recording: Recording) {
        delete(recording.uri)
    }

    fun delete(uri: Uri) {
        source.delete(uri)
    }

    fun delete(uris: List<Uri>) {
        source.delete(uris)
    }

    fun rename(recording: Recording, newName: String) {
        rename(recording.uri, newName)
    }

    fun rename(uri: Uri, newName: String) {
        source.rename(uri, newName)
    }

    fun recordings(): Flow<List<Recording>> {
        return source.recordings()
    }

    fun update(uri: Uri, values: ContentValues) {
        source.update(uri, values)
    }
}