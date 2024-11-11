package com.maxrave.simpmusic.ui.fragment.settings

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.databinding.FragmentSettingsAudioBinding
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.launch

@UnstableApi
@AndroidEntryPoint
class AudioSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsAudioBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<SettingsViewModel>()
    private val sharedViewModel by activityViewModels<SharedViewModel>()

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsAudioBinding.inflate(inflater, container, false)
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

        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        viewModel.getLoggedIn()
        viewModel.getNormalizeVolume()
        viewModel.getSkipSilent()

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                val job13 = launch {
                    viewModel.normalizeVolume.collect {
                        binding.swNormalizeVolume.isChecked = it == DataStoreManager.TRUE
                    }
                }
                val job14 = launch {
                    viewModel.skipSilent.collect {
                        binding.swSkipSilent.isChecked = it == DataStoreManager.TRUE
                    }
                }

                job13.join()
                job14.join()
            }
        }

        binding.btEqualizer.setOnClickListener {
            val eqIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
            eqIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, requireContext().packageName)
            eqIntent.putExtra(
                AudioEffect.EXTRA_AUDIO_SESSION,
                sharedViewModel.simpleMediaServiceHandler?.player?.audioSessionId
            )
            eqIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            val packageManager = requireContext().packageManager
            val resolveInfo: List<*> = packageManager.queryIntentActivities(eqIntent, 0)
            if (resolveInfo.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.no_equalizer),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                resultLauncher.launch(eqIntent)
            }
        }

        binding.swNormalizeVolume.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                viewModel.setNormalizeVolume(true)
            } else {
                viewModel.setNormalizeVolume(false)
            }
        }

        binding.swSkipSilent.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                viewModel.setSkipSilent(true)
            } else {
                viewModel.setSkipSilent(false)
            }
        }
    }
}