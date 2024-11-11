package com.maxrave.simpmusic.ui.fragment.settings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.adapter.account.AccountAdapter
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.databinding.FragmentSettingsMainBinding
import com.maxrave.simpmusic.databinding.YoutubeAccountDialogBinding
import com.maxrave.simpmusic.extension.navigateSafe
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@UnstableApi
@AndroidEntryPoint
class MainSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsMainBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<SettingsViewModel>()
    private val sharedViewModel by activityViewModels<SharedViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsMainBinding.inflate(inflater, container, false)
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
        viewModel.getAllGoogleAccount()

        lifecycleScope.launch {

            val job1 = launch {
                sharedViewModel.logInToYouTube().distinctUntilChanged().collect {
                    if (it == DataStoreManager.TRUE) {
                        binding.layoutSignedIn.visibility = View.VISIBLE
                        binding.layoutSignedOut.visibility = View.GONE
                    } else {
                        binding.layoutSignedIn.visibility = View.GONE
                        binding.layoutSignedOut.visibility = View.VISIBLE
                    }
                }
            }

            val job2 = launch {
                viewModel.googleAccounts.collect {
                    if (it != null) {
                        val selectedAccount =
                            it.firstOrNull { googleAccountEntity -> googleAccountEntity.isUsed }
                        Log.d("Account", "onViewCreated: "+selectedAccount?.name)
                        Log.d("Account", "onViewCreated: "+selectedAccount?.email)
                        Log.d("Account", "onViewCreated: "+selectedAccount?.thumbnailUrl)
                        binding.tvAccountName.text = selectedAccount?.name
                        binding.tvEmail.text = selectedAccount?.email
                        binding.ivAccount.load(selectedAccount?.thumbnailUrl) {
                            crossfade(true)
                            placeholder(android.R.drawable.ic_menu_gallery)
                            error(android.R.drawable.ic_menu_report_image)
                        }
                    }
                }
            }

            job1.join()
            job2.join()
        }

        binding.signIn.setOnClickListener {
            findNavController().navigateSafe(R.id.action_global_logInFragment)
        }

        binding.signOut.setOnClickListener {
            val subAlertDialogBuilder = MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.warning))
                .setMessage(getString(R.string.log_out_warning))
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.log_out)) { dialog, _ ->
                    viewModel.logOutAllYouTube()
                }
            subAlertDialogBuilder.show()
        }

        binding.switchAccount.setOnClickListener {
            val dialog = MaterialAlertDialogBuilder(requireContext())
            val accountBinding =
                YoutubeAccountDialogBinding.inflate(LayoutInflater.from(requireContext()))
            val accountAdapter = AccountAdapter(arrayListOf())
            dialog.setView(accountBinding.root)
            val alertDialog = dialog.create()

            accountBinding.rvAccount.apply {
                adapter = accountAdapter
                layoutManager = LinearLayoutManager(requireContext())
            }

            accountBinding.btClose.setOnClickListener {
                alertDialog.dismiss()
            }

            accountAdapter.setOnAccountClickListener(object :
                AccountAdapter.OnAccountClickListener {
                override fun onAccountClick(pos: Int) {
                    if (accountAdapter.getAccountList().getOrNull(pos) != null) {
                        viewModel.setUsedAccount(accountAdapter.getAccountList()[pos])
                        alertDialog.dismiss()
                    }
                }
            })

            viewModel.getAllGoogleAccount()
            accountBinding.loadingLayout.visibility = View.VISIBLE

            lifecycleScope.launch {
                val job2 = launch {
                    viewModel.loading.collectLatest {
                        if (it) {
                            accountBinding.loadingLayout.visibility = View.VISIBLE
                        } else {
                            accountBinding.loadingLayout.visibility = View.GONE
                        }
                    }
                }

                val job3 = launch {
                    viewModel.googleAccounts.collect {
                        if (it != null) {
                            accountAdapter.updateAccountList(it)
                            accountBinding.tvNoAccount.visibility = View.GONE
                            accountBinding.rvAccount.visibility = View.VISIBLE
                        } else {
                            accountAdapter.updateAccountList(arrayListOf())
                            accountBinding.tvNoAccount.visibility = View.VISIBLE
                            accountBinding.rvAccount.visibility = View.GONE
                        }
                    }
                }

                job2.join()
                job3.join()
            }
            alertDialog.show()
        }

        binding.addAccount.setOnClickListener {
            findNavController().navigateSafe(R.id.action_global_logInFragment)
        }

        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.settingsContent.setOnClickListener {
            findNavController().navigateSafe(R.id.action_mainSettingsFragment_to_contentSettingsFragment)
        }

        binding.settingsAudio.setOnClickListener {
            findNavController().navigateSafe(R.id.action_mainSettingsFragment_to_audioSettingsFragment)
        }

        binding.settingsPlayback.setOnClickListener {
            findNavController().navigateSafe(R.id.action_mainSettingsFragment_to_playbackSettingsFragment)
        }

        binding.settingsLyrics.setOnClickListener {
            findNavController().navigateSafe(R.id.action_mainSettingsFragment_to_lyricsSettingsFragment)
        }

        binding.settingsSpotify.setOnClickListener {
            findNavController().navigateSafe(R.id.action_mainSettingsFragment_to_spotifySettingsFragment)
        }

        binding.settingsStorage.setOnClickListener {
            findNavController().navigateSafe(R.id.action_mainSettingsFragment_to_storageSettingsFragment)
        }

        binding.settingsBackup.setOnClickListener {
            findNavController().navigateSafe(R.id.action_mainSettingsFragment_to_backupSettingsFragment)
        }

        binding.settingsAbout.setOnClickListener {
            findNavController().navigateSafe(R.id.action_mainSettingsFragment_to_aboutSettingsFragment)
        }
    }
}