package com.maxrave.simpmusic.downloader.util;

import android.content.Context;

import com.maxrave.simpmusic.R;
import com.maxrave.simpmusic.downloader.DownloaderImpl;

import org.schabi.newpipe.extractor.stream.Stream;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class StreamSizeWrapper<T extends Stream> implements Serializable {

    private static final StreamSizeWrapper<Stream> EMPTY = new StreamSizeWrapper<>(Collections.emptyList(), null);
    private final List<T> streamsList;
    private final long[] streamSizes;
    private final String unknownSize;

    public StreamSizeWrapper(List<T> sL, Context context) {
        this.streamsList = sL != null ? sL : Collections.emptyList();
        this.streamSizes = new long[streamsList.size()];
        this.unknownSize = context == null ? "--.-" : context.getString(R.string.unknown_content);
        Arrays.fill(streamSizes, -2);
    }

    /**
     * Helper method to fetch the sizes of all the streams in a wrapper.
     *
     * @param streamsWrapper the wrapper
     * @return a {@link Single} that returns a boolean indicating if any elements were changed
     */
    public static <X extends Stream> Single<Boolean> fetchSizeForWrapper(StreamSizeWrapper<X> streamsWrapper) {
        final Callable<Boolean> fetchAndSet = () -> {
            boolean hasChanged = false;
            for (X stream : streamsWrapper.getStreamsList()) {
                if (streamsWrapper.getSizeInBytes(stream) > -2) {
                    continue;
                }
                final long contentLength = DownloaderImpl.getInstance().getContentLength(stream.getUrl());
                streamsWrapper.setSize(stream, contentLength);
                hasChanged = true;
            }
            return hasChanged;
        };

        return Single.fromCallable(fetchAndSet)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorReturnItem(true);
    }

    public List<T> getStreamsList() {
        return streamsList;
    }

    public long getSizeInBytes(int streamIndex) {
        return streamSizes[streamIndex];
    }

    public long getSizeInBytes(T stream) {
        return streamSizes[streamsList.indexOf(stream)];
    }

    public String getFormattedSize(int streamIndex) {
        return formatSize(getSizeInBytes(streamIndex));
    }

    public String getFormattedSize(T stream) {
        return formatSize(getSizeInBytes(stream));
    }

    private String formatSize(long size) {
        if (size > -1) {
            return Utility.formatBytes(size);
        }
        return unknownSize;
    }

    public void setSize(int streamIndex, long sizeInBytes) {
        streamSizes[streamIndex] = sizeInBytes;
    }

    public void setSize(T stream, long sizeInBytes) {
        streamSizes[streamsList.indexOf(stream)] = sizeInBytes;
    }

    public static <X extends Stream> StreamSizeWrapper<X> empty() {
        //noinspection unchecked
        return (StreamSizeWrapper<X>) EMPTY;
    }
}