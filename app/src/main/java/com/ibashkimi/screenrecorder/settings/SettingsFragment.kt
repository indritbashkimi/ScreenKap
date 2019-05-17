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

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.preference.*
import com.ibashkimi.screenrecorder.R
import com.ibashkimi.screenrecorder.applyNightMode


class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var recordAudio: SwitchPreference

    private val realDisplayMetrics: DisplayMetrics
        get() {
            val metrics = DisplayMetrics()
            val window = requireActivity().getSystemService(Context.WINDOW_SERVICE) as WindowManager
            window.defaultDisplay.getRealMetrics(metrics)
            return metrics
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings)

        initResolutionPreference(findPreference(getString(R.string.pref_key_resolution))!!)

        onSharedPreferenceChanged(preferenceScreen.sharedPreferences, getString(R.string.pref_key_file_prefix))

        recordAudio = findPreference(getString(R.string.pref_key_audio))!!
        checkAudioRecPermission()
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences
                .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences
                .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            getString(R.string.pref_key_dark_theme) -> {
                requireActivity().applyNightMode(
                        PreferenceHelper(requireContext()).nightMode
                )
            }
            getString(R.string.pref_key_resolution) -> initResolutionPreference(findPreference<Preference>(key) as ListPreference)
            getString(R.string.pref_key_audio) -> if (recordAudio.isChecked) {
                requestAudioPermission()
            }
            getString(R.string.pref_key_file_prefix) -> {
                val prefix = sharedPreferences.getString(key, "")!!
                val trimmedPrefix = prefix.trim()
                if (prefix != trimmedPrefix) {
                    sharedPreferences.edit().putString(key, trimmedPrefix).apply()
                } else {
                    findPreference<EditTextPreference>(key)?.summary = prefix
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_AUDIO_PERMISSION -> {
                checkAudioRecPermission()
            }
        }
    }

    private fun checkAudioRecPermission() {
        if (recordAudio.isChecked) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                recordAudio.isChecked = false
                if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                    AlertDialog.Builder(requireContext())
                            .setMessage(R.string.audio_permission_need)
                            .setPositiveButton(R.string.dialog_ask_again) { _, _ ->
                                requestAudioPermission()
                            }
                            .setNegativeButton(android.R.string.cancel) { dialogInterface, _ ->
                                dialogInterface.dismiss()
                            }
                            .create().show()
                } else {
                    AlertDialog.Builder(requireContext())
                            .setMessage(R.string.audio_permission_need)
                            .setPositiveButton(R.string.dialog_open_app_settings) { _, _ ->
                                startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", requireActivity().packageName, null)))
                            }
                            .setNegativeButton(android.R.string.cancel) { dialogInterface, _ ->
                                dialogInterface.dismiss()
                            }
                            .create().show()
                }
            }
        }
    }

    private fun requestAudioPermission() {
        requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_AUDIO_PERMISSION)
    }

    private fun initResolutionPreference(resolutionPreference: ListPreference) {
        val resolutionValues = resources.getStringArray(R.array.resolution_values)
                .filter { it.toInt() <= realDisplayMetrics.widthPixels }
        val resolutionTitles = resources.getStringArray(R.array.resolution_entries)
                .slice(0 until resolutionValues.size)
        resolutionPreference.entryValues = resolutionValues.toTypedArray()
        resolutionPreference.entries = resolutionTitles.toTypedArray()
    }

    companion object {
        private const val REQUEST_AUDIO_PERMISSION = 222
    }
}
