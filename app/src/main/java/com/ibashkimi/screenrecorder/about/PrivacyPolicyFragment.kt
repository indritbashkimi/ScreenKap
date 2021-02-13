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

import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import com.ibashkimi.screenrecorder.R
import com.ibashkimi.screenrecorder.databinding.FragmentPrivacyPolicyBinding

class PrivacyPolicyFragment : Fragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentPrivacyPolicyBinding.inflate(inflater, container, false).run {
            wvPrivacyPolicy.loadUrl("file:///android_asset/privacy_policy.html")
            root
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.privacy_policy, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.open_website) {
            CustomTabsIntent.Builder().build().launchUrl(
                requireContext(),
                Uri.parse(getString(R.string.privacy_policy_website))
            )
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
