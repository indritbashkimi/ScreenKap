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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ibashkimi.screenrecorder.R
import com.ibashkimi.screenrecorder.databinding.AboutLibIntroBinding
import com.ibashkimi.screenrecorder.databinding.AboutLibraryBinding
import com.ibashkimi.screenrecorder.databinding.FragmentLicensesBinding
import java.security.InvalidParameterException


class LicensesFragment : Fragment() {

    private val intro: Int = R.string.about_lib_intro

    private val libraries: Array<Library> = arrayOf(
        Library(
            R.string.android_jetpack_name,
            R.string.android_jetpack_website,
            R.string.apache_v2
        ),
        Library(
            R.string.kotlin_name,
            R.string.kotlin_website,
            R.string.apache_v2
        ),
        Library(
            R.string.glide_name,
            R.string.glide_website,
            R.string.glide_license
        ),
        Library(
            R.string.material_components_name,
            R.string.material_components_website,
            R.string.apache_v2
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentLicensesBinding.inflate(inflater, container, false).run {
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = LibraryAdapter(libraries)
            root
        }
    }


    private inner class LibraryAdapter(val libs: Array<Library>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                VIEW_TYPE_INTRO -> {
                    LibraryIntroHolder(AboutLibIntroBinding.inflate(parent.inflater, parent, false))
                }
                VIEW_TYPE_LIBRARY -> {
                    LibraryHolder(AboutLibraryBinding.inflate(parent.inflater, parent, false))
                }
                else -> throw InvalidParameterException()
            }
        }

        private val ViewGroup.inflater: LayoutInflater
            get() = LayoutInflater.from(context)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (getItemViewType(position) == VIEW_TYPE_LIBRARY) {
                bindLibrary(holder as LibraryHolder, libs[position - 1])
            } else {
                (holder as LibraryIntroHolder).binding.intro.setText(intro)
            }
        }

        override fun getItemViewType(position: Int): Int {
            return if (position == 0) VIEW_TYPE_INTRO else VIEW_TYPE_LIBRARY
        }

        override fun getItemCount(): Int {
            return libs.size + 1 // + 1 for the static intro view
        }

        private fun bindLibrary(holder: LibraryHolder, lib: Library) {
            holder.binding.apply {
                libraryName.setText(lib.name)
                libraryLink.setText(lib.website)
                libraryLicense.setText(lib.license)

                val clickListener: View.OnClickListener = View.OnClickListener {
                    val position = holder.adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        CustomTabsIntent.Builder().build().launchUrl(
                            requireContext(),
                            Uri.parse(getString(lib.website))
                        )
                    }
                }
                root.setOnClickListener(clickListener)
            }
        }
    }

    private class LibraryHolder(val binding: AboutLibraryBinding) :
        RecyclerView.ViewHolder(binding.root)

    private class LibraryIntroHolder(val binding: AboutLibIntroBinding) :
        RecyclerView.ViewHolder(binding.root)

    /**
     * Models an open source library we want to credit
     */
    data class Library(
        @StringRes val name: Int,
        @StringRes val website: Int,
        @StringRes val license: Int
    )

    companion object {

        private const val VIEW_TYPE_INTRO = 0
        private const val VIEW_TYPE_LIBRARY = 1
    }
}