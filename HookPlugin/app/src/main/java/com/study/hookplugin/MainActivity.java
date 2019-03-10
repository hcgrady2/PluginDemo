package com.study.hookplugin;

import android.Manifest;
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

import com.study.hookplugin.utils.Constants;
import com.study.hookplugin.utils.PluginManager;
import com.study.hookplugin.utils.ReflectUtil;

public class MainActivity extends AppCompatActivity {
    // https://zhuanlan.zhihu.com/p/33017826
    //https://cloud.tencent.com/developer/article/1351074

    private String mPluginPackageName = "com.study.pluginmodule";
    private String mPluginClassName = "com.study.pluginmodule.MainActivity";

    //读写权限
    private static String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    //请求状态码
    private static int REQUEST_PERMISSION_CODE = 1;

    private PluginManager mPluginManager;


    Button launch;
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        launch = findViewById(R.id.luanch);

        init();
        launch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPluginManager.loadPlugin(Constants.PLUGIN_PATH)) {
                    Intent intent = new Intent();
                    intent.setClassName(mPluginPackageName, mPluginClassName);
                    startActivity(intent);
                }
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void init(){
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_PERMISSION_CODE);
            }
        }

        // !! must first
        ReflectUtil.init();

        mPluginManager = PluginManager.getInstance(getApplicationContext());
        mPluginManager.hookInstrumentation();
        mPluginManager.hookCurrentActivityInstrumentation(this);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        // !!! 不要在此Hook,看源码发现mInstrumentaion会在此方法后初始化
    }
}
