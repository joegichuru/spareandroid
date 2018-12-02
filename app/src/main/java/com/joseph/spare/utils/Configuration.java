package com.joseph.spare.utils;

import android.support.annotation.NonNull;

import com.yayandroid.locationmanager.configuration.DefaultProviderConfiguration;
import com.yayandroid.locationmanager.configuration.GooglePlayServicesConfiguration;
import com.yayandroid.locationmanager.configuration.LocationConfiguration;
import com.yayandroid.locationmanager.configuration.LocationConfiguration.Builder;
import com.yayandroid.locationmanager.configuration.PermissionConfiguration;

public final class Configuration {
    private Configuration() {
    }

    public static LocationConfiguration silentConfiguration() {
        return silentConfiguration(true);
    }

    public static LocationConfiguration silentConfiguration(boolean keepTracking) {
        return new Builder().keepTracking(keepTracking).useGooglePlayServices(new GooglePlayServicesConfiguration.Builder().askForSettingsApi(false).build()).useDefaultProviders(new DefaultProviderConfiguration.Builder().build()).build();
    }

    public static LocationConfiguration defaultConfiguration(@NonNull String rationalMessage, @NonNull String gpsMessage) {
        return new Builder().askForPermission(new PermissionConfiguration.Builder().rationaleMessage(rationalMessage).build()).useGooglePlayServices(new GooglePlayServicesConfiguration.Builder().build()).useDefaultProviders(new DefaultProviderConfiguration.Builder().build()).build();
    }
}
