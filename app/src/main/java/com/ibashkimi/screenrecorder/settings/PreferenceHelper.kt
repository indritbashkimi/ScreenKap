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

package com.ibashkimi.screenrecorder.settings

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaRecorder
import android.os.Build
import android.util.DisplayMetrics
import android.view.Surface
import android.view.WindowManager
import androidx.core.content.edit
import androidx.core.os.BuildCompat.isAtLeastQ
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.ibashkimi.screenrecorder.R
import java.text.SimpleDateFormat
import java.util.*

class PreferenceHelper(private val context: Context,
                       private val sharedPreferences: SharedPreferences =
                               PreferenceManager.getDefaultSharedPreferences(context)) {

    @Suppress("unused")
    val nightModeLiveData: LiveData<String> by lazy {
        createPreferenceLiveData(sharedPreferences, context.getString(R.string.pref_key_dark_theme)) { _, _ ->
            nightMode
        }
    }

    var isFirstTime: Boolean
        get() = sharedPreferences.getBoolean("is_first_time", true)
        set(value) {
            sharedPreferences.edit { putBoolean("is_first_time", value) }
        }

    val displayMetrics: DisplayMetrics by lazy {
        val metrics = DisplayMetrics()
        val window = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        window.defaultDisplay.getRealMetrics(metrics)
        metrics
    }

    var videoEncoder: Int
        get() {
            return when (getString(R.string.pref_key_video_encoder) ?: "default") {
                "default" -> MediaRecorder.VideoEncoder.DEFAULT
                "H264" -> MediaRecorder.VideoEncoder.H264
                "HEVC" -> when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> MediaRecorder.VideoEncoder.HEVC
                    else -> {
                        // Fallback to default value to avoid crash
                        putString(R.string.pref_key_video_encoder, "default")
                        throw IllegalArgumentException("HEVC codec requires Android N.")
                    }
                }
                "VP8" -> MediaRecorder.VideoEncoder.VP8
                else -> MediaRecorder.VideoEncoder.H264
            }
        }
        set(value) {
            putString(R.string.pref_key_video_encoder, value)
        }

    var videoBitrate: Int
        get() = getString(R.string.pref_key_video_bitrate)?.toIntOrNull() ?: 8388608
        set(value) {
            putString(
                    R.string.pref_key_video_bitrate, value)
        }
    var fps: Int
        get() = getString(R.string.pref_key_fps)?.toIntOrNull() ?: 30
        set(value) {
            putString(R.string.pref_key_fps, value)
        }

    var videoWidth: Int
        get() {
            val deviceWidth = displayMetrics.widthPixels
            return getString(R.string.pref_key_resolution)?.toIntOrNull() ?: deviceWidth
        }
        set(value) {
            putString(R.string.pref_key_resolution, value)
        }

    fun initResolutionPreference() {
        val realDisplayMetrics = DisplayMetrics()
        val window = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        window.defaultDisplay.getRealMetrics(realDisplayMetrics)
        val resolutionValues = context.resources.getStringArray(R.array.resolution_values)
                .filter { it.toInt() <= realDisplayMetrics.widthPixels }
        videoWidth = resolutionValues.last().toInt()
    }

    val resolution: Pair<Int, Int>
        get() {
            val width = videoWidth
            val height: Int = (width * getAspectRatio(displayMetrics)).toInt()
            val orientationPrefs = sharedPreferences.getString(context.getString(R.string.pref_key_orientation), "auto")
            val screenOrientation = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
            val (finalWidth, finalHeight) = when (orientationPrefs) {
                "auto" -> {
                    if (screenOrientation == Surface.ROTATION_0 || screenOrientation == Surface.ROTATION_180) {
                        Pair(width, height)
                    } else {
                        Pair(height, width)
                    }
                }
                "portrait" -> Pair(width, height)
                "landscape" -> Pair(height, width)
                else -> Pair(width, height)
            }
            return Pair(finalWidth, finalHeight)
        }

    var baseFilename: String
        get() {
            return getString(R.string.pref_key_filename) ?: "yyyyMMdd_hhmmss"
        }
        set(value) {
            putString(R.string.pref_key_filename, value)
        }

    var prefixFilename: String
        get() {
            return getString(R.string.pref_key_file_prefix) ?: "REC"
        }
        set(value) {
            putString(R.string.pref_key_file_prefix, value)
        }

    val filename: String
        get() {
            val formatter = SimpleDateFormat(baseFilename, Locale.getDefault())
            val userPrefix = prefixFilename
            val prefix = when {
                userPrefix.isBlank() -> ""
                userPrefix.endsWith("_") -> userPrefix
                else -> userPrefix + "_"
            }
            return prefix + formatter.format(Calendar.getInstance().time)
        }

    var nightMode: String
        get() = getString(R.string.pref_key_dark_theme) ?: "system_default"
        set(value) {
            putString(R.string.pref_key_dark_theme, value)
        }

    var sortBy: SortBy
        get() {
            val sortBy = sharedPreferences.getString(KEY_SORT_BY, null) ?: SortBy.DATE.name
            return SortBy.valueOf(sortBy)
        }
        set(value) {
            sharedPreferences.edit().putString(KEY_SORT_BY, value.name).apply()
        }

    var orderBy: OrderBy
        get() {
            val orderBy = sharedPreferences.getString(KEY_ORDER_BY, null) ?: OrderBy.DESCENDING.name
            return OrderBy.valueOf(orderBy)
        }
        set(value) {
            sharedPreferences.edit().putString(KEY_ORDER_BY, value.name).apply()
        }

    var recordAudio: Boolean
        get() = getBoolean(R.string.pref_key_audio)
        set(value) {
            sharedPreferences.edit {
                putBoolean(getString(R.string.pref_key_audio), value)
            }
        }

    var audioBitrate: Int
        get() = getString(R.string.pref_key_audio_bit_rate)?.toIntOrNull() ?: 1280000
        set(value) {
            putString(R.string.pref_key_audio_bit_rate, value)
        }

    var audioSamplingRate: Int
        get() = getString(R.string.pref_key_audio_sampling_rate)?.toIntOrNull() ?: 44100
        set(value) {
            putString(R.string.pref_key_audio_sampling_rate, value.toString())
        }

    var audioEncoder: Int
        get() {
            return when (getString(R.string.pref_key_audio_encoder) ?: "default") {
                "default" -> MediaRecorder.AudioEncoder.DEFAULT
                "aac" -> MediaRecorder.AudioEncoder.AAC
                "opus" -> if (isAtLeastQ()) {
                    MediaRecorder.AudioEncoder.OPUS
                } else {
                    // Fallback to default value to avoid crash
                    putString(R.string.pref_key_audio_encoder, "default")
                    throw java.lang.IllegalArgumentException("Opus codec requires Android Q.")
                }
                "vorbis" -> MediaRecorder.AudioEncoder.VORBIS
                else -> MediaRecorder.AudioEncoder.AAC
            }
        }
        set(value) {
            putString(R.string.pref_key_audio_encoder, value)
        }

    fun createPreferenceChangedLiveData(vararg keys: String): LiveData<String> {
        return createPreferenceChangedLiveData(sharedPreferences, keys.toList())
    }

    private fun getAspectRatio(metrics: DisplayMetrics): Float {
        val width = metrics.widthPixels.toFloat()
        val height = metrics.heightPixels.toFloat()
        return if (width > height) {
            width / height
        } else {
            height / width
        }
    }

    private fun getString(stringRes: Int): String? {
        return sharedPreferences.getString(context.getString(stringRes), null)
    }

    private fun putString(stringRes: Int, value: Int) {
        putString(stringRes, value.toString())
    }

    private fun putString(stringRes: Int, value: String) {
        sharedPreferences.edit().putString(context.getString(stringRes), value).apply()
    }

    private fun getBoolean(stringRes: Int, defaultValue: Boolean = false): Boolean {
        return sharedPreferences.getBoolean(context.getString(stringRes), defaultValue)
    }

    enum class SortBy {
        NAME, DATE, DURATION, SIZE
    }

    enum class OrderBy {
        ASCENDING, DESCENDING
    }

    companion object {
        const val KEY_SORT_BY = "sort_by"
        const val KEY_ORDER_BY = "order_by"
    }
}

class PreferenceChangedLiveData(private val sharedPreferences: SharedPreferences,
                                private val keys: List<String>) : MutableLiveData<String>(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onActive() {
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onInactive() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key in keys) {
            value = key
        }
    }
}

class PreferenceLiveData<T>(private val sharedPreferences: SharedPreferences,
                            private val key: String,
                            private val loadFirst: Boolean = false,
                            private val onKeyChanged: (String) -> T) : MutableLiveData<T>(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onActive() {
        if (loadFirst) {
            value = onKeyChanged(key)
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onInactive() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, changed: String) {
        if (key == changed) {
            value = onKeyChanged(key)
        }
    }
}

fun createPreferenceChangedLiveData(sharedPreferences: SharedPreferences, keys: List<String>): LiveData<String> {
    return PreferenceChangedLiveData(sharedPreferences, keys)
}

fun <T> createPreferenceLiveData(sharedPreferences: SharedPreferences, key: String, onChanged: (SharedPreferences, String) -> T): LiveData<T> {
    return PreferenceLiveData<T>(sharedPreferences, key) {
        onChanged(sharedPreferences, it)
    }
}