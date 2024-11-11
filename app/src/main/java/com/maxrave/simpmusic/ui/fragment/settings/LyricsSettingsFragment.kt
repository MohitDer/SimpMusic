package com.maxrave.simpmusic.ui.fragment.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.LYRICS_PROVIDER
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.databinding.FragmentSettingsLyricsBinding
import com.maxrave.simpmusic.extension.navigateSafe
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter

@UnstableApi
@AndroidEntryPoint
class LyricsSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsLyricsBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<SettingsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsLyricsBinding.inflate(inflater, container, false)
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
        viewModel.getTranslationLanguage()
        viewModel.getLyricsProvider()
        viewModel.getUseTranslation()
        viewModel.getMusixmatchLoggedIn()

        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.btMainLyricsProvider.setOnClickListener {
            var checkedIndex = -1
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.main_lyrics_provider))
                .setSingleChoiceItems(LYRICS_PROVIDER.items, -1) { _, which ->
                    checkedIndex = which
                }
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.change)) { dialog, _ ->
                    if (checkedIndex != -1) {
                        if (checkedIndex == 0) {
                            viewModel.setLyricsProvider(DataStoreManager.MUSIXMATCH)
                            binding.tvMainLyricsProvider.text = DataStoreManager.MUSIXMATCH
                        } else if (checkedIndex == 1) {
                            viewModel.setLyricsProvider(DataStoreManager.YOUTUBE)
                            binding.tvMainLyricsProvider.text = DataStoreManager.YOUTUBE
                        }
                    }
                    viewModel.getLyricsProvider()
                    dialog.dismiss()
                }
            dialog.show()
        }

        binding.btTranslationLanguage.setOnClickListener {
            val materialAlertDialogBuilder = MaterialAlertDialogBuilder(requireContext())
            materialAlertDialogBuilder.setTitle(getString(R.string.translation_language))
            materialAlertDialogBuilder.setMessage(getString(R.string.translation_language_message))
            val editText = EditText(requireContext())
            materialAlertDialogBuilder.setView(editText)
            materialAlertDialogBuilder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            materialAlertDialogBuilder.setPositiveButton(getString(R.string.change)) { dialog, _ ->
                if (editText.text.toString().isNotEmpty()) {
                    if (editText.text.toString().length == 2) {
                        viewModel.setTranslationLanguage(editText.text.toString())
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.invalid_language_code),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    if (viewModel.language.value != null && viewModel.language.value!!.length >= 2) {
                        viewModel.language.value?.slice(0..1)
                            ?.let { it1 -> viewModel.setTranslationLanguage(it1) }
                    }
                }
                dialog.dismiss()
            }
            materialAlertDialogBuilder.show()
        }

        binding.btMusixmatchLogin.setOnClickListener {
            if (viewModel.musixmatchLoggedIn.value == DataStoreManager.TRUE) {
                viewModel.clearMusixmatchCookie()
                Toast.makeText(requireContext(), getString(R.string.logged_out), Toast.LENGTH_SHORT)
                    .show()
            } else if (viewModel.musixmatchLoggedIn.value == DataStoreManager.FALSE) {
                findNavController().navigateSafe(R.id.action_global_musixmatchFragment)
            }
        }
    }
}