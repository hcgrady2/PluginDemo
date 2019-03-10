package com.study.plugin;

import android.os.Bundle;

/**
 * @author        hcw
 * @time          2019/3/8 21:12
 * @description  测试组件 apk,plugin apk 还需要 继承我们自己的 PluginActivity
*/

public class PluginDemo extends com.study.pluginlibrary.PluginActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pugin);
    }
}
