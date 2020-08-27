package com.mopub.mobileads;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPub;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.verizon.ads.ActivityStateManager;
import com.verizon.ads.Bid;
import com.verizon.ads.BidRequestListener;
import com.verizon.ads.CreativeInfo;
import com.verizon.ads.ErrorInfo;
import com.verizon.ads.RequestMetadata;
import com.verizon.ads.VASAds;
import com.verizon.ads.edition.StandardEdition;
import com.verizon.ads.interstitialplacement.InterstitialAd;
import com.verizon.ads.interstitialplacement.InterstitialAdFactory;

import java.util.HashMap;
import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.DID_DISAPPEAR;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.WILL_LEAVE_APPLICATION;
import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.INTERNAL_ERROR;
import static com.mopub.mobileads.VerizonAdapterConfiguration.convertErrorInfoToMoPub;

public class VerizonInterstitial extends BaseAd {

    private static final String ADAPTER_NAME = VerizonInterstitial.class.getSimpleName();

    private static final String PLACEMENT_ID_KEY = "placementId";
    private static final String SITE_ID_KEY = "siteId";
    private static final String AD_IMPRESSION_EVENT_ID = "adImpression";

    private Context context;
    private InterstitialAd verizonInterstitialAd;
    private String mPlacementId;

    @NonNull
    private VerizonAdapterConfiguration verizonAdapterConfiguration;

    public VerizonInterstitial() {
        verizonAdapterConfiguration = new VerizonAdapterConfiguration();
    }

    @Override
    protected void load(@NonNull final Context context, @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        setAutomaticImpressionAndClickTracking(false);

        this.context = context;

        final Map<String, String> extras = adData.getExtras();
        if (extras.isEmpty()) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Ad request to Verizon " +
                    "failed because server data is null or empty");

            logAndNotifyInterstitialFailed(LOAD_FAILED, ADAPTER_CONFIGURATION_ERROR, true);

            return;
        }

        // Cache server data so siteId can be used to initalizate VAS early at next launch
        verizonAdapterConfiguration.setCachedInitializationParameters(context, extras);

        final String siteId = extras.get(getSiteIdKey());
        mPlacementId = extras.get(getPlacementIdKey());

        if (!VASAds.isInitialized()) {
            Application application = null;

            if (context instanceof Application) {
                application = (Application) context;
            } else if (context instanceof Activity) {
                application = ((Activity) context).getApplication();
            }


            if (!StandardEdition.initialize(application, siteId)) {
                logAndNotifyInterstitialFailed(LOAD_FAILED, ADAPTER_CONFIGURATION_ERROR, true);
            }
        }

        // The current activity must be set as resumed so VAS can track ad visibility
        final ActivityStateManager activityStateManager = VASAds.getActivityStateManager();
        if (activityStateManager != null && context instanceof Activity) {
            activityStateManager.setState((Activity) context, ActivityStateManager.ActivityState.RESUMED);
        }

        if (TextUtils.isEmpty(mPlacementId)) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Ad request to Verizon " +
                    "failed because placement ID is empty");

            logAndNotifyInterstitialFailed(LOAD_FAILED, ADAPTER_CONFIGURATION_ERROR, true);

            return;
        }

        VASAds.setLocationEnabled(MoPub.getLocationAwareness() != MoPub.LocationAwareness.DISABLED);

        final InterstitialAdFactory interstitialAdFactory = new InterstitialAdFactory(context, mPlacementId,
                new VerizonInterstitialFactoryListener());

        final Bid bid = BidCache.get(mPlacementId);

        if (bid == null) {
            final RequestMetadata.Builder requestMetadataBuilder = new RequestMetadata.Builder
                    (VASAds.getRequestMetadata());
            requestMetadataBuilder.setMediator(VerizonAdapterConfiguration.MEDIATOR_ID);

            final String adContent = extras.get(VerizonAdapterConfiguration.SERVER_EXTRAS_AD_CONTENT_KEY);

            if (!TextUtils.isEmpty(adContent)) {
                final Map<String, Object> placementData = new HashMap<>();

                placementData.put(VerizonAdapterConfiguration.REQUEST_METADATA_AD_CONTENT_KEY, adContent);
                placementData.put("overrideWaterfallProvider", "waterfallprovider/sideloading");

                requestMetadataBuilder.setPlacementData(placementData);
            }

            interstitialAdFactory.setRequestMetaData(requestMetadataBuilder.build());
            interstitialAdFactory.load(new VerizonInterstitialListener());
        } else {
            interstitialAdFactory.load(bid, new VerizonInterstitialListener());
        }
    }

    /**
     * Call this method to cache a super auction bid for the specified placement ID
     *
     * @param context            a non-null Context
     * @param placementId        a valid placement ID. Cannot be null or empty.
     * @param requestMetadata    a {@link RequestMetadata} instance for the request or null
     * @param bidRequestListener an instance of {@link BidRequestListener}. Cannot be null.
     */
    public void requestBid(final Context context, final String placementId, final RequestMetadata requestMetadata,
                           final BidRequestListener bidRequestListener) {

        Preconditions.checkNotNull(context, "Super auction bid skipped because context " +
                "is null");
        Preconditions.checkNotNull(placementId, "Super auction bid skipped because the " +
                "placement ID is null");
        Preconditions.checkNotNull(bidRequestListener, "Super auction bid skipped because " +
                "the bidRequestListener is null");

        if (TextUtils.isEmpty(placementId)) {
            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Super auction bid skipped " +
                    "because the placement ID is empty");

            return;
        }

        final RequestMetadata.Builder builder = new RequestMetadata.Builder(requestMetadata);
        final RequestMetadata actualRequestMetadata = builder.setMediator(VerizonAdapterConfiguration.MEDIATOR_ID).build();

        InterstitialAdFactory.requestBid(context, placementId, actualRequestMetadata, new BidRequestListener() {
            @Override
            public void onComplete(Bid bid, ErrorInfo errorInfo) {

                if (errorInfo == null) {
                    BidCache.put(placementId, bid);
                }

                bidRequestListener.onComplete(bid, errorInfo);
            }
        });
    }

    @Override
    protected void show() {

        MoPubLog.log(getAdNetworkId(), SHOW_ATTEMPTED, ADAPTER_NAME);

        VerizonAdapterConfiguration.postOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (verizonInterstitialAd != null) {
                    verizonInterstitialAd.show(context);
                    return;
                }

                logAndNotifyInterstitialFailed(SHOW_FAILED, INTERNAL_ERROR, false);
            }
        });
    }

    @Override
    protected void onInvalidate() {

        VerizonAdapterConfiguration.postOnUiThread(new Runnable() {

            @Override
            public void run() {
                // Destroy any hanging references
                if (verizonInterstitialAd != null) {
                    verizonInterstitialAd.destroy();
                    verizonInterstitialAd = null;
                }
            }
        });
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    private String getPlacementIdKey() {
        return PLACEMENT_ID_KEY;
    }

    private String getSiteIdKey() {
        return SITE_ID_KEY;
    }

    private void logAndNotifyInterstitialFailed(final MoPubLog.AdapterLogEvent event,
                                                final MoPubErrorCode errorCode,
                                                final boolean isLoad) {

        MoPubLog.log(getAdNetworkId(), event, ADAPTER_NAME, errorCode.getIntCode(), errorCode);

        if (isLoad && mLoadListener != null) {
            mLoadListener.onAdLoadFailed(errorCode);
        } else if (!isLoad && mInteractionListener != null) {
            mInteractionListener.onAdFailed(errorCode);
        }
    }

    @NonNull
    public String getAdNetworkId() {
        return mPlacementId != null ? mPlacementId : "";
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull final Activity activity, @NonNull final AdData adData) {
        return false;
    }

    private class VerizonInterstitialFactoryListener implements
            InterstitialAdFactory.InterstitialAdFactoryListener {

        @Override
        public void onLoaded(final InterstitialAdFactory interstitialAdFactory,
                             final InterstitialAd interstitialAd) {

            verizonInterstitialAd = interstitialAd;

            MoPubLog.log(getAdNetworkId(), LOAD_SUCCESS, ADAPTER_NAME);

            VerizonAdapterConfiguration.postOnUiThread(new Runnable() {

                @Override
                public void run() {
                    final CreativeInfo creativeInfo = verizonInterstitialAd == null ? null :
                            verizonInterstitialAd.getCreativeInfo();
                    MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Verizon creative " +
                            "info: " + creativeInfo);

                    if (mLoadListener != null) {
                        mLoadListener.onAdLoaded();
                    }
                }
            });
        }


        @Override
        public void onError(final InterstitialAdFactory interstitialAdFactory, final ErrorInfo errorInfo) {

            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Failed to load Verizon " +
                    "interstitial due to error: " + errorInfo.toString());
            VerizonAdapterConfiguration.postOnUiThread(new Runnable() {

                @Override
                public void run() {
                    logAndNotifyInterstitialFailed(LOAD_FAILED, convertErrorInfoToMoPub(errorInfo), true);
                }
            });
        }
    }

    private class VerizonInterstitialListener implements InterstitialAd.InterstitialAdListener {

        @Override
        public void onError(final InterstitialAd interstitialAd, final ErrorInfo errorInfo) {

            MoPubLog.log(getAdNetworkId(), CUSTOM, ADAPTER_NAME, "Failed to show Verizon " +
                    "interstitial due to error: " + errorInfo.toString());
            VerizonAdapterConfiguration.postOnUiThread(new Runnable() {

                @Override
                public void run() {
                    logAndNotifyInterstitialFailed(SHOW_FAILED, convertErrorInfoToMoPub(errorInfo), false);
                }
            });
        }

        @Override
        public void onShown(final InterstitialAd interstitialAd) {

            MoPubLog.log(getAdNetworkId(), SHOW_SUCCESS, ADAPTER_NAME);
            VerizonAdapterConfiguration.postOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (mInteractionListener != null) {
                        mInteractionListener.onAdShown();
                    }
                }
            });
        }

        @Override
        public void onClosed(final InterstitialAd interstitialAd) {

            MoPubLog.log(getAdNetworkId(), DID_DISAPPEAR, ADAPTER_NAME);
            VerizonAdapterConfiguration.postOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (mInteractionListener != null) {
                        mInteractionListener.onAdDismissed();
                    }
                }
            });
        }

        @Override
        public void onClicked(final InterstitialAd interstitialAd) {

            MoPubLog.log(getAdNetworkId(), CLICKED, ADAPTER_NAME);
            VerizonAdapterConfiguration.postOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (mInteractionListener != null) {
                        mInteractionListener.onAdClicked();
                    }
                }
            });
        }

        @Override
        public void onAdLeftApplication(final InterstitialAd interstitialAd) {
            // Only logging this event. No need to call interstitialListener.onLeaveApplication()
            // because it's an alias for interstitialListener.onInterstitialClicked()
            MoPubLog.log(getAdNetworkId(), WILL_LEAVE_APPLICATION, ADAPTER_NAME);
        }

        @Override
        public void onEvent(final InterstitialAd interstitialAd, final String source,
                            final String eventId, final Map<String, Object> arguments) {

            if (AD_IMPRESSION_EVENT_ID.equals(eventId)) {
                VerizonAdapterConfiguration.postOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        if (mInteractionListener != null) {
                            mInteractionListener.onAdImpression();
                        }
                    }
                });
            }
        }
    }
}
