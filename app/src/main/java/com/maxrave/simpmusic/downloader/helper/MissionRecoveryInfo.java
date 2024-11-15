package com.maxrave.simpmusic.downloader.helper;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.Stream;
import org.schabi.newpipe.extractor.stream.SubtitlesStream;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.io.Serializable;

public class MissionRecoveryInfo implements Serializable, Parcelable {
    private static final long serialVersionUID = 0L;

    MediaFormat format;
    String desired;
    boolean desired2;
    int desiredBitrate;
    byte kind;
    String validateCondition = null;

    public MissionRecoveryInfo(@NonNull Stream stream) {
        if (stream instanceof AudioStream) {
            desiredBitrate = ((AudioStream) stream).getAverageBitrate();
            desired2 = false;
            kind = 'a';
        } else if (stream instanceof VideoStream) {
            desired = ((VideoStream) stream).getResolution();
            desired2 = ((VideoStream) stream).isVideoOnly();
            kind = 'v';
        } else if (stream instanceof SubtitlesStream) {
            desired = ((SubtitlesStream) stream).getLanguageTag();
            desired2 = ((SubtitlesStream) stream).isAutoGenerated();
            kind = 's';
        } else {
            throw new RuntimeException("Unknown stream kind");
        }

        format = stream.getFormat();
        if (format == null) throw new NullPointerException("Stream format cannot be null");
    }

    @NonNull
    @Override
    public String toString() {
        String info;
        StringBuilder str = new StringBuilder();
        str.append("{type=");
        switch (kind) {
            case 'a':
                str.append("audio");
                info = "bitrate=" + desiredBitrate;
                break;
            case 'v':
                str.append("video");
                info = "quality=" + desired + " videoOnly=" + desired2;
                break;
            case 's':
                str.append("subtitles");
                info = "language=" + desired + " autoGenerated=" + desired2;
                break;
            default:
                info = "";
                str.append("other");
        }

        str.append(" format=")
                .append(format.getName())
                .append(' ')
                .append(info)
                .append('}');

        return str.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(this.format.ordinal());
        parcel.writeString(this.desired);
        parcel.writeInt(this.desired2 ? 0x01 : 0x00);
        parcel.writeInt(this.desiredBitrate);
        parcel.writeByte(this.kind);
        parcel.writeString(this.validateCondition);
    }

    private MissionRecoveryInfo(Parcel parcel) {
        this.format = MediaFormat.values()[parcel.readInt()];
        this.desired = parcel.readString();
        this.desired2 = parcel.readInt() != 0x00;
        this.desiredBitrate = parcel.readInt();
        this.kind = parcel.readByte();
        this.validateCondition = parcel.readString();
    }

    public static final Creator<MissionRecoveryInfo> CREATOR = new Creator<MissionRecoveryInfo>() {
        @Override
        public MissionRecoveryInfo createFromParcel(Parcel source) {
            return new MissionRecoveryInfo(source);
        }

        @Override
        public MissionRecoveryInfo[] newArray(int size) {
            return new MissionRecoveryInfo[size];
        }
    };
}
