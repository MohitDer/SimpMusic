package com.maxrave.simpmusic.adapter.moodandgenre.mood

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.explore.mood.moodmoments.Content
import com.maxrave.simpmusic.data.model.explore.mood.moodmoments.Item
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.databinding.ItemMoodMomentPlaylistBinding
import com.maxrave.simpmusic.extension.navigateSafe
import com.maxrave.simpmusic.extension.toTrack

class MoodItemAdapter(private var itemList: ArrayList<Item>, val context: Context, val navController: NavController): RecyclerView.Adapter<MoodItemAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemMoodMomentPlaylistBinding): RecyclerView.ViewHolder(binding.root)

    fun updateData(newList: ArrayList<Item>){
        itemList.clear()
        itemList.addAll(newList.filter { !it.header.contains("Music videos") })
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemMoodMomentPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemList[position]
        with(holder) {
            if (!item.header.contains("Music videos")) {
                binding.tvTitle.text = item.header

                // Filter out null playlistBrowseId items using the extension function
                val filteredContent: ArrayList<Content> = item.contents.filterByNonNullPlaylistBrowseId() as ArrayList<Content>

                val contentAdapter = MoodContentAdapter(filteredContent)
                val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                binding.childRecyclerview.apply {
                    adapter = contentAdapter
                    layoutManager = linearLayoutManager
                }
                contentAdapter.setOnClickListener(object : MoodContentAdapter.OnClickListener {
                    override fun onClick(position: Int) {
                        val args = Bundle()
                        if (item.header.contains("Albums")) {
                            args.putString("browseId", filteredContent[position].playlistBrowseId)
                            navController.navigateSafe(R.id.action_global_albumFragment, args)
                        } else if (item.header.contains("Music videos")) {
                            // Handle Music videos click
                            args.putString("videoId", filteredContent[position].playlistBrowseId)
                            args.putString("from", item.header)
                            Queue.clear()
                            val firstQueue: Track = filteredContent[position].toTrack()
                            Queue.setNowPlaying(firstQueue)
                            args.putString("type", Config.SONG_CLICK)
                            navController.navigateSafe(R.id.action_global_nowPlayingFragment, args)
                        } else {
                            args.putString("id", filteredContent[position].playlistBrowseId)
                            navController.navigateSafe(R.id.action_global_playlistFragment, args)
                        }
                    }
                })
            }

        }
    }

    fun List<Content>.filterByNonNullPlaylistBrowseId(): List<Content> {
        return this.filter { content ->
            !(content.playlistBrowseId == null || content.playlistBrowseId.isEmpty())
        }
    }
}