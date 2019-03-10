package com.study.hookplugin.utils;

import android.content.res.Resources;

/**
 * Created by hcw on 2019/3/10.
 * Copyright©hcw.All rights reserved.
 */

public class PluginApp {
    public Resources mResources;
    public ClassLoader mClassLoader;


    public PluginApp(Resources mResources) {
        this.mResources = mResources;
    }
}
