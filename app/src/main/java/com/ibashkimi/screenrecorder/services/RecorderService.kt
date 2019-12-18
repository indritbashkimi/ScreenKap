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

package com.ibashkimi.screenrecorder.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ibashkimi.screenrecorder.FINISH_NOTIFICATION_CHANNEL_ID
import com.ibashkimi.screenrecorder.MainActivity
import com.ibashkimi.screenrecorder.R
import com.ibashkimi.screenrecorder.RECORDING_NOTIFICATION_CHANNEL_ID
import com.ibashkimi.screenrecorder.data.DataManager
import com.ibashkimi.screenrecorder.data.MediaStoreDataSource
import com.ibashkimi.screenrecorder.data.SAFDataSource
import com.ibashkimi.screenrecorder.settings.PreferenceHelper


class RecorderService : Service() {

    private var session: RecordingSession? = null

    private val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (intent.action) {
            ACTION_RECORDING_START ->
                return when (session?.state) {
                    RecorderState.State.RECORDING -> START_STICKY
                    RecorderState.State.PAUSED -> {
                        resume(this)
                        START_STICKY
                    }
                    RecorderState.State.STOPPED, null -> {
                        startForeground(
                            NOTIFICATION_ID_RECORDING,
                            createOnRecordingNotification().build()
                        )
                        return (createNewRecordingSession()?.let {
                            if (it.start(intent)) {
                                session = it
                                START_STICKY
                            } else {
                                START_NOT_STICKY
                            }
                        } ?: START_NOT_STICKY).also {
                            if (it == START_NOT_STICKY) stopForeground(true)
                        }
                    }
                }
            ACTION_RECORDING_PAUSE -> {
                return session?.let {
                    when (it.state) {
                        RecorderState.State.RECORDING -> {
                            it.pause()
                            updateNotification(
                                createOnPausedNotification().setUsesChronometer(false).build(),
                                NOTIFICATION_ID_RECORDING
                            )
                            Toast.makeText(
                                this,
                                R.string.recording_paused_message,
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            START_STICKY
                        }
                        RecorderState.State.PAUSED -> return START_STICKY
                        RecorderState.State.STOPPED -> START_NOT_STICKY
                    }
                    START_STICKY
                } ?: START_NOT_STICKY
            }
            ACTION_RECORDING_RESUME -> {
                return session?.let {
                    when (it.state) {
                        RecorderState.State.PAUSED -> {
                            it.resume()
                            updateNotification(
                                createOnRecordingNotification().setUsesChronometer(true)
                                    .setWhen(System.currentTimeMillis() - it.elapsedTime).build(),
                                NOTIFICATION_ID_RECORDING
                            )
                            START_STICKY
                        }
                        RecorderState.State.RECORDING -> START_STICKY
                        RecorderState.State.STOPPED -> START_NOT_STICKY
                    }
                } ?: START_NOT_STICKY
            }
            ACTION_RECORDING_STOP -> {
                session?.let {
                    when (it.state) {
                        RecorderState.State.RECORDING, RecorderState.State.PAUSED -> {
                            if (it.stop()) {
                                Log.d(TAG, "Recording finished.")
                                onRecordingCompleted()
                                showFinishNotification(it.options.output.uri)
                                session = null
                            } else {
                                //delete(options.output.uri)
                                Toast.makeText(
                                    this,
                                    getString(R.string.recording_error_message),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        RecorderState.State.STOPPED -> {
                        }
                    }
                }

                stopForeground(true)
                stopSelf()
                return START_NOT_STICKY

            }
            ACTION_RECORDING_DELETE -> {
                intent.getParcelableExtra<SaveUri>(EXTRA_RECORDING_DELETE_URI)?.also {
                    createNewDataManager(it).delete(it.uri)
                    onRecordingDeleted()
                }
                notificationManager.cancel(NOTIFICATION_ID_FINISH)
                return if (session?.state == RecorderState.State.STOPPED) START_NOT_STICKY else START_STICKY
            }
            else -> return START_NOT_STICKY
        }
    }

    private fun createNewDataManager(saveUri: SaveUri): DataManager {
        return DataManager(
            (when (saveUri.type) {
                UriType.SAF -> SAFDataSource(this, saveUri.uri)
                else -> MediaStoreDataSource(this, saveUri.uri)
            })
        )
    }

    private fun createNewRecordingSession(): RecordingSession? {
        val preferences = PreferenceHelper(this)
        val saveLocation = preferences.saveLocation ?: return null
        val dataManager = createNewDataManager(saveLocation)
        val options = preferences.generateOptions(dataManager)
        if (options == null) {
            Toast.makeText(
                this,
                R.string.recording_error_message,
                Toast.LENGTH_SHORT
            ).show()
            return null
        }
        return RecordingSession(this, options, dataManager)
    }

    private fun onRecordingCompleted() = broadcast(ACTION_RECORDING_COMPLETED)

    private fun onRecordingDeleted() = broadcast(ACTION_RECORDING_DELETE)

    private fun broadcast(action: String) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(action))
    }

    private val pauseAction: NotificationCompat.Action
        get() {
            val pauseIntent = Intent(this, RecorderService::class.java).apply {
                action = ACTION_RECORDING_PAUSE
            }
            return NotificationCompat.Action(
                    R.drawable.ic_notification_pause,
                    getString(R.string.notification_action_pause),
                    PendingIntent.getService(this, 0, pauseIntent, 0))
        }

    private val resumeAction: NotificationCompat.Action
        get() {
            val resumeIntent = Intent(this, RecorderService::class.java).apply {
                action = ACTION_RECORDING_RESUME
            }
            return NotificationCompat.Action(R.drawable.ic_notification_resume,
                    getString(R.string.notification_action_resume),
                    PendingIntent.getService(this, 0, resumeIntent, 0))
        }

    private val openAppPendingIntent: PendingIntent
        get() {
            val openAppIntent = Intent(this, MainActivity::class.java)
            return PendingIntent.getActivity(this, 0, openAppIntent, 0)
        }

    private val stopAction: NotificationCompat.Action
        get() {
            val stopIntent = Intent(this, RecorderService::class.java).apply {
                this.action = ACTION_RECORDING_STOP
            }
            return NotificationCompat.Action(R.drawable.ic_notification_stop,
                    getString(R.string.notification_action_stop),
                    PendingIntent.getService(this, 0, stopIntent, 0))
        }

    private fun createOnRecordingNotification(): NotificationCompat.Builder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            createNotification(stopAction, pauseAction)
        } else {
            createNotification(stopAction)
        }
    }

    private fun createOnPausedNotification(): NotificationCompat.Builder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            createNotification(stopAction, resumeAction)
        } else {
            createNotification(stopAction)
        }
    }

    private fun createNotification(vararg actions: NotificationCompat.Action): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, RECORDING_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_summary_recording))
                .setTicker(getString(R.string.notification_title))
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setUsesChronometer(true)
                .setOngoing(true)
                .setColor(Color.RED)
                .setContentIntent(openAppPendingIntent)
                .setPriority(NotificationManagerCompat.IMPORTANCE_HIGH)
                .apply {
                    actions.forEach { addAction(it) }
                }
    }

    private fun showFinishNotification(uri: SaveUri) {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT
        )

        val sharePendingIntent = PendingIntent.getActivity(this, 0,
                Intent.createChooser(createShareIntent(options.output.uri), getString(R.string.share)),
                PendingIntent.FLAG_UPDATE_CURRENT)

        val deleteIntent = Intent(this, RecorderService::class.java)
            .setAction(ACTION_RECORDING_DELETE)
            .putExtra(EXTRA_RECORDING_DELETE_URI, uri)

        val deletePendingIntent = PendingIntent.getService(this, 0,
                deleteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)

        val shareAction = NotificationCompat.Action(R.drawable.ic_share, getString(R.string.share), sharePendingIntent)
        val deleteAction = NotificationCompat.Action(R.drawable.ic_delete, getString(R.string.notification_action_delete), deletePendingIntent)
        val notification = NotificationCompat.Builder(this, FINISH_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_finish_title))
                .setContentText(getString(R.string.notification_finish_summary))
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .addAction(shareAction)
                .addAction(deleteAction)
        updateNotification(notification.build(), NOTIFICATION_ID_FINISH)
    }

    private fun createShareIntent(uri: Uri) = Intent()
            .setAction(Intent.ACTION_SEND)
            .setType("video/*")
            .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .putExtra(Intent.EXTRA_STREAM, uri)

    //Update existing notification with its id and new Notification data
    private fun updateNotification(notification: Notification, id: Int) {
        notificationManager.notify(id, notification)
    }

    companion object {
        private val TAG = RecorderService::class.java.simpleName

        const val ACTION_RECORDING_START = "com.ibashkimi.screenrecorder.action.RECORDING_START"
        const val ACTION_RECORDING_STOP = "com.ibashkimi.screenrecorder.action.RECORDING_STOP"
        const val ACTION_RECORDING_PAUSE = "com.ibashkimi.screenrecorder.action.RECORDING_PAUSE"
        const val ACTION_RECORDING_RESUME = "com.ibashkimi.screenrecorder.action.RECORDING_RESUME"

        const val ACTION_RECORDING_DELETE = "con.ibashkimi.screenrecorder.action.RECORDING_DELETE"
        const val EXTRA_RECORDING_DELETE_URI = "arg_delete_uri"

        const val ACTION_RECORDING_COMPLETED = "com.ibashkimi.screenrecorder.action.RECORDING_COMPLETED"
        const val ACTION_RECORDING_DELETED = "com.ibashkimi.screenrecorder.action.RECORDING_DELETED"

        const val RECORDER_INTENT_DATA = "recorder_intent_data"
        const val RECORDER_INTENT_RESULT = "recorder_intent_result"

        const val NOTIFICATION_ID_RECORDING = 1001
        const val NOTIFICATION_ID_FINISH = 1002

        fun start(context: Context, resultCode: Int, data: Intent?) {
            Intent(context, RecorderService::class.java).apply {
                action = ACTION_RECORDING_START
                putExtra(RECORDER_INTENT_DATA, data)
                putExtra(RECORDER_INTENT_RESULT, resultCode)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                context.startService(this)
            }
        }

        @Suppress("unused")
        fun pause(context: Context) {
            Intent(context, RecorderService::class.java).apply {
                action = ACTION_RECORDING_PAUSE
                context.startService(this)
            }
        }

        @Suppress("unused")
        fun resume(context: Context) {
            Intent(context, RecorderService::class.java).apply {
                action = ACTION_RECORDING_RESUME
                context.startService(this)
            }
        }

        fun stop(context: Context) {
            Intent(context, RecorderService::class.java).apply {
                action = ACTION_RECORDING_STOP
                context.startService(this)
            }
        }
    }
}
