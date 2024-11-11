package com.maxrave.simpmusic.ui.fragment.settings

import android.app.usage.StorageStatsManager
import android.os.Bundle
import android.os.storage.StorageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.LIMIT_CACHE_SIZE
import com.maxrave.simpmusic.databinding.FragmentSettingsStorageBinding
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.launch
import java.io.File

@UnstableApi
@AndroidEntryPoint
class StorageSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsStorageBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<SettingsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsStorageBinding.inflate(inflater, container, false)
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
        viewModel.getPlayerCacheSize()
        viewModel.getDownloadedCacheSize()
        viewModel.getPlayerCacheLimit()
        viewModel.getThumbCacheSize()

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                val job11 = launch {
                    viewModel.cacheSize.collect {
                        if (it != null) {
                            drawDataStat()
                            binding.tvPlayerCache.text =
                                getString(R.string.cache_size, bytesToMB(it).toString())
                        }
                    }
                }

                val job12 = launch {
                    viewModel.downloadedCacheSize.collect {
                        if (it != null) {
                            drawDataStat()
                            binding.tvDownloadedCache.text =
                                getString(R.string.cache_size, bytesToMB(it).toString())
                        }
                    }
                }

                val job20 = launch {
                    viewModel.playerCacheLimit.collect {
                        binding.tvLimitPlayerCache.text =
                            if (it != -1) "$it MB" else getString(R.string.unlimited)
                    }
                }

                val job22 = launch {
                    viewModel.thumbCacheSize.collect {
                        binding.tvThumbnailCache.text = getString(
                            R.string.cache_size, if (it != null) {
                                bytesToMB(it)
                            } else {
                                0
                            }.toString()
                        )
                    }
                }

                job11.join()
                job12.join()
                job20.join()
                job22.join()
            }
        }

        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.btStoragePlayerCache.setOnClickListener {
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.clear_player_cache))
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.clear)) { dialog, _ ->
                    viewModel.clearPlayerCache()
                    dialog.dismiss()
                }
            dialog.show()
        }

        binding.btStorageDownloadedCache.setOnClickListener {
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.clear_downloaded_cache))
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.clear)) { dialog, _ ->
                    viewModel.clearDownloadedCache()
                    dialog.dismiss()
                }
            dialog.show()
        }

        binding.btStorageThumbnailCache.setOnClickListener {
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.clear_thumbnail_cache))
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.clear)) { dialog, _ ->
                    viewModel.clearThumbnailCache()
                    dialog.dismiss()
                }
            dialog.show()
        }

        binding.btLimitPlayerCache.setOnClickListener {
            var checkedIndex = -1
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setSingleChoiceItems(LIMIT_CACHE_SIZE.items, -1) { _, which ->
                    checkedIndex = which
                }
                .setTitle(getString(R.string.limit_player_cache))
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.change)) { dialog, _ ->
                    if (checkedIndex != -1) {
                        viewModel.setPlayerCacheLimit(LIMIT_CACHE_SIZE.data[checkedIndex])
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.restart_app),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    dialog.dismiss()
                }
            dialog.show()
        }
    }

    private fun browseFiles(dir: File): Long {
        var dirSize: Long = 0
        if (!dir.listFiles().isNullOrEmpty()) {
            for (f in dir.listFiles()!!) {
                dirSize += f.length()
                if (f.isDirectory) {
                    dirSize += browseFiles(f)
                }
            }
        }
        return dirSize
    }

    private fun drawDataStat() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    val mStorageStatsManager =
                        ContextCompat.getSystemService(
                            requireContext(),
                            StorageStatsManager::class.java
                        )
                    if (mStorageStatsManager != null) {

                        val totalByte =
                            mStorageStatsManager.getTotalBytes(StorageManager.UUID_DEFAULT)
                        val freeSpace =
                            mStorageStatsManager.getFreeBytes(StorageManager.UUID_DEFAULT)
                        val usedSpace = totalByte - freeSpace
                        val simpMusicSize = browseFiles(requireContext().filesDir)
                        val otherApp = simpMusicSize.let { usedSpace.minus(it) }
                        val databaseSize =
                            simpMusicSize - viewModel.playerCache.cacheSpace - viewModel.downloadCache.cacheSpace
                        if (totalByte == freeSpace + otherApp + databaseSize + viewModel.playerCache.cacheSpace + viewModel.downloadCache.cacheSpace) {
                            (binding.flexBox.getChildAt(0).layoutParams as FlexboxLayout.LayoutParams).flexBasisPercent =
                                otherApp.toFloat().div(totalByte.toFloat())
                            (binding.flexBox.getChildAt(1).layoutParams as FlexboxLayout.LayoutParams).flexBasisPercent =
                                viewModel.downloadCache.cacheSpace.toFloat()
                                    .div(totalByte.toFloat())
                            (binding.flexBox.getChildAt(2).layoutParams as FlexboxLayout.LayoutParams).flexBasisPercent =
                                viewModel.playerCache.cacheSpace.toFloat().div(totalByte.toFloat())
                            (binding.flexBox.getChildAt(3).layoutParams as FlexboxLayout.LayoutParams).flexBasisPercent =
                                databaseSize.toFloat().div(totalByte.toFloat())
                            (binding.flexBox.getChildAt(4).layoutParams as FlexboxLayout.LayoutParams).flexBasisPercent =
                                freeSpace.toFloat().div(totalByte.toFloat())
                        }
                    }
                }
            }
        }
    }

    private fun bytesToMB(bytes: Long): Long {
        val mbInBytes = 1024 * 1024
        return bytes / mbInBytes
    }
}