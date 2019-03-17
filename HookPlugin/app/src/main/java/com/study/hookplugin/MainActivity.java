package com.study.hookplugin;

import android.Manifest;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.study.hookplugin.test.EvilInstrumentation;
import com.study.hookplugin.utils.Constants;
import com.study.hookplugin.utils.PluginManager;
import com.study.hookplugin.utils.ReflectUtil;

import java.lang.reflect.Field;

public class MainActivity extends AppCompatActivity {

    private String mPluginPackageName = "com.study.pluginmodule";
    private String mPluginClassName = "com.study.pluginmodule.HookDemoActivity";

    //读写权限
    private static String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    //请求状态码
    private static int REQUEST_PERMISSION_CODE = 1;

    private PluginManager mPluginManager;


    Button launch,test;
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        launch = findViewById(R.id.luanch);
        test = findViewById(R.id.test);

       // test(MainActivity.this);

        init();
        launch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /**
                 * 跳转插件之前，肯定需要先加载了这个插件
                 */
                if (mPluginManager.loadPlugin(Constants.PLUGIN_PATH)) {
                    Intent intent = new Intent();
                    intent.setClassName(mPluginPackageName, mPluginClassName);
                    startActivity(intent);
                }

            }
        });

        test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               // Activity的startActivity 是替换 Activity类内部的mInstrumentation
                //startActivity(new Intent(MainActivity.this, SecondActivity.class));
               // Context的startActivity 是替换 ActivityThread类内部的mInstrumentation

                Intent intent = new Intent();
                intent.setClassName(mPluginPackageName, mPluginClassName);
                startActivity(intent);

            }
        });
    }


    /**
     * 这个方法只是为了演示 Hook，跳转 activity 会打印一个内容
     * @param activity
     */
    private void test(Activity activity){
        try {
            // 拿到原始的 mInstrumentation字段
            Field mInstrumentationField  = Activity.class.getDeclaredField("mInstrumentation");
            mInstrumentationField.setAccessible(true);
            Instrumentation originalInstrumentation = (Instrumentation) mInstrumentationField.get(activity);
            mInstrumentationField.set(activity, new EvilInstrumentation(originalInstrumentation));

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }


    /**
     * 拿到 ActivityThread 与 activity 的 instrumentation，并 hook execStartActivity
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void init(){
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_PERMISSION_CODE);
            }
        }
        //主要是是为了拿到 ActivityThread，ActivityThread 中的 mInstrumentation 和 activity 中的 mInstrumentation
        ReflectUtil.init();
        //单例，加载 apk，hook 操作等。
        mPluginManager = PluginManager.getInstance(getApplicationContext());
        //主要是 Hook execStartActivity,修改 intent 为跳转到插件类,hook 的是 activityThread 的 instrumentation
        mPluginManager.hookInstrumentation();
        // hook 当前 activity 的 instrumentation
        mPluginManager.hookCurrentActivityInstrumentation(this);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
    }
}
