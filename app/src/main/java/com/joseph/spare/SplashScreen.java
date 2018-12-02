package com.joseph.spare;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import com.crashlytics.android.Crashlytics;
import com.joseph.spare.utils.ServiceUtils;

import io.fabric.sdk.android.Fabric;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_splash_screen);
        new Handler().postDelayed(() -> {
            String userId= ServiceUtils.getLoggedUserId(this);
            if(userId==null){
                startActivity(new Intent(SplashScreen.this,LoginActivity.class));
            }else {
                startActivity(new Intent(this,Dashboard.class));
            }
            finish();
        },2000);

    }

}
