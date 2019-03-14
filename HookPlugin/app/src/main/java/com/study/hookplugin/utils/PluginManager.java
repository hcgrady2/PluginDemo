package com.study.hookplugin.utils;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

/**
 * Created by hcw on 2019/3/10.
 * Copyright©hcw.All rights reserved.
 */

/**
 * 插件框架管理，单例模式
 */
public class PluginManager {
    private final static String TAG = "PluginManager";
    private static PluginManager sInstance;
    private Context mContext;
    private PluginApp mPluginApp;


    public static PluginManager getInstance(Context context) {
        if (sInstance == null && context != null) {
            sInstance = new PluginManager(context);
        }
        return sInstance;
    }

    private PluginManager(Context context) {
        mContext = context;
    }


    /**
     *
     */
    public void hookInstrumentation() {
        try {
            //拿到 ActivityThread 中的 instrumentation 对象
            Instrumentation baseInstrumentation = ReflectUtil.getInstrumentation();
            //最终还是需要执行execStartActivity，因此需要传入 baseInstrumentation，最终反射调用这个对象的方法,只不过是替换了一些参数
            final HookedInstrumentation instrumentation = new HookedInstrumentation(baseInstrumentation, this);
            Object activityThread = ReflectUtil.getActivityThread();
            ReflectUtil.setInstrumentation(activityThread, instrumentation);
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    /**
     * hook 当前 activity 的 instrumentation
     * @param activity
     */
    public void hookCurrentActivityInstrumentation(Activity activity) {
        ReflectUtil.setActivityInstrumentation(activity, sInstance);
    }


    /**

     Activity 的启动过程：
     1、Activity 调用 startActivity，实际会调用 Instrumentation 类的 execStartActivity 方法。
     2、通过跨进程的 Binder 调用，进入到 ActivityManagerService 中，其内部会处理 Activity 栈。之后又通过跨进程调用进入到需要调用的 Activity 所在的进程中。
     3、ApplicationThread 是一个 Binder 对象，其运行在 Binder 线程池中，内部包含一个 H 类，该类继承于类 Handler。ApplicationThread 将启动需要调用的 Activity 的信息通过 H 对象发送给主线程。
     4、主线程拿到需要调用的 Activity 的信息后，调用 Instrumentation 类的 newActivity 方法，其内通过 ClassLoader 创建 Activity 实例。


     因为 activity 启动之前会判断 这个 activity 是否在 manifest 注册过，没有注册肯定是不行的，那么在启动之前，我们先把插件类替换为预先定义的一个 stubactivity 类，就可以绕过检测。
     当真正启动的时候，就可以替换为真正的插件类。

     */
    public void hookToStubActivity(Intent intent) {
        if (Constants.DEBUG) Log.e(TAG, "hookToStubActivity");

        if (intent == null || intent.getComponent() == null) {
            return;
        }
        String targetPackageName = intent.getComponent().getPackageName();
        String targetClassName = intent.getComponent().getClassName();

        if (mContext != null
                && !mContext.getPackageName().equals(targetPackageName)
                && isPluginLoaded(targetPackageName)) {
            if (Constants.DEBUG) Log.e(TAG, "hook " +  targetClassName + " to " + Constants.STUB_ACTIVITY);

            intent.setClassName(Constants.STUB_PACKAGE, Constants.STUB_ACTIVITY);
            intent.putExtra(Constants.KEY_IS_PLUGIN, true);
            intent.putExtra(Constants.KEY_PACKAGE, targetPackageName);
            intent.putExtra(Constants.KEY_ACTIVITY, targetClassName);
        }
    }

    public boolean hookToPluginActivity(Intent intent) {
        if (Constants.DEBUG) Log.e(TAG, "hookToPluginActivity");
        if (intent.getBooleanExtra(Constants.KEY_IS_PLUGIN, false)) {
            String pkg = intent.getStringExtra(Constants.KEY_PACKAGE);
            String activity = intent.getStringExtra(Constants.KEY_ACTIVITY);
            if (Constants.DEBUG) Log.e(TAG, "hook " + intent.getComponent().getClassName() + " to " + activity);
            intent.setClassName(pkg, activity);
            return true;
        }
        return false;
    }

    private boolean isPluginLoaded(String packageName) {
        // TODO 检查packageNmae是否匹配
        return mPluginApp != null;
    }


    /**
     * 加载插件就是把插件添加到系统 addAssetPath 路径中,并且要有对应的 classLoader 才能调用
     * @param apkPath
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public PluginApp loadPluginApk(String apkPath) {
        String addAssetPathMethod = "addAssetPath";
        PluginApp pluginApp = null;
        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPath = assetManager.getClass().getMethod(addAssetPathMethod, String.class);
            addAssetPath.invoke(assetManager, apkPath);
            Resources pluginRes = new Resources(assetManager,
                    mContext.getResources().getDisplayMetrics(),
                    mContext.getResources().getConfiguration());
            pluginApp = new PluginApp(pluginRes);
            pluginApp.mClassLoader = createDexClassLoader(apkPath);
        } catch (IllegalAccessException
                | InstantiationException
                | NoSuchMethodException
                | InvocationTargetException e) {
            e.printStackTrace();
        }

        return pluginApp;
    }

    private DexClassLoader createDexClassLoader(String apkPath) {
        File dexOutputDir = mContext.getDir("dex", Context.MODE_PRIVATE);
        return new DexClassLoader(apkPath, dexOutputDir.getAbsolutePath(),
                null, mContext.getClassLoader());

    }

    /**
     * 加载插件,这里返回的 pluginapp 就是包含了 apk 资源以及对应 classloader
     * @param apkPath
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public boolean loadPlugin(String apkPath) {
        File apk = new File(apkPath);
        if (!apk.exists()) {
            return false;
        }
        mPluginApp = loadPluginApk(apkPath);
        return mPluginApp != null;
    }

    public PluginApp getLoadedPluginApk() {
        return mPluginApp;
    }
}
