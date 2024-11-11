package com.maxrave.simpmusic.ui.fragment.download.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
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
import com.bumptech.glide.Glide
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.material.imageview.ShapeableImageView
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.ads.admob.AdMobConfig
import com.maxrave.simpmusic.ads.config.AdConfig
import com.maxrave.simpmusic.databinding.ActivityLocalMusicPlayerBinding
import com.maxrave.simpmusic.ui.fragment.download.DownloadActivity
import com.maxrave.simpmusic.ui.fragment.download.songs.FileUtil
import com.maxrave.simpmusic.ui.fragment.download.songs.Song
import com.maxrave.simpmusic.ui.fragment.download.songs.SongGlideRequest
import com.maxrave.simpmusic.utils.SharedPrefsHelper
import com.maxrave.simpmusic.utils.Utils
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class MusicPlayerActivity : AppCompatActivity(), AudioManager.OnAudioFocusChangeListener {

    private lateinit var binding: ActivityLocalMusicPlayerBinding

    private lateinit var adView: AdView

    private lateinit var customeNativeSmall:View

    companion object {
        private var audioManager: AudioManager? = null
        private lateinit var player: ExoPlayer

        @JvmField
        var playerList: ArrayList<Song> = ArrayList()

        @JvmField
        var position: Int = -1

        private lateinit var loudnessEnhancer: LoudnessEnhancer
        private var speed: Float = 1.0f
        var nowPlayingId: String = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = ActivityLocalMusicPlayerBinding.inflate(layoutInflater)
        setTheme(R.style.playerActivityTheme)
        setContentView(binding.root)

        customeNativeSmall = findViewById(R.id.custom_template_music)
        showRectangleBannerAd()

        var adConfig = (SharedPrefsHelper.loadObjectPrefs(
            this, SharedPrefsHelper.Key.Native_Offline_Music.name,
            AdConfig::class.java
        ) as? AdConfig)!!
        if (adConfig != null && adConfig.Show && !this.isDestroyed) {
            AdMobConfig.showNativeAdWithLoad(
                this,
                binding.templateViewMusic,
                adConfig.Ad_Id,
                binding.shimmerNativeSmall,
                customeNativeSmall,
                "small"
            )
        }else{
            binding.shimmerNativeSmall.stopShimmer()
            binding.shimmerNativeSmall.visibility = View.GONE
        }
        setSupportActionBar(binding.defaultToolbar)
        supportActionBar.let {
            binding.defaultToolbar.setNavigationOnClickListener {
                finish()
            }
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


        try {
            if (intent?.data?.scheme.contentEquals("content")) {
                playerList = ArrayList()
                position = 0
                handleContentUri(intent!!.data!!)
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

        Log.d("MusicPlayer", "Intent Data: ${intent?.data}")
        Log.d("MusicPlayer", "Intent Scheme: ${intent?.data?.scheme}")

        this.getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        try {
            if (intent?.data?.scheme.contentEquals("content")) {
                playerList = ArrayList()
                position = 0
                handleContentUri(intent!!.data!!)
            } else {
                initializeLayout()
                initializeBinding()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleContentUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("audio", ".mp3", cacheDir)

            // Copy InputStream to a temp file
            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input?.copyTo(output)
                }
            }

            // Use tempFile for playback
            val song = Song(
                0L,
                Utils.getFileName(this, uri), // Fetch the name
                0,
                0,
                0L,
                tempFile.absolutePath, // Play from tempFile
                0L,
                0L,
                "",
                0L,
                ""
            )
            playerList.add(song)
            initializeLayout()
            initializeBinding()
            createPlayer()

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MusicPlayer", "Error handling content URI", e)
        }
    }

    @SuppressLint("CutPasteId")
    private fun initializeLayout() {
        when (intent.getStringExtra("class")) {
            "NowPlaying" -> {
                speed = 1.0f
                findViewById<TextView>(R.id.title).text = playerList[position].title
                findViewById<TextView>(R.id.author).text = playerList[position].artistName
                SongGlideRequest.Builder.from(Glide.with(this), playerList[position])
                    .generatePalette(this).build()
                    .centerCrop()
                    .into(findViewById<ShapeableImageView>(R.id.art))
                createPlayer()
                doubleTapEnable()
                playSong()
                seekBarFeature()
            }
        }
    }

    private fun initializeBinding() {
        findViewById<ImageButton>(R.id.playPauseBtn).setOnClickListener {
            if (player.isPlaying) pauseSong()
            else playSong()
        }
        findViewById<ImageButton>(R.id.nextBtn).setOnClickListener { nextPrevSong() }
        findViewById<ImageButton>(R.id.prevBtn).setOnClickListener { nextPrevSong(isNext = false) }
    }

    @OptIn(UnstableApi::class)
    private fun createPlayer() {
        try {
            player.release()
        } catch (ignored: Exception) {
        }

        speed = 1.0f
        findViewById<TextView>(R.id.title).text = playerList[position].title
        findViewById<TextView>(R.id.author).text = playerList[position].artistName
        SongGlideRequest.Builder.from(Glide.with(this), playerList[position])
            .generatePalette(this).build()
            .centerCrop()
            .into(findViewById<ShapeableImageView>(R.id.art))
        player = ExoPlayer.Builder(this).setTrackSelector(DefaultTrackSelector(this)).build()
        doubleTapEnable()
        val mediaItem = MediaItem.fromUri(playerList[position].data)
        player.setMediaItem(mediaItem)
        player.setPlaybackSpeed(speed)

        player.prepare()
        playSong()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_ENDED) nextPrevSong()
            }
        })
        loudnessEnhancer = LoudnessEnhancer(player.audioSessionId)
        loudnessEnhancer.enabled = true
        nowPlayingId = playerList[position].id.toString()
        seekBarFeature()
    }

    private fun playSong() {
        findViewById<ImageButton>(R.id.playPauseBtn).setImageResource(R.drawable.pause_widget)
        player.play()
    }

    private fun pauseSong() {
        findViewById<ImageButton>(R.id.playPauseBtn).setImageResource(R.drawable.play_widget)
        player.pause()
    }

    private fun nextPrevSong(isNext: Boolean = true) {
        if (isNext) setPosition()
        else setPosition(isIncrement = false)
        createPlayer()
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
            AdMobConfig.destroyRectangleBannerAd(findViewById<LinearLayout>(R.id.adViewContainer), adView)
        }
        if (player.isPlaying) {
            player.pause()
        }
        player.release()
        audioManager?.abandonAudioFocus(this)
    }

    override fun onAudioFocusChange(focusChange: Int) {
        if (focusChange <= 0) pauseSong()
    }

    override fun onResume() {
        super.onResume()
        if (audioManager == null) audioManager =
            getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager!!.requestAudioFocus(
            this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
        )
        if (::adView.isInitialized) {
            AdMobConfig.resume(adView)
        }
    }

    @OptIn(UnstableApi::class)
    private fun seekBarFeature() {
        findViewById<DefaultTimeBar>(R.id.exo_progress).addListener(object :
            TimeBar.OnScrubListener {
            override fun onScrubStart(timeBar: TimeBar, position: Long) {
                pauseSong()
            }

            override fun onScrubMove(timeBar: TimeBar, position: Long) {
                player.seekTo(position)
            }

            override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                playSong()
            }
        })
    }

    private fun doubleTapEnable() {
        binding.playerView.player = player
    }

    private fun getPathFromURI(context: Context, uri: Uri): String? {
        var filePath: String? = null
        if (DocumentsContract.isDocumentUri(context, uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":")
            val type = split[0]

            if ("primary".equals(type, ignoreCase = true)) {
                filePath = "${Environment.getExternalStorageDirectory()}/${split[1]}"
            } else {
                // Handle external storage documents
                val externalStorageVolumes = context.externalMediaDirs
                if (externalStorageVolumes.isNotEmpty()) {
                    filePath = "${externalStorageVolumes[0].absolutePath}/${split[1]}"
                }
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
            // Handle content URIs
            filePath = getDataColumn(context, uri, null, null)
        }
        return filePath
    }

    private fun getDataColumn(context: Context, uri: Uri, selection: String?, selectionArgs: Array<String>?): String? {
        val column = "_data"
        val projection = arrayOf(column)
        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(columnIndex)
            }
        }
        return null
    }

    override fun onBackPressed() {
        // Check if the activity was opened from an external app
        val action = intent?.action
        val data = intent?.data

        if (Intent.ACTION_VIEW == action && data != null) {
            // If the user came from another app (external link), navigate to MainActivity
            val intent = Intent(this, DownloadActivity::class.java).apply {
                putExtra("type", "audio")  // Pass the "type" as audio or change based on logic
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
        var  adViewContainer = findViewById<LinearLayout>(R.id.adViewContainer)
        var  btnAdCollapsible = findViewById<ImageView>(R.id.btnAdCollapsible)
        var  art = findViewById<ShapeableImageView>(R.id.art)
        adViewContainer.removeAllViews()
        adViewContainer.isVisible = false
        btnAdCollapsible.isVisible = false
        adViewContainer.bringToFront()
        btnAdCollapsible.bringToFront()

        val adConfig = SharedPrefsHelper.loadObjectPrefs(
            this, SharedPrefsHelper.Key.Banner_Offline_Music.name,
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
                        art.visibility = View.INVISIBLE
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
                        art.visibility = View.VISIBLE
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
                art.visibility = View.VISIBLE
                adViewContainer.visibility = View.GONE
                btnAdCollapsible.visibility = View.GONE
            }

        }

        btnAdCollapsible.setOnClickListener {
            AdMobConfig.destroyRectangleBannerAd(adViewContainer, adView)
            adViewContainer.isVisible = false
            btnAdCollapsible.isVisible = false
            lifecycleScope.launch {
                art.visibility = View.VISIBLE
                adViewContainer.visibility = View.GONE
                btnAdCollapsible.visibility = View.GONE
            }
        }
    }
}