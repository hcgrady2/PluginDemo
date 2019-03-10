package com.study.pluginlibrary;

import android.content.pm.PackageInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;

import dalvik.system.DexClassLoader;

/**
 * Created by hcw on 2019/3/7.
 * Copyright©hcw.All rights reserved.
 */

public class PluginApk {

    public PackageInfo mPackageInfo;
    public Resources mResource;
    public AssetManager mAssetManager;
    public DexClassLoader mDexClassLoader;

    public PluginApk(PackageInfo packageInfo, Resources resource , DexClassLoader dexClassLoader) {
        this.mPackageInfo = packageInfo;
        this.mResource = resource;
        this.mAssetManager = resource.getAssets();
        this.mDexClassLoader = dexClassLoader;

    }
}
