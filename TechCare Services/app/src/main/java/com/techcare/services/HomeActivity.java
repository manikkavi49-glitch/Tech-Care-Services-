package com.techcare.services;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HomeActivity extends Activity {
	
	private FirebaseAuth mAuth;
	private DatabaseReference mDatabase;
	private TextView tvWelcome;
	private ImageView imgProfileIcon;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		
		mAuth = FirebaseAuth.getInstance();
		mDatabase = FirebaseDatabase.getInstance("https://techcare-services-default-rtdb.firebaseio.com/").getReference();
		
		tvWelcome = findViewById(R.id.tvWelcome);
		imgProfileIcon = findViewById(R.id.imgProfileIcon);
		
		// Load Name and Profile Picture
		loadUserInfo();
		
		// Profile Icon Click Listener
		if (imgProfileIcon != null) {
			imgProfileIcon.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
				}
			});
		}
		
		// --- SETUP SERVICE CARDS (Anti-Crash Logic) ---
		// We use "View" because these are now CardViews (Images), not Buttons
		setupButton(R.id.btnMobile, BookingActivity.class, "Mobile Phone");
		setupButton(R.id.btnLaptop, BookingActivity.class, "Laptop");
		setupButton(R.id.btnTV, BookingActivity.class, "TV");
		setupButton(R.id.btnAC, BookingActivity.class, "AC");
		
		// --- SETUP DASHBOARD BUTTONS ---
		setupButton(R.id.btnHistory, HistoryActivity.class, null);
		setupButton(R.id.btnSupport, SupportActivity.class, null);
		
		// --- LOGOUT LOGIC ---
		View btnLogout = findViewById(R.id.btnLogout);
		if (btnLogout != null) {
			btnLogout.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mAuth.signOut();
					Toast.makeText(HomeActivity.this, "Logged Out", Toast.LENGTH_SHORT).show();
					Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
					// Clear history so user cannot press "Back" to return
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
					startActivity(intent);
					finish();
				}
			});
		}
	}
	
	// Refresh data when returning from Profile or Booking pages
	@Override
	protected void onResume() {
		super.onResume();
		loadUserInfo();
	}
	
	// --- HELPER METHOD ---
	// Finds any View (Button or CardView) and adds a click listener
	private void setupButton(int btnId, final Class<?> targetActivity, final String deviceType) {
		View btn = findViewById(btnId);
		if (btn != null) {
			btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(HomeActivity.this, targetActivity);
					if (deviceType != null) {
						intent.putExtra("DEVICE_TYPE", deviceType);
					}
					startActivity(intent);
				}
			});
		}
	}
	
	// --- LOAD USER DATA (Name & Photo) ---
	private void loadUserInfo() {
		FirebaseUser user = mAuth.getCurrentUser();
		if (user != null) {
			String userId = user.getUid();
			mDatabase.child("Users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
				@Override
				public void onDataChange(@NonNull DataSnapshot snapshot) {
					if (snapshot.exists()) {
						// 1. Set Name
						String name = snapshot.child("fullName").getValue(String.class);
						if (name != null) tvWelcome.setText(name + "!");
						
						// 2. Set Profile Picture
						String imgStr = snapshot.child("profileImage").getValue(String.class);
						if (imgStr != null && !imgStr.isEmpty()) {
							try {
								byte[] decodedString = Base64.decode(imgStr, Base64.DEFAULT);
								Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
								
								imgProfileIcon.setImageBitmap(decodedByte);
								imgProfileIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
								} catch (Exception e) {
								// Keep default icon if error occurs
							}
						}
					}
				}
				@Override
				public void onCancelled(@NonNull DatabaseError error) { }
			});
		}
	}
}