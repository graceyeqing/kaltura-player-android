package com.kaltura.tvplayer.ott;

import android.content.Context;
import android.support.annotation.NonNull;

import com.kaltura.netkit.connect.response.ResultElement;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.mediaproviders.base.OnMediaLoadCompletion;
import com.kaltura.playkit.mediaproviders.ott.PhoenixMediaProvider;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsConfig;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsPlugin;
import com.kaltura.playkit.plugins.ott.PhoenixAnalyticsConfig;
import com.kaltura.playkit.plugins.ott.PhoenixAnalyticsPlugin;
import com.kaltura.playkit.plugins.ovp.KalturaLiveStatsConfig;
import com.kaltura.playkit.plugins.ovp.KalturaStatsConfig;
import com.kaltura.tvplayer.KalturaPlayer;
import com.kaltura.tvplayer.PlayerInitOptions;

public class KalturaOTTPlayer extends KalturaPlayer<OTTMediaOptions> {

    private static final PKLog log = PKLog.get("KalturaOTTPlayer");
    private static boolean pluginsRegistered;

    public static KalturaOTTPlayer create(final Context context, PlayerInitOptions options) {
        return new KalturaOTTPlayer(context, options);
    }

    private KalturaOTTPlayer(Context context, PlayerInitOptions initOptions) {
        super(context, initOptions);

        this.serverUrl = KalturaPlayer.safeServerUrl(initOptions.serverUrl, null);
    }

    @Override
    protected void registerPlugins(Context context) {
        if (!pluginsRegistered) {
            registerCommonPlugins(context);

            PlayKitManager.registerPlugins(context, PhoenixAnalyticsPlugin.factory);

            pluginsRegistered = true;
        }
    }

    @Override
    protected void updateKS(String ks) {
        // update plugins and provider
        pkPlayer.updatePluginConfig(PhoenixAnalyticsPlugin.factory.getName(), getPhoenixAnalyticsConfig());
        pkPlayer.updatePluginConfig(KavaAnalyticsPlugin.factory.getName(), getKavaAnalyticsConfig());
    }

    @Override
    protected void addKalturaPluginConfigs(PKPluginConfigs combined) {
        combined.setPluginConfig(PhoenixAnalyticsPlugin.factory.getName(), getPhoenixAnalyticsConfig());
        combined.setPluginConfig(KavaAnalyticsPlugin.factory.getName(), getKavaAnalyticsConfig());
    }

    private KavaAnalyticsConfig getKavaAnalyticsConfig() {
        return new KavaAnalyticsConfig().setPartnerId(getPartnerId()).setReferrer(referrer);
    }

    private KalturaLiveStatsConfig getLiveStatsConfig() {
        final PKMediaEntry mediaEntry = getMediaEntry();
        return new KalturaLiveStatsConfig(getPartnerId(), mediaEntry != null ? mediaEntry.getId() : null);
    }

    private KalturaStatsConfig getStatsConfig() {
        final PKMediaEntry mediaEntry = getMediaEntry();
        return new KalturaStatsConfig(getUiConfId(), getPartnerId(), mediaEntry != null ? mediaEntry.getId() : null, null, 0, true);
    }

    @NonNull
    private PhoenixAnalyticsConfig getPhoenixAnalyticsConfig() {
        return new PhoenixAnalyticsConfig(getPartnerId(), getServerUrl(), getKS(), 30);
    }


    @Override
    public void loadMedia(OTTMediaOptions mediaOptions, final OnEntryLoadListener listener) {

        if (mediaOptions.getKs() != null) {
            setKS(mediaOptions.getKs());
        }
        setPreferrdMediaFormat(mediaOptions.getPreferredMediaFormat());
        setStartPosition(mediaOptions.getStartPosition());

        final PhoenixMediaProvider provider = new PhoenixMediaProvider(getServerUrl(), getPartnerId(), getKS())
                .setAssetId(mediaOptions.assetId);


        if (mediaOptions.fileIds != null) {
            provider.setFileIds(mediaOptions.fileIds);
        }

        if (mediaOptions.contextType != null) {
            provider.setContextType(mediaOptions.contextType);
        }

        if (mediaOptions.assetType != null) {
            provider.setAssetType(mediaOptions.assetType);
        }

        if (mediaOptions.formats != null) {
            provider.setFormats(mediaOptions.formats);
        }

        provider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                mediaLoadCompleted(response, listener);
            }
        });
    }
}
