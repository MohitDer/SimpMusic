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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.databinding.FragmentSettingsSpotifyBinding
import com.maxrave.simpmusic.extension.navigateSafe
import com.maxrave.simpmusic.extension.setEnabledAll
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@UnstableApi
@AndroidEntryPoint
class SpotifySettingsFragment : Fragment() {

    private var _binding: FragmentSettingsSpotifyBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<SettingsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsSpotifyBinding.inflate(inflater, container, false)
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
        viewModel.getSpotifyLogIn()
        viewModel.getSpotifyLyrics()
        viewModel.getSpotifyCanvas()

        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                val job23 = launch {
                    viewModel.spotifyLogIn.collect {
                        if (it) {
                            binding.tvSpotifyLogin.text = getString(R.string.logged_in)
                            setEnabledAll(binding.btEnableCanvas, true)
                            setEnabledAll(binding.btEnableSpotifyLyrics, true)
                        } else {
                            binding.tvSpotifyLogin.text = getString(R.string.intro_login_to_spotify)
                            setEnabledAll(binding.btEnableCanvas, false)
                            setEnabledAll(binding.btEnableSpotifyLyrics, false)
                        }
                    }
                }

                val job24 = launch {
                    viewModel.spotifyLyrics.collect {
                        if (it) {
                            binding.swEnableSpotifyLyrics.isChecked = true
                        } else {
                            binding.swEnableSpotifyLyrics.isChecked = false
                        }
                    }
                }

                val job25 = launch {
                    viewModel.spotifyCanvas.collect {
                        if (it) {
                            binding.swEnableCanvas.isChecked = true
                        } else {
                            binding.swEnableCanvas.isChecked = false
                        }
                    }
                }

                job23.join()
                job24.join()
                job25.join()
            }
        }

        binding.btSpotifyLogin.setOnClickListener {
            if (runBlocking { viewModel.spotifyLogIn.value }) {
                val subAlertDialogBuilder = MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.warning))
                    .setMessage(getString(R.string.log_out_warning))
                    .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setPositiveButton(getString(R.string.log_out)) { dialog, _ ->
                        viewModel.setSpotifyLogIn(false)
                    }
                subAlertDialogBuilder.show()
            } else {
                findNavController().navigateSafe(R.id.action_global_spotifyLogInFragment)
            }
        }

        binding.swEnableSpotifyLyrics.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                viewModel.setSpotifyLyrics(true)
            } else {
                viewModel.setSpotifyLyrics(false)
            }
        }

        binding.swEnableCanvas.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                viewModel.setSpotifyCanvas(true)
            } else {
                viewModel.setSpotifyCanvas(false)
            }
        }
    }
}