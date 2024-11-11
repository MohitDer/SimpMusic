package com.maxrave.simpmusic.ui.fragment.player

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.TransitionDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.request.CachePolicy
import coil.size.Size
import coil.transform.RoundedCornersTransformation
import coil.transform.Transformation
import com.daimajia.swipe.SwipeLayout
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.adapter.artist.SeeArtistOfNowPlayingAdapter
import com.maxrave.simpmusic.adapter.lyrics.LyricsAdapter
import com.maxrave.simpmusic.adapter.playlist.AddToAPlaylistAdapter
import com.maxrave.simpmusic.ads.admob.AdMobConfig
import com.maxrave.simpmusic.ads.admob.AdMobInterstitialAdDownload
import com.maxrave.simpmusic.ads.config.AdConfig
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.common.Config.ALBUM_CLICK
import com.maxrave.simpmusic.common.Config.MINIPLAYER_CLICK
import com.maxrave.simpmusic.common.Config.PLAYLIST_CLICK
import com.maxrave.simpmusic.common.Config.SHARE
import com.maxrave.simpmusic.common.Config.SONG_CLICK
import com.maxrave.simpmusic.common.Config.VIDEO_CLICK
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.common.LYRICS_PROVIDER
import com.maxrave.simpmusic.common.STATUS_DONE
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.PairSongLocalPlaylist
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.metadata.Line
import com.maxrave.simpmusic.data.model.metadata.MetadataSong
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.databinding.BottomSheetAddToAPlaylistBinding
import com.maxrave.simpmusic.databinding.BottomSheetNowPlayingBinding
import com.maxrave.simpmusic.databinding.BottomSheetSeeArtistOfNowPlayingBinding
import com.maxrave.simpmusic.databinding.BottomSheetSleepTimerBinding
import com.maxrave.simpmusic.databinding.FragmentNowPlayingBinding
import com.maxrave.simpmusic.downloader.ExtractorHelper
import com.maxrave.simpmusic.downloader.util.PermissionHelper
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.navigateSafe
import com.maxrave.simpmusic.extension.removeConflicts
import com.maxrave.simpmusic.extension.setEnabledAll
import com.maxrave.simpmusic.extension.setTextAnimation
import com.maxrave.simpmusic.extension.toListName
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.service.RepeatState
import com.maxrave.simpmusic.ui.fragment.download.DownloadDialog
import com.maxrave.simpmusic.ui.fragment.loading.LoadingDialogFragment
import com.maxrave.simpmusic.utils.CenterLayoutManager
import com.maxrave.simpmusic.utils.DisableTouchEventRecyclerView
import com.maxrave.simpmusic.utils.InteractiveTextMaker
import com.maxrave.simpmusic.utils.LanguagePreference
import com.maxrave.simpmusic.utils.Resource
import com.maxrave.simpmusic.utils.SharedPrefsHelper
import com.maxrave.simpmusic.viewModel.LyricsProvider
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.schabi.newpipe.extractor.ServiceList
import java.time.LocalDateTime
import java.util.UUID

@UnstableApi
@AndroidEntryPoint
class NowPlayingFragment : Fragment() {

    val viewModel by activityViewModels<SharedViewModel>()
    private var _binding: FragmentNowPlayingBinding? = null
    private val binding get() = _binding!!
    private var metadataCurSong: MetadataSong? = null
    private var videoId: String? = null
    private var from: String? = null
    private var type: String? = null
    private var index: Int? = null
    private var downloaded: Int? = null
    private var playlistId: String? = null
    private var gradientDrawable: GradientDrawable? = null
    private var lyricsBackground: Int? = null
    private lateinit var lyricsAdapter: LyricsAdapter
    private lateinit var lyricsFullAdapter: LyricsAdapter
    private lateinit var disableScrolling: DisableTouchEventRecyclerView
    private var overlayJob: Job? = null
    private var canvasOverlayJob: Job? = null
    private var player: ExoPlayer? = null
    private var isFullScreen = false
    private lateinit var adView: AdView

    override fun onResume() {
        super.onResume()
        val track = viewModel.canvas.value?.canvases?.firstOrNull()
        if (track != null && track.canvas_url.contains(".mp4")) {
            player?.stop()
            player?.release()
            player = ExoPlayer.Builder(requireContext()).build()
            binding.playerCanvas.player = player
            binding.playerCanvas.resizeMode =
                androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            player?.repeatMode = Player.REPEAT_MODE_ONE
            player?.setMediaItem(
                MediaItem.fromUri(track.canvas_url)
            )
            player?.prepare()
            player?.play()
        }
        if (::adView.isInitialized) {
            AdMobConfig.resume(adView)
        }
    }

    private fun getScreenHeight(activity: Activity): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = activity.windowManager.currentWindowMetrics
            (windowMetrics.bounds.height())
        } else {
            val displayMetrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
            (displayMetrics.heightPixels)
        }
    }

    override fun onStop() {
        super.onStop()
        player?.stop()
        player?.release()
        canvasOverlayJob?.cancel()
    }

    override fun onPause() {
        if (::adView.isInitialized) {
            AdMobConfig.pause(adView)
        }
        super.onPause()
        player?.stop()
        player?.release()
        canvasOverlayJob?.cancel()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNowPlayingBinding.inflate(inflater, container, false)
        binding.topAppBar.applyInsetter {
            type(statusBars = true) {
                margin()
            }
        }
        activity?.window?.navigationBarColor = Color.TRANSPARENT
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.canvasLayout.apply {
            val layoutParamsCopy = layoutParams
            layoutParamsCopy.height = getScreenHeight(requireActivity())
            layoutParams = layoutParamsCopy
        }
        val activity = requireActivity()
        val bottom = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        val miniplayer = activity.findViewById<SwipeLayout>(R.id.miniplayer)

        bottom.visibility = View.GONE
        miniplayer.visibility = View.GONE
        binding.lyricsFullLayout.visibility = View.GONE
        binding.buffered.max = 100
        type = arguments?.getString("type")
        videoId = arguments?.getString("videoId")
        from = arguments?.getString("from") ?: viewModel.from.value
        index = arguments?.getInt("index")
        downloaded = arguments?.getInt("downloaded")
        playlistId = arguments?.getString("playlistId")

        disableScrolling = DisableTouchEventRecyclerView()
//        LanguagePreference.setAppLanguage(requireContext(), LanguagePreference.getSelectedLanguage(requireContext()))



        lyricsAdapter = LyricsAdapter(null)
        lyricsAdapter.setOnItemClickListener(object : LyricsAdapter.OnItemClickListener {
            override fun onItemClick(line: Line?) {
            }
        })
        lyricsFullAdapter = LyricsAdapter(null)
        lyricsFullAdapter.setOnItemClickListener(object : LyricsAdapter.OnItemClickListener {
            override fun onItemClick(line: Line?) {
                if (line != null) {
                    val duration = runBlocking { viewModel.duration.first() }
                    if (duration > 0 && line.startTimeMs.toLong() < duration) {
                        val seek =
                            ((line.startTimeMs.toLong() * 100).toDouble() / duration).toFloat()
                        viewModel.onUIEvent(UIEvent.UpdateProgress(seek))
                    }
                }
            }
        })
        binding.rvLyrics.apply {
            adapter = lyricsAdapter
            layoutManager = CenterLayoutManager(requireContext())
        }
        binding.rvFullLyrics.apply {
            adapter = lyricsFullAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        binding.playerView.player = viewModel.simpleMediaServiceHandler?.player

        when (type) {
            SONG_CLICK -> {
                viewModel.playlistId.value = null
                if (viewModel.videoId.value == videoId) {
                    gradientDrawable = viewModel.gradientDrawable.value
                    lyricsBackground = viewModel.lyricsBackground.value
                    metadataCurSong = viewModel.metadata.value?.data
                    updateUIfromCurrentMediaItem(viewModel.getCurrentMediaItem())
                } else {
                    binding.ivArt.setImageResource(0)
                    binding.loadingArt.visibility = View.VISIBLE
                    viewModel.gradientDrawable.postValue(null)
                    viewModel.lyricsBackground.postValue(null)
                    binding.tvSongTitle.visibility = View.GONE
                    binding.tvSongArtist.visibility = View.GONE
                    Queue.getNowPlaying()?.let {
                        viewModel.simpleMediaServiceHandler?.reset()
                        viewModel.resetRelated()
                        viewModel.loadMediaItemFromTrack(it, SONG_CLICK)
                        viewModel.videoId.postValue(it.videoId)
                        viewModel.from.postValue(from)
                        updateUIfromQueueNowPlaying()
                    }
                }
            }

            SHARE -> {
                viewModel.playlistId.value = null
                viewModel.stopPlayer()
                binding.ivArt.setImageResource(0)
                binding.loadingArt.visibility = View.VISIBLE
                viewModel.gradientDrawable.postValue(null)
                viewModel.lyricsBackground.postValue(null)
                binding.tvSongTitle.visibility = View.GONE
                binding.tvSongArtist.visibility = View.GONE
                if (videoId != null) {
                    viewModel.getSongFull(videoId!!)
                    viewModel.songFull.observe(viewLifecycleOwner) {
                        if (it != null && it.videoDetails?.videoId == videoId && it.videoDetails?.videoId != null) {
                            val track = it.toTrack()
                            Queue.clear()
                            Queue.setNowPlaying(track)
                            viewModel.simpleMediaServiceHandler?.reset()
                            viewModel.resetRelated()
                            viewModel.loadMediaItemFromTrack(track, SHARE)
                            viewModel.videoId.postValue(track.videoId)
                            viewModel.from.postValue(from)
                            updateUIfromQueueNowPlaying()
                            miniplayer.visibility = View.GONE
                            bottom.visibility = View.GONE
                        }
                    }
                }
            }

            VIDEO_CLICK -> {
                viewModel.playlistId.value = null
                if (viewModel.videoId.value == videoId) {
                    gradientDrawable = viewModel.gradientDrawable.value
                    lyricsBackground = viewModel.lyricsBackground.value
                    metadataCurSong = viewModel.metadata.value?.data
                    updateUIfromCurrentMediaItem(viewModel.getCurrentMediaItem())
                } else {
                    binding.ivArt.setImageResource(0)
                    binding.loadingArt.visibility = View.VISIBLE
                    viewModel.gradientDrawable.postValue(null)
                    viewModel.lyricsBackground.postValue(null)
                    binding.tvSongTitle.visibility = View.GONE
                    binding.tvSongArtist.visibility = View.GONE
                    Queue.getNowPlaying()?.let {
                        viewModel.simpleMediaServiceHandler?.reset()
                        viewModel.resetRelated()
                        viewModel.loadMediaItemFromTrack(it, VIDEO_CLICK)
                        viewModel.videoId.postValue(it.videoId)
                        viewModel.from.postValue(from)
                        updateUIfromQueueNowPlaying()
                    }
                }
            }

            ALBUM_CLICK -> {
                if (playlistId != null) {
                    viewModel.playlistId.value = playlistId
                }
                binding.ivArt.setImageResource(0)
                binding.loadingArt.visibility = View.VISIBLE
                viewModel.gradientDrawable.postValue(null)
                viewModel.lyricsBackground.postValue(null)
                binding.tvSongTitle.visibility = View.GONE
                binding.tvSongArtist.visibility = View.GONE
                Queue.getNowPlaying()?.let {
                    viewModel.simpleMediaServiceHandler?.reset()
                    viewModel.resetRelated()
                    viewModel.loadMediaItemFromTrack(it, ALBUM_CLICK, index)
                    viewModel.videoId.postValue(it.videoId)
                    viewModel.from.postValue(from)
                    updateUIfromQueueNowPlaying()
                }
            }

            PLAYLIST_CLICK -> {
                if (playlistId != null) {
                    viewModel.playlistId.value = playlistId
                }
                binding.ivArt.setImageResource(0)
                binding.loadingArt.visibility = View.VISIBLE
                viewModel.gradientDrawable.postValue(null)
                viewModel.lyricsBackground.postValue(null)
                binding.tvSongTitle.visibility = View.GONE
                binding.tvSongArtist.visibility = View.GONE
                Queue.getNowPlaying()?.let {
                    viewModel.simpleMediaServiceHandler?.reset()
                    viewModel.resetRelated()
                    viewModel.loadMediaItemFromTrack(it, PLAYLIST_CLICK, index)
                    viewModel.videoId.postValue(it.videoId)
                    viewModel.from.postValue(from)
                    updateUIfromQueueNowPlaying()
                }
            }

            MINIPLAYER_CLICK -> {
                videoId = viewModel.videoId.value
                from = viewModel.from.value
                metadataCurSong = viewModel.metadata.value?.data
                gradientDrawable = viewModel.gradientDrawable.value
                lyricsBackground = viewModel.lyricsBackground.value
                if (videoId == null) {
                    videoId = viewModel.nowPlayingMediaItem.value?.mediaId
                    viewModel.videoId.postValue(videoId)
                }
                updateUIfromCurrentMediaItem(viewModel.getCurrentMediaItem())
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {

                val job7 =
                    launch {
                        viewModel.simpleMediaServiceHandler?.nowPlaying?.collectLatest { song ->
                            if (song != null) {
                                videoId = viewModel.videoId.value
                                binding.ivArt.setImageResource(0)
                                binding.loadingArt.visibility = View.VISIBLE
                                updateUIfromCurrentMediaItem(song)
                                viewModel.simpleMediaServiceHandler?.setCurrentSongIndex(viewModel.getCurrentMediaItemIndex())
                                viewModel.changeSongTransitionToFalse()
                                if (viewModel.listYouTubeLiked.first()
                                        ?.contains(song.mediaId) == true
                                ) {
                                    binding.btAddToYouTubeLiked.setImageResource(R.drawable.base_thumb_up_filled_24)
                                } else {
                                    binding.btAddToYouTubeLiked.setImageResource(R.drawable.base_thumb_up_outline_24)
                                }
                            }
                        }
                    }
                val job13 = launch {
                    viewModel.progress.collect {
                        if (it in 0.0..1.0) {
                            binding.progressSong.value = it * 100
                        }
                    }
                }
                val job1 = launch {
                    viewModel.progressString.collect {
                        binding.tvCurrentTime.text = it
                    }
                }
                val job2 = launch {
                    viewModel.isPlaying.collect {
                        if (it) {
                            binding.btPlayPause.setImageResource(R.drawable.baseline_pause_circle_24)
                        } else {
                            binding.btPlayPause.setImageResource(R.drawable.baseline_play_circle_24)
                        }
                    }
                }
                //Update progress bar from buffered percentage
                val job3 = launch {
                    viewModel.bufferedPercentage.collect {
                        binding.buffered.progress = it
                    }
                }
                //Check if song is ready to play. And make progress bar indeterminate
                val job4 = launch {
                    viewModel.notReady.observe(viewLifecycleOwner) {
                        binding.buffered.isIndeterminate = it
                    }
                }
                val job5 = launch {
                    viewModel.progressMillis.collect {
                        if (viewModel._lyrics.value?.data != null) {
                            val lyrics = viewModel._lyrics.value!!.data
                            binding.tvSyncState.text = when (viewModel.getLyricsSyncState()) {
                                Config.SyncState.NOT_FOUND -> null
                                Config.SyncState.LINE_SYNCED -> getString(R.string.line_synced)
                                Config.SyncState.UNSYNCED -> getString(R.string.unsynced)
                            }
                            viewModel._lyrics.value?.data?.let {
                                lyricsFullAdapter.updateOriginalLyrics(it)
                                lyricsFullAdapter.setActiveLyrics(-1)
                            }
                            val index = viewModel.getActiveLyrics(it)
                            if (index != null) {
                                if (lyrics?.lines?.get(0)?.words == "Lyrics not found") {
                                    binding.lyricsLayout.visibility = View.GONE
                                    binding.lyricsTextLayout.visibility = View.GONE
                                } else {
                                    viewModel._lyrics.value!!.data?.let { it1 ->
                                        lyricsAdapter.updateOriginalLyrics(
                                            it1
                                        )
                                        if (viewModel.getLyricsSyncState() == Config.SyncState.LINE_SYNCED) {
                                            binding.rvLyrics.addOnItemTouchListener(disableScrolling)
                                            lyricsAdapter.setActiveLyrics(index)
                                            lyricsFullAdapter.setActiveLyrics(index)
                                            if (index == -1) {
                                                binding.rvLyrics.smoothScrollToPosition(0)
                                            } else {
                                                binding.rvLyrics.smoothScrollToPosition(index)
                                            }
                                        } else if (viewModel.getLyricsSyncState() == Config.SyncState.UNSYNCED) {
                                            lyricsAdapter.setActiveLyrics(-1)
                                            binding.rvLyrics.removeOnItemTouchListener(
                                                disableScrolling
                                            )
                                        }
                                    }

                                    if (binding.btFull.text == getString(R.string.show)) {
                                        binding.lyricsTextLayout.visibility = View.VISIBLE
                                    }
                                    binding.lyricsLayout.visibility = View.VISIBLE
                                }
                            }
                        } else {
                            binding.lyricsLayout.visibility = View.GONE
                            binding.lyricsTextLayout.visibility = View.GONE
                        }
                    }
                }
                val job8 = launch {
                    viewModel.shuffleModeEnabled.collect { shuffle ->
                        when (shuffle) {
                            true -> {
                                binding.btShuffle.setImageResource(R.drawable.baseline_shuffle_24_enable)
                            }

                            false -> {
                                binding.btShuffle.setImageResource(R.drawable.baseline_shuffle_24)
                            }
                        }
                    }
                }
                val job9 = launch {
                    viewModel.repeatMode.collect { repeatMode ->
                        when (repeatMode) {
                            RepeatState.None -> {
                                binding.btRepeat.setImageResource(R.drawable.baseline_repeat_24)
                            }

                            RepeatState.One -> {
                                binding.btRepeat.setImageResource(R.drawable.baseline_repeat_one_24)
                            }

                            RepeatState.All -> {
                                binding.btRepeat.setImageResource(R.drawable.baseline_repeat_24_enable)
                            }
                        }
                    }
                }
                val job10 = launch {
                    viewModel.liked.collect { liked ->
                        binding.cbFavorite.isChecked = liked
                    }
                }
                val job11 = launch {
                    viewModel.simpleMediaServiceHandler?.previousTrackAvailable?.collect { available ->
                        setEnabledAll(binding.btPrevious, available)
                    }
                }
                val job12 = launch {
                    viewModel.simpleMediaServiceHandler?.nextTrackAvailable?.collect { available ->
                        setEnabledAll(binding.btNext, available)
                    }
                }
                val job14 = launch {
                    viewModel.songInfo.collectLatest { songInfo ->
                        if (songInfo != null) {
                            binding.uploaderLayout.visibility = View.VISIBLE
                            binding.infoLayout.visibility = View.VISIBLE
                            binding.tvUploader.text = songInfo.author
                            binding.tvSmallArtist.text = songInfo.author
                            binding.ivSmallArtist.load(songInfo.authorThumbnail) {
                                crossfade(true)
                                placeholder(R.drawable.holder)
                            }
                            binding.ivAuthor.load(songInfo.authorThumbnail) {
                                crossfade(true)
                                placeholder(R.drawable.holder_video)
                            }
                            binding.tvSubCount.text = songInfo.subscribers
                            binding.tvPublishAt.text =
                                getString(R.string.published_at, songInfo.uploadDate)
                            binding.tvPlayCount.text = getString(
                                R.string.view_count,
                                String.format("%,d", songInfo.viewCount)
                            )
                            binding.tvLikeCount.text = getString(
                                R.string.like_and_dislike,
                                songInfo.like,
                                songInfo.dislike
                            )
                            binding.tvDescription.text =
                                songInfo.description ?: getString(R.string.no_description)
                            InteractiveTextMaker.of(binding.tvDescription).setOnTextClickListener {
                                val timestamp = parseTimestampToMilliseconds(it)
                                if (timestamp != 0.0 && timestamp < runBlocking { viewModel.duration.first() }) {
                                    viewModel.onUIEvent(UIEvent.UpdateProgress(((timestamp * 100) / runBlocking { viewModel.duration.first() }).toFloat()))
                                }
                            }
                                .setSpecialTextColorRes(R.color.light_blue_A400)
                                .initialize()
                        } else {
                            binding.uploaderLayout.visibility = View.GONE
                            binding.infoLayout.visibility = View.GONE
                            binding.playerLayout.visibility = View.GONE
                            binding.ivArt.visibility = View.VISIBLE
                        }
                    }
                }
                val job19 = launch {
                    viewModel.format.collect { f ->
                        if (f != null) {
                            if (f.itag == 22 || f.itag == 18) {
                                //binding.playerLayout.visibility = View.VISIBLE
                                binding.ivArt.visibility = View.INVISIBLE
                                binding.loadingArt.visibility = View.GONE
                            } else {
                                binding.playerLayout.visibility = View.GONE
                                binding.ivArt.visibility = View.VISIBLE
                            }
                        } else {
                            binding.playerLayout.visibility = View.GONE
                            binding.ivArt.visibility = View.VISIBLE
                        }
                    }
                }
                val job15 = launch {
                    viewModel.related.collectLatest { response ->
                        if (response != null) {
                            when (response) {
                                is Resource.Success -> {
                                    val data = response.data!!
                                    data.add(Queue.getNowPlaying()!!)
                                    val listWithoutDuplicateElements: ArrayList<Track> = ArrayList()
                                    for (element in data) {
                                        // Check if element not exist in list, perform add element to list
                                        if (!listWithoutDuplicateElements.contains(element)) {
                                            listWithoutDuplicateElements.add(element)
                                        }
                                    }
                                    Queue.clear()
                                    Queue.addAll(listWithoutDuplicateElements)
                                    viewModel.addQueueToPlayer()
                                }

                                is Resource.Error -> {
                                    if (response.message != "null") {
                                        Toast.makeText(
                                            requireContext(),
                                            response.message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                    }
                }
                val job16 = launch {
                    viewModel.translateLyrics.collect {
                        lyricsAdapter.updateTranslatedLyrics(it)
                        lyricsFullAdapter.updateTranslatedLyrics(it)
                    }
                }
                val job17 = launch {
                    viewModel.lyricsProvider.collect {
                        when (it) {
                            LyricsProvider.SPOTIFY -> {
                                binding.tvLyricsProvider.text =
                                    getString(R.string.spotify_lyrics_provider)
                            }

                            LyricsProvider.MUSIXMATCH -> {
                                binding.tvLyricsProvider.text = getString(R.string.lyrics_provider)
                            }

                            LyricsProvider.YOUTUBE -> {
                                binding.tvLyricsProvider.text =
                                    getString(R.string.lyrics_provider_youtube)
                            }

                            else -> {
                                binding.tvLyricsProvider.text = getString(R.string.offline_mode)
                            }
                        }
                    }
                }
                val job18 = launch {
                    viewModel.duration.collect {
                        if (viewModel.formatDuration(it).contains('-')) {
                            binding.tvFullTime.text = getString(R.string.na_na)
                        } else {
                            binding.tvFullTime.text = viewModel.formatDuration(it)
                        }
                    }
                }
                val job20 = launch {
                    viewModel.canvas.collect {
                        val canva = it?.canvases?.firstOrNull()
                        if (canva != null) {
                            if (canva.canvas_url.contains(".mp4")) {
                                player?.stop()
                                player?.release()
                                player = ExoPlayer.Builder(requireContext()).build()
                                binding.playerCanvas.visibility = View.VISIBLE
                                binding.ivCanvas.visibility = View.GONE
                                player?.setMediaItem(
                                    MediaItem.Builder()
                                        .setUri(
                                            canva.canvas_url.toUri()
                                        )
                                        .build()
                                )
                                binding.playerCanvas.player = player
                                binding.playerCanvas.resizeMode =
                                    androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                player?.prepare()
                                player?.play()
                                player?.repeatMode = Player.REPEAT_MODE_ONE
                            } else {
                                binding.playerCanvas.visibility = View.GONE
                                binding.ivCanvas.visibility = View.VISIBLE
                                binding.ivCanvas.load(canva.canvas_url) {
                                    crossfade(true)
                                }
                            }

                            binding.middleLayout.visibility = View.INVISIBLE
                            binding.canvasLayout.visibility = View.VISIBLE
                            binding.rootLayout.background = ColorDrawable(
                                resources.getColor(
                                    R.color.md_theme_dark_background,
                                    null
                                )
                            )
                            binding.overlayCanvas.visibility = View.VISIBLE
                            binding.smallArtistLayout.visibility = View.GONE
                            val shortAnimationDuration =
                                resources.getInteger(android.R.integer.config_mediumAnimTime)
                            canvasOverlayJob?.cancel()
                            if (binding.root.scrollY == 0 && binding.root.scrollX == 0) {
                                canvasOverlayJob = lifecycleScope.launch {
                                    repeatOnLifecycle(Lifecycle.State.CREATED) {
                                        delay(5000)
                                        if (binding.root.scrollY == 0) {
                                            binding.overlayCanvas.visibility = View.GONE
                                            binding.infoControllerLayout.visibility = View.INVISIBLE
                                            binding.smallArtistLayout.visibility = View.VISIBLE
                                        }
                                    }
                                }
                            }
                            binding.root.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                                if (scrollY > 0 && binding.overlayCanvas.visibility == View.GONE) {
                                    canvasOverlayJob?.cancel()
                                    binding.overlayCanvas.alpha = 0f
                                    binding.overlayCanvas.apply {
                                        visibility = View.VISIBLE
                                        animate()
                                            .alpha(1f)
                                            .setDuration(shortAnimationDuration.toLong())
                                            .setListener(null)
                                    }
                                    binding.infoControllerLayout.alpha = 0f
                                    binding.infoControllerLayout.apply {
                                        visibility = View.VISIBLE
                                        animate()
                                            .alpha(1f)
                                            .setDuration(shortAnimationDuration.toLong())
                                            .setListener(null)
                                    }
                                    binding.smallArtistLayout.visibility = View.GONE
                                } else if (scrollY == 0 && scrollX == 0 && binding.overlayCanvas.visibility == View.VISIBLE) {
                                    canvasOverlayJob = lifecycleScope.launch {
                                        repeatOnLifecycle(Lifecycle.State.CREATED) {
                                            delay(5000)
                                            if (binding.root.scrollY == 0) {
                                                binding.overlayCanvas.visibility = View.GONE
                                                binding.infoControllerLayout.visibility =
                                                    View.INVISIBLE
                                                binding.smallArtistLayout.visibility = View.VISIBLE
                                            }
                                        }
                                    }
                                }
                            }
                            binding.helpMeBro.setOnClickListener {
                                if (binding.root.scrollY == 0 && binding.root.scrollX == 0) {
                                    if (binding.overlayCanvas.visibility == View.VISIBLE) {
                                        canvasOverlayJob?.cancel()
                                        binding.overlayCanvas.visibility = View.GONE
                                        binding.infoControllerLayout.visibility = View.INVISIBLE
                                        binding.smallArtistLayout.visibility = View.VISIBLE
                                    } else {
                                        canvasOverlayJob?.cancel()
                                        binding.overlayCanvas.alpha = 0f
                                        binding.overlayCanvas.apply {
                                            visibility = View.VISIBLE
                                            animate()
                                                .alpha(1f)
                                                .setDuration(shortAnimationDuration.toLong())
                                                .setListener(null)
                                        }
                                        binding.infoControllerLayout.alpha = 0f
                                        binding.infoControllerLayout.apply {
                                            visibility = View.VISIBLE
                                            animate()
                                                .alpha(1f)
                                                .setDuration(shortAnimationDuration.toLong())
                                                .setListener(null)
                                        }
                                        binding.smallArtistLayout.visibility = View.GONE
                                        canvasOverlayJob = lifecycleScope.launch {
                                            repeatOnLifecycle(Lifecycle.State.CREATED) {
                                                delay(5000)
                                                if (binding.root.scrollY == 0) {
                                                    binding.overlayCanvas.visibility = View.GONE
                                                    binding.infoControllerLayout.visibility =
                                                        View.INVISIBLE
                                                    binding.smallArtistLayout.visibility =
                                                        View.VISIBLE
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            canvasOverlayJob?.cancel()
                            player?.stop()
                            player?.release()
                            binding.root.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->

                            }
                            binding.overlayCanvas.visibility = View.GONE
                            binding.infoControllerLayout.visibility = View.VISIBLE
                            binding.canvasLayout.visibility = View.INVISIBLE
                            binding.middleLayout.visibility = View.VISIBLE
                        }
                    }
                }
                val job21 = launch {
                    viewModel.logInToYouTube().distinctUntilChanged().collect {
                        if (it == DataStoreManager.TRUE) {
                            setEnabledAll(binding.btAddToYouTubeLiked, true)
                            if (viewModel.isFirstLiked) {
                                val balloon = Balloon.Builder(requireContext())
                                    .setWidthRatio(0.5f)
                                    .setHeight(BalloonSizeSpec.WRAP)
                                    .setText(getString(R.string.guide_liked_title))
                                    .setTextColorResource(R.color.md_theme_dark_onSurface)
                                    .setTextSize(11f)
                                    .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
                                    .setArrowSize(10)
                                    .setArrowPosition(0.5f)
                                    .setAutoDismissDuration(5000L)
                                    .setPadding(12)
                                    .setCornerRadius(8f)
                                    .setBackgroundColorResource(R.color.md_theme_dark_onSecondary)
                                    .setBalloonAnimation(BalloonAnimation.ELASTIC)
                                    .setLifecycleOwner(viewLifecycleOwner)
                                    .build()
                                balloon.showAlignTop(binding.btAddToYouTubeLiked)
                                viewModel.putString("liked_guide", STATUS_DONE)
                                viewModel.isFirstSuggestions = false
                            }
                        } else {
                            setEnabledAll(binding.btAddToYouTubeLiked, false)
                        }
                    }
                }
                val job22 = launch {
                    viewModel.listYouTubeLiked.collect {
                        if (it?.contains(viewModel.simpleMediaServiceHandler?.nowPlaying?.first()?.mediaId) == true) {
                            binding.btAddToYouTubeLiked.setImageResource(R.drawable.base_thumb_up_filled_24)
                        } else {
                            binding.btAddToYouTubeLiked.setImageResource(R.drawable.base_thumb_up_outline_24)
                        }
                    }
                }
                job1.join()
                job2.join()
                job3.join()
                job4.join()
                job5.join()
                job7.join()
                job8.join()
                job9.join()
                job10.join()
                job13.join()
                job11.join()
                job12.join()
                job14.join()
                job15.join()
                job16.join()
                job17.join()
                job18.join()
                job19.join()
                job20.join()
                job21.join()
                job22.join()
            }
        }
        binding.btAddToYouTubeLiked.setOnClickListener {
            viewModel.addToYouTubeLiked()
        }
        binding.tvMore.setOnClickListener {

            if (binding.tvDescription.maxLines == 2) {
                val animation = ObjectAnimator.ofInt(
                    binding.tvDescription,
                    "maxLines",
                    1000
                )
                animation.setDuration(1000)
                animation.start()
                binding.tvMore.setText(R.string.less)
            } else {
                val animation = ObjectAnimator.ofInt(
                    binding.tvDescription,
                    "maxLines",
                    2
                )
                animation.setDuration(200)
                animation.start()
                binding.tvMore.setText(R.string.more)
            }
        }
        binding.btFull.setOnClickListener {
            if (binding.btFull.text == getString(R.string.show)) {
                binding.btFull.text = getString(R.string.hide)
                binding.lyricsTextLayout.visibility = View.GONE
                binding.lyricsFullLayout.visibility = View.VISIBLE
            } else {
                binding.btFull.text = getString(R.string.show)
                binding.lyricsTextLayout.visibility = View.VISIBLE
                binding.lyricsFullLayout.visibility = View.GONE
            }
        }

        binding.progressSong.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {

            }

            override fun onStopTrackingTouch(slider: Slider) {
                viewModel.onUIEvent(UIEvent.UpdateProgress(slider.value))
            }
        })
        binding.btFullscreen.setOnClickListener {
            binding.playerView.player = null
            isFullScreen = true
            findNavController().navigateSafe(R.id.action_global_fullscreenFragment)
        }
        binding.playerLayout.setOnClickListener {
            val shortAnimationDuration =
                resources.getInteger(android.R.integer.config_mediumAnimTime)
            if (binding.overlay.visibility == View.VISIBLE) {
                binding.overlay.visibility = View.GONE
                overlayJob?.cancel()
            } else {
                binding.overlay.alpha = 0f
                binding.overlay.apply {
                    visibility = View.VISIBLE
                    animate()
                        .alpha(1f)
                        .setDuration(shortAnimationDuration.toLong())
                        .setListener(null)
                }
                overlayJob?.cancel()
                overlayJob = lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.CREATED) {
                        delay(3000)
                        binding.overlay.visibility = View.GONE
                    }
                }
            }
        }

        binding.btPlayPause.setOnClickListener {
            viewModel.onUIEvent(UIEvent.PlayPause)
        }
        binding.btNext.setOnClickListener {
            viewModel.onUIEvent(UIEvent.Next)
            showRectangleBannerAd()
        }
        binding.btPrevious.setOnClickListener {
            viewModel.onUIEvent(UIEvent.Previous)
            showRectangleBannerAd()
        }
        binding.btShuffle.setOnClickListener {
            viewModel.onUIEvent(UIEvent.Shuffle)
        }
        binding.btRepeat.setOnClickListener {
            viewModel.onUIEvent(UIEvent.Repeat)
        }

        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.btQueue.setOnClickListener {
            findNavController().navigateSafe(R.id.action_global_queueFragment)
        }
        binding.btSongInfo.setOnClickListener {
            findNavController().navigateSafe(R.id.action_global_infoFragment)
        }
        binding.cbFavorite.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                viewModel.songDB.value?.let { nowPlayingSong ->
                    viewModel.updateLikeStatus(
                        nowPlayingSong.videoId,
                        false
                    )
                    viewModel.updateLikeInNotification(false)
                }
            } else {
                viewModel.songDB.value?.let { nowPlayingSong ->
                    viewModel.updateLikeStatus(
                        nowPlayingSong.videoId,
                        true
                    )
                    viewModel.updateLikeInNotification(true)
                }
            }
        }
        binding.btnDownload.setOnClickListener {
            if (PermissionHelper.checkStoragePermissions(
                    activity,
                    PermissionHelper.DOWNLOAD_DIALOG_REQUEST_CODE
                )
            ) {
                if (!viewModel.simpleMediaServiceHandler?.catalogMetadata.isNullOrEmpty()) {
                    val song =
                        viewModel.simpleMediaServiceHandler!!.catalogMetadata[viewModel.getCurrentMediaItemIndex()]
                    AdMobInterstitialAdDownload.getInstance()
                        .showInterstitialAd(activity) {
                            lifecycleScope.launch {
                                ExtractorHelper.getStreamInfo(
                                    ServiceList.YouTube.serviceId,
                                    "https://youtube.com/watch?v=${song.videoId}"
                                ).doOnSubscribe {
                                    showLoadingDialog()
                                }
                                    .doOnTerminate {
                                        hideLoadingDialog()
                                    }
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe({
                                        hideLoadingDialog()
                                        if (it != null) {
                                            val downloadDialog: DownloadDialog =
                                                DownloadDialog.newInstance(
                                                    activity,
                                                    it
                                                )
                                            downloadDialog.show(
                                                activity.supportFragmentManager,
                                                "DownloadDialog"
                                            )
                                        }
                                    }, { throwable ->
                                        Toast.makeText(
                                            requireContext(),
                                            getString(R.string.oops_unable_to_download_this_song),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        hideLoadingDialog()
                                    })
                            }
                        }
                }
            }
        }
        binding.uploaderLayout.setOnClickListener {
            val browseId = if (!viewModel.songDB.value?.artistId.isNullOrEmpty()) {
                viewModel.songDB.value?.artistId?.firstOrNull()
            } else {
                runBlocking { viewModel.songInfo.first()?.authorId }
            }
            findNavController().navigateSafe(
                R.id.action_global_artistFragment,
                Bundle().apply {
                    putString("channelId", browseId)
                })
        }
        binding.smallArtistLayout.setOnClickListener {
            val browseId = if (!viewModel.songDB.value?.artistId.isNullOrEmpty()) {
                viewModel.songDB.value?.artistId?.firstOrNull()
            } else {
                runBlocking { viewModel.songInfo.first()?.authorId }
            }
            findNavController().navigateSafe(
                R.id.action_global_artistFragment,
                Bundle().apply {
                    putString("channelId", browseId)
                })
        }
        binding.tvSongArtist.setOnClickListener {
            if (!viewModel.simpleMediaServiceHandler?.catalogMetadata.isNullOrEmpty()) {
                val song =
                    viewModel.simpleMediaServiceHandler!!.catalogMetadata[viewModel.getCurrentMediaItemIndex()]
                if (song.artists?.firstOrNull()?.id != null) {
                    findNavController().navigateSafe(
                        R.id.action_global_artistFragment,
                        Bundle().apply {
                            putString("channelId", song.artists.firstOrNull()?.id)
                        })
                }
            }
        }
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.now_playing_dialog_menu_item_more -> {
                    if (!viewModel.simpleMediaServiceHandler?.catalogMetadata.isNullOrEmpty()) {
                        viewModel.refreshSongDB()
                        val dialog = BottomSheetDialog(requireContext())
                        dialog.apply {
                            behavior.state = BottomSheetBehavior.STATE_EXPANDED
                        }
                        val bottomSheetView = BottomSheetNowPlayingBinding.inflate(layoutInflater)
                        with(bottomSheetView) {
                            lifecycleScope.launch {
                                viewModel.simpleMediaServiceHandler?.sleepMinutes?.collect { min ->
                                    if (min > 0) {
                                        tvSleepTimer.text =
                                            getString(R.string.sleep_timer, min.toString())
                                        ivSleepTimer.setImageResource(R.drawable.alarm_enable)
                                    } else {
                                        tvSleepTimer.text = getString(R.string.sleep_timer_off)
                                        ivSleepTimer.setImageResource(R.drawable.baseline_access_alarm_24)
                                    }
                                }
                            }
                            btAddQueue.visibility = View.GONE
                            if (runBlocking { viewModel.liked.first() }) {
                                tvFavorite.text = getString(R.string.liked)
                                cbFavorite.isChecked = true
                            } else {
                                tvFavorite.text = getString(R.string.like)
                                cbFavorite.isChecked = false
                            }
                            when (viewModel.songDB.value?.downloadState) {
                                DownloadState.STATE_PREPARING -> {
                                    tvDownload.text = getString(R.string.preparing)
                                    ivDownload.setImageResource(R.drawable.outline_download_for_offline_24)
                                    setEnabledAll(btDownload, true)
                                }

                                DownloadState.STATE_NOT_DOWNLOADED -> {
                                    tvDownload.text = getString(R.string.download)
                                    ivDownload.setImageResource(R.drawable.outline_download_for_offline_24)
                                    setEnabledAll(btDownload, true)
                                }

                                DownloadState.STATE_DOWNLOADING -> {
                                    tvDownload.text = getString(R.string.downloading)
                                    ivDownload.setImageResource(R.drawable.baseline_downloading_white)
                                    setEnabledAll(btDownload, true)
                                }

                                DownloadState.STATE_DOWNLOADED -> {
                                    tvDownload.text = getString(R.string.downloaded)
                                    ivDownload.setImageResource(R.drawable.baseline_downloaded)
                                    setEnabledAll(btDownload, true)
                                }
                            }
                            if (!viewModel.simpleMediaServiceHandler?.catalogMetadata.isNullOrEmpty()) {
                                val song =
                                    viewModel.simpleMediaServiceHandler!!.catalogMetadata[viewModel.getCurrentMediaItemIndex()]
                                tvSongTitle.text = song.title
                                tvSongTitle.isSelected = true
                                tvSongArtist.text = song.artists.toListName().connectArtists()
                                tvSongArtist.isSelected = true
                                ivThumbnail.load(song.thumbnails?.last()?.url)
                                if (song.album != null) {
                                    setEnabledAll(btAlbum, true)
                                    tvAlbum.text =
                                        getString(R.string.album) + ": " + song.album.name
                                } else {
                                    tvAlbum.text = getString(R.string.no_album)
                                    setEnabledAll(btAlbum, false)
                                }
                                btAlbum.setOnClickListener {
                                    val albumId = song.album?.id
                                    if (albumId != null) {
                                        findNavController().navigateSafe(
                                            R.id.action_global_albumFragment,
                                            Bundle().apply {
                                                putString("browseId", albumId)
                                            })
                                        dialog.dismiss()
                                    } else {
                                        Toast.makeText(
                                            requireContext(),
                                            getString(R.string.no_album),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }

                                btLike.setOnClickListener {
                                    if (cbFavorite.isChecked) {
                                        cbFavorite.isChecked = false
                                        tvFavorite.text = getString(R.string.like)
                                        viewModel.updateLikeStatus(song.videoId, false)
                                    } else {
                                        cbFavorite.isChecked = true
                                        tvFavorite.text = getString(R.string.liked)
                                        viewModel.updateLikeStatus(song.videoId, true)
                                    }
                                }
                                btPlayNext.visibility = View.GONE
                                btRadio.setOnClickListener {
                                    val args = Bundle()
                                    args.putString("radioId", "RDAMVM${song.videoId}")
                                    args.putString(
                                        "videoId",
                                        song.videoId
                                    )
                                    dialog.dismiss()
                                    findNavController().navigateSafe(
                                        R.id.action_global_playlistFragment,
                                        args
                                    )
                                }
                                btSleepTimer.setOnClickListener {
                                    if (viewModel.sleepTimerRunning.value == true) {
                                        MaterialAlertDialogBuilder(requireContext())
                                            .setTitle(getString(R.string.warning))
                                            .setMessage(getString(R.string.sleep_timer_warning))
                                            .setPositiveButton(getString(R.string.yes)) { d, _ ->
                                                viewModel.stopSleepTimer()
                                                Toast.makeText(
                                                    requireContext(),
                                                    getString(R.string.sleep_timer_off_done),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                d.dismiss()
                                            }
                                            .setNegativeButton(getString(R.string.cancel)) { d, _ ->
                                                d.dismiss()
                                            }
                                            .show()
                                    } else {
                                        val d = BottomSheetDialog(requireContext())
                                        d.apply {
                                            behavior.state = BottomSheetBehavior.STATE_EXPANDED
                                        }
                                        val v = BottomSheetSleepTimerBinding.inflate(layoutInflater)
                                        v.btSet.setOnClickListener {
                                            val min = v.etTime.editText?.text.toString()
                                            if (min.isNotBlank() && min.toInt() > 0) {
                                                viewModel.setSleepTimer(min.toInt())
                                                d.dismiss()
                                            } else {
                                                Toast.makeText(
                                                    requireContext(),
                                                    getString(R.string.sleep_timer_set_error),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                        d.setContentView(v.root)
                                        d.setCancelable(true)
                                        d.show()
                                    }
                                }
                                btAddPlaylist.setOnClickListener {
                                    viewModel.getAllLocalPlaylist()
                                    val listLocalPlaylist: ArrayList<LocalPlaylistEntity> =
                                        arrayListOf()
                                    val addPlaylistDialog = BottomSheetDialog(requireContext())
                                    addPlaylistDialog.apply {
                                        behavior.state = BottomSheetBehavior.STATE_EXPANDED
                                    }
                                    val viewAddPlaylist =
                                        BottomSheetAddToAPlaylistBinding.inflate(layoutInflater)
                                    val addToAPlaylistAdapter = AddToAPlaylistAdapter(arrayListOf())
                                    addToAPlaylistAdapter.setVideoId(song.videoId)
                                    viewAddPlaylist.rvLocalPlaylists.apply {
                                        adapter = addToAPlaylistAdapter
                                        layoutManager = LinearLayoutManager(requireContext())
                                    }
                                    viewModel.localPlaylist.observe(viewLifecycleOwner) { list ->
                                        listLocalPlaylist.clear()
                                        listLocalPlaylist.addAll(list)
                                        addToAPlaylistAdapter.updateList(listLocalPlaylist)
                                    }
                                    addToAPlaylistAdapter.setOnItemClickListener(object :
                                        AddToAPlaylistAdapter.OnItemClickListener {
                                        override fun onItemClick(position: Int) {
                                            val playlist = listLocalPlaylist[position]
                                            viewModel.updateInLibrary(song.videoId)
                                            val tempTrack = ArrayList<String>()
                                            if (playlist.tracks != null) {
                                                tempTrack.addAll(playlist.tracks)
                                            }
                                            if (!tempTrack.contains(song.videoId) && playlist.syncedWithYouTubePlaylist == 1 && playlist.youtubePlaylistId != null) {
                                                viewModel.addToYouTubePlaylist(
                                                    playlist.id,
                                                    playlist.youtubePlaylistId,
                                                    song.videoId
                                                )
                                            }
                                            if (!tempTrack.contains(song.videoId)) {
                                                viewModel.insertPairSongLocalPlaylist(
                                                    PairSongLocalPlaylist(
                                                        playlistId = playlist.id,
                                                        songId = song.videoId,
                                                        position = playlist.tracks?.size ?: 0,
                                                        inPlaylist = LocalDateTime.now()
                                                    )
                                                )
                                                tempTrack.add(song.videoId)
                                            }
                                            viewModel.updateLocalPlaylistTracks(
                                                tempTrack.removeConflicts(),
                                                playlist.id
                                            )
                                            addPlaylistDialog.dismiss()
                                            dialog.dismiss()
                                        }
                                    })
                                    addPlaylistDialog.setContentView(viewAddPlaylist.root)
                                    addPlaylistDialog.setCancelable(true)
                                    addPlaylistDialog.show()
                                }

                                btSeeArtists.setOnClickListener {
                                    val subDialog = BottomSheetDialog(requireContext())
                                    subDialog.apply {
                                        behavior.state = BottomSheetBehavior.STATE_EXPANDED
                                    }
                                    val subBottomSheetView =
                                        BottomSheetSeeArtistOfNowPlayingBinding.inflate(
                                            layoutInflater
                                        )
                                    if (song.artists != null) {
                                        val artistAdapter =
                                            SeeArtistOfNowPlayingAdapter(song.artists)
                                        subBottomSheetView.rvArtists.apply {
                                            adapter = artistAdapter
                                            layoutManager = LinearLayoutManager(requireContext())
                                        }
                                        artistAdapter.setOnClickListener(object :
                                            SeeArtistOfNowPlayingAdapter.OnItemClickListener {
                                            override fun onItemClick(position: Int) {
                                                val artist = song.artists[position]
                                                if (artist.id != null) {
                                                    findNavController().navigateSafe(
                                                        R.id.action_global_artistFragment,
                                                        Bundle().apply {
                                                            putString("channelId", artist.id)
                                                        })
                                                    subDialog.dismiss()
                                                    dialog.dismiss()
                                                }
                                            }

                                        })
                                    }

                                    subDialog.setCancelable(true)
                                    subDialog.setContentView(subBottomSheetView.root)
                                    subDialog.show()
                                }
                                btChangeLyricsProvider.setOnClickListener {
                                    val mainLyricsProvider = viewModel.getLyricsProvider()
                                    var checkedIndex =
                                        if (mainLyricsProvider == DataStoreManager.MUSIXMATCH) 0 else 1
                                    val dialogChange = MaterialAlertDialogBuilder(requireContext())
                                        .setTitle(getString(R.string.main_lyrics_provider))
                                        .setSingleChoiceItems(
                                            LYRICS_PROVIDER.items,
                                            checkedIndex
                                        ) { _, which ->
                                            checkedIndex = which
                                        }
                                        .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                                            dialog.dismiss()
                                        }
                                        .setPositiveButton(getString(R.string.change)) { dialog, _ ->
                                            if (checkedIndex != -1) {
                                                if (checkedIndex == 0) {
                                                    if (mainLyricsProvider != DataStoreManager.MUSIXMATCH) {
                                                        viewModel.setLyricsProvider(DataStoreManager.MUSIXMATCH)
                                                    }
                                                } else if (checkedIndex == 1) {
                                                    if (mainLyricsProvider != DataStoreManager.YOUTUBE) {
                                                        viewModel.setLyricsProvider(DataStoreManager.YOUTUBE)
                                                    }
                                                }
                                            }
                                            dialog.dismiss()
                                        }
                                    dialogChange.show()
                                }
                                btShare.setOnClickListener {
                                    val shareIntent = Intent(Intent.ACTION_SEND)
                                    shareIntent.type = "text/plain"
                                    val url = "https://youtube.com/watch?v=${song.videoId}"
                                    shareIntent.putExtra(Intent.EXTRA_TEXT, url +"\nDownload using the Audio Downloader app! Download the app here: \n https://play.google.com/store/apps/details?id="+requireContext().getPackageName())
                                    val chooserIntent = Intent.createChooser(
                                        shareIntent,
                                        getString(R.string.share_url)
                                    )
                                    startActivity(chooserIntent)
                                }
                                btDownload.setOnClickListener {
                                    if (PermissionHelper.checkStoragePermissions(
                                            activity,
                                            PermissionHelper.DOWNLOAD_DIALOG_REQUEST_CODE
                                        )
                                    ) {
                                        AdMobInterstitialAdDownload.getInstance()
                                            .showInterstitialAd(activity) {
                                                lifecycleScope.launch {
                                                    ExtractorHelper.getStreamInfo(
                                                        ServiceList.YouTube.serviceId,
                                                        "https://youtube.com/watch?v=${song.videoId}"
                                                    ).doOnSubscribe {
                                                        showLoadingDialog()
                                                        dialog.dismiss()
                                                    }
                                                        .doOnTerminate {
                                                            hideLoadingDialog()
                                                        }
                                                        .subscribeOn(Schedulers.io())
                                                        .observeOn(AndroidSchedulers.mainThread())
                                                        .subscribe({
                                                            if (it != null) {
                                                                dialog.dismiss()
                                                                val downloadDialog: DownloadDialog =
                                                                    DownloadDialog.newInstance(
                                                                        activity,
                                                                        it
                                                                    )
                                                                downloadDialog.show(
                                                                    activity.supportFragmentManager,
                                                                    "DownloadDialog"
                                                                )
                                                            }
                                                        }, { throwable ->
                                                            Toast.makeText(
                                                                requireContext(),
                                                                "Oops! Unable to download this video.",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        })
                                                }
                                            }
                                    }

//                                        if (tvDownload.text == getString(R.string.download)) {
//                                            viewModel.updateDownloadState(
//                                                song.videoId,
//                                                DownloadState.STATE_PREPARING
//                                            )
//                                            val downloadRequest =
//                                                DownloadRequest.Builder(
//                                                    song.videoId,
//                                                    song.videoId.toUri()
//                                                )
//                                                    .setData(song.title.toByteArray())
//                                                    .setCustomCacheKey(song.videoId)
//                                                    .build()
//                                            viewModel.updateDownloadState(
//                                                song.videoId,
//                                                DownloadState.STATE_DOWNLOADING
//                                            )
//                                            viewModel.getDownloadStateFromService(song.videoId)
//                                            DownloadService.sendAddDownload(
//                                                requireContext(),
//                                                MusicDownloadService::class.java,
//                                                downloadRequest,
//                                                false
//                                            )
//                                            lifecycleScope.launch {
//                                                viewModel.downloadState.collect { download ->
//                                                    if (download != null) {
//                                                        when (download.state) {
//                                                            Download.STATE_DOWNLOADING -> {
//                                                                viewModel.updateDownloadState(
//                                                                    song.videoId,
//                                                                    DownloadState.STATE_DOWNLOADING
//                                                                )
//                                                                tvDownload.text =
//                                                                    getString(R.string.downloading)
//                                                                ivDownload.setImageResource(R.drawable.baseline_downloading_white)
//                                                                setEnabledAll(btDownload, true)
//                                                            }
//
//                                                            Download.STATE_FAILED -> {
//                                                                viewModel.updateDownloadState(
//                                                                    song.videoId,
//                                                                    DownloadState.STATE_NOT_DOWNLOADED
//                                                                )
//                                                                tvDownload.text =
//                                                                    getString(R.string.download)
//                                                                ivDownload.setImageResource(R.drawable.outline_download_for_offline_24)
//                                                                setEnabledAll(btDownload, true)
//                                                                Toast.makeText(
//                                                                    requireContext(),
//                                                                    getString(androidx.media3.exoplayer.R.string.exo_download_failed),
//                                                                    Toast.LENGTH_SHORT
//                                                                ).show()
//                                                            }
//
//                                                            Download.STATE_COMPLETED -> {
//                                                                viewModel.updateDownloadState(
//                                                                    song.videoId,
//                                                                    DownloadState.STATE_DOWNLOADED
//                                                                )
//                                                                Toast.makeText(
//                                                                    requireContext(),
//                                                                    getString(androidx.media3.exoplayer.R.string.exo_download_completed),
//                                                                    Toast.LENGTH_SHORT
//                                                                ).show()
//                                                                tvDownload.text =
//                                                                    getString(R.string.downloaded)
//                                                                ivDownload.setImageResource(R.drawable.baseline_downloaded)
//                                                                setEnabledAll(btDownload, true)
//                                                            }
//
//                                                            else -> {
//                                                            }
//                                                        }
//                                                    }
//                                                }
//                                            }
//                                        } else if (tvDownload.text == getString(R.string.downloaded) || tvDownload.text == getString(
//                                                R.string.downloading
//                                            )
//                                        ) {
//                                            DownloadService.sendRemoveDownload(
//                                                requireContext(),
//                                                MusicDownloadService::class.java,
//                                                song.videoId,
//                                                false
//                                            )
//                                            viewModel.updateDownloadState(
//                                                song.videoId,
//                                                DownloadState.STATE_NOT_DOWNLOADED
//                                            )
//                                            tvDownload.text = getString(R.string.download)
//                                            ivDownload.setImageResource(R.drawable.outline_download_for_offline_24)
//                                            setEnabledAll(btDownload, true)
//                                            Toast.makeText(
//                                                requireContext(),
//                                                getString(R.string.removed_download),
//                                                Toast.LENGTH_SHORT
//                                            ).show()
//                                        }
                                }
                            }
                        }
                        dialog.setCancelable(true)
                        dialog.setContentView(bottomSheetView.root)
                        dialog.show()
                    }
                    true
                }

                else -> false
            }
        }

        showRectangleBannerAd()
    }

    private fun showRectangleBannerAd() {
        Log.d("Mohit", "showRectangleBannerAd: ")
        binding.adViewContainer.removeAllViews()
        binding.adViewContainer.isVisible = false
        binding.btnAdCollapsible.isVisible = false
        activity?.let {
            val adConfig = SharedPrefsHelper.loadObjectPrefs(
                it, SharedPrefsHelper.Key.Banner_PlayerView.name,
                AdConfig::class.java
            ) as? AdConfig
            if (adConfig != null && adConfig.Show) {
                adView = AdView(it)
                val extras = Bundle()
                extras.putString("collapsible", "bottom")
                extras.putString("collapsible_request_id", UUID.randomUUID().toString());
                val adRequest: AdRequest = AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
                    .build()
                adView.setAdSize(AdSize.MEDIUM_RECTANGLE)
                val adId =
                    adConfig.Ad_Id
                adView.adUnitId = adId
                adView.adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d("Mohit", "onAdLoaded: ")
                        // Code to be executed when an ad finishes loading.
                        binding.adViewContainer.addView(adView)
                        binding.adViewContainer.isVisible = true
                        binding.btnAdCollapsible.isVisible = true
                        lifecycleScope.launch {
                            viewModel.format.collect { _format ->
                                if (_format != null && (_format.itag == 22 || _format.itag == 18)) {
                                    binding.playerLayout.visibility = View.GONE
                                    binding.ivArt.visibility = View.INVISIBLE
                                    binding.loadingArt.visibility = View.GONE
                                }
                            }
                        }
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.d("Mohit", "onAdFailedToLoad: ")
                        // Code to be executed when an ad request fails.
                        binding.adViewContainer.removeAllViews()
                        binding.adViewContainer.isVisible = false
                        binding.btnAdCollapsible.isVisible = false
                        lifecycleScope.launch {
                            viewModel.format.collect { _format ->
                                if (_format != null && (_format.itag == 22 || _format.itag == 18)) {
                                    binding.playerLayout.visibility = View.VISIBLE
                                    binding.ivArt.visibility = View.INVISIBLE
                                    binding.loadingArt.visibility = View.GONE
                                }
                            }
                        }
                    }
                }
                adView.loadAd(adRequest)
            } else {
                binding.adViewContainer.removeAllViews()
                binding.adViewContainer.isVisible = false
                binding.btnAdCollapsible.isVisible = false
                lifecycleScope.launch {
                    viewModel.format.collect { _format ->
                        if (_format != null && (_format.itag == 22 || _format.itag == 18)) {
                            binding.playerLayout.visibility = View.VISIBLE
                            binding.ivArt.visibility = View.INVISIBLE
                            binding.loadingArt.visibility = View.GONE
                        }
                    }
                }
            }
        }

        binding.btnAdCollapsible.setOnClickListener {
            AdMobConfig.destroyRectangleBannerAd(binding.adViewContainer, adView)
            binding.adViewContainer.isVisible = false
            binding.btnAdCollapsible.isVisible = false
            lifecycleScope.launch {
                viewModel.format.collect { _format ->
                    if (_format != null && (_format.itag == 22 || _format.itag == 18)) {
                        binding.playerLayout.visibility = View.VISIBLE
                        binding.ivArt.visibility = View.INVISIBLE
                        binding.loadingArt.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun updateUIfromQueueNowPlaying() {
        val nowPlaying = Queue.getNowPlaying()
        if (nowPlaying != null) {
            binding.ivArt.setImageResource(0)
            binding.loadingArt.visibility = View.VISIBLE
            var thumbUrl = nowPlaying.thumbnails?.last()?.url!!
            if (thumbUrl.contains("w120")) {
                thumbUrl = Regex("([wh])120").replace(thumbUrl, "$1544")
            }
            binding.ivArt.load(Uri.parse(thumbUrl)) {
                diskCacheKey(nowPlaying.videoId)
                diskCachePolicy(CachePolicy.ENABLED)
                listener(
                    onStart = {
                        binding.ivArt.setImageResource(0)
                        binding.loadingArt.visibility = View.VISIBLE
                    },
                    onSuccess = { _, _ ->
                        binding.ivArt.visibility = View.VISIBLE
                        binding.loadingArt.visibility = View.GONE
                        if (viewModel.gradientDrawable.value != null) {
                            viewModel.gradientDrawable.observe(viewLifecycleOwner) {
                                if (it != null && viewModel.canvas.value?.canvases.isNullOrEmpty()) {
                                    var start = binding.rootLayout.background
                                    if (start == null) {
                                        start = ColorDrawable(Color.BLACK)
                                    }
                                    val transition = TransitionDrawable(arrayOf(start, it))
                                    binding.rootLayout.background = transition
                                    transition.isCrossFadeEnabled = true
                                    transition.startTransition(500)
                                }
                                viewModel.lyricsBackground.value?.let { it1 ->
                                    binding.lyricsLayout.setCardBackgroundColor(
                                        it1
                                    )
                                    binding.infoLayout.setCardBackgroundColor(it1)
                                }
                            }
                        }
                    },
                )
                transformations(
                    object : Transformation {
                        override val cacheKey: String
                            get() = nowPlaying.videoId

                        override suspend fun transform(input: Bitmap, size: Size): Bitmap {
                            val p = Palette.from(input).generate()
                            val defaultColor = 0x000000
                            var startColor = p.getDarkVibrantColor(defaultColor)
                            if (startColor == defaultColor) {
                                startColor = p.getDarkMutedColor(defaultColor)
                                if (startColor == defaultColor) {
                                    startColor = p.getVibrantColor(defaultColor)
                                    if (startColor == defaultColor) {
                                        startColor = p.getMutedColor(defaultColor)
                                        if (startColor == defaultColor) {
                                            startColor = p.getLightVibrantColor(defaultColor)
                                            if (startColor == defaultColor) {
                                                startColor = p.getLightMutedColor(defaultColor)
                                            }
                                        }
                                    }
                                }
                            }
                            val endColor = 0x1b1a1f
                            val gd = GradientDrawable(
                                GradientDrawable.Orientation.TOP_BOTTOM,
                                intArrayOf(startColor, endColor)
                            )
                            gd.cornerRadius = 0f
                            gd.gradientType = GradientDrawable.LINEAR_GRADIENT
                            gd.gradientRadius = 0.5f
                            gd.alpha = 150
                            val bg = ColorUtils.setAlphaComponent(startColor, 230)
                            viewModel.gradientDrawable.postValue(gd)
                            viewModel.lyricsBackground.postValue(bg)
                            return input
                        }

                    },
                    RoundedCornersTransformation(8f)
                )
            }
            binding.topAppBar.subtitle = from
            viewModel.from.postValue(from)
            binding.tvSongTitle.text = nowPlaying.title
            binding.tvSongTitle.isSelected = true
            val tempArtist = mutableListOf<String>()
            if (nowPlaying.artists != null) {
                for (artist in nowPlaying.artists) {
                    tempArtist.add(artist.name)
                }
            }
            val artistName: String = connectArtists(tempArtist)
            binding.tvSongArtist.text = artistName
            binding.tvSongArtist.isSelected = true
            binding.tvSongTitle.visibility = View.VISIBLE
            binding.tvSongArtist.visibility = View.VISIBLE

        }
    }

    private fun updateUIfromCurrentMediaItem(mediaItem: MediaItem?) {
        if (mediaItem != null) {
            binding.ivArt.setImageResource(0)
            binding.loadingArt.visibility = View.VISIBLE
            binding.tvSongTitle.visibility = View.VISIBLE
            binding.tvSongArtist.visibility = View.VISIBLE
            binding.topAppBar.subtitle = from
            viewModel.from.postValue(from)
            binding.tvSongTitle.setTextAnimation(mediaItem.mediaMetadata.title.toString())
            binding.tvSongTitle.isSelected = true
            binding.tvSongArtist.setTextAnimation(mediaItem.mediaMetadata.artist.toString())
            binding.tvSongArtist.isSelected = true
            binding.ivArt.load(mediaItem.mediaMetadata.artworkUri) {
                diskCacheKey(mediaItem.mediaId)
                diskCachePolicy(CachePolicy.ENABLED)
                crossfade(true)
                crossfade(300)
                listener(
                    onStart = {
                        binding.ivArt.setImageResource(0)
                        binding.loadingArt.visibility = View.VISIBLE
                    },
                    onSuccess = { _, _ ->
                        binding.ivArt.visibility = View.VISIBLE
                        binding.loadingArt.visibility = View.GONE
                        if (viewModel.gradientDrawable.value != null) {
                            viewModel.gradientDrawable.observe(viewLifecycleOwner) {
                                if (it != null && viewModel.canvas.value?.canvases.isNullOrEmpty()) {
                                    var start = binding.rootLayout.background
                                    if (start == null) {
                                        start = ColorDrawable(Color.BLACK)
                                    }
                                    val transition = TransitionDrawable(arrayOf(start, it))
                                    binding.rootLayout.background = transition
                                    transition.isCrossFadeEnabled = true
                                    transition.startTransition(500)
                                }
                                viewModel.lyricsBackground.value?.let { it1 ->
                                    binding.lyricsLayout.setCardBackgroundColor(
                                        it1
                                    )
                                    binding.infoLayout.setCardBackgroundColor(it1)
                                }
                            }
                        }
                    },
                )
                transformations(object : Transformation {
                    override val cacheKey: String
                        get() = "paletteArtTransformer"

                    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
                        val p = Palette.from(input).generate()
                        val defaultColor = 0x000000
                        var startColor = p.getDarkVibrantColor(defaultColor)
                        if (startColor == defaultColor) {
                            startColor = p.getDarkMutedColor(defaultColor)
                            if (startColor == defaultColor) {
                                startColor = p.getVibrantColor(defaultColor)
                                if (startColor == defaultColor) {
                                    startColor = p.getMutedColor(defaultColor)
                                    if (startColor == defaultColor) {
                                        startColor = p.getLightVibrantColor(defaultColor)
                                        if (startColor == defaultColor) {
                                            startColor = p.getLightMutedColor(defaultColor)
                                        }
                                    }
                                }
                            }
                        }
                        val endColor = 0x1b1a1f
                        val gd = GradientDrawable(
                            GradientDrawable.Orientation.TOP_BOTTOM,
                            intArrayOf(startColor, endColor)
                        )
                        gd.cornerRadius = 0f
                        gd.gradientType = GradientDrawable.LINEAR_GRADIENT
                        gd.gradientRadius = 0.5f
                        gd.alpha = 150
                        val bg = ColorUtils.setAlphaComponent(startColor, 230)
                        viewModel.gradientDrawable.postValue(gd)
                        viewModel.lyricsBackground.postValue(bg)
                        return input
                    }
                })
            }
        }
    }

    fun connectArtists(artists: List<String>): String {
        val stringBuilder = StringBuilder()

        for ((index, artist) in artists.withIndex()) {
            stringBuilder.append(artist)

            if (index < artists.size - 1) {
                stringBuilder.append(", ")
            }
        }

        return stringBuilder.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::adView.isInitialized) {
            AdMobConfig.destroyRectangleBannerAd(binding.adViewContainer, adView)
        }
        binding.btnAdCollapsible.isVisible = false
        arguments?.putString("type", null)
        arguments?.putString("videoId", null)
        val activity = requireActivity()
        activity.window.navigationBarColor = Color.parseColor("#CB0B0A0A")
        val bottom = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        val miniplayer = activity.findViewById<SwipeLayout>(R.id.miniplayer)
        if (!isFullScreen) {
            bottom.animation = AnimationUtils.loadAnimation(requireContext(), R.anim.bottom_to_top)
            bottom.visibility = View.VISIBLE
            miniplayer.animation =
                AnimationUtils.loadAnimation(requireContext(), R.anim.bottom_to_top)
            miniplayer.visibility = View.VISIBLE
            if (viewModel.isFirstMiniplayer) {
                val balloon = Balloon.Builder(requireContext())
                    .setWidthRatio(0.5f)
                    .setHeight(BalloonSizeSpec.WRAP)
                    .setText(getString(R.string.guide_miniplayer_content))
                    .setTextColorResource(R.color.md_theme_dark_onSurface)
                    .setTextSize(11f)
                    .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
                    .setAutoDismissDuration(5000L)
                    .setArrowSize(10)
                    .setArrowPosition(0.5f)
                    .setPadding(8)
                    .setCornerRadius(8f)
                    .setBackgroundColorResource(R.color.md_theme_dark_onSecondary)
                    .setBalloonAnimation(BalloonAnimation.ELASTIC)
                    .setLifecycleOwner(activity)
                    .build()
                balloon.showAlignTop(miniplayer)
                viewModel.putString("miniplayer_guide", STATUS_DONE)
                viewModel.isFirstMiniplayer = false
            }
        }
        isFullScreen = false
        overlayJob?.cancel()
        canvasOverlayJob?.cancel()
    }

    private fun parseTimestampToMilliseconds(timestamp: String): Double {
        val parts = timestamp.split(":")
        val totalSeconds = when (parts.size) {
            2 -> {
                try {
                    val minutes = parts[0].toDouble()
                    val seconds = parts[1].toDouble()
                    (minutes * 60 + seconds)
                } catch (e: NumberFormatException) {
                    // Handle parsing error
                    e.printStackTrace()
                    return 0.0
                }
            }

            3 -> {
                try {
                    val hours = parts[0].toDouble()
                    val minutes = parts[1].toDouble()
                    val seconds = parts[2].toDouble()
                    (hours * 3600 + minutes * 60 + seconds)
                } catch (e: NumberFormatException) {
                    // Handle parsing error
                    e.printStackTrace()
                    return 0.0
                }
            }

            else -> {
                // Handle incorrect format
                return 0.0
            }
        }
        return totalSeconds * 1000
    }

    private fun showLoadingDialog() {
        val loadingDialog = LoadingDialogFragment()
        loadingDialog.isCancelable = false
        activity?.supportFragmentManager?.let { loadingDialog.show(it, "LoadingDialog") }
    }

    private fun hideLoadingDialog() {
        val loadingDialog =
            activity?.supportFragmentManager?.findFragmentByTag("LoadingDialog") as? LoadingDialogFragment
        loadingDialog?.dismiss()
    }
}