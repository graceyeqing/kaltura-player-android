package com.kaltura.tvplayer;


import com.kaltura.playkit.PKMediaFormat;

public class MediaOptions {
    private String ks;
    private double startPosition;
    private PKMediaFormat preferredMediaFormat;

    public MediaOptions setKS(String ks) {
        this.ks = ks;
        return this;
    }

    public MediaOptions setStartPosition(double startPosition) {
        this.startPosition = startPosition;
        return this;
    }

    public MediaOptions setPreferredMediaFormat(String preferredMediaFormat) {
        if (preferredMediaFormat != null) {
            this.preferredMediaFormat = PKMediaFormat.valueOf(preferredMediaFormat);
        }
        return this;
    }

    public String getKs() {
        return ks;
    }

    public double getStartPosition() {
        return startPosition;
    }

    public PKMediaFormat getPreferredMediaFormat() {
        return preferredMediaFormat;
    }
}

