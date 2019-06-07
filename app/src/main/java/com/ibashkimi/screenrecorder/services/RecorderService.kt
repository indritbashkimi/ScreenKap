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

import android.app.*
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ibashkimi.screenrecorder.FINISH_NOTIFICATION_CHANNEL_ID
import com.ibashkimi.screenrecorder.MainActivity
import com.ibashkimi.screenrecorder.R
import com.ibashkimi.screenrecorder.RECORDING_NOTIFICATION_CHANNEL_ID
import com.ibashkimi.screenrecorder.data.DataManager
import com.ibashkimi.screenrecorder.settings.PreferenceHelper


class RecorderService : Service() {

    private var recorder: Recorder? = null

    private var startTime: Long = 0

    private var elapsedTime: Long = 0

    private var state: RecorderState.State
        get() = RecorderState.state.value ?: RecorderState.State.STOPPED
        set(value) {
            RecorderState.state.value = value
        }

    private lateinit var options: Recorder.Options

    private val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "action: ${intent.action}")
        when (intent.action) {
            ACTION_RECORDING_START ->
                if (state == RecorderState.State.STOPPED) {
                    intent.getParcelableExtra<Intent?>(RECORDER_INTENT_DATA)?.let { intentData ->
                        startForeground(NOTIFICATION_ID_RECORDING, createOnRecordingNotification().build())
                        val result = intent.getIntExtra(RECORDER_INTENT_RESULT, Activity.RESULT_OK)
                        this.options = loadOptions()
                        this.recorder = Recorder(this)
                        return if (this.recorder!!.start(result, intentData, options)) {
                            startTime = System.currentTimeMillis()
                            state = RecorderState.State.RECORDING
                            START_STICKY
                        } else {
                            Toast.makeText(this, R.string.recording_error_message, Toast.LENGTH_SHORT).show()
                            delete(options.output.uri)
                            state = RecorderState.State.STOPPED
                            this.recorder = null
                            START_NOT_STICKY
                        }
                    } ?: return START_NOT_STICKY
                } else {
                    return START_STICKY
                }
            ACTION_RECORDING_PAUSE -> {
                return when (state) {
                    RecorderState.State.STOPPED -> START_NOT_STICKY
                    RecorderState.State.RECORDING -> {
                        recorder!!.pause()
                        //calculate total elapsed time until pause
                        elapsedTime += System.currentTimeMillis() - startTime

                        updateNotification(createOnPausedNotification().setUsesChronometer(false).build(), NOTIFICATION_ID_RECORDING)
                        Toast.makeText(this, R.string.recording_paused_message, Toast.LENGTH_SHORT).show()
                        state = RecorderState.State.PAUSED
                        START_STICKY
                    }
                    RecorderState.State.PAUSED -> return START_STICKY
                }
            }
            ACTION_RECORDING_RESUME -> {
                return when (state) {
                    RecorderState.State.PAUSED -> {
                        recorder?.resume()
                        //Reset startTime to current time again
                        startTime = System.currentTimeMillis()

                        updateNotification(createOnRecordingNotification().setUsesChronometer(true)
                                .setWhen(System.currentTimeMillis() - elapsedTime).build(), NOTIFICATION_ID_RECORDING)
                        state = RecorderState.State.RECORDING
                        START_STICKY
                    }
                    RecorderState.State.RECORDING -> START_STICKY
                    RecorderState.State.STOPPED -> START_NOT_STICKY
                }
            }
            ACTION_RECORDING_STOP -> {
                if (state == RecorderState.State.RECORDING || state == RecorderState.State.PAUSED) {
                    if (recorder!!.stop()) {
                        Log.d(TAG, "Recording finished.")
                        val values = ContentValues()
                        values.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis())
                        values.put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            values.put(MediaStore.Video.Media.IS_PENDING, 0)
                        }
                        DataManager(this).update(options.output.uri, values)
                        showFinishNotification()
                    } else {
                        delete(options.output.uri)
                        Toast.makeText(this, getString(R.string.recording_error_message), Toast.LENGTH_SHORT).show()
                    }
                }
                stopForeground(true)
                state = RecorderState.State.STOPPED
                stopSelf()
                return START_NOT_STICKY

            }
            ACTION_RECORDING_DELETE -> {
                intent.getParcelableExtra<Uri>(EXTRA_URI_TO_DELETE)?.let { delete(it) }
                notificationManager.cancel(NOTIFICATION_ID_FINISH)
                return if (state == RecorderState.State.STOPPED) START_NOT_STICKY else START_STICKY
            }
            else -> return START_NOT_STICKY
        }
    }

    private fun delete(uri: Uri) {
        DataManager(this).delete(uri)
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

    private fun showFinishNotification() {
        val contentIntent = PendingIntent.getActivity(this, 0,
                Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT)

        val shareIntent = Intent()
                .setAction(Intent.ACTION_SEND)
                .setType("video/*")
                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .putExtra(Intent.EXTRA_STREAM, options.output.uri)

        val sharePendingIntent = PendingIntent.getActivity(this, 0,
                Intent.createChooser(shareIntent, getString(R.string.share)),
                PendingIntent.FLAG_UPDATE_CURRENT)

        val deleteIntent = Intent(this, RecorderService::class.java)
                .setAction(ACTION_RECORDING_DELETE)
                .putExtra(EXTRA_URI_TO_DELETE, options.output.uri)

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

    //Update existing notification with its id and new Notification data
    private fun updateNotification(notification: Notification, id: Int) {
        notificationManager.notify(id, notification)
    }

    private fun loadOptions(): Recorder.Options {
        val prefs = PreferenceHelper(this)
        val fileTitle = prefs.filename
        val values = ContentValues()
        //values.put(MediaStore.Video.Media.TITLE, fileTitle)
        values.put(MediaStore.Video.Media.DISPLAY_NAME, fileTitle)
        // DATE_TAKEN is in milliseconds
        // DATE_MODIFIED is in seconds
        values.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis())
        values.put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Video.Media.IS_PENDING, 1)
            values.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + getString(R.string.app_name))
        }

        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)!!
        return prefs.run {
            Recorder.Options(
                    video = Recorder.VideoOptions(
                            resolution = resolution.run {
                                Recorder.Resolution(first, second)
                            },
                            bitrate = videoBitrate,
                            encoder = videoEncoder,
                            fps = fps,
                            virtualDisplayDpi = displayMetrics.densityDpi
                    ),
                    audio = if (recordAudio) {
                        Recorder.AudioOptions.RecordAudio(
                                source = MediaRecorder.AudioSource.MIC,
                                samplingRate = audioSamplingRate,
                                encoder = audioEncoder,
                                bitRate = audioBitrate
                        )
                    } else Recorder.AudioOptions.NoAudio,
                    output = Recorder.OutputOptions(
                            uri = uri
                    ))
        }
    }

    companion object {
        private val TAG = RecorderService::class.java.simpleName

        const val ACTION_RECORDING_START = "com.ibashkimi.screenrecorder.action.RECORDING_START"
        const val ACTION_RECORDING_STOP = "com.ibashkimi.screenrecorder.action.RECORDING_STOP"
        const val ACTION_RECORDING_PAUSE = "com.ibashkimi.screenrecorder.action.RECORDING_PAUSE"
        const val ACTION_RECORDING_RESUME = "com.ibashkimi.screenrecorder.action.RECORDING_RESUME"

        const val ACTION_RECORDING_DELETE = "con.ibashkimi.screenrecorder,action.RECORDING_DELETE"
        const val EXTRA_URI_TO_DELETE = "arg_delete_uri"

        const val RECORDER_INTENT_DATA = "recorder_intent_data"
        const val RECORDER_INTENT_RESULT = "recorder_intent_result"

        const val NOTIFICATION_ID_RECORDING = 1001
        const val NOTIFICATION_ID_FINISH = 1002

        fun start(context: Context, resultCode: Int, data: Intent?) {
            Intent(context, RecorderService::class.java).apply {
                action = ACTION_RECORDING_START
                putExtra(RECORDER_INTENT_DATA, data)
                putExtra(RECORDER_INTENT_RESULT, resultCode)
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
