package com.study.hookplugin.utils;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.ContextThemeWrapper;

import java.lang.reflect.Method;

/**
 * Created by hcw on 2019/3/10.
 * Copyright©hcw.All rights reserved.
 */

public class HookedInstrumentation extends Instrumentation implements Handler.Callback {
    protected Instrumentation mBase;
    private PluginManager mPluginManager;

    public HookedInstrumentation(Instrumentation base, PluginManager pluginManager) {
        mBase = base;
        mPluginManager = pluginManager;
    }

    /**
     * 覆盖掉原始Instrumentation类的对应方法,用于插件内部跳转Activity时适配
     *
     * @Override
     */
    public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode, Bundle options) {

        mPluginManager.hookToStubActivity(intent);

        try {
            Method execStartActivity = Instrumentation.class.getDeclaredMethod(
                    "execStartActivity", Context.class, IBinder.class, IBinder.class,
                    Activity.class, Intent.class, int.class, Bundle.class);
            execStartActivity.setAccessible(true);
            return (ActivityResult) execStartActivity.invoke(mBase, who,
                    contextThread, token, target, intent, requestCode, options);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("do not support!!!" + e.getMessage());
        }
    }

    @Override
    public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {

        if (mPluginManager.hookToPluginActivity(intent)) {
            String targetClassName = intent.getComponent().getClassName();
            PluginApp pluginApp = mPluginManager.getLoadedPluginApk();
            Activity activity = mBase.newActivity(pluginApp.mClassLoader, targetClassName, intent);
            activity.setIntent(intent);
            ReflectUtil.setField(ContextThemeWrapper.class, activity, Constants.FIELD_RESOURCES, pluginApp.mResources);
            return activity;
        }

        return super.newActivity(cl, className, intent);
    }

    @Override
    public boolean handleMessage(Message message) {
        return false;
    }


    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        super.callActivityOnCreate(activity, icicle);
    }
}
