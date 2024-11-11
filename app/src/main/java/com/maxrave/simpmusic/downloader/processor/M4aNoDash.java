package com.maxrave.simpmusic.downloader.processor;

import java.io.IOException;

import com.maxrave.simpmusic.downloader.streams.Mp4DashReader;
import com.maxrave.simpmusic.downloader.streams.Mp4FromDashWriter;
import com.maxrave.simpmusic.downloader.streams.io.SharpStream;

class M4aNoDash extends PostProcessing {

    M4aNoDash() {
        super(false, true, ALGORITHM_M4A_NO_DASH);
    }

    @Override
    boolean test(SharpStream... sources) throws IOException {
        // check if the mp4 file is DASH (youtube)

        Mp4DashReader reader = new Mp4DashReader(sources[0]);
        reader.parse();

        switch (reader.getBrands()[0]) {
            case 0x64617368:// DASH
            case 0x69736F35:// ISO5
                return true;
            default:
                return false;
        }
    }

    @Override
    int process(SharpStream out, SharpStream... sources) throws IOException {
        Mp4FromDashWriter muxer = new Mp4FromDashWriter(sources[0]);
        muxer.setMainBrand(0x4D344120);// binary string "M4A "
        muxer.parseSources();
        muxer.selectTracks(0);
        muxer.build(out);

        return OK_RESULT;
    }
}
