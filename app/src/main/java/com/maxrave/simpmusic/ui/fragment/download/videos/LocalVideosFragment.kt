package com.maxrave.simpmusic.ui.fragment.download.videos

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.ads.admob.AdMobInterstitialAdSong
import com.maxrave.simpmusic.databinding.FragmentLocalVideosBinding
import com.maxrave.simpmusic.ui.fragment.download.player.MusicPlayerActivity
import com.maxrave.simpmusic.ui.fragment.download.player.VideoPlayerActivity
import com.maxrave.simpmusic.ui.fragment.download.songs.Song
import kotlinx.coroutines.launch

class LocalVideosFragment : Fragment(), LocalVideoAdapter.Listener {

    private lateinit var adapter: LocalVideoAdapter
    private lateinit var binding: FragmentLocalVideosBinding
    private var videos: List<Video> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_local_videos, container, false)
        binding = FragmentLocalVideosBinding.bind(view)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let {
            AdMobInterstitialAdSong.getInstance().init(it)
            binding.itemsList.layoutManager = LinearLayoutManager(it)
            adapter = LocalVideoAdapter(it, this)
            binding.itemsList.adapter = adapter

            updateVideos()

            binding.swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(it, R.color.youtube_primary_color)
            )
            binding.swipeRefreshLayout.setOnRefreshListener {
                adapter.clear()
                updateVideos()
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun updateVideos() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val job = launch {
                    binding.loadingProgressBar.visibility = View.VISIBLE
                    Handler(Looper.getMainLooper()).postDelayed({
                        activity?.let {
                            it.runCatching {
                                videos = getAllVideos(it)
                                adapter.setVideos(videos)
                                binding.loadingProgressBar.visibility = View.GONE
                                binding.itemsList.visibility =
                                    if (videos.isEmpty()) View.GONE else View.VISIBLE
                                binding.emptyStateView.root.visibility =
                                    if (videos.isEmpty()) View.VISIBLE else View.GONE
                            }
                        }
                    }, 1000)
                }
                job.join()
            }
        }

    }

    override fun onItemSelected(position: Int) {
        AdMobInterstitialAdSong.getInstance().showInterstitialAd(activity) {
            sendIntent(pos = position)
        }

    }

    private fun sendIntent(pos: Int) {
        VideoPlayerActivity.playerList = videos as ArrayList<Video>
        VideoPlayerActivity.position = pos
        val intent = Intent(context, VideoPlayerActivity::class.java)
        intent.putExtra("class", "NowPlaying")
        ContextCompat.startActivity(requireActivity(), intent, null)
    }
}