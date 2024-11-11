package com.maxrave.simpmusic.downloader.util;

import static org.schabi.newpipe.extractor.ServiceList.YouTube;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.preference.PreferenceManager;

import com.annimon.stream.Optional;
import com.maxrave.simpmusic.R;

import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.DeliveryMethod;
import org.schabi.newpipe.extractor.stream.Stream;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public final class ListHelper {

    // Video format in order of quality. 0=lowest quality, n=highest quality
    private static final List<MediaFormat> VIDEO_FORMAT_QUALITY_RANKING = Arrays.asList(MediaFormat.v3GPP, MediaFormat.WEBM, MediaFormat.MPEG_4);

    // Audio format in order of quality. 0=lowest quality, n=highest quality
    private static final List<MediaFormat> AUDIO_FORMAT_QUALITY_RANKING = Arrays.asList(MediaFormat.MP3, MediaFormat.WEBMA, MediaFormat.M4A);
    // Audio format in order of efficiency. 0=most efficient, n=least efficient
    private static final List<MediaFormat> AUDIO_FORMAT_EFFICIENCY_RANKING = Arrays.asList(MediaFormat.WEBMA, MediaFormat.M4A, MediaFormat.MP3);

    private static final List<String> HIGH_RESOLUTION_LIST = Arrays.asList("1440p", "2160p", "1440p60", "2160p60");

    private static final List<Integer> SUPPORTED_ITAG_IDS = List.of(
            17, 36, // video v3GPP
            18, 34, 35, 59, 78, 22, 37, 38, // video MPEG4
            43, 44, 45, 46, // video webm
            171, 172, 139, 140, 141, 249, 250, 251, // audio
            160, 133, 134, 135, 212, 136, 298, 137, 299, 266, // video only
            278, 242, 243, 244, 245, 246, 247, 248, 271, 272, 302, 303, 308, 313, 315
    );

    public static int getDefaultResolutionIndex(@NonNull Context context, List<VideoStream> videoStreams) {
        String defaultResolution = computeDefaultResolution(context, R.string.default_resolution_key, R.string.default_resolution_value);
        return getDefaultResolutionWithDefaultFormat(context, defaultResolution, videoStreams);
    }

    public static int getResolutionIndex(Context context, List<VideoStream> videoStreams, String defaultResolution) {
        return getDefaultResolutionWithDefaultFormat(context, defaultResolution, videoStreams);
    }

    public static int getDefaultAudioFormat(Context context, List<AudioStream> audioStreams) {
        MediaFormat defaultFormat = getDefaultFormat(context, R.string.default_audio_format_key, R.string.default_audio_format_value);

        // If the user has chosen to limit resolution to conserve mobile data
        // usage then we should also limit our audio usage.
        if (isLimitingDataUsage(context)) {
            return getMostCompactAudioIndex(defaultFormat, audioStreams);
        } else {
            return getHighestQualityAudioIndex(defaultFormat, audioStreams);
        }
    }

    /**
     * Join the two lists of video streams (video_only and normal videos), and sort them according with default format
     * chosen by the user
     *
     * @param context          context to search for the format to give preference
     * @param videoStreams     normal videos list
     * @param videoOnlyStreams video only stream list
     * @param ascendingOrder   true -> smallest to greatest | false -> greatest to smallest
     * @return the sorted list
     */
    @NonNull
    public static List<VideoStream> getSortedStreamVideosList(
            @NonNull final Context context,
            @Nullable final List<VideoStream> videoStreams,
            @Nullable final List<VideoStream> videoOnlyStreams,
            final boolean ascendingOrder,
            final boolean preferVideoOnlyStreams) {
        final SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(context);

        final boolean showHigherResolutions = preferences.getBoolean(
                context.getString(R.string.show_higher_resolutions_key), false);
        final MediaFormat defaultFormat = getDefaultFormat(context,
                R.string.default_video_format_key, R.string.default_video_format_value);

        return getSortedStreamVideosList(defaultFormat, showHigherResolutions, videoStreams,
                videoOnlyStreams, ascendingOrder, preferVideoOnlyStreams);
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Utils
    //////////////////////////////////////////////////////////////////////////*/

    private static String computeDefaultResolution(Context context, int key, int value) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        // Load the prefered resolution otherwise the best available
        String resolution = preferences != null ? preferences.getString(context.getString(key), context.getString(value)) : context.getString(R.string.best_resolution_key);

        String maxResolution = getResolutionLimit(context);
        if (maxResolution != null && (resolution.equals(context.getString(R.string.best_resolution_key)) || compareVideoStreamResolution(maxResolution, resolution) < 1)) {
            resolution = maxResolution;
        }
        return resolution;
    }

    /**
     * Return the index of the default stream in the list, based on the parameters
     * defaultResolution and defaultFormat
     *
     * @return index of the default resolution&format
     */
    static int getDefaultResolutionIndex(String defaultResolution, String bestResolutionKey, MediaFormat defaultFormat, List<VideoStream> videoStreams) {
        if (videoStreams == null || videoStreams.isEmpty())
            return -1;

        sortStreamList(videoStreams, false);
        if (defaultResolution.equals(bestResolutionKey)) {
            return 0;
        }

        int defaultStreamIndex = getVideoStreamIndex(defaultResolution, defaultFormat, videoStreams);

        // this is actually an error,
        // but maybe there is really no stream fitting to the default value.
        if (defaultStreamIndex == -1) {
            return 0;
        }
        return defaultStreamIndex;
    }

    /**
     * Join the two lists of video streams (video_only and normal videos), and sort them according with default format
     * chosen by the user
     *
     * @param defaultFormat         format to give preference
     * @param showHigherResolutions show >1080p resolutions
     * @param videoStreams          normal videos list
     * @param videoOnlyStreams      video only stream list
     * @param ascendingOrder        true -> smallest to greatest | false -> greatest to smallest    @return the sorted list
     * @return the sorted list
     */
    @NonNull
    static List<VideoStream> getSortedStreamVideosList(
            @Nullable final MediaFormat defaultFormat,
            final boolean showHigherResolutions,
            @Nullable final List<VideoStream> videoStreams,
            @Nullable final List<VideoStream> videoOnlyStreams,
            final boolean ascendingOrder,
            final boolean preferVideoOnlyStreams
    ) {
        // Determine order of streams
        // The last added list is preferred
        final List<List<VideoStream>> videoStreamsOrdered =
                preferVideoOnlyStreams
                        ? Arrays.asList(videoStreams, videoOnlyStreams)
                        : Arrays.asList(videoOnlyStreams, videoStreams);

        final List<VideoStream> allInitialStreams = videoStreamsOrdered.stream()
                // Ignore lists that are null
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                // Filter out higher resolutions (or not if high resolutions should always be shown)
                .filter(stream -> showHigherResolutions
                        || !HIGH_RESOLUTION_LIST.contains(stream.getResolution()
                        // Replace any frame rate with nothing
                        .replaceAll("p\\d+$", "p")))
                .collect(Collectors.toList());

        final HashMap<String, VideoStream> hashMap = new HashMap<>();
        // Add all to the hashmap
        for (final VideoStream videoStream : allInitialStreams) {
            hashMap.put(videoStream.getResolution(), videoStream);
        }

        // Override the values when the key == resolution, with the defaultFormat
        for (final VideoStream videoStream : allInitialStreams) {
            if (videoStream.getFormat() == defaultFormat) {
                hashMap.put(videoStream.getResolution(), videoStream);
            }
        }

        // Return the sorted list
        return sortStreamList(new ArrayList<>(hashMap.values()), ascendingOrder);
    }

    /**
     * Sort the streams list depending on the parameter ascendingOrder;
     * <p>
     * It works like that:<br>
     * - Take a string resolution, remove the letters, replace "0p60" (for 60fps videos) with "1"
     * and sort by the greatest:<br>
     * <blockquote><pre>
     *      720p     ->  720
     *      720p60   ->  721
     *      360p     ->  360
     *      1080p    ->  1080
     *      1080p60  ->  1081
     * <br>
     *  ascendingOrder  ? 360 < 720 < 721 < 1080 < 1081
     *  !ascendingOrder ? 1081 < 1080 < 721 < 720 < 360</pre></blockquote>
     *
     * @param videoStreams   list that the sorting will be applied
     * @param ascendingOrder true -> smallest to greatest | false -> greatest to smallest
     */
    private static List<VideoStream> sortStreamList(final List<VideoStream> videoStreams,
                                                    final boolean ascendingOrder) {
        // Compares the quality of two video streams.
        final Comparator<VideoStream> comparator = Comparator.nullsLast(Comparator
                .comparing(VideoStream::getResolution, ListHelper::compareVideoStreamResolution)
                .thenComparingInt(s -> VIDEO_FORMAT_QUALITY_RANKING.indexOf(s.getFormat())));
        videoStreams.sort(ascendingOrder ? comparator : comparator.reversed());
        return videoStreams;
    }

    /**
     * Get the audio from the list with the highest quality. Format will be ignored if it yields
     * no results.
     *
     * @param audioStreams list the audio streams
     * @return index of the audio with the highest average bitrate of the default format
     */
    static int getHighestQualityAudioIndex(MediaFormat format, List<AudioStream> audioStreams) {
        int result = -1;
        if (audioStreams != null) {
            while (result == -1) {
                AudioStream prevStream = null;
                for (int idx = 0; idx < audioStreams.size(); idx++) {
                    AudioStream stream = audioStreams.get(idx);
                    if ((format == null || stream.getFormat() == format) &&
                            (prevStream == null || compareAudioStreamBitrate(prevStream, stream, AUDIO_FORMAT_QUALITY_RANKING) < 0)) {
                        prevStream = stream;
                        result = idx;
                    }
                }
                if (result == -1 && format == null) {
                    break;
                }
                format = null;
            }
        }
        return result;
    }

    /**
     * Get the audio from the list with the lowest bitrate and efficient format. Format will be
     * ignored if it yields no results.
     *
     * @param format       The target format type or null if it doesn't matter
     * @param audioStreams list the audio streams
     * @return index of the audio stream that can produce the most compact results or -1 if not found.
     */
    static int getMostCompactAudioIndex(MediaFormat format, List<AudioStream> audioStreams) {
        int result = -1;
        if (audioStreams != null) {
            while (result == -1) {
                AudioStream prevStream = null;
                for (int idx = 0; idx < audioStreams.size(); idx++) {
                    AudioStream stream = audioStreams.get(idx);
                    if ((format == null || stream.getFormat() == format) &&
                            (prevStream == null || compareAudioStreamBitrate(prevStream, stream, AUDIO_FORMAT_EFFICIENCY_RANKING) > 0)) {
                        prevStream = stream;
                        result = idx;
                    }
                }
                if (result == -1 && format == null) {
                    break;
                }
                format = null;
            }
        }
        return result;
    }

    /**
     * Locates a possible match for the given resolution and format in the provided list.
     * In this order:
     * 1. Find a format and resolution match
     * 2. Find a format and resolution match and ignore the refresh
     * 3. Find a resolution match
     * 4. Find a resolution match and ignore the refresh
     * 5. Find a resolution just below the requested resolution and ignore the refresh
     * 6. Give up
     */
    static int getVideoStreamIndex(String targetResolution, MediaFormat targetFormat,
                                   List<VideoStream> videoStreams) {
        int fullMatchIndex = -1;
        int fullMatchNoRefreshIndex = -1;
        int resMatchOnlyIndex = -1;
        int resMatchOnlyNoRefreshIndex = -1;
        int lowerResMatchNoRefreshIndex = -1;
        String targetResolutionNoRefresh = targetResolution.replaceAll("p\\d+$", "p");

        for (int idx = 0; idx < videoStreams.size(); idx++) {
            MediaFormat format = targetFormat == null ? null : videoStreams.get(idx).getFormat();
            String resolution = videoStreams.get(idx).getResolution();
            String resolutionNoRefresh = resolution.replaceAll("p\\d+$", "p");

            if (format == targetFormat && resolution.equals(targetResolution)) {
                fullMatchIndex = idx;
            }

            if (format == targetFormat && resolutionNoRefresh.equals(targetResolutionNoRefresh)) {
                fullMatchNoRefreshIndex = idx;
            }

            if (resMatchOnlyIndex == -1 && resolution.equals(targetResolution)) {
                resMatchOnlyIndex = idx;
            }

            if (resMatchOnlyNoRefreshIndex == -1 && resolutionNoRefresh.equals(targetResolutionNoRefresh)) {
                resMatchOnlyNoRefreshIndex = idx;
            }

            if (lowerResMatchNoRefreshIndex == -1 && compareVideoStreamResolution(resolutionNoRefresh, targetResolutionNoRefresh) < 0) {
                lowerResMatchNoRefreshIndex = idx;
            }
        }

        if (fullMatchIndex != -1) {
            return fullMatchIndex;
        }
        if (fullMatchNoRefreshIndex != -1) {
            return fullMatchNoRefreshIndex;
        }
        if (resMatchOnlyIndex != -1) {
            return resMatchOnlyIndex;
        }
        if (resMatchOnlyNoRefreshIndex != -1) {
            return resMatchOnlyNoRefreshIndex;
        }
        return lowerResMatchNoRefreshIndex;
    }

    /**
     * Fetches the desired resolution or returns the default if it is not found. The resolution
     * will be reduced if video chocking is active.
     */
    private static int getDefaultResolutionWithDefaultFormat(Context context, String defaultResolution, List<VideoStream> videoStreams) {
        MediaFormat defaultFormat = getDefaultFormat(context, R.string.default_video_format_key, R.string.default_video_format_value);
        return getDefaultResolutionIndex(defaultResolution, context.getString(R.string.best_resolution_key), defaultFormat, videoStreams);
    }

    private static MediaFormat getDefaultFormat(Context context, @StringRes int defaultFormatKey, @StringRes int defaultFormatValueKey) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        String defaultFormat = context.getString(defaultFormatValueKey);
        String defaultFormatString = preferences.getString(context.getString(defaultFormatKey), defaultFormat);

        MediaFormat defaultMediaFormat = getMediaFormatFromKey(context, defaultFormatString);
        if (defaultMediaFormat == null) {
            preferences.edit().putString(context.getString(defaultFormatKey), defaultFormat).apply();
            defaultMediaFormat = getMediaFormatFromKey(context, defaultFormat);
        }

        return defaultMediaFormat;
    }

    private static MediaFormat getMediaFormatFromKey(Context context, String formatKey) {
        MediaFormat format = null;
        if (formatKey.equals(context.getString(R.string.video_webm_key))) {
            format = MediaFormat.WEBM;
        } else if (formatKey.equals(context.getString(R.string.video_mp4_key))) {
            format = MediaFormat.MPEG_4;
        } else if (formatKey.equals(context.getString(R.string.video_3gp_key))) {
            format = MediaFormat.v3GPP;
        } else if (formatKey.equals(context.getString(R.string.audio_webm_key))) {
            format = MediaFormat.WEBMA;
        } else if (formatKey.equals(context.getString(R.string.audio_m4a_key))) {
            format = MediaFormat.M4A;
        }
        return format;
    }

    // Compares the quality of two audio streams
    private static int compareAudioStreamBitrate(AudioStream streamA, AudioStream streamB, List<MediaFormat> formatRanking) {
        if (streamA == null) {
            return -1;
        }
        if (streamB == null) {
            return 1;
        }
        if (streamA.getAverageBitrate() < streamB.getAverageBitrate()) {
            return -1;
        }
        if (streamA.getAverageBitrate() > streamB.getAverageBitrate()) {
            return 1;
        }

        // Same bitrate and format
        return formatRanking.indexOf(streamA.getFormat()) - formatRanking.indexOf(streamB.getFormat());
    }

    private static int compareVideoStreamResolution(String r1, String r2) {
        int res1 = Integer.parseInt(r1.replaceAll("0p\\d+$", "1").replaceAll("[^\\d.]", ""));
        int res2 = Integer.parseInt(r2.replaceAll("0p\\d+$", "1").replaceAll("[^\\d.]", ""));
        return res1 - res2;
    }

    private static boolean isLimitingDataUsage(Context context) {
        return getResolutionLimit(context) != null;
    }

    /**
     * The maximum resolution allowed
     *
     * @param context App context
     * @return maximum resolution allowed or null if there is no maximum
     */
    private static String getResolutionLimit(Context context) {
        String resolutionLimit = null;
        if (!isWifiActive(context)) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            String defValue = context.getString(R.string.limit_data_usage_none_key);
            String value = preferences.getString(context.getString(R.string.limit_mobile_data_usage_key), defValue);
            resolutionLimit = value.equals(defValue) ? null : value;
        }
        return resolutionLimit;
    }

    /**
     * Are we connected to wifi?
     *
     * @param context App context
     * @return True if connected to wifi
     */
    private static boolean isWifiActive(Context context) {
        // true if online
        return Boolean.TRUE.equals(Optional.ofNullable(((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)))
                .map(ConnectivityManager::getActiveNetworkInfo)
                .map(networkInfo -> networkInfo.getType() == ConnectivityManager.TYPE_WIFI)
                .orElse(false));
    }

    @NonNull
    public static <S extends Stream> List<S> getNonTorrentStreams(final List<S> streamList) {
        return getFilteredStreamList(streamList, stream -> stream.getDeliveryMethod() != DeliveryMethod.TORRENT);
    }

    private static <S extends Stream> List<S> getFilteredStreamList(final List<S> streamList, final Predicate<S> streamListPredicate) {
        if (streamList == null) {
            return Collections.emptyList();
        }

        return streamList.stream().filter(streamListPredicate).collect(Collectors.toList());
    }

    @NonNull
    public static <S extends Stream> List<S> getUrlAndNonTorrentStreams(@Nullable final List<S> streamList) {
        return getFilteredStreamList(streamList, stream -> stream.isUrl() && stream.getDeliveryMethod() != DeliveryMethod.TORRENT);
    }

    @NonNull
    public static <S extends Stream> List<S> getPlayableStreams(@Nullable final List<S> streamList, final int serviceId) {
        final int youtubeServiceId = YouTube.getServiceId();
        return getFilteredStreamList(streamList,
                stream -> stream.getDeliveryMethod() != DeliveryMethod.TORRENT && (serviceId != youtubeServiceId
                        || stream.getItagItem() == null
                        || SUPPORTED_ITAG_IDS.contains(stream.getItagItem().id)));
    }
}
