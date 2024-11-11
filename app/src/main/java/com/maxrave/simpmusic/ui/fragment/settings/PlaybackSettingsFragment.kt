package com.maxrave.simpmusic.ui.fragment.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.databinding.FragmentSettingsPlaybackBinding
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.launch

@UnstableApi
@AndroidEntryPoint
class PlaybackSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsPlaybackBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<SettingsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsPlaybackBinding.inflate(inflater, container, false)
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
        viewModel.getSavedPlaybackState()
        viewModel.getSaveRecentSongAndQueue()

        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                val job15 = launch {
                    viewModel.savedPlaybackState.collect {
                        binding.swSavePlaybackState.isChecked = it == DataStoreManager.TRUE
                    }
                }
                val job16 = launch {
                    viewModel.saveRecentSongAndQueue.collect {
                        binding.swSaveLastPlayed.isChecked = it == DataStoreManager.TRUE
                    }
                }
                job15.join()
                job16.join()
            }
        }

        binding.swSavePlaybackState.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                viewModel.setSavedPlaybackState(true)
            } else {
                viewModel.setSavedPlaybackState(false)
            }
        }
        binding.swSaveLastPlayed.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                viewModel.setSaveLastPlayed(true)
            } else {
                viewModel.setSaveLastPlayed(false)
            }
        }
    }
}