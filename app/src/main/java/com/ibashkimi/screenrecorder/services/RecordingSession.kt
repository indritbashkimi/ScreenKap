package com.ibashkimi.screenrecorder.services

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import com.ibashkimi.screenrecorder.data.DataManager

class RecordingSession(
    val context: Context,
    val options: Options,
    private val dataManager: DataManager
) {

    private var recorder: Recorder? = null

    var state: RecorderState.State
        get() = RecorderState.state.value ?: RecorderState.State.STOPPED
        set(value) {
            RecorderState.state.value = value
        }

    var startTime: Long = 0

    var elapsedTime: Long = 0

    fun start(intent: Intent): Boolean {
        return if (state == RecorderState.State.STOPPED) {
            intent.getParcelableExtra<Intent?>(RecorderService.RECORDER_INTENT_DATA)?.let { intentData ->
                val result =
                    intent.getIntExtra(RecorderService.RECORDER_INTENT_RESULT, Activity.RESULT_OK)
                val newRecorder = Recorder(context)
                if (newRecorder.start(result, intentData, options)) {
                    startTime = System.currentTimeMillis()
                    state = RecorderState.State.RECORDING
                    recorder = newRecorder
                    true
                } else {
                    state = RecorderState.State.STOPPED
                    //recorder = null
                    false
                }
            } ?: false
        } else {
            true
        }
    }

    fun pause(): Boolean {
        return recorder?.let {
            when (state) {
                RecorderState.State.RECORDING -> {
                    it.pause()
                    //calculate total elapsed time until pause
                    elapsedTime += System.currentTimeMillis() - startTime
                    state = RecorderState.State.PAUSED
                    true
                }
                RecorderState.State.STOPPED -> false
                RecorderState.State.PAUSED -> true
            }
        } ?: false
    }

    fun resume(): Boolean {
        return recorder?.let {
            return when (state) {
                RecorderState.State.PAUSED -> {
                    it.resume()
                    //Reset startTime to current time again
                    startTime = System.currentTimeMillis()
                    state = RecorderState.State.RECORDING
                    true
                }
                RecorderState.State.RECORDING -> true
                RecorderState.State.STOPPED -> false
            }
        } ?: false
    }

    fun stop(): Boolean {
        recorder?.let {
            if (state == RecorderState.State.RECORDING || state == RecorderState.State.PAUSED) {
                if (it.stop()) {
                    val now = System.currentTimeMillis()
                    val values = ContentValues()
                    values.put(MediaStore.Video.Media.DATE_ADDED, now)
                    values.put(MediaStore.Video.Media.DATE_MODIFIED, now / 1000)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    }

                    dataManager.update(options.output.uri.uri, values)
                }
            }
        }

        recorder = null
        state = RecorderState.State.STOPPED
        return true
    }
}