package com.study.pluginlibrary;

/**
 * Created by hcw on 2019/3/7.
 * Copyright©hcw.All rights reserved.
 */

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * 约束 activity
 */
public interface IPlugin {
    int FROM_INTERNAL = 0;      //内部跳转
    int FROM_EXTERNAL = 1;      //外部跳转

    void attach(Activity activity);

    void onCreate(Bundle savedInstanceState);
    void onStart();
    void onResume();
    void onRestart();
    void onActivityResult(int requestCode, int resultCode, Intent data);
    void onPause();
    void onStop();
    void onDestroy();

}
