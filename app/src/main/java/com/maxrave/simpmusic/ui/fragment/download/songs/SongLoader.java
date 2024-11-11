package com.maxrave.simpmusic.ui.fragment.download.songs;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AudioColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SongLoader {
    protected static final String BASE_SELECTION = AudioColumns.IS_MUSIC + "=1" + " AND " + AudioColumns.TITLE + " != ''";
    protected static final String[] BASE_PROJECTION = new String[]{
            BaseColumns._ID,// 0
            AudioColumns.TITLE,// 1
            AudioColumns.TRACK,// 2
            AudioColumns.YEAR,// 3
            AudioColumns.DURATION,// 4
            AudioColumns.DATA,// 5
            AudioColumns.DATE_MODIFIED,// 6
            AudioColumns.ALBUM_ID,// 7
            AudioColumns.ALBUM,// 8
            AudioColumns.ARTIST_ID,// 9
            AudioColumns.ARTIST,// 10
    };

    @NonNull
    public static List<Song> getAllSongs(@NonNull Context context) {
        Cursor cursor = makeSongCursor(context, null, null);
        return getSongs(cursor);
    }

    @NonNull
    public static List<Song> getSongs(@NonNull final Context context, final String query) {
        Cursor cursor = makeSongCursor(context, AudioColumns.TITLE + " LIKE ?", new String[]{"%" + query + "%"});
        return getSongs(cursor);
    }

    @NonNull
    public static Song getSong(@NonNull final Context context, final long queryId) {
        Cursor cursor = makeSongCursor(context, AudioColumns._ID + "=?", new String[]{String.valueOf(queryId)});
        return getSong(cursor);
    }

    @NonNull
    public static List<Song> getSongs(@Nullable final Cursor cursor) {
        List<Song> songs = new ArrayList<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                songs.add(getSongFromCursorImpl(cursor));
            } while (cursor.moveToNext());
        }

        if (cursor != null)
            cursor.close();
        return songs;
    }

    @NonNull
    public static Song getSong(@Nullable Cursor cursor) {
        Song song;
        if (cursor != null && cursor.moveToFirst()) {
            song = getSongFromCursorImpl(cursor);
        } else {
            song = Song.EMPTY_SONG;
        }
        if (cursor != null) {
            cursor.close();
        }
        return song;
    }

    @NonNull
    private static Song getSongFromCursorImpl(@NonNull Cursor cursor) {
        final long id = cursor.getLong(0);
        final String title = cursor.getString(1);
        final int trackNumber = cursor.getInt(2);
        final int year = cursor.getInt(3);
        final long duration = cursor.getLong(4);
        final String data = cursor.getString(5);
        final long dateModified = cursor.getLong(6);
        final long albumId = cursor.getLong(7);
        final String albumName = cursor.getString(8);
        final long artistId = cursor.getLong(9);
        final String artistName = cursor.getString(10);

        return new Song(id, title, trackNumber, year, duration, data, dateModified, albumId, albumName, artistId, artistName.equals("<unknown>") ? albumName : artistName);
    }

    @Nullable
    public static Cursor makeSongCursor(@NonNull final Context context, @Nullable final String selection, final String[] selectionValues) {
        return makeSongCursor(context, selection, selectionValues, MediaStore.Audio.Media.DATE_MODIFIED + " DESC");
    }

    @Nullable
    public static Cursor makeSongCursor(@NonNull final Context context, @Nullable String selection, String[] selectionValues, final String sortOrder) {
        if (selection != null && !selection.trim().isEmpty()) {
            selection = BASE_SELECTION + " AND " + selection;
        } else {
            selection = BASE_SELECTION;
        }

        try {
            if (context.getContentResolver() != null){
                return context.getContentResolver().query(MusicUtil.getCollection(), BASE_PROJECTION, selection, selectionValues, sortOrder);
            }else {
                return null;
            }
        } catch (SecurityException e) {
            return null;
        }
    }
}
