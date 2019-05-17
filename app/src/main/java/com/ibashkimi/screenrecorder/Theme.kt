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

package com.ibashkimi.screenrecorder

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate


fun applyGlobalNightMode(nightMode: String) {
    AppCompatDelegate.setDefaultNightMode(when (nightMode) {
        "dark" -> AppCompatDelegate.MODE_NIGHT_YES
        "system_default" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        "battery_saver" -> AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
        "light" -> AppCompatDelegate.MODE_NIGHT_NO
        else -> throw IllegalArgumentException("Invalid night mode $nightMode.")
    })
}

fun Activity.applyNightMode(nightMode: String) {
    applyGlobalNightMode(nightMode)
    recreate()
}

@Suppress("unused")
fun AppCompatActivity.applyLocalNightMode(nightMode: String) {
    delegate.localNightMode = when (nightMode) {
        "dark" -> AppCompatDelegate.MODE_NIGHT_YES
        "system_default" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        "battery_saver" -> AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
        "light" -> AppCompatDelegate.MODE_NIGHT_NO
        else -> throw IllegalArgumentException("Invalid night mode $nightMode.")
    }
    delegate.applyDayNight()
}