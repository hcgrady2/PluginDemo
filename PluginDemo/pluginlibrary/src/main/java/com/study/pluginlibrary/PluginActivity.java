package com.study.pluginlibrary;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by hcw on 2019/3/7.
 * Copyright©hcw.All rights reserved.
 */

public class PluginActivity extends AppCompatActivity implements IPlugin{
    private int mFrom = FROM_INTERNAL;

    private Activity mActivity;
    @Override
    public void attach(Activity activity) {
        mActivity = activity;
    }

    @Override
    public void setContentView(int layoutResId) {
        if (mFrom == FROM_INTERNAL){
            super.setContentView(layoutResId);
        }else {
            mActivity.setContentView(layoutResId);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null){
            mFrom = savedInstanceState.getInt("FROM");
        }
        if (mFrom == FROM_INTERNAL){
            super.onCreate(savedInstanceState);
            mActivity = this;
        }
    }

    @Override
    public void onStart() {
        if (mFrom == FROM_INTERNAL){
            super.onStart();
        }
    }

    @Override
    public void onResume() {
        if (mFrom == FROM_INTERNAL){
            super.onResume();
        }
    }

    @Override
    public void onRestart() {
        if (mFrom == FROM_INTERNAL){
            super.onRestart();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mFrom == FROM_INTERNAL){
            super.onActivityResult(requestCode,resultCode,data);
        }
    }

    @Override
    public void onPause() {
        if (mFrom == FROM_INTERNAL){
            super.onPause();
        }
    }

    @Override
    public void onStop() {
        if (mFrom == FROM_INTERNAL){
            super.onStop();
        }
    }

    @Override
    public void onDestroy() {
        if (mFrom == FROM_INTERNAL){
            super.onDestroy();
        }
    }
}
