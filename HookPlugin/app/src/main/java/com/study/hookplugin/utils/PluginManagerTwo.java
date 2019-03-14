package com.study.hookplugin.utils;

import android.content.Context;

/**
 * Created by WenTong on 2019/3/12.
 */

public class PluginManagerTwo {

    public static final void install(Context context, String apkPath){
        try {
            FixDexManager manager = new FixDexManager(context);
            manager.fixDex(apkPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
