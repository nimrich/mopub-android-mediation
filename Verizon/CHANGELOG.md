## Changelog
  * 1.14.0.0
    * This version of the adapters has been certified with Verizon 1.14.0 and MoPub 5.18.0.

  * 1.13.0.0
    * This version of the adapters has been certified with Verizon 1.13.0 and MoPub 5.17.0.

  * 1.9.0.1
    * Fix a bug where a missing native ad component would cause an NPE.

  * 1.9.0.0
    * This version of the adapters has been certified with Verizon 1.9.0 and MoPub 5.15.0.
    * Refactor native ad impression tracking logic. No external changes for publishers.
    * Native ads now require a disclaimer/sponsored text view.
  
  * 1.8.2.1
    * Fix a bug where the rewarded video adapter fails to request a new ad after a show-related error happens.

  * 1.8.2.0
    * This version of the adapters has been certified with Verizon 1.8.2 and MoPub 5.15.0.

  * 1.8.1.0
    * This version of the adapters has been certified with Verizon 1.8.1 and MoPub 5.14.0. 

  * 1.8.0.2
    * Refactor native ad impression tracking logic. No external changes for publishers. 

  * 1.8.0.1
    * Fire `onNativeAdFailed()` on native ad failures so failovers can happen immediately.

  * 1.8.0.0
    * This version of the adapters has been certified with Verizon 1.8.0 and MoPub 5.13.1.
    * No longer pass the ability to collect location.

  * 1.7.0.0
    * This version of the adapters has been certified with Verizon 1.7.0 and MoPub 5.13.1.

  * 1.6.0.0
    * This version of the adapters has been certified with Verizon 1.6.0 and MoPub 5.13.0.
    * Fix interstitial timeouts.
    * Enable Advanced Bidding support for rewarded video.

  * 1.5.0.1
    * Refactor non-native adapter classes to use the new consolidated API from MoPub.
    * This and newer adapter versions are only compatible with 5.13.0+ MoPub SDK.

  * 1.5.0.0
    * Compress the Advanced Bidding token to adhere to MoPub's spec.
    * This version of the adapters has been certified with Verizon 1.5.0 and MoPub 5.11.1.

  * 1.4.0.0
    * Update Advanced Bidding API
    * This version of the adapters has been certified with Verizon 1.4.0.

  * 1.3.1.0
    * This version of the adapters has been certified with Verizon 1.3.1.
  
  * 1.3.0.0
    * This version of the adapters has been certified with Verizon 1.3.0.
    * Remove Advanced Bidding token generation logic from the adapters. The equivalent logic will be added to the Verizon SDK.

  * 1.2.1.3
    * Log the Verizon placement ID in ad lifecycle events.

  * 1.2.1.2
    * Add support for Advanced Bidding for banner and interstitial.

  * 1.2.1.1
    * Migrate utility methods in `VerizonUtils.java` to `VerizonAdapterConfiguration.java` and delete the former.

  * 1.2.1.0
    * This version of the adapters has been certified with Verizon 1.2.1.
    * Remove all permissions except INTERNET in the adapter's AndroidManifest (to be in parity with the [Verizon SDK](https://sdk.verizonmedia.com/standard-edition/releasenotes-android.html)).

  * 1.2.0.1
    * Log the Verizon SDK edition name (if available) together with the network SDK version.
  
  * 1.2.0.0
    * This version of the adapters has been certified with Verizon 1.2.0.

  * 1.1.4.1
    * Add support for AndroidX. This is the minimum version compatible with MoPub 5.9.0.

  * 1.1.4.0
    * Add support for rewarded video and native ad.
    * This version of the adapters has been certified with Verizon 1.1.4.

  * 1.1.3.0
    * This version of the adapters has been certified with Verizon 1.1.3.

  * 1.1.1.1
    * Add support for parsing banner's width and height from `serverExtras`. This provides backwards compatibility for legacy Millennial adapters.

  * 1.1.1.0
    * This version of the adapters has been certified with Verizon 1.1.1.
    * Add support to initialize the Verizon SDK in conjunction with MoPub's initialization.
  
  * 1.0.2.2
    * Remove `maxSdkVersion` from a permission included in the AndroidManifest to avoid merge conflicts.

  * 1.0.2.1
    * Pass the banner size from the MoPub ad response to Verizon.

  * 1.0.2.0
    * This version of the adapters has been certified with Verizon 1.0.2.
