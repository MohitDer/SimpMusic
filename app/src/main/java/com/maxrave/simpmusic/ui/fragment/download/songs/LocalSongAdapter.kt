package com.maxrave.simpmusic.ui.fragment.download.songs

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.databinding.ListLocalSongItemBinding

class LocalSongAdapter(private val context: Context, private val listener: Listener) :
    RecyclerView.Adapter<LocalSongAdapter.ViewHolder>() {

    interface Listener {
        fun onItemSelected(position: Int)
    }

    private var songs: List<Song> = ArrayList()

    @SuppressLint("NotifyDataSetChanged")
    fun setSongs(songs: List<Song>) {
        this.songs = songs
        notifyDataSetChanged()
    }

    fun getSongs(): List<Song> {
        return this.songs
    }

    fun getSong(position: Int): Song {
        return songs[position]
    }

    fun clear() {
        this.songs = ArrayList()
        notifyDataSetChanged()
    }

    fun isEmpty(): Boolean {
        return this.songs.isEmpty()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ListLocalSongItemBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return songs.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.title.text = getSong(position).title
        holder.folder.text = getSong(position).artistName
        SongGlideRequest.Builder.from(Glide.with(context), getSong(position))
            .generatePalette(context).build()
            .centerCrop()
            .error(R.drawable.baseline_music_note_24)
            .into(holder.image)
        holder.itemView.setOnClickListener { listener.onItemSelected(position) }
    }

    class ViewHolder(binding: ListLocalSongItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.itemVideoTitleView
        val folder = binding.itemAdditionalDetails
        val image = binding.itemThumbnailView
        val root = binding.root
    }
}