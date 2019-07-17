package com.mopub.mobileads;

import android.app.Activity;
import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.BaseLifecycleListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.common.logging.MoPubLog;
import com.verizon.ads.ActivityStateManager;
import com.verizon.ads.CreativeInfo;
import com.verizon.ads.ErrorInfo;
import com.verizon.ads.RequestMetadata;
import com.verizon.ads.VASAds;
import com.verizon.ads.edition.StandardEdition;
import com.verizon.ads.interstitialplacement.InterstitialAd;
import com.verizon.ads.interstitialplacement.InterstitialAdFactory;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.DID_DISAPPEAR;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.WILL_LEAVE_APPLICATION;
import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
import static com.mopub.mobileads.VerizonUtils.convertErrorInfoToMoPub;


public class VerizonRewardedVideo extends CustomEventRewardedVideo {

    private static final String ADAPTER_NAME = VerizonRewardedVideo.class.getSimpleName();
    private static final LifecycleListener lifecycleListener = new VerizonLifecycleListener();

    private static final String PLACEMENT_ID_KEY = "placementId";
    private static final String SITE_ID_KEY = "siteId";
    private static final String VIDEO_COMPLETE_EVENT_ID = "onVideoComplete";

    private InterstitialAd verizonInterstitialAd;
    private Activity activity;
    private String placementId = null;
    private Class currentClass;
    private boolean rewarded = false;

    @NonNull
    private VerizonAdapterConfiguration verizonAdapterConfiguration;


    public VerizonRewardedVideo() {

        currentClass = this.getClass();
        verizonAdapterConfiguration = new VerizonAdapterConfiguration();
    }


    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {

        return lifecycleListener;
    }


    @NonNull
    @Override
    protected String getAdNetworkId() {

        return (placementId == null) ? "" : placementId;
    }


    @Override
    protected void onInvalidate() {

        if (verizonInterstitialAd != null) {
            verizonInterstitialAd.destroy();
            verizonInterstitialAd = null;
        }
    }


    private static final class VerizonLifecycleListener extends BaseLifecycleListener {
        @Override
        public void onCreate(@NonNull final Activity activity) {
            super.onCreate(activity);
        }

        @Override
        public void onResume(@NonNull final Activity activity) {
            super.onResume(activity);
        }
    }


    protected String getPlacementIdKey() {

        return PLACEMENT_ID_KEY;
    }


    protected String getSiteIdKey() {

        return SITE_ID_KEY;
    }


    @Override
    protected boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity,
        @NonNull final Map<String, Object> localExtras, @NonNull final Map<String, String> serverExtras) {

        if (serverExtras == null || serverExtras.isEmpty()) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Ad request to Verizon failed because " +
                "serverExtras is null or empty");

            return false;
        }

        // Cache serverExtras so siteId can be used to initalizate VAS early at next launch
        verizonAdapterConfiguration.setCachedInitializationParameters(launcherActivity, serverExtras);

        String siteId = serverExtras.get(getSiteIdKey());

        if (!VASAds.isInitialized()) {
            Application application = null;

            if (launcherActivity != null) {
                application = launcherActivity.getApplication();
            }

            if (application == null || !StandardEdition.initialize(application, siteId)) {
                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, ADAPTER_CONFIGURATION_ERROR.getIntCode(), ADAPTER_CONFIGURATION_ERROR);

                return false;
            }
        }

        // The current activity must be set as resumed so VAS can track ad visibility
        ActivityStateManager activityStateManager = VASAds.getActivityStateManager();
        if (activityStateManager != null) {
            activityStateManager.setState(launcherActivity, ActivityStateManager.ActivityState.RESUMED);
        }

        return true;
    }


    @Override
    protected void loadWithSdkInitialized(@NonNull final Activity activity, @NonNull final Map<String, Object> localExtras,
        @NonNull final Map<String, String> serverExtras) {

        this.activity = activity;
        placementId = serverExtras.get(getPlacementIdKey());

        if (TextUtils.isEmpty(placementId)) {
            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Invalid extras-- Be sure you have a placement ID specified.");
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(currentClass, "",
                MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            return;
        }

        VASAds.setLocationEnabled(MoPub.getLocationAwareness() != MoPub.LocationAwareness.DISABLED);

        final InterstitialAdFactory interstitialAdFactory = new InterstitialAdFactory(activity, placementId,
            new VerizonInterstitialFactoryListener());

        final RequestMetadata requestMetadata = new RequestMetadata.Builder().setMediator(VerizonAdapterConfiguration.MEDIATOR_ID)
            .build();
        interstitialAdFactory.setRequestMetaData(requestMetadata);

        interstitialAdFactory.load(new VerizonInterstitialListener());
    }


    @Override
    protected boolean hasVideoAvailable() {
        // forwarding deprecated method to its replacement
        return isReady();
    }


    @Override
    protected void showVideo() {
        // forwarding deprecated method to its replacement
        show();
    }


    @Override
    protected boolean isReady() {

        return verizonInterstitialAd != null;
    }


    @Override
    protected void show() {

        VerizonUtils.postOnUiThread(new Runnable() {

            @Override
            public void run() {

                if (verizonInterstitialAd != null) {
                    verizonInterstitialAd.show(activity);
                } else {
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "show called before Verizon rewarded video ad was loaded.");
                }
            }
        });
    }


    private class VerizonInterstitialFactoryListener implements InterstitialAdFactory.InterstitialAdFactoryListener {


        @Override
        public void onLoaded(final InterstitialAdFactory interstitialAdFactory, final InterstitialAd interstitialAd) {

            verizonInterstitialAd = interstitialAd;

            MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);

            VerizonUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {

                    final CreativeInfo creativeInfo = verizonInterstitialAd == null ? null : verizonInterstitialAd.getCreativeInfo();
                    MoPubLog.log(CUSTOM, ADAPTER_NAME, "Verizon creative info: " + creativeInfo);
                }
            });

            MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(currentClass, placementId);
        }


        @Override
        public void onCacheLoaded(final InterstitialAdFactory interstitialAdFactory,
            final int numRequested, final int numReceived) {

        }


        @Override
        public void onCacheUpdated(final InterstitialAdFactory interstitialAdFactory, final int cacheSize) {

        }


        @Override
        public void onError(final InterstitialAdFactory interstitialAdFactory, final ErrorInfo errorInfo) {

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Failed to load Verizon rewarded video due to " +
                "error: " + errorInfo.toString());

            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(currentClass, placementId,
                convertErrorInfoToMoPub(errorInfo));
        }
    }


    private class VerizonInterstitialListener implements InterstitialAd.InterstitialAdListener {


        @Override
        public void onError(final InterstitialAd interstitialAd, final ErrorInfo errorInfo) {

            MoPubLog.log(CUSTOM, ADAPTER_NAME, "Failed to show Verizon rewarded video due to " +
                "error: " + errorInfo.toString());

            MoPubRewardedVideoManager.onRewardedVideoPlaybackError(currentClass, placementId,
                MoPubErrorCode.INTERNAL_ERROR);
        }


        @Override
        public void onShown(final InterstitialAd interstitialAd) {

            MoPubLog.log(SHOW_SUCCESS, ADAPTER_NAME);
            MoPubRewardedVideoManager.onRewardedVideoStarted(currentClass, placementId);
        }


        @Override
        public void onClosed(final InterstitialAd interstitialAd) {

            MoPubLog.log(DID_DISAPPEAR, ADAPTER_NAME);
            MoPubRewardedVideoManager.onRewardedVideoClosed(currentClass, placementId);
        }


        @Override
        public void onClicked(final InterstitialAd interstitialAd) {

            MoPubLog.log(CLICKED, ADAPTER_NAME);
            MoPubRewardedVideoManager.onRewardedVideoClicked(currentClass, placementId);
        }


        @Override
        public void onAdLeftApplication(final InterstitialAd interstitialAd) {
            // Only logging this event. No need to call interstitialListener.onLeaveApplication()
            // because it's an alias for interstitialListener.onInterstitialClicked()
            MoPubLog.log(WILL_LEAVE_APPLICATION, ADAPTER_NAME);
        }


        @Override
        public void onEvent(final InterstitialAd interstitialAd, final String source,
            final String eventId, final Map<String, Object> arguments) {

            if (!rewarded && VIDEO_COMPLETE_EVENT_ID.equals(eventId)) {

                rewarded = true;

                MoPubRewardedVideoManager.onRewardedVideoCompleted(currentClass, placementId,
                    MoPubReward.success(MoPubReward.NO_REWARD_LABEL, MoPubReward.DEFAULT_REWARD_AMOUNT));
            }
        }
    }
}
