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

import android.annotation.SuppressLint
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.provider.MediaStore
import androidx.core.os.BuildCompat.isAtLeastQ
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class DataManager(private val context: Context) {

    fun delete(recording: Recording) {
        delete(recording.uri)
    }

    fun delete(uri: Uri) {
        context.contentResolver.delete(uri, null, null)
    }

    fun delete(uris: List<Uri>) {
        val operations = ArrayList<ContentProviderOperation>(uris.size)
        uris.forEach { operations.add(ContentProviderOperation.newDelete(it).build()) }
        context.contentResolver.applyBatch(MediaStore.AUTHORITY, operations)
    }

    fun rename(recording: Recording, newName: String) {
        rename(recording.uri, newName)
    }

    fun rename(uri: Uri, newName: String) {
        val values = ContentValues()
        //values.put(MediaStore.Video.Media.TITLE, newName)
        values.put(MediaStore.Video.Media.DISPLAY_NAME, newName)
        // DATE_MODIFIED is in secondsContentProviderOperation
        //values.put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
        context.contentResolver.update(uri, values, null, null)
    }

    fun update(uri: Uri, values: ContentValues) {
        context.contentResolver.update(uri, values, null, null)
    }

    fun fetchRecordings(): List<Recording> {
        val recordings = ArrayList<Recording>()
        @SuppressLint("InlinedApi")
        val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.IS_PENDING)

        context.contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, null)?.apply {
            if (moveToFirst()) {
                do {
                    val isPending: Boolean = if (isAtLeastQ()) {
                        getInt(getColumnIndexOrThrow(MediaStore.Video.Media.IS_PENDING)) == 1
                    } else {
                        false
                    }
                    val displayName = getString(
                            getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME))
                    val modified = getLong(
                            getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED))
                    val size = getLong(
                            getColumnIndexOrThrow(MediaStore.Video.Media.SIZE))
                    val duration = getInt(
                            getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
                    val uri = ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            getLong(getColumnIndex(MediaStore.Video.Media._ID)))
                    recordings.add(Recording(uri, displayName, duration, size, modified, isPending))
                } while (moveToNext())
            }
            close()
        }
        return recordings
    }

    @Suppress("unused")
    val uriChangeLiveData: LiveData<Boolean> by lazy {
        UriChangeLiveData(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
    }

    fun registerUriChangeObserver(observer: ContentObserver) {
        context.contentResolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer)
    }

    fun unregisterUriChangeObserver(observer: ContentObserver) {
        context.contentResolver.unregisterContentObserver(observer)
    }


    class UriChangeLiveData(private val context: Context, private val uri: Uri) : MutableLiveData<Boolean>() {

        private val observer = MyContentObserver(Handler())

        override fun onActive() {
            super.onActive()
            context.contentResolver.registerContentObserver(uri, true, observer)
        }

        override fun onInactive() {
            super.onInactive()
            context.contentResolver.unregisterContentObserver(observer)
        }

        inner class MyContentObserver(handler: Handler) : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                onChange(selfChange, null)
            }

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                postValue(true)
            }

            override fun deliverSelfNotifications() = true
        }
    }
}