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

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.Log
import java.io.IOException


class Recorder(private val context: Context) {
    var isRecording: Boolean = false

    private var mediaRecorder: MediaRecorder? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaProjectionCallback: MediaProjectionCallback? = null

    fun start(result: Int, data: Intent, options: Options): Boolean {
        if (isRecording) {
            throw IllegalStateException("start called but Recorder is already recording.")
        }
        mediaRecorder = MediaRecorder()
        if (!mediaRecorder!!.init(options)) {
            isRecording = false
            return false
        }

        //Set Callback for MediaProjection
        mediaProjectionCallback = MediaProjectionCallback()
        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        //Initialize MediaProjection using data received from Intent
        mediaProjection = projectionManager.getMediaProjection(result, data)
        mediaProjection!!.registerCallback(mediaProjectionCallback, null)

        virtualDisplay = mediaProjection!!.createVirtualDisplay("ScreenRecorder",
                options.video.resolution.width, options.video.resolution.height, options.video.virtualDisplayDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder!!.surface, null, null)
        return try {
            mediaRecorder!!.start()
            isRecording = true
            true
        } catch (e: IllegalStateException) {
            isRecording = false
            mediaProjection!!.stop()
            false
        }
    }

    fun stop(): Boolean {
        return stopScreenSharing()
    }

    fun pause() {
        if (!isRecording) {
            throw IllegalStateException("Called pause but Recorder is not recording.")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaRecorder!!.pause()
        }
    }

    fun resume() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaRecorder!!.resume()
        }
    }

    private fun MediaRecorder.init(options: Options): Boolean {
        val fileDescriptor = context.contentResolver
                .openFileDescriptor(options.output.uri, "w")?.fileDescriptor ?: return false

        try {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFile(fileDescriptor)
            when (options.audio) {
                is AudioOptions.RecordAudio -> {
                    setAudioSource(options.audio.source)
                    setAudioEncodingBitRate(options.audio.bitRate)
                    setAudioSamplingRate(options.audio.samplingRate)
                }
            }
            setOutputFormat(options.output.format)
            setVideoSize(options.video.resolution.width, options.video.resolution.height)
            setVideoEncoder(options.video.encoder)
            if (options.audio is AudioOptions.RecordAudio) {
                setAudioEncoder(options.audio.encoder)
            }
            setVideoEncodingBitRate(options.video.bitrate)
            setVideoFrameRate(options.video.fps)

            prepare()
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    private fun stopScreenSharing(): Boolean {
        if (virtualDisplay == null) {
            Log.d("Recorder", "Virtual display is null. Screen sharing already stopped")
            return true
        }
        var success: Boolean
        try {
            mediaRecorder!!.stop()
            Log.i("Recorder", "MediaProjection Stopped")
            success = true
        } catch (e: RuntimeException) {
            Log.e(
                "Recorder",
                "Fatal exception! Destroying media projection failed." + "\n" + e.message
            )
            success = false
        } finally {
            mediaRecorder!!.reset()
            virtualDisplay!!.release()
            mediaRecorder!!.release()
            if (mediaProjection != null) {
                mediaProjection!!.unregisterCallback(mediaProjectionCallback)
                mediaProjection!!.stop()
                mediaProjection = null
            }
        }
        isRecording = false
        return success
    }

    private inner class MediaProjectionCallback : MediaProjection.Callback() {
        override fun onStop() {
            stopScreenSharing()
        }
    }
}