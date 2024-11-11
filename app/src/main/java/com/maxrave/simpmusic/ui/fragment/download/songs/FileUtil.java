package com.maxrave.simpmusic.ui.fragment.download.songs;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public final class FileUtil {

    private FileUtil() {
    }

    @NonNull
    public static List<File> listFilesDeep(@NonNull File directory, @Nullable FileFilter fileFilter) {
        List<File> files = new LinkedList<>();
        internalListFilesDeep(files, directory, fileFilter);
        return files;
    }

    private static void internalListFilesDeep(@NonNull Collection<File> files, @NonNull File directory, @Nullable FileFilter fileFilter) {
        File[] found = directory.listFiles(fileFilter);

        if (found != null) {
            for (File file : found) {
                if (file.isDirectory()) {
                    internalListFilesDeep(files, file, fileFilter);
                } else {
                    files.add(file);
                }
            }
        }
    }

    public static boolean fileIsMimeType(File file, String mimeType, MimeTypeMap mimeTypeMap) {
        if (mimeType == null || mimeType.equals("*/*")) {
            return true;
        } else {
            // get the file mime type
            String filename = file.toURI().toString();
            int dotPos = filename.lastIndexOf('.');
            if (dotPos == -1) {
                return false;
            }
            String fileExtension = filename.substring(dotPos + 1).toLowerCase();
            String fileType = mimeTypeMap.getMimeTypeFromExtension(fileExtension);
            if (fileType == null) {
                return false;
            }
            // check the 'type/subtype' pattern
            if (fileType.equals(mimeType)) {
                return true;
            }
            // check the 'type/*' pattern
            int mimeTypeDelimiter = mimeType.lastIndexOf('/');
            if (mimeTypeDelimiter == -1) {
                return false;
            }
            String mimeTypeMainType = mimeType.substring(0, mimeTypeDelimiter);
            String mimeTypeSubtype = mimeType.substring(mimeTypeDelimiter + 1);
            if (!mimeTypeSubtype.equals("*")) {
                return false;
            }
            int fileTypeDelimiter = fileType.lastIndexOf('/');
            if (fileTypeDelimiter == -1) {
                return false;
            }
            String fileTypeMainType = fileType.substring(0, fileTypeDelimiter);
            return fileTypeMainType.equals(mimeTypeMainType);
        }
    }

    public static String safeGetCanonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }

    public static final FileFilter AUDIO_FILE_FILTER = file -> !file.isHidden() && (file.isDirectory() ||
            FileUtil.fileIsMimeType(file, "audio/*", MimeTypeMap.getSingleton()) ||
            FileUtil.fileIsMimeType(file, "application/ogg", MimeTypeMap.getSingleton()));

    public static File getDefaultStartDirectory() {
        File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        File startFolder;
        if (musicDir.exists() && musicDir.isDirectory()) {
            startFolder = musicDir;
        } else {
            File externalStorage = Environment.getExternalStorageDirectory();
            if (externalStorage.exists() && externalStorage.isDirectory()) {
                startFolder = externalStorage;
            } else {
                startFolder = new File("/"); // root
            }
        }
        return startFolder;
    }

    public static String getFileName(Context context, Uri uri) {
        String result = null;
        try {
            if (Objects.equals(uri.getScheme(), "content")) {
                if (context.getContentResolver() != null){
                    try (Cursor cursor = context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                        if (cursor != null && cursor.moveToFirst()) {
                            final int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                            if (columnIndex > -1) {
                                result = cursor.getString(columnIndex);
                            }
                        }
                    }
                }

            }
            if (result == null) {
                result = uri.getPath();
                if (result != null) {
                    int cut = result.lastIndexOf('/');
                    if (cut != -1) {
                        result = result.substring(cut + 1);
                    }
                }
            }
            if (result != null && result.indexOf(".") > 0) {
                result = result.substring(0, result.lastIndexOf("."));
            }
        } catch (Exception ignored) {

        }
        return result;
    }
}
