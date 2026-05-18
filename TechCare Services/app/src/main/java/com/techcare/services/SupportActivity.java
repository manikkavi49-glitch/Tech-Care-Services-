package com.techcare.services;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class SupportActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Link to the new professional XML layout
		setContentView(R.layout.activity_support);
		
		// 1. Setup Call Button
		Button btnCall = findViewById(R.id.btnCallSupport);
		btnCall.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_DIAL);
				intent.setData(Uri.parse("tel:+94814777888"));
				startActivity(intent);
			}
		});
		
		// 2. Setup Map Button
		Button btnMap = findViewById(R.id.btnOpenMap);
		btnMap.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Opens Google Maps directly to ICBT Kandy Campus coordinates
				Uri gmmIntentUri = Uri.parse("geo:7.3024324,80.6355463?q=ICBT+Kandy+Campus");
				Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
				mapIntent.setPackage("com.google.android.apps.maps");
				
				if (mapIntent.resolveActivity(getPackageManager()) != null) {
					startActivity(mapIntent);
					} else {
					// Fallback to browser if App not installed
					startActivity(new Intent(Intent.ACTION_VIEW,
					Uri.parse("https://www.google.com/maps/search/?api=1&query=ICBT+Kandy+Campus")));
				}
			}
		});
	}
}