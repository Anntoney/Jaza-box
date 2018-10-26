package com.tonney.shop;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.tonney.shop.entity.UserObject;
import com.tonney.shop.utils.CustomApplication;


public class SplashScreen extends AppCompatActivity {


    private ImageView logo;
    private static int splashTimeOut=5000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);


        logo=(ImageView)findViewById(R.id.imgLogo);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {




                UserObject mUser = ((CustomApplication)getApplication()).getLoginUser();
                if(mUser != null){
                    Intent loginUserIntent = new Intent(SplashScreen.this, ShopActivity.class);
                    startActivity(loginUserIntent);
                } else {

                    Intent LogingIn = new Intent(SplashScreen.this, LoginActivity.class);
                    startActivity(LogingIn);

                }

//                Intent i = new Intent(SplashScreen.this,LoginOptionActivity.class);
//                startActivity(i);
//                finish();
            }
        },splashTimeOut);

        Animation myanim = AnimationUtils.loadAnimation(this,R.anim.mysplashanimation);
        logo.startAnimation(myanim);

    }
}
