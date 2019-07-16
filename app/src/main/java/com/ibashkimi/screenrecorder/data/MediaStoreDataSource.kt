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
import android.os.Build
import android.os.Handler
import android.provider.MediaStore


class MediaStoreDataSource(val context: Context, val uri: Uri) : DataSource {

    private val observers = HashMap<ContentChangeObserver, MyContentObserver>()

    override fun create(folderUri: Uri, name: String, mimeType: String, extra: ContentValues?): Uri? {
        val values = ContentValues()
        //values.put(MediaStore.Video.Media.TITLE, fileTitle)
        values.put(MediaStore.Video.Media.DISPLAY_NAME, name)
        val now = System.currentTimeMillis()
        // DATE_ADDED is in milliseconds
        // DATE_MODIFIED is in seconds
        values.put(MediaStore.Video.Media.DATE_ADDED, now)
        values.put(MediaStore.Video.Media.DATE_MODIFIED, now / 1000)
        values.put(MediaStore.Video.Media.MIME_TYPE, mimeType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Video.Media.IS_PENDING, 1)
            extra?.apply {
                if (containsKey(MediaStore.Video.Media.RELATIVE_PATH))
                    values.put(MediaStore.Video.Media.RELATIVE_PATH,
                            extra.getAsString(MediaStore.Video.Media.RELATIVE_PATH))
            }
        }

        return context.contentResolver.insert(folderUri, values)
    }

    override fun delete(uri: Uri) {
        context.contentResolver.delete(uri, null, null)
    }

    override fun delete(uris: List<Uri>) {
        val operations = ArrayList<ContentProviderOperation>(uris.size)
        uris.forEach { operations.add(ContentProviderOperation.newDelete(it).build()) }
        context.contentResolver.applyBatch(MediaStore.AUTHORITY, operations)
    }

    override fun rename(uri: Uri, newName: String) {
        val values = ContentValues()
        //values.put(MediaStore.Video.Media.TITLE, newName)
        values.put(MediaStore.Video.Media.DISPLAY_NAME, newName)
        // DATE_MODIFIED is in secondsContentProviderOperation
        //values.put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
        context.contentResolver.update(uri, values, null, null)
    }

    override fun fetchRecordings(): List<Recording> {
        val recordings = ArrayList<Recording>()
        @SuppressLint("InlinedApi")
        val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.IS_PENDING)

        // Note: newUri works also with DocumentFile uris. Otherwise this is not necessary.
        //val newUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
        context.contentResolver.query(uri,
                projection, null, null, null)?.apply {
            if (moveToFirst()) {
                do {
                    val isPending: Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
                    val duration = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        getInt(getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
                    } else {
                        0
                    }
                    val fileUri = ContentUris.withAppendedId(
                            uri,
                            getLong(getColumnIndex(MediaStore.Video.Media._ID)))
                    // DATE_MODIFIED is in seconds
                    recordings.add(Recording(fileUri, displayName, duration, size, modified * 1000, isPending))
                } while (moveToNext())
            }
            close()
        }
        return recordings
    }

    override fun update(uri: Uri, values: ContentValues) {
        context.contentResolver.update(uri, values, null, null)
    }

    override fun registerContentChangedObserver(observer: ContentChangeObserver) {
        val contentObserver = observers[observer] ?: MyContentObserver(observer).apply {
            observers[observer] = this
        }
        context.contentResolver.registerContentObserver(uri, true, contentObserver)
    }

    override fun unregisterContentChangeObserver(observer: ContentChangeObserver) {
        observers[observer]?.let {
            context.contentResolver.unregisterContentObserver(it)
            observers.remove(observer)
        }
    }

    inner class MyContentObserver(private val observer: ContentChangeObserver, handler: Handler = Handler()) : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            onChange(selfChange, null)
        }

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            observer.contentChanged()
        }

        override fun deliverSelfNotifications() = true
    }
}