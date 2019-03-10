package com.study.hookplugin.utils;

import android.os.Environment;

/**
 * Created by hcw on 2019/3/10.
 * Copyright©hcw.All rights reserved.
 */

public class Constants {
    public final static String KEY_IS_PLUGIN = "key_is_plugin";
    public final static String KEY_PACKAGE =  "key_package";
    public final static String KEY_ACTIVITY = "key_activity";

    public final static String FIELD_RESOURCES = "mResources";

    public static final String STUB_ACTIVITY = "com.study.hookplugin.StubActivity";
    public static final String STUB_PACKAGE = "com.study.hookplugin";

    public static final String PLUGIN_PATH = Environment.getExternalStorageDirectory() + "/plugin/plugin.apk";
    public final static boolean DEBUG = true;

}
