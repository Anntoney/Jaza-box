package com.tonney.shop;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

public class IntroActivity extends AppCompatActivity {


    ImageView anmtnImageview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);


        anmtnImageview = (ImageView) findViewById(R.id.ImageviewAdapter);


        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        ActionBar actionBar = getSupportActionBar();
        if(null != actionBar){
            actionBar.hide();
        }


        Button startButton = (Button)findViewById(R.id.start_app);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent startAppIntent = new Intent(IntroActivity.this, LoginOptionActivity.class);
                startActivity(startAppIntent);
            }
        });
    }


}
//        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(welcomImage,"y",300f);
//        objectAnimator.setDuration(animationDuration);

//        ObjectAnimator alphaAnimation = ObjectAnimator.ofFloat(welcomImage,View.ALPHA,1.0f,0.3f);
//        alphaAnimation.setDuration(animationDuration);
//
//        AnimatorSet animatorSet = new AnimatorSet();
//        animatorSet.playTogether(alphaAnimation);
//        animatorSet.start();
