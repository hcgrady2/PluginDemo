package com.study.pluginlibrary;

import android.app.Activity;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by hcw on 2019/3/7.
 * Copyright©hcw.All rights reserved.
 */

/**
 * 代理管理插件activity 生命周期
 */
public class ProxyActivity extends Activity{
    private String mClassName;
    private String mPackageName;

    private PluginApk mPluginApk;
    private IPlugin mIplugin;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mClassName = getIntent().getStringExtra("className");
        mPackageName = getIntent().getStringExtra("package_name");
        mPluginApk = PluginManager.getInstance().getPluginApk();
        launchPluginActivity();
    }

    private void launchPluginActivity() {
        if (mPluginApk == null){
            throw  new RuntimeException("please load plugin apk");
        }
        try {
            //这个 clazz 就是 activity 实例，但是没有生命周期，没有 context
            Class<?> clazz = mPluginApk.mDexClassLoader.loadClass(mClassName);
            //Object object = clazz.newInstance();
            Constructor<?> constructor = clazz.getConstructor(new Class[] {});
            constructor.setAccessible(true);
            Object object =  constructor.newInstance(new Object[] {});

            if (object instanceof IPlugin){
                mIplugin = (IPlugin)object;
                mIplugin.attach(this);
                Bundle bundle = new Bundle();
                bundle.putInt("FROM",IPlugin.FROM_EXTERNAL);
                mIplugin.onCreate(bundle);
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }


    /**
     * @return
     */
    @Override
    public Resources getResources() {
        return  mPluginApk != null ?mPluginApk.mResource:super.getResources();
    }
    /**
     *
     * @return
     */
    @Override
    public AssetManager getAssets() {
        return  mPluginApk != null ?mPluginApk.mAssetManager:super.getAssets();
    }

    @Override
    public ClassLoader getClassLoader() {
        return  mPluginApk != null ?mPluginApk.mDexClassLoader:super.getClassLoader();
    }
}
