package com.maxrave.simpmusic.ui.fragment.settings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.QUALITY
import com.maxrave.simpmusic.common.SUPPORTED_LANGUAGE
import com.maxrave.simpmusic.common.SUPPORTED_LOCATION
import com.maxrave.simpmusic.common.VIDEO_QUALITY
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.databinding.FragmentSettingsContentBinding
import com.maxrave.simpmusic.extension.setEnabledAll
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.launch

@UnstableApi
@AndroidEntryPoint
class ContentSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsContentBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<SettingsViewModel>()
    private val sharedViewModel by activityViewModels<SharedViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsContentBinding.inflate(inflater, container, false)
        binding.topAppBarLayout.applyInsetter {
            type(statusBars = true) {
                margin()
            }
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        viewModel.getLoggedIn()
    }

    @UnstableApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getLoggedIn()
        viewModel.getLocation()
        viewModel.getLanguage()
        viewModel.getQuality()
        viewModel.getNormalizeVolume()
        viewModel.getSendBackToGoogle()
        viewModel.getHomeLimit()
        viewModel.getPlayVideoInsteadOfAudio()
        viewModel.getVideoQuality()

        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                val job2 = launch {
                    viewModel.playVideoInsteadOfAudio.collect {
                        if (it == DataStoreManager.TRUE) {
                            binding.swEnableVideo.isChecked = true
                            setEnabledAll(binding.btVideoQuality, true)
                        } else if (it == DataStoreManager.FALSE) {
                            binding.swEnableVideo.isChecked = false
                            setEnabledAll(binding.btVideoQuality, false)
                        }
                    }
                }

                val job3 = launch {
                    viewModel.videoQuality.collect {
                        binding.tvVideoQuality.text = it
                    }
                }
                val job7 = launch {
                    viewModel.sendBackToGoogle.collect {
                        binding.swSaveHistory.isChecked = it == DataStoreManager.TRUE
                    }
                }
                val job8 = launch {
                    viewModel.location.collect {
                        binding.tvContentCountry.text = it
                    }
                }
                val job9 = launch {
                    viewModel.language.collect {
                        if (it != null) {
                            if (it.contains("id") || it.contains("in")) {
                                binding.tvLanguage.text = "Bahasa Indonesia"
                            } else {
                                val temp =
                                    SUPPORTED_LANGUAGE.items.getOrNull(
                                        SUPPORTED_LANGUAGE.codes.indexOf(
                                            it
                                        )
                                    )
                                binding.tvLanguage.text = temp
                            }
                        } else {
                            binding.tvLanguage.text = "Automatic"
                        }
                    }
                }
                val job10 = launch {
                    viewModel.quality.collect {
                        binding.tvQuality.text = it
                    }
                }
                val job26 = launch {
                    viewModel.homeLimit.collect {
                        binding.tvHomeLimit.text = it.toString()
                        if (it != null) {
                            binding.sliderHomeLimit.value = it.toFloat()
                        }
                    }
                }
                job2.join()
                job3.join()
                job7.join()
                job8.join()
                job9.join()
                job10.join()
                job26.join()
            }
        }

        binding.sliderHomeLimit.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                // Responds to when slider's touch event is being started
            }

            override fun onStopTrackingTouch(slider: Slider) {
                viewModel.setHomeLimit(slider.value.toInt())
            }
        })

        binding.btContentCountry.setOnClickListener {
            var checkedIndex = -1
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setSingleChoiceItems(SUPPORTED_LOCATION.items, -1) { _, which ->
                    checkedIndex = which
                }
                .setTitle(getString(R.string.content_country))
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.change)) { dialog, _ ->
                    if (checkedIndex != -1) {
                        viewModel.changeLocation(SUPPORTED_LOCATION.items[checkedIndex].toString())
                    }
                    dialog.dismiss()
                }
            dialog.show()
        }

        binding.btLanguage.setOnClickListener {
            var checkedIndex = -1
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setSingleChoiceItems(SUPPORTED_LANGUAGE.items, -1) { _, which ->
                    checkedIndex = which
                }
                .setTitle(getString(R.string.language))
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.change)) { dialog, _ ->
                    if (checkedIndex != -1) {
                        val alertDialog = MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.warning)
                            .setMessage(R.string.change_language_warning)
                            .setNegativeButton(getString(R.string.cancel)) { d, _ ->
                                d.dismiss()
                                dialog.dismiss()
                            }
                            .setPositiveButton(getString(R.string.change)) { d, _ ->
                                viewModel.changeLanguage(SUPPORTED_LANGUAGE.codes[checkedIndex])
                                if (SUPPORTED_LANGUAGE.codes.getOrNull(checkedIndex) != null) {
                                    runCatching {
                                        SUPPORTED_LANGUAGE.items[SUPPORTED_LANGUAGE.codes.indexOf(
                                            SUPPORTED_LANGUAGE.codes[checkedIndex]
                                        )]
                                    }.onSuccess { temp ->
                                        binding.tvLanguage.text = temp
                                        val localeList = LocaleListCompat.forLanguageTags(
                                            SUPPORTED_LANGUAGE.codes.getOrNull(checkedIndex)
                                        )
                                        sharedViewModel.activityRecreate()
                                        AppCompatDelegate.setApplicationLocales(localeList)
                                    }
                                        .onFailure {
                                            Toast.makeText(
                                                requireContext(),
                                                getString(R.string.invalid_language_code),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                                d.dismiss()
                                dialog.dismiss()
                            }
                        alertDialog.show()
                    }
                    dialog.dismiss()
                }
            dialog.show()
        }

        binding.btQuality.setOnClickListener {
            var checkedIndex = -1
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setSingleChoiceItems(QUALITY.items, -1) { _, which ->
                    checkedIndex = which
                }
                .setTitle(getString(R.string.quality))
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.change)) { dialog, _ ->
                    if (checkedIndex != -1) {
                        viewModel.changeQuality(checkedIndex)
                    }
                    dialog.dismiss()
                }
            dialog.show()

        }

        binding.btVideoQuality.setOnClickListener {
            var checkedIndex = -1
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setSingleChoiceItems(VIDEO_QUALITY.items, -1) { _, which ->
                    checkedIndex = which
                }
                .setTitle(getString(R.string.quality_video))
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.change)) { dialog, _ ->
                    if (checkedIndex != -1) {
                        viewModel.changeVideoQuality(checkedIndex)
                    }
                    dialog.dismiss()
                }
            dialog.show()
        }

        binding.swEnableVideo.setOnCheckedChangeListener { _, checked ->
            val test = viewModel.playVideoInsteadOfAudio.value
            val checkReal = (test == DataStoreManager.TRUE) != checked
            if (checkReal) {
                val dialog = MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.warning))
                    .setMessage(getString(R.string.play_video_instead_of_audio_warning))
                    .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                        binding.swEnableVideo.isChecked = false
                        dialog.dismiss()
                    }
                    .setPositiveButton(getString(R.string.change)) { dialog, _ ->
                        viewModel.clearPlayerCache()
                        if (checked) {
                            viewModel.setPlayVideoInsteadOfAudio(true)
                        } else {
                            viewModel.setPlayVideoInsteadOfAudio(false)
                        }
                        dialog.dismiss()
                    }
                dialog.show()
            }
        }

        binding.swSaveHistory.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                viewModel.setSendBackToGoogle(true)
            } else {
                viewModel.setSendBackToGoogle(false)
            }
        }
    }
}