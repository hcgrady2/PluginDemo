package com.study.pluginlibrary;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import dalvik.system.DexClassLoader;

/**
 * Created by hcw on 2019/3/7.
 * Copyright©hcw.All rights reserved.
 */

public class PluginManager {
    private static  PluginManager instance;
    Map<String, PluginApk> sMap = new HashMap<>();
    public static PluginManager getInstance( ){
        if (instance == null  ){
            instance = new PluginManager();
        }
        return instance;
    }

    private PluginManager( ){
    }

    private Context mContext;
    private PluginApk mPluginApk;


    public PluginApk getPluginApk(){
        return mPluginApk;
    }

    public void init(Context context){
        mContext = context.getApplicationContext();
    }
    //加载 apk
    public void loadApk(String apkPath){
        PackageInfo packageInfo = mContext.getPackageManager().getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES
        |PackageManager.GET_SERVICES);
        if (packageInfo == null){
            return;
        }

        // check cache
        PluginApk pluginApk = sMap.get(packageInfo.packageName);
        if (pluginApk == null) {
            //创建 classloader
            DexClassLoader classLoader = createDexClassloader(apkPath);
            //创建 AssetManager
            AssetManager assetManager = createAssetManager(apkPath);
            //创建 resource
            Resources resources = createResource(assetManager);
            //创建 PluginApk
            mPluginApk = new PluginApk(packageInfo,resources,classLoader);
        }
    }

    /**
     *  通过 AssetManager 和设备配置来构造 Resouces
     * @param assetManager
     * @return
     */
    private Resources createResource(AssetManager assetManager) {
        Resources resources = mContext.getResources();
        return new Resources(assetManager,resources.getDisplayMetrics(),resources.getConfiguration());
    }

    /**
     * 创建 AssetManager
     * @param apkPath 插件 apk 路径
     * @return
     */
    private AssetManager createAssetManager(String apkPath) {
        AssetManager am = null;
        try {
            //反射构造 AssetManager
             am =  AssetManager.class.newInstance();
            Method method = AssetManager.class.getDeclaredMethod("addAssetPath",String.class);
            //通过反射 将 apk 的目录添加到 AssetManager 的资源路径下
            method.invoke(am,apkPath);
            return am;

        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return am;

    }

    /**
     * 创建 DexClassLoaser
     * @param apkPath 插件 apk
     * @return
     */
    private DexClassLoader createDexClassloader(String apkPath) {
        File file = mContext.getDir("dex",Context.MODE_PRIVATE);
        return new DexClassLoader(apkPath,file.getAbsolutePath(),null,mContext.getClassLoader());
    }

}
