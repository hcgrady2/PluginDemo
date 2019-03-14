package com.study.pluginmodule;

import android.app.Activity;
import android.os.Bundle;


/**
 * 这里的 插件 Activity 需要集成 Activity 而不是 AppComptActivity，否则会报错 * Caused by: java.lang.IllegalArgumentException: android.content.pm.PackageManager$NameNotFoundException: ComponentInfo{...}
 */
public class HookDemoActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hook_module);
    }
}
