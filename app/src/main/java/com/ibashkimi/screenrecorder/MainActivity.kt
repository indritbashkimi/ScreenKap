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

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.ibashkimi.screenrecorder.settings.PreferenceHelper

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            onFirstCreate()
        }

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        navController = findNavController(R.id.main_nav_host_fragment)

        findViewById<Toolbar?>(R.id.toolbar)?.let {
            setSupportActionBar(it)
            val appBarConfiguration = AppBarConfiguration(
                setOf(R.id.home, R.id.navigation_dialog, R.id.more_settings_dialog)
            )
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
        }
    }

    /**
     * Called the first time the activity is created.
     */
    private fun onFirstCreate() {
        PreferenceHelper(this).apply {
            // Apply theme before onCreate
            applyNightMode(nightMode)
            initIfFirstTimeAnd {
                createNotificationChannels()
            }
        }
    }

    override fun onSupportNavigateUp() = navController.navigateUp()

    companion object {
        const val ACTION_TOGGLE_RECORDING = "com.ibashkimi.screenrecorder.TOGGLE_RECORDING"
    }
}