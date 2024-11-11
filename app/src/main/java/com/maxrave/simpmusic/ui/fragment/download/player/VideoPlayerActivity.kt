package com.maxrave.simpmusic.ui.fragment.download.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.TimeBar
import com.github.vkay94.dtpv.youtube.YouTubeOverlay
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.ads.admob.AdMobConfig
import com.maxrave.simpmusic.ads.config.AdConfig
import com.maxrave.simpmusic.databinding.ActivityLocalVideoPlayerBinding
import com.maxrave.simpmusic.ui.fragment.download.DownloadActivity
import com.maxrave.simpmusic.ui.fragment.download.videos.Video
import com.maxrave.simpmusic.utils.SharedPrefsHelper
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class VideoPlayerActivity : AppCompatActivity(), AudioManager.OnAudioFocusChangeListener {

    private lateinit var binding: ActivityLocalVideoPlayerBinding
    private lateinit var videoTitle: TextView
    private lateinit var playPauseBtn: ImageButton
    private lateinit var nextBtn: ImageButton
    private lateinit var prevBtn: ImageButton
    private lateinit var orientationBtn: ImageButton
    private lateinit var backBtn: ImageButton

    private lateinit var adView: AdView
    private lateinit var adViewContainer: LinearLayout
    private lateinit var btnAdCollapsible: ImageView


    companion object {
        private var audioManager: AudioManager? = null
        private lateinit var player: ExoPlayer

        @JvmField
        var playerList: ArrayList<Video> = ArrayList()

        @JvmField
        var position: Int = -1

        private lateinit var loudnessEnhancer: LoudnessEnhancer
        private var speed: Float = 1.0f
        var nowPlayingId: String = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = ActivityLocalVideoPlayerBinding.inflate(layoutInflater)
        setTheme(R.style.playerActivityTheme)
        setContentView(binding.root)

        videoTitle = findViewById(R.id.videoTitle)
        playPauseBtn = findViewById(R.id.playPauseBtn)
        orientationBtn = findViewById(R.id.orientationBtn)
        backBtn = findViewById(R.id.backBtn)
        nextBtn = findViewById(R.id.nextBtn)
        prevBtn = findViewById(R.id.prevBtn)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        this.getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );


        try {
            if (intent.data?.scheme.contentEquals("content")) {
                playerList = java.util.ArrayList()
                position = 0

                try {
                    // Use the Uri directly
                    val title = getFileNameFromUri(intent.data!!)
                    val video = Video(
                        id = "", // Generate ID if necessary
                        title = title,
                        duration = 0L,  // Duration can be fetched separately if needed
                        artUri = intent.data!!,  // Use Uri directly
                        path = intent.data.toString(),  // Use Uri string as the path
                        size = "",  // Optionally, fetch file size
                        folderName = ""  // Optionally, fetch folder name
                    )

                    playerList.add(video)

                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Initialize the player with the Uri
                createPlayer(intent.data)
                initializeBinding()
            } else {
                initializeLayout()
                initializeBinding()
            }
        } catch (ignored: Exception) {
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        try {
            if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
                val uri = intent.data

                if (uri?.scheme.contentEquals("content")) {
                    playerList = java.util.ArrayList()
                    position = 0

                    try {
                        // Use the Uri directly
                        val title = getFileNameFromUri(uri!!)
                        val video = Video(
                            id = "", // Generate ID if necessary
                            title = title,
                            duration = 0L,  // Duration can be fetched separately if needed
                            artUri = uri!!,  // Use Uri directly
                            path = uri.toString(),  // Use Uri string as the path
                            size = "",  // Optionally, fetch file size
                            folderName = ""  // Optionally, fetch folder name
                        )

                        playerList.add(video)

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // Initialize the player with the Uri
                    createPlayer(uri)
                    initializeBinding()

                } else {
                    // Handle other cases where intent data is not "content://"
                    initializeLayout()
                    initializeBinding()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var result: String? = null
        if (uri.scheme.equals("content")) {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    result =
                        cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result ?: "unknown_file"
    }

    private fun initializeLayout() {
        when (intent.getStringExtra("class")) {
            "NowPlaying" -> {
                speed = 1.0f
                videoTitle.text = playerList[position].title
                videoTitle.isSelected = true
                createPlayer()
                doubleTapEnable()
                playVideo()
                seekBarFeature()
            }
        }
    }

    private fun initializeBinding() {
        orientationBtn.setOnClickListener {
            requestedOrientation =
                if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    orientationBtn.setImageResource(R.drawable.baseline_fullscreen_exit_24)
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                    orientationBtn.setImageResource(R.drawable.baseline_fullscreen_24)
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                }
        }

        backBtn.setOnClickListener {
            finish()
        }
        playPauseBtn.setOnClickListener {
            if (player.isPlaying) {
                pauseVideo()
                if (::adView.isInitialized) {
                    showRectangleBannerAd()
                } else {
                    showRectangleBannerAd()
                }
            } else {
                playVideo()
                if (::adView.isInitialized) {
                    findViewById<LinearLayout>(R.id.adViewContainer).visibility = View.GONE
                    findViewById<ImageView>(R.id.btnAdCollapsible).visibility = View.GONE
                } else {
                    findViewById<LinearLayout>(R.id.adViewContainer).visibility = View.GONE
                    findViewById<ImageView>(R.id.btnAdCollapsible).visibility = View.GONE
                }
            }
        }
        nextBtn.setOnClickListener { nextPrevVideo() }
        prevBtn.setOnClickListener { nextPrevVideo(isNext = false) }
    }

    @OptIn(UnstableApi::class)
    private fun createPlayer(uri: Uri? = null) {
        try {
            player.release()
        } catch (ignored: Exception) {
        }

        speed = 1.0f
        videoTitle.text = playerList[position].title
        videoTitle.isSelected = true

        // Initialize ExoPlayer
        player = ExoPlayer.Builder(this)
            .setTrackSelector(DefaultTrackSelector(this))
            .build()

        doubleTapEnable()

        // If a Uri is passed, create a media item with it
        val mediaItem = uri?.let {
            MediaItem.fromUri(it)
        } ?: MediaItem.fromUri(playerList[position].artUri)

        player.setMediaItem(mediaItem)
        player.setPlaybackSpeed(speed)

        player.prepare()
        playVideo()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == com.google.android.exoplayer2.Player.STATE_ENDED) nextPrevVideo()
            }
        })

        loudnessEnhancer = LoudnessEnhancer(player.audioSessionId)
        loudnessEnhancer.enabled = true
        nowPlayingId = playerList[position].id
        seekBarFeature()
    }

    @OptIn(UnstableApi::class)
    private fun createPlayer() {
        try {
            player.release()
        } catch (ignored: Exception) {
        }

        speed = 1.0f
        videoTitle.text = playerList[position].title
        videoTitle.isSelected = true
        player = ExoPlayer.Builder(this).setTrackSelector(DefaultTrackSelector(this)).build()
        doubleTapEnable()
        val mediaItem = MediaItem.fromUri(playerList[position].artUri)
        player.setMediaItem(mediaItem)
        player.setPlaybackSpeed(speed)

        player.prepare()
        playVideo()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_ENDED) nextPrevVideo()
            }
        })
        loudnessEnhancer = LoudnessEnhancer(player.audioSessionId)
        loudnessEnhancer.enabled = true
        nowPlayingId = playerList[position].id
        seekBarFeature()
    }

    private fun playVideo() {
        playPauseBtn.setImageResource(R.drawable.pause_widget)
        player.play()
        if (::adView.isInitialized) {
            findViewById<LinearLayout>(R.id.adViewContainer).visibility = View.GONE
            findViewById<ImageView>(R.id.btnAdCollapsible).visibility = View.GONE
        } else {
            findViewById<LinearLayout>(R.id.adViewContainer).visibility = View.GONE
            findViewById<ImageView>(R.id.btnAdCollapsible).visibility = View.GONE
        }
    }

    private fun pauseVideo() {
        playPauseBtn.setImageResource(R.drawable.play_widget)
        player.pause()
        if (::adView.isInitialized) {
            findViewById<LinearLayout>(R.id.adViewContainer).visibility = View.GONE
            findViewById<ImageView>(R.id.btnAdCollapsible).visibility = View.GONE
        } else {
            findViewById<LinearLayout>(R.id.adViewContainer).visibility = View.GONE
            findViewById<ImageView>(R.id.btnAdCollapsible).visibility = View.GONE
        }
    }

    private fun nextPrevVideo(isNext: Boolean = true) {
        if (isNext) setPosition()
        else setPosition(isIncrement = false)
        createPlayer()
        if (::adView.isInitialized) {
            findViewById<LinearLayout>(R.id.adViewContainer).visibility = View.GONE
            findViewById<ImageView>(R.id.btnAdCollapsible).visibility = View.GONE
        } else {
            findViewById<LinearLayout>(R.id.adViewContainer).visibility = View.GONE
            findViewById<ImageView>(R.id.btnAdCollapsible).visibility = View.GONE
        }
    }

    private fun setPosition(isIncrement: Boolean = true) {
        if (isIncrement) {
            if (playerList.size - 1 == position) position = 0
            else ++position
        } else {
            if (position == 0) position = playerList.size - 1
            else --position
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::adView.isInitialized) {
            AdMobConfig.destroyRectangleBannerAd(
                findViewById<LinearLayout>(R.id.adViewContainer),
                adView
            )
        }
        if (player.isPlaying) {
            player.pause()
        }
        player.release()
        audioManager?.abandonAudioFocus(this)
    }

    override fun onAudioFocusChange(focusChange: Int) {
        if (focusChange <= 0) pauseVideo()
    }

    override fun onResume() {
        super.onResume()
        if (audioManager == null) audioManager =
            getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager!!.requestAudioFocus(
            this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun doubleTapEnable() {
        binding.playerView.player = player
        binding.ytOverlay.performListener(object : YouTubeOverlay.PerformListener {
            override fun onAnimationEnd() {
                binding.ytOverlay.visibility = View.GONE
            }

            override fun onAnimationStart() {
                binding.ytOverlay.visibility = View.VISIBLE
            }
        })
        binding.ytOverlay.player(player)
        binding.playerView.setOnTouchListener { _, motionEvent ->
            binding.playerView.isDoubleTapEnabled = false
            binding.playerView.isDoubleTapEnabled = true
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                WindowInsetsControllerCompat(window, binding.root).let { controller ->
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
            return@setOnTouchListener false
        }
    }

    @OptIn(UnstableApi::class)
    private fun seekBarFeature() {
        findViewById<DefaultTimeBar>(R.id.exo_progress).addListener(object :
            TimeBar.OnScrubListener {
            override fun onScrubStart(timeBar: TimeBar, position: Long) {
                pauseVideo()
            }

            override fun onScrubMove(timeBar: TimeBar, position: Long) {
                player.seekTo(position)
            }

            override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                playVideo()
            }
        })
    }

    private fun getPathFromURI(context: Context, uri: Uri): String {
        var filePath = ""
        val docId = DocumentsContract.getDocumentId(uri)
        val split = docId.split(':')
        val type = split[0]

        return if ("primary".equals(type, ignoreCase = true)) {
            "${Environment.getExternalStorageDirectory()}/${split[1]}"
        } else {
            val external = context.externalMediaDirs
            if (external.size > 1) {
                filePath = external[1].absolutePath
                filePath = filePath.substring(0, filePath.indexOf("Android")) + split[1]
            }
            filePath
        }
    }

    override fun onBackPressed() {
        // Check if the activity was opened from an external app
        val action = intent?.action
        val data = intent?.data

        if (Intent.ACTION_VIEW == action && data != null) {
            // If the user came from another app (external link), navigate to MainActivity
            val intent = Intent(this, DownloadActivity::class.java).apply {
                putExtra("type", "video")  // Pass the "type" as audio or change based on logic
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish() // Finish the current activity to avoid going back to it
        } else {
            // Otherwise, perform the default back press behavior
            super.onBackPressed()
        }
    }

    private fun showRectangleBannerAd() {
        Log.d("Mohit", "showRectangleBannerAd: ")
        adViewContainer = findViewById<LinearLayout>(R.id.adViewContainer)
        btnAdCollapsible = findViewById<ImageView>(R.id.btnAdCollapsible)
        adViewContainer.removeAllViews()
        adViewContainer.isVisible = false
        btnAdCollapsible.isVisible = false
        adViewContainer.bringToFront()
        btnAdCollapsible.bringToFront()

        val adConfig = SharedPrefsHelper.loadObjectPrefs(
            this, SharedPrefsHelper.Key.Banner_Offline_Video.name,
            AdConfig::class.java
        ) as? AdConfig
        if (adConfig != null && adConfig.Show) {
            adView = AdView(this)
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
                    adViewContainer.addView(adView)
                    adViewContainer.isVisible = true
                    btnAdCollapsible.isVisible = true
                    lifecycleScope.launch {
                        adViewContainer.visibility = View.VISIBLE
                        btnAdCollapsible.visibility = View.VISIBLE
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.d("Mohit", "onAdFailedToLoad: ")
                    // Code to be executed when an ad request fails.
                    adViewContainer.removeAllViews()
                    adViewContainer.isVisible = false
                    btnAdCollapsible.isVisible = false
                    lifecycleScope.launch {
                        adViewContainer.visibility = View.GONE
                        btnAdCollapsible.visibility = View.GONE
                    }
                }
            }
            adView.loadAd(adRequest)
        } else {
            adViewContainer.removeAllViews()
            adViewContainer.isVisible = false
            btnAdCollapsible.isVisible = false
            lifecycleScope.launch {
                adViewContainer.visibility = View.GONE
                btnAdCollapsible.visibility = View.GONE
            }

        }

        btnAdCollapsible.setOnClickListener {
            AdMobConfig.destroyRectangleBannerAd(adViewContainer, adView)
            adViewContainer.isVisible = false
            btnAdCollapsible.isVisible = false
            lifecycleScope.launch {
                adViewContainer.visibility = View.GONE
                btnAdCollapsible.visibility = View.GONE
            }
        }
    }
}