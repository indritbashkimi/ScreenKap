package com.ibashkimi.screenrecorder.services

import android.media.MediaRecorder
import android.net.Uri
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


data class Options(
    val video: VideoOptions,
    val audio: AudioOptions,
    val output: OutputOptions
)

data class VideoOptions(
    val resolution: Resolution,
    val encoder: Int = MediaRecorder.VideoEncoder.H264,
    val fps: Int = 30,
    val bitrate: Int = 7130317,
    val virtualDisplayDpi: Int
)

data class Resolution(val width: Int, val height: Int)

sealed class AudioOptions {
    object NoAudio : AudioOptions()
    data class RecordAudio(
        val source: Int = MediaRecorder.AudioSource.DEFAULT,
        val samplingRate: Int = 44100,
        val encoder: Int = MediaRecorder.AudioEncoder.AAC,
        val bitRate: Int = 128000
    ) : AudioOptions()
}

class OutputOptions(val uri: SaveUri, val format: Int = MediaRecorder.OutputFormat.DEFAULT)

@Parcelize
data class SaveUri(val uri: Uri, val type: UriType): Parcelable

@Parcelize
enum class UriType: Parcelable {
    MEDIA_STORE, SAF
}