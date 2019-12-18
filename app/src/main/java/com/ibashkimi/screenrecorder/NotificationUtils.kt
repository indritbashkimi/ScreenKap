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

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build

const val RECORDING_NOTIFICATION_CHANNEL_ID = "channel_recording"
const val FINISH_NOTIFICATION_CHANNEL_ID = "channel_finished"

@TargetApi(26)
fun Context.createNotificationChannels() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        return
    }
    val notificationChannels = ArrayList<NotificationChannel>()
    val recordingChannel = NotificationChannel(
        RECORDING_NOTIFICATION_CHANNEL_ID,
        getString(R.string.notification_channel_recording),
        NotificationManager.IMPORTANCE_HIGH
    )
    recordingChannel.enableLights(true)
    recordingChannel.lightColor = Color.RED
    recordingChannel.setShowBadge(false)
    recordingChannel.enableVibration(false)
    recordingChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
    notificationChannels.add(recordingChannel)

    val finishChannel = NotificationChannel(
        FINISH_NOTIFICATION_CHANNEL_ID,
        getString(R.string.notification_channel_finish),
        NotificationManager.IMPORTANCE_DEFAULT
    )
    //finishChannel.enableLights(true)
    finishChannel.setShowBadge(true)
    finishChannel.enableVibration(false)
    finishChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
    notificationChannels.add(finishChannel)

    (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        .createNotificationChannels(notificationChannels)
}
