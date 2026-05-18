package com.techcare.services;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends Activity {
	
	private EditText etName, etPhone, etAddress;
	private ImageView imgProfile;
	private Button btnSave;
	
	private FirebaseAuth mAuth;
	private DatabaseReference mDatabase;
	private String userId;
	
	private static final int PICK_IMAGE_REQUEST = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Load the new Professional XML Layout
		setContentView(R.layout.activity_profile);
		
		// Initialize Views
		imgProfile = findViewById(R.id.imgProfile);
		etName = findViewById(R.id.etProfileName);
		etPhone = findViewById(R.id.etProfilePhone);
		etAddress = findViewById(R.id.etProfileAddress);
		btnSave = findViewById(R.id.btnSaveProfile);
		
		// Initialize Firebase
		mAuth = FirebaseAuth.getInstance();
		mDatabase = FirebaseDatabase.getInstance("https://techcare-services-default-rtdb.firebaseio.com/").getReference();
		
		FirebaseUser user = mAuth.getCurrentUser();
		if (user != null) {
			userId = user.getUid();
			loadUserProfile();
		}
		
		// Image Click Listener
		imgProfile.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openGallery();
			}
		});
		
		// Save Button Listener
		btnSave.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				saveUserProfile();
			}
		});
	}
	
	// --- 1. OPEN GALLERY ---
	private void openGallery() {
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
	}
	
	// --- 2. HANDLE IMAGE SELECTION ---
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
			Uri filePath = data.getData();
			try {
				Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
				
				// Show Image
				imgProfile.setImageBitmap(bitmap);
				imgProfile.setScaleType(ImageView.ScaleType.CENTER_CROP);
				
				// Save immediately
				saveImageToFirebase(bitmap);
				
				} catch (IOException e) {
				e.printStackTrace();
				Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	// --- 3. COMPRESS & SAVE IMAGE ---
	private void saveImageToFirebase(Bitmap bitmap) {
		Bitmap resized = Bitmap.createScaledBitmap(bitmap, 400, 400, true);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		resized.compress(Bitmap.CompressFormat.JPEG, 70, baos);
		byte[] data = baos.toByteArray();
		String imageString = Base64.encodeToString(data, Base64.DEFAULT);
		
		mDatabase.child("Users").child(userId).child("profileImage").setValue(imageString);
		Toast.makeText(this, "Photo Updated!", Toast.LENGTH_SHORT).show();
	}
	
	// --- 4. LOAD USER DATA ---
	private void loadUserProfile() {
		mDatabase.child("Users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				if (snapshot.exists()) {
					String name = snapshot.child("fullName").getValue(String.class);
					String phone = snapshot.child("phone").getValue(String.class);
					String address = snapshot.child("address").getValue(String.class);
					String imgStr = snapshot.child("profileImage").getValue(String.class);
					
					if (name != null) etName.setText(name);
					if (phone != null) etPhone.setText(phone);
					if (address != null) etAddress.setText(address);
					
					if (imgStr != null && !imgStr.isEmpty()) {
						try {
							byte[] decodedString = Base64.decode(imgStr, Base64.DEFAULT);
							Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
							imgProfile.setImageBitmap(decodedByte);
							imgProfile.setScaleType(ImageView.ScaleType.CENTER_CROP);
						} catch (Exception e) {}
					}
				}
			}
			@Override
			public void onCancelled(@NonNull DatabaseError error) { }
		});
	}
	
	// --- 5. SAVE TEXT DATA ---
	private void saveUserProfile() {
		String newName = etName.getText().toString().trim();
		String newPhone = etPhone.getText().toString().trim();
		String newAddress = etAddress.getText().toString().trim();
		
		if (newName.isEmpty()) {
			Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
			return;
		}
		
		Map<String, Object> updates = new HashMap<>();
		updates.put("fullName", newName);
		updates.put("phone", newPhone);
		updates.put("address", newAddress);
		
		mDatabase.child("Users").child(userId).updateChildren(updates, new DatabaseReference.CompletionListener() {
			@Override
			public void onComplete(DatabaseError error, @NonNull DatabaseReference ref) {
				if (error == null) {
					Toast.makeText(ProfileActivity.this, "Profile Saved!", Toast.LENGTH_SHORT).show();
					finish();
					} else {
					Toast.makeText(ProfileActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
				}
			}
		});
	}
}