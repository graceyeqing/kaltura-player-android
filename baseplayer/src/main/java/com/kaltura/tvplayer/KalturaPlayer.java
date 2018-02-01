package com.kaltura.tvplayer;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kaltura.netkit.connect.response.ResultElement;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.PKTrackConfig;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.ads.AdController;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsPlugin;
import com.kaltura.tvplayer.utils.GsonReader;
import com.kaltura.tvplayer.utils.TokenResolver;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import static com.kaltura.tvplayer.PlayerInitOptions.CONFIG;
import static com.kaltura.tvplayer.PlayerInitOptions.PLAYER;
import static com.kaltura.tvplayer.PlayerInitOptions.PLUGINS;


public abstract class KalturaPlayer <MOT extends MediaOptions> {

    private static final PKLog log = PKLog.get("KalturaPlayer");

    public static final String DEFAULT_OVP_SERVER_URL =
            BuildConfig.DEBUG ? "http://cdnapi.kaltura.com/" : "https://cdnapisec.kaltura.com/";


    protected String serverUrl;
    private String ks;
    private int partnerId;
    private final Integer uiConfId;
    protected final String referrer;
    private final Context context;
    protected Player pkPlayer;
    private static Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean autoPlay;
    private boolean preload;
    private double startPosition;
    private PKMediaFormat preferredMeidaFormat;
    private View view;
    private PKMediaEntry mediaEntry;
    private boolean prepared;
    private Resolver tokenResolver = new Resolver(mediaEntry);
    private PlayerInitOptions initOptions;

    protected KalturaPlayer(Context context, PlayerInitOptions initOptions) {

        this.context = context;

        this.preload = initOptions.preload != null ? initOptions.preload : false;
        this.autoPlay = initOptions.autoplay != null ? initOptions.autoplay : false;
        if (this.autoPlay) {
            this.preload = true; // autoplay implies preload
        }

        this.referrer = buildReferrer(context, initOptions.referrer);
        this.partnerId = initOptions.partnerId;
        this.uiConfId = initOptions.uiConfId;
        this.ks = initOptions.ks;

        registerPlugins(context);

        loadPlayer(initOptions);
    }

    protected static String safeServerUrl(String url, String defaultUrl) {
        return url == null ? defaultUrl :
                url.endsWith("/") ? url : url + "/";
    }

    private static String buildReferrer(Context context, String referrer) {
        if (referrer != null) {
            // If a referrer is given, it must be a valid URL.
            // Parse and check that scheme and authority are not empty.
            final Uri uri = Uri.parse(referrer);
            if (!TextUtils.isEmpty(uri.getScheme()) && !TextUtils.isEmpty(uri.getAuthority())) {
                return referrer;
            }
            // If referrer is not a valid URL, fall back to the generated default.
        }

        return new Uri.Builder().scheme("app").authority(context.getPackageName()).toString();
    }

    public void setPreferrdMediaFormat(PKMediaFormat preferedMediaFormat) {
        this.preferredMeidaFormat = preferedMediaFormat;
    }

    private static class Resolver implements TokenResolver {
        final Map<String, String> map = new HashMap<>();
        String[] sources;
        String[] destinations;

        Resolver(PKMediaEntry mediaEntry) {
            refresh(mediaEntry);
        }

        void refresh(PKMediaEntry mediaEntry) {
            // TODO: more tokens.
            if (mediaEntry != null) {
                map.put("{{entryId}}", mediaEntry.getId());
            }

            sources = new String[map.size()];
            destinations = new String[map.size()];

            final Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
            int i = 0;
            while (it.hasNext()) {
                final Map.Entry<String, String> entry = it.next();
                sources[i] = entry.getKey();
                destinations[i] = entry.getValue();
                i++;
            }
        }

        @Override
        public String resolve(String string) {
            if (string == null || sources.length == 0 || destinations.length == 0) {
                return string;
            }
            return TextUtils.replace(string, sources, destinations).toString();
        }
    }

    private JsonObject kavaDefaults(int partnerId, int uiConfId, String referrer) {
        JsonObject object = new JsonObject();
        object.addProperty("partnerId", partnerId);
        object.addProperty("uiConfId", uiConfId);
        object.addProperty("referrer", referrer);
        return object;
    }

    private JsonObject prepareKava(JsonObject uiConf, int partnerId, int uiConfId, String referrer) {
        return mergeJsonConfig(uiConf, kavaDefaults(partnerId, uiConfId, referrer));
    }

    private void loadPlayer(PlayerInitOptions initOptions) {
        this.initOptions = initOptions;
        // Assuming that at this point, all plugins are already registered.

        PKPluginConfigs combinedPluginConfigs = new PKPluginConfigs();


        PKPluginConfigs pluginConfigs = initOptions.pluginConfigs;
        GsonReader uiConf = GsonReader.withObject(initOptions.uiConf);

        //JsonObject providerUIConf = (uiConf != null && uiConf.getObject("config") != null && uiConf.getObject("config").getAsJsonObject("player") != null) ? uiConf.getObject("config").getAsJsonObject("player").getAsJsonObject("proivder") : null;

        //JsonObject playbackUIConf = (uiConf != null && uiConf.getObject("config") != null && uiConf.getObject("config").getAsJsonObject("player") != null) ? uiConf.getObject("config").getAsJsonObject("player").getAsJsonObject("playback") : null;

        JsonObject pluginsUIConf = (uiConf != null && uiConf.getObject(CONFIG) != null && uiConf.getObject(CONFIG).getAsJsonObject(PLAYER) != null) ? uiConf.getObject(CONFIG).getAsJsonObject(PLAYER).getAsJsonObject(PLUGINS) : new JsonObject();


        // Special case: Kaltura Analytics plugins

        // KAVA
        String name = KavaAnalyticsPlugin.factory.getName();
        if (initOptions.uiConfId == null) {
            JsonObject uic = mergeJsonConfig(GsonReader.getObject(pluginsUIConf, name), kavaDefaults(partnerId, initOptions.uiConfId, referrer));
            if (uic != null) {
                pluginsUIConf.add(name, uic);
            }
        }

        if (pluginConfigs != null) {
            if (pluginsUIConf != null) {
                for (Map.Entry<String, Object> entry : pluginConfigs) {
                    final String pluginName = entry.getKey();
                    final JsonObject config = (JsonObject) entry.getValue();
                    JsonObject uiConfObject = GsonReader.getObject(pluginsUIConf, pluginName);

                    JsonObject mergedConfig = mergeJsonConfig(config, uiConfObject);
                    if (mergedConfig == null) {
                        if (config instanceof JsonObject) {
                            mergedConfig = mergeJsonConfig(config, uiConfObject);
                        } else {
                            // no merge support
                            continue;
                        }
                    }
                    combinedPluginConfigs.setPluginConfig(pluginName, mergedConfig);
                }
            } else {
                for (Map.Entry<String, Object> entry : pluginConfigs) {
                    combinedPluginConfigs.setPluginConfig(entry.getKey(), entry.getValue());
                }
            }
        }

        // Add the plugins that are ONLY mentioned in UIConf
        if (pluginsUIConf != null) {
            for (Map.Entry<String, JsonElement> entry : pluginsUIConf.entrySet()) {
                String pluginName = entry.getKey();
                if (combinedPluginConfigs.hasConfig(pluginName)) {
                    // Already handled.
                    continue;
                }

                JsonElement entryValue = entry.getValue();
                if (!(entryValue instanceof JsonObject)) {
                    log.w("Ignoring invalid config format for plugin " + pluginName);
                    continue;
                }

                JsonObject jsonObject = (JsonObject) entryValue;
                JsonObject config = mergeJsonConfig(null, jsonObject);

                combinedPluginConfigs.setPluginConfig(pluginName, config);
            }
        }

        //addKalturaPluginConfigs(combined)
        replaceKeysInConfig(combinedPluginConfigs);

        pkPlayer = PlayKitManager.loadPlayer(context, combinedPluginConfigs);
        if (initOptions.audioLanguageMode != null && initOptions.audioLanguage != null) {
            pkPlayer.getSettings().setPreferredAudioTrack(new PKTrackConfig().setPreferredMode(initOptions.audioLanguageMode).setTrackLanguage(initOptions.audioLanguage));
        }
        if (initOptions.textLanguageMode != null && initOptions.textLanguage != null) {
            pkPlayer.getSettings().setPreferredTextTrack(new PKTrackConfig().setPreferredMode(initOptions.textLanguageMode).setTrackLanguage(initOptions.textLanguage));
        }
        PlayManifestRequestAdapter.install(pkPlayer, referrer);
    }

    private void replaceKeysInConfig(PKPluginConfigs combinedPluginConfigs) {
        Map<String,String> replacMap = new HashMap<>();
        //replacMap.put("{{entryId}}}", getMediaEntry().getId());

        //replacMap.put("{{entryName}}", getMediaEntry().getName());

        //replacMap.put("{{entryType}}", getMediaEntry().getMediaType().name());

        replacMap.put("{{ks}}", ks);

        replacMap.put("{{uiConfId}}", String.valueOf(uiConfId));

        replacMap.put("{{partnerId}}", String.valueOf(partnerId));

    }

    private JsonObject mergeJsonConfig(JsonObject original, JsonObject uiConf) {
        if (original == null && uiConf != null) {
            return uiConf;
        } else if (original != null && uiConf == null) {
            return original;
        } else if (original == null || uiConf == null) {
            return null;
        }

        JsonObject merged = original.deepCopy();

        for (Map.Entry<String, JsonElement> entry : uiConf.entrySet()) {
            if (!merged.has(entry.getKey())) {
                merged.add(entry.getKey(), entry.getValue().deepCopy());
            }
        }
        return null;
    }


    public View getView() {

        if (this.view != null) {
            return view;

        } else {
            FrameLayout view = new FrameLayout(context);
            view.setBackgroundColor(Color.BLACK);
            view.addView(pkPlayer.getView(), FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

            PlaybackControlsView controlsView = new PlaybackControlsView(context);

            final FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.BOTTOM | Gravity.START);
            view.addView(controlsView, layoutParams);

            controlsView.setPlayer(this);

            this.view = view;
        }
        return view;
    }

    public void setMedia(PKMediaEntry mediaEntry) {
        this.mediaEntry = mediaEntry;
        tokenResolver.refresh(mediaEntry);
        prepared = false;

        if (preload) {
            prepare();
        }
    }

    public void setMedia(PKMediaEntry entry, MediaOptions mediaOptions) {
        setStartPosition(mediaOptions.getStartPosition());
        setPreferrdMediaFormat(mediaOptions.getPreferredMediaFormat());
        setKS(mediaOptions.getKs());
        setMedia(entry);
    }

    protected void registerCommonPlugins(Context context) {
        KnownPlugin.registerAll(context);
    }

    public void setKS(String ks) {
        this.ks = ks;
        updateKS(ks);
    }

    public void prepare() {

        if (prepared) {
            return;
        }

        final PKMediaConfig config = new PKMediaConfig()
                .setMediaEntry(mediaEntry)
                .setStartPosition((long) (startPosition))
                .setPreferredMediaFormat(preferredMeidaFormat);

        pkPlayer.prepare(config);
        prepared = true;

        if (autoPlay) {
            pkPlayer.play();
        }
    }

    public PKMediaEntry getMediaEntry() {
        return mediaEntry;
    }

   public int getUiConfId() {
        return uiConfId;
    }

    // Player controls
    public void updatePluginConfig(@NonNull String pluginName, @Nullable Object pluginConfig) {
        pkPlayer.updatePluginConfig(pluginName, pluginConfig);
    }

    public void onApplicationPaused() {
        pkPlayer.onApplicationPaused();
    }

    public void onApplicationResumed() {
        pkPlayer.onApplicationResumed();
    }

    public void destroy() {
        pkPlayer.destroy();
    }

    public void stop() {
        pkPlayer.stop();
    }

    public void play() {
        if (!prepared) {
            prepare();
        }

        pkPlayer.play();
    }

    public void pause() {
        pkPlayer.pause();
    }

    public void replay() {
        pkPlayer.replay();
    }

    public long getCurrentPosition() {
        return pkPlayer.getCurrentPosition();
    }

    public long getDuration() {
        return pkPlayer.getDuration();
    }

    public long getBufferedPosition() {
        return pkPlayer.getBufferedPosition();
    }

    public void setVolume(float volume) {
        pkPlayer.setVolume(volume);
    }

    public boolean isPlaying() {
        return pkPlayer.isPlaying();
    }

    public void addEventListener(@NonNull PKEvent.Listener listener, Enum... events) {
        pkPlayer.addEventListener(listener, events);
    }

    public void addStateChangeListener(@NonNull PKEvent.Listener listener) {
        pkPlayer.addStateChangeListener(listener);
    }

    public void changeTrack(String uniqueId) {
        pkPlayer.changeTrack(uniqueId);
    }

    public void seekTo(long position) {
        pkPlayer.seekTo(position);
    }

    public AdController getAdController() {
        return pkPlayer.getAdController();
    }

    public String getSessionId() {
        return pkPlayer.getSessionId();
    }

    public double getStartPosition() {
        return startPosition;
    }

    public KalturaPlayer setStartPosition(double startPosition) {
        this.startPosition = startPosition;
        return this;
    }

    public boolean isPreload() {
        return preload;
    }

    public KalturaPlayer setPreload(boolean preload) {
        this.preload = preload;
        return this;
    }

    public boolean isAutoPlay() {
        return autoPlay;
    }

    public KalturaPlayer setAutoPlay(boolean autoPlay) {
        this.autoPlay = autoPlay;
        return this;
    }

    public int getPartnerId() {
        return partnerId;
    }

    public String getKS() {
        return ks;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    // Called by implementation of loadMedia().
    protected void mediaLoadCompleted(final ResultElement<PKMediaEntry> response, final OnEntryLoadListener onEntryLoadListener) {
        final PKMediaEntry entry = response.getResponse();

        mediaEntry = entry;

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                onEntryLoadListener.onEntryLoadComplete(entry, response.getError());
                if (entry != null) {
                    setMedia(entry);
                }
            }
        });
    }

    public abstract void loadMedia(MOT mediaOptions, OnEntryLoadListener listener);
    protected abstract void registerPlugins(Context context);
    protected abstract void addKalturaPluginConfigs(PKPluginConfigs combined);
    protected abstract void updateKS(String ks);

    public interface OnEntryLoadListener {
        void onEntryLoadComplete(PKMediaEntry entry, ErrorElement error);
    }
}
