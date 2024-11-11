package com.maxrave.simpmusic.ui.fragment.download.videos

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import java.io.File


data class Video(
    val id: String,
    var title: String,
    val duration: Long = 0,
    val folderName: String,
    val size: String,
    var path: String,
    var artUri: Uri
)

@SuppressLint("Range")
fun getAllVideos(context: Context): List<Video> {
    val videos = mutableListOf<Video>()
    val projection = arrayOf(
        MediaStore.Video.Media.TITLE,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Video.Media.DATA,
        MediaStore.Video.Media.DATE_ADDED,
        MediaStore.Video.Media.DURATION
    )

    val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

    val cursor = context.contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null, sortOrder
    )

    cursor?.use {
        while (it.moveToNext()) {
            val titleC = it.getString(it.getColumnIndex(MediaStore.Video.Media.TITLE)) ?: "Unknown"
            val idC = it.getString(it.getColumnIndex(MediaStore.Video.Media._ID)) ?: "Unknown"
            val folderC = it.getString(it.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)) ?: "Internal Storage"
            val sizeC = it.getString(it.getColumnIndex(MediaStore.Video.Media.SIZE)) ?: "0"
            val pathC = it.getString(it.getColumnIndex(MediaStore.Video.Media.DATA)) ?: "Unknown"
            var durationC = it.getLong(it.getColumnIndex(MediaStore.Video.Media.DURATION))

            if (durationC == 0L) {
                durationC = getVideoDuration(pathC)
            }

            try {
                val file = File(pathC)
                val artUriC = Uri.fromFile(file)
                val video = Video(
                    id = idC,
                    title = titleC,
                    folderName = folderC,
                    duration = durationC,
                    size = sizeC,
                    path = pathC,
                    artUri = artUriC
                )
                if (file.exists()) {
                    videos.add(video)
                }
            } catch (ignored: Exception) {
            }
        }
    }
    return videos
}

private fun getVideoDuration(filePath: String): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(filePath)
        val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        time?.toLong() ?: 0L
    } catch (e: Exception) {
        e.printStackTrace()
        0L
    } finally {
        retriever.release()
    }
}
