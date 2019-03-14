package com.study.hookplugin.utils;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by hcw on 2019/3/10.
 * Copyright©hcw.All rights reserved.
 */

/**
 * 反射工具
 */
public class ReflectUtil {
    public static final String METHOD_currentActivityThread = "currentActivityThread";
    public static final String CLASS_ActivityThread = "android.app.ActivityThread";
    public static final String FIELD_mInstrumentation = "mInstrumentation";
    public static final String TAG = "ReflectUtil";


    private static Instrumentation sInstrumentation;
    private static Instrumentation sActivityInstrumentation;
    private static Field sActivityThreadInstrumentationField;
    private static Field sActivityInstrumentationField;
    private static Object sActivityThread;


    /**
     *  init 方法的作用主要用两个：
     *  1、 拿到 ActivityThread 对象，进而拿到 AdtivityThread 中的 mInstrumentation
     *  2、拿到 Activity 中的 mInstrumentation
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static boolean init() {

        /**
         *  // 这里会启动新的Activity，核心功能都在 mMainThread.getApplicationThread() 中完成
             Instrumentation.ActivityResult ar =
             mInstrumentation.execStartActivity(
             this, mMainThread.getApplicationThread(), mToken, this,
             intent, requestCode, options);
         */

        //获取当前的ActivityThread对象
        Class<?> activityThreadClass = null;
        try {
            activityThreadClass = Class.forName(CLASS_ActivityThread);
            Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod(METHOD_currentActivityThread);
            currentActivityThreadMethod.setAccessible(true);
            Object currentActivityThread = currentActivityThreadMethod.invoke(null);

            //拿到在ActivityThread类里面的原始mInstrumentation对象
            Field instrumentationField = activityThreadClass.getDeclaredField(FIELD_mInstrumentation);
            instrumentationField.setAccessible(true);
            //拿到这个 Filed 是为了方便修改这个值
            sActivityThreadInstrumentationField = instrumentationField;

            //通过反射拿到 mInstrumentation 并保存
            sInstrumentation = (Instrumentation) instrumentationField.get(currentActivityThread);
            //保存 ActivityThread 对象
            sActivityThread = currentActivityThread;


            //拿到 Activity 的 mInstrumentation
            sActivityInstrumentationField =  Activity.class.getDeclaredField(FIELD_mInstrumentation);
            sActivityInstrumentationField.setAccessible(true);
            return true;
        } catch (ClassNotFoundException
                | NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException
                | NoSuchFieldException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static Instrumentation getInstrumentation() {
        return sInstrumentation;
    }

    public static Object getActivityThread() {
        return sActivityThread;
    }

    public static void setInstrumentation(Object activityThread, HookedInstrumentation hookedInstrumentation) {
        try {
            sActivityThreadInstrumentationField.set(activityThread, hookedInstrumentation);
            if (Constants.DEBUG) Log.e(TAG, "set hooked instrumentation");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static void setActivityInstrumentation(Activity activity, PluginManager manager) {
        try {
            //获取 activity 的 instrumentation
            sActivityInstrumentation = (Instrumentation) sActivityInstrumentationField.get(activity);
            //通过 获取的 instrumentation，构造一个新的 instrumentation ( hook 了 execStartActivity)
            HookedInstrumentation instrumentation = new HookedInstrumentation(sActivityInstrumentation, manager);
            //重新将对象设置给 activity
            sActivityInstrumentationField.set(activity, instrumentation);

            if (Constants.DEBUG) Log.e(TAG, "set activity hooked instrumentation");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    public static void setField(Class clazz, Object target, String field, Object object) {
        try {
            Field f = clazz.getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, object);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
