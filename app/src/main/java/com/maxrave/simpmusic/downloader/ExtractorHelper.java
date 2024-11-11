package com.maxrave.simpmusic.downloader;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.stream.StreamInfo;

import io.reactivex.Single;

public class ExtractorHelper {

    public static Single<StreamInfo> getStreamInfo(final int serviceId, final String url) {
       return Single.fromCallable(() -> StreamInfo.getInfo(NewPipe.getService(serviceId), url));
    }

    public static boolean hasAssignableCauseThrowable(Throwable throwable, Class<?>... causesToCheck) {

        // Check if getCause is not the same as cause (the getCause is already the root),
        // as it will cause a infinite loop if it is
        Throwable cause, getCause = throwable;

        // Check if throwable is a subclass of any of the filtered classes
        final Class<? extends Throwable> throwableClass = throwable.getClass();
        for (Class<?> causesEl : causesToCheck) {
            if (causesEl.isAssignableFrom(throwableClass)) {
                return true;
            }
        }

        // Iteratively checks if the root cause of the throwable is a subclass of the filtered class
        while ((cause = throwable.getCause()) != null && getCause != cause) {

            getCause = cause;

            final Class<? extends Throwable> causeClass = cause.getClass();
            for (Class<?> causesEl : causesToCheck) {
                if (causesEl.isAssignableFrom(causeClass)) {
                    return true;
                }
            }
        }
        return false;
    }
}
