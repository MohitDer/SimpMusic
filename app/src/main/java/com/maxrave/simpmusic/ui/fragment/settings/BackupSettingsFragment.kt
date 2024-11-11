package com.maxrave.simpmusic.ui.fragment.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.databinding.FragmentSettingsBackupBinding
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@UnstableApi
@AndroidEntryPoint
class BackupSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBackupBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<SettingsViewModel>()

    private val backupLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
            if (uri != null) {
                viewModel.backup(requireContext(), uri)
            }
        }

    private val restoreLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                viewModel.restore(requireContext(), uri)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsBackupBinding.inflate(inflater, container, false)
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

        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.btBackup.setOnClickListener {
            val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            backupLauncher.launch(
                "${getString(R.string.app_name)}_${
                    LocalDateTime.now().format(formatter)
                }.backup"
            )
        }

        binding.btRestore.setOnClickListener {
            restoreLauncher.launch(arrayOf("application/octet-stream"))
        }
    }
}