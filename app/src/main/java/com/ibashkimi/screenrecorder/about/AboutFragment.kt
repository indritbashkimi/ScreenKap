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

package com.ibashkimi.screenrecorder.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.ibashkimi.screenrecorder.BuildConfig
import com.ibashkimi.screenrecorder.R


class AboutFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_about, container, false)

        root.findViewById<TextView>(R.id.version).text =
                getString(R.string.version, BuildConfig.VERSION_NAME)
        root.findViewById<TextView>(R.id.source_code).setOnClickListener {
            CustomTabsIntent.Builder().build().launchUrl(requireContext(),
                    Uri.parse(requireContext().getString(R.string.app_source_code)))
        }
        root.findViewById<View>(R.id.send_feedback).setOnClickListener {
            sendFeedback()
        }
        root.findViewById<View>(R.id.privacy_policy).setOnClickListener(
                Navigation.createNavigateOnClickListener(R.id.action_about_to_privacy_policy)
        )
        root.findViewById<View>(R.id.licenses).setOnClickListener(
                Navigation.createNavigateOnClickListener(R.id.action_about_to_licenses)
        )

        return root
    }

    private fun sendFeedback() {
        val address = getString(R.string.author_email)
        val subject = getString(R.string.feedback_subject)

        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address"))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject)

        val chooserTitle = getString(R.string.feedback_chooser_title)
        startActivity(Intent.createChooser(emailIntent, chooserTitle))
    }
}
