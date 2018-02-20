package com.kaltura.offlinemanager;

import android.content.Context;

import com.kaltura.dtg.DownloadState;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.tvplayer.MediaOptions;

import java.util.List;
import java.util.Map;

@SuppressWarnings({"WeakerAccess", "unused", "JavaDoc"})
public abstract class OfflineManager {

    public static OfflineManager getInstance() {
        return new OfflineManagerImp();
    }

    /**
     * Start the download service. Essential for using all other methods.
     * @param appContext
     * @param listener
     */
    public abstract void startService(Context appContext, OnServiceStart listener);

    public abstract void stopService();

    /**
     * Set the global download state listener, to be notified about state changes.
     * @param listener
     */
    public abstract void setAssetInfoUpdateListener(AssetInfoUpdateListener listener);

    /**
     * Set the global download progress listener
     * @param listener
     */
    public abstract void setDownloadProgressListener(DownloadProgressListener listener);

    /**
     * Set the global DRM license update listener.
     * @param listener
     * @see DrmLicenseUpdateListener
     */
    public abstract void setDrmLicenseUpdateListener(DrmLicenseUpdateListener listener);

    /**
     * Temporarily pause all downloads. Doesn't change assets' download state. Revert with {@link #resumeDownloads()}.
     */
    public abstract void pauseDownloads();

    /**
     * Resume downloading assets. Should be called in two places:
     * - After calling {@link #startService(Context, OnServiceStart)}, to resume the downloads that
     * were in progress in the previous session
     * - After calling {@link #pauseDownloads()}, to resume the paused downloads.
     */
    public abstract void resumeDownloads();

    /**
     * Find asset by id.
     * @param assetId
     * @return asset info or null if not found.
     */
    public abstract AssetInfo getAssetInfo(String assetId);

    /**
     * Get list of {@link AssetInfo} objects for all assets in the given state.
     * @param state
     * @return
     */
    public abstract List<AssetInfo> getAssetsByState(DownloadState state);

    /**
     * Get an asset's PKMediaEntry object, as stored by {@link #addAsset(PKMediaEntry)} or
     * {@link #addAsset(int, String, String, MediaOptions)}.
     * @param assetId
     * @return
     */
    public abstract PKMediaEntry getOriginalMediaEntry(String assetId);

    /**
     * Get an offline-playable PKMediaEntry object.
     * @param assetId
     * @return
     */
    public abstract PKMediaEntry getLocalPlaybackEntry(String assetId);

    /**
     * Add a new asset with information from mediaEntry. All relevant metadata from mediaEntry is
     * stored, but only for the selection download source.
     * @param mediaEntry
     * @return true if the asset was added, false otherwise. Note: returns false if asset already exists.
     */
    public abstract boolean addAsset(PKMediaEntry mediaEntry);

    /**
     * Add a new asset by connecting to the backend with the provided details.
     * @param partnerId
     * @param ks
     * @param serverUrl
     * @param mediaOptions
     * @return true if the asset was added, false otherwise. Note: returns false if asset already exists.
     */
    public abstract boolean addAsset(int partnerId, String ks, String serverUrl, MediaOptions mediaOptions);

    /**
     * Load asset's metadata (tracks, size, etc).
     * @param assetId
     * @param trackSelectionListener
     * @param assetInfoUpdateListener
     * @return false if asset is not found.
     */
    public abstract boolean loadAssetDownloadInfo(String assetId, MediaPrefs selection,
                                                  TrackSelectionListener trackSelectionListener,
                                                  AssetInfoUpdateListener assetInfoUpdateListener);


    /**
     * Start (or resume) downloading an asset.
     * @param assetId
     * @return false if asset is not found, true otherwise.
     */
    public abstract boolean startAsset(String assetId);

    /**
     * Pause downloading an asset.
     * @param assetId
     * @return false if asset is not found, true otherwise.
     */
    public abstract boolean pauseAsset(String assetId);

    /**
     * Remove asset with all its data.
     * @param assetId
     * @return false if asset is not found, true otherwise.
     */
    public abstract boolean removeAsset(String assetId);

    /**
     * Check the license status of an asset.
     * @param assetId
     * @return DRM license status - {@link AssetDrmInfo}.
     */
    public abstract AssetDrmInfo getDrmStatus(String assetId);

    /**
     * Renew an asset's license. The result is passed asynchronously to the global {@link DrmLicenseUpdateListener}.
     * @param assetId
     * @return false if asset is not found, true otherwise.
     */
    public abstract boolean renewAssetDrmLicense(String assetId);

    public interface OnServiceStart {
        void onServiceStart();
    }

    /**
     * Invoked during asset info loading ({@link #loadAssetDownloadInfo(String, MediaPrefs, TrackSelectionListener, AssetInfoUpdateListener)}).
     * Allows the app to inspect and change the track selection. If app returns non-null, it overrides the automatic selection.
     * @see {@link MediaPrefs} for higher-level track selection customization.
     */
    public interface TrackSelectionListener {
        Map<TrackType, List<Track>> onTracksAvailable(String assetId, Map<TrackType, List<Track>> available,
                               Map<TrackType, List<Track>> selected);
    }

    /**
     * Invoked during asset info loading ({@link #loadAssetDownloadInfo(String, MediaPrefs, TrackSelectionListener, AssetInfoUpdateListener)}).
     */
    public interface AssetInfoUpdateListener {
        void onAssetInfoUpdated(String assetId, AssetInfo assetInfo);
    }

    /**
     * Invoked while downloading an asset; use with {@link #setDownloadProgressListener(DownloadProgressListener)}.
     */
    public interface DownloadProgressListener {
        void onDownloadProgress(String assetId, long downloadedBytes, long totalEstimatedBytes);
    }

    /**
     * Global listener for DRM actions. Use with {@link #setDrmLicenseUpdateListener(DrmLicenseUpdateListener)}.
     */
    public interface DrmLicenseUpdateListener {
        void onLicenceInstall(String assetId, int totalTime, int timeToRenew);
        void onLicenseRenew(String assetId, int totalTime, int timeToRenew);
        void onLicenseRemove(String assetId);
    }

    public static class AssetDrmInfo {
        public Status status;
        public int totalRemainingTime;
        public int currentRemainingTime;

        public enum Status {
            valid, unknown, expired, clear
        }
    }

    public static class Track {
        TrackType type;
        String language;
        CodecType codec;
        long bitrate;
        int width;
        int height;
    }

    public static class AssetInfo {
        String id;
        AssetDownloadState state;
        long estimatedSize;
        long downloadedSize;
    }

    public enum AssetDownloadState {
        added, metadataLoaded, started, paused, completed, failed
    }

    public enum TrackType {
        video, audio, text
    }

    public enum CodecType {
        avc, hevc, vp9
    }

    /**
     * Pre-download media preferences. Used with {@link #loadAssetDownloadInfo(String, MediaPrefs, TrackSelectionListener, AssetInfoUpdateListener)}.
     */
    public static class MediaPrefs {
        public Long preferredVideoBitrate;
        public Long preferredVideoHeight;
        public Long preferredVideoWidth;

        public List<String> preferredAudioLanguages;
        public List<String> preferredTextLanguages;

        public MediaPrefs setPreferredVideoBitrate(Long preferredVideoBitrate) {
            this.preferredVideoBitrate = preferredVideoBitrate;
            return this;
        }

        public MediaPrefs setPreferredVideoHeight(Long preferredVideoHeight) {
            this.preferredVideoHeight = preferredVideoHeight;
            return this;
        }

        public MediaPrefs setPreferredVideoWidth(Long preferredVideoWidth) {
            this.preferredVideoWidth = preferredVideoWidth;
            return this;
        }

        public MediaPrefs setPreferredAudioLanguages(List<String> preferredAudioLanguages) {
            this.preferredAudioLanguages = preferredAudioLanguages;
            return this;
        }

        public MediaPrefs setPreferredTextLanguages(List<String> preferredTextLanguages) {
            this.preferredTextLanguages = preferredTextLanguages;
            return this;
        }
    }
}