package com.phoenix.hookdemo.app;

import android.app.Application;

import com.phoenix.hookdemo.hook.HookUtil;
import com.phoenix.hookdemo.hook.ProxyActivity;

/**
 * Created by lcf on 2017/5/11.
 */

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        HookUtil hookUtil = new HookUtil(this, ProxyActivity.class);
        hookUtil.hookAM();
        hookUtil.hookActivityThread();
    }
}
