package com.phoenix.hookdemo;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

/**
 * Created by lcf on 2017/5/11.
 */

public class ThirdActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thrid);
    }

    public void goNext(View v){
        Intent intent = new Intent();
        ComponentName componentName = new ComponentName(this,MainActivity.class);
        intent.setComponent(componentName);
        startActivity(intent);
    }
}
