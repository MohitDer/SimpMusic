package com.maxrave.simpmusic.ui.fragment.settings

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import com.maxrave.simpmusic.databinding.FragmentSettingsAboutBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter

@UnstableApi
@AndroidEntryPoint
class AboutSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsAboutBinding.inflate(inflater, container, false)
        binding.topAppBarLayout.applyInsetter {
            type(statusBars = true) {
                margin()
            }
        }
        return binding.root
    }

    @UnstableApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        val packageManager: PackageManager = requireContext().packageManager
        try {
            val packageInfo: PackageInfo =
                packageManager.getPackageInfo(requireContext().packageName, 0)
            val versionName = packageInfo.versionName
            binding.tvVersion.text = "v$versionName"
        } catch (_: PackageManager.NameNotFoundException) {
        }
    }
}