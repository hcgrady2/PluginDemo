package com.study.pingindemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.study.pingindemo.utils.FileUtils;
import com.study.pluginlibrary.PluginManager;
import com.study.pluginlibrary.ProxyActivity;

public class MainActivity extends AppCompatActivity {

    Button  load,jump;

    public final static String PLUGIN_NAME = "plugin.apk";
    public final static String PLUGIN_PACKAGE_NAME = "com.study.plugin";
    public final static String PLUGIN_CLAZZ_NAME = "com.study.plugin.PluginDemo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * 1、init 传递 context
         * 2、生成 apkPath 并 load（创建了各种资源）
         * 3、跳转到目标 activity
         */

        PluginManager.getInstance().init(this);

        load = findViewById(R.id.load);
        jump = findViewById(R.id.jump);

        load.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String apkPath = FileUtils.copyAssetsAndWrite(MainActivity.this,"plugin.apk");
                //加载 apk,初始化 PluginApk
                PluginManager.getInstance().loadApk(apkPath);
            }
        });

        jump.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //通过代理
                Intent intent = new Intent(MainActivity.this, ProxyActivity.class);
                intent.putExtra("className","com.study.plugin.PluginDemo");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
    }
}
