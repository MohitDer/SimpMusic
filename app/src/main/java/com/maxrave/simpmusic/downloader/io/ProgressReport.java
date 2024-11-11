package com.maxrave.simpmusic.downloader.io;

public interface ProgressReport {

    /**
     * Report the size of the new file
     *
     * @param progress the new size
     */
    void report(long progress);
}