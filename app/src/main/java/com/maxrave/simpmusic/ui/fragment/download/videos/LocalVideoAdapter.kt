package com.maxrave.simpmusic.ui.fragment.download.videos

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.databinding.ListLocalVideoItemBinding

class LocalVideoAdapter(private val context: Context, private val listener: Listener) :
    RecyclerView.Adapter<LocalVideoAdapter.ViewHolder>() {

    interface Listener {
        fun onItemSelected(position: Int)
    }

    private var videos: List<Video> = ArrayList()
    private var position = 0

    @SuppressLint("NotifyDataSetChanged")
    fun setVideos(videos: List<Video>) {
        this.videos = videos
        notifyDataSetChanged()
    }

    fun clear() {
        videos = ArrayList()
        notifyDataSetChanged()
    }

    fun getVideo(position: Int): Video {
        return videos[position]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ListLocalVideoItemBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return videos.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.title.text = videos[position].title
        holder.folder.text = videos[position].folderName
        Log.d("Mohit", "onBindViewHolder: "+videos.get(position).duration/1000)
        holder.duration.text = DateUtils.formatElapsedTime(videos[position].duration / 1000)
        Glide.with(context)
            .asBitmap()
            .load(videos[position].artUri)
            .apply(RequestOptions().placeholder(R.drawable.holder).centerCrop())
            .into(holder.image)
        holder.itemView.setOnClickListener { listener.onItemSelected(position) }
    }

    class ViewHolder(binding: ListLocalVideoItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.itemVideoTitleView
        val folder = binding.itemAdditionalDetails
        val duration = binding.itemDurationView
        val image = binding.itemThumbnailView
        val root = binding.root
    }
}