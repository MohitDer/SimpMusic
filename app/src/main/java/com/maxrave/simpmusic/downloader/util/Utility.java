package com.maxrave.simpmusic.downloader.util;

import android.content.Context;
import android.util.Log;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.maxrave.simpmusic.R;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.util.Locale;

public class Utility {

    public enum FileType {
        VIDEO,
        MUSIC,
        SUBTITLE,
        UNKNOWN
    }

    public static String formatBytes(long bytes) {
        Locale locale = Locale.getDefault();
        if (bytes < 1024) {
            return String.format(locale, "%d B", bytes);
        } else if (bytes < 1024 * 1024) {
            return String.format(locale, "%.2f kB", bytes / 1024d);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format(locale, "%.2f MB", bytes / 1024d / 1024d);
        } else {
            return String.format(locale, "%.2f GB", bytes / 1024d / 1024d / 1024d);
        }
    }

    public static String formatSpeed(double speed) {
        Locale locale = Locale.getDefault();
        if (speed < 1024) {
            return String.format(locale, "%.2f B/s", speed);
        } else if (speed < 1024 * 1024) {
            return String.format(locale, "%.2f kB/s", speed / 1024);
        } else if (speed < 1024 * 1024 * 1024) {
            return String.format(locale, "%.2f MB/s", speed / 1024 / 1024);
        } else {
            return String.format(locale, "%.2f GB/s", speed / 1024 / 1024 / 1024);
        }
    }

    public static void writeToFile(@NonNull File file, @NonNull Serializable serializable) {
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(file.toPath())))) {
            objectOutputStream.writeObject(serializable);
        } catch (Exception ignored) {
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T readFromFile(File file) {
        T object;
        ObjectInputStream objectInputStream = null;

        try {
            objectInputStream = new ObjectInputStream(Files.newInputStream(file.toPath()));
            object = (T) objectInputStream.readObject();
        } catch (Exception e) {
            Log.e("Utility", "Failed to deserialize the object", e);
            object = null;
        }

        if (objectInputStream != null) {
            try {
                objectInputStream.close();
            } catch (Exception e) {
                //nothing to do
            }
        }

        return object;
    }

    @Nullable
    public static String getFileExtension(String url) {
        int index;
        if ((index = url.indexOf("?")) > -1) {
            url = url.substring(0, index);
        }

        index = url.lastIndexOf(".");
        if (index == -1) {
            return null;
        } else {
            String ext = url.substring(index);
            if ((index = ext.indexOf("%")) > -1) {
                ext = ext.substring(0, index);
            }
            if ((index = ext.indexOf("/")) > -1) {
                ext = ext.substring(0, index);
            }
            return ext.toLowerCase();
        }
    }

    public static FileType getFileType(char kind) {
        switch (kind) {
            case 'v':
                return FileType.VIDEO;
            case 'a':
                return FileType.MUSIC;
            default:
                return FileType.UNKNOWN;
        }
    }

    public static FileType getFileType(char kind, String file) {
        switch (kind) {
            case 'v':
                return FileType.VIDEO;
            case 'a':
                return FileType.MUSIC;
            case 's':
                return FileType.SUBTITLE;
        }

        if (file.endsWith(".srt") || file.endsWith(".vtt") || file.endsWith(".ssa")) {
            return FileType.SUBTITLE;
        } else if (file.endsWith(".mp3") || file.endsWith(".wav") || file.endsWith(".flac") || file.endsWith(".m4a") || file.endsWith(".opus")) {
            return FileType.MUSIC;
        } else if (file.endsWith(".mp4") || file.endsWith(".mpeg") || file.endsWith(".rm") || file.endsWith(".rmvb")
                || file.endsWith(".flv") || file.endsWith(".webp") || file.endsWith(".webm")) {
            return FileType.VIDEO;
        }

        return FileType.UNKNOWN;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean mkdir(File p, boolean allDirs) {
        if (p.exists())
            return true;

        if (allDirs)
            p.mkdirs();
        else
            p.mkdir();

        return p.exists();
    }

    public static long getContentLength(HttpURLConnection connection) {
        return connection.getContentLengthLong();
    }

    private static String pad(int number) {
        return number < 10 ? ("0" + number) : String.valueOf(number);
    }

    public static String stringifySeconds(double seconds) {
        int h = (int) Math.floor(seconds / 3600);
        int m = (int) Math.floor((seconds - (h * 3600)) / 60);
        int s = (int) (seconds - (h * 3600) - (m * 60));

        String str = "";

        if (h < 1 && m < 1) {
            str = "00:";
        } else {
            if (h > 0)
                str = pad(h) + ":";
            if (m > 0)
                str += pad(m) + ":";
        }

        return str + pad(s);
    }

    public static long getTotalContentLength(final HttpURLConnection connection) {
        try {
            if (connection.getResponseCode() == 206) {
                final String rangeStr = connection.getHeaderField("Content-Range");
                final String bytesStr = rangeStr.split("/", 2)[1];
                return Long.parseLong(bytesStr);
            } else {
                return getContentLength(connection);
            }
        } catch (Exception err) {
            // nothing to do
        }

        return -1;
    }

    @DrawableRes
    public static int getIconForFileType(FileType type) {
        switch (type) {
            case MUSIC:
                return R.drawable.ic_music_download;
            default:
            case VIDEO:
                return R.drawable.ic_video_download;
        }
    }

    @ColorInt
    public static int getBackgroundForFileType(Context ctx) {
        int colorRes = R.color.dark_separator_color;
        return ContextCompat.getColor(ctx, colorRes);
    }

    @ColorInt
    public static int getForegroundForFileType(Context ctx) {
        int colorRes = R.color.youtube_primary_color;
        return ContextCompat.getColor(ctx, colorRes);
    }
}
