package com.maxrave.simpmusic.ui.fragment.download.songs;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import java.util.Locale;

public class MusicUtil {

    public static Uri getMediaStoreAlbumCoverUri(long albumId) {
        final Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
        return ContentUris.withAppendedId(sArtworkUri, albumId);
    }

    public static Uri getCollection() {
        return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    }
}
