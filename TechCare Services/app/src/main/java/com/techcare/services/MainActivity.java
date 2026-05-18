package com.techcare.services;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// 1. Initialize Views (Added TextViews)
		Button btnGetStarted = findViewById(R.id.btnGetStarted);
		ImageView logo = findViewById(R.id.logoImage);
		TextView tvTitle = findViewById(R.id.tvTitle);
		TextView tvDescription = findViewById(R.id.tvDescription);
		
		// 2. Load Animation
		// Make sure you have res/anim/fade_in.xml created!
		Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
		
		// 3. Start Animation on ALL views
		logo.startAnimation(fadeIn);
		tvTitle.startAnimation(fadeIn);       // New
		tvDescription.startAnimation(fadeIn); // New
		btnGetStarted.startAnimation(fadeIn);
		
		// Click listener
		btnGetStarted.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(MainActivity.this, LoginActivity.class));
				finish();
			}
		});
	}
}