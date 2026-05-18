package com.techcare.services;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends Activity {
	
	private FirebaseAuth mAuth;
	private DatabaseReference mDatabase;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);
		
		// Initialize Firebase Auth
		mAuth = FirebaseAuth.getInstance();
		mDatabase = FirebaseDatabase.getInstance("https://techcare-services-default-rtdb.firebaseio.com/").getReference();
		
		final EditText etFullName = findViewById(R.id.etFullName);
		final EditText etEmail = findViewById(R.id.etRegEmail);
		final EditText etPassword = findViewById(R.id.etRegPassword);
		Button btnRegister = findViewById(R.id.btnRegister);
		TextView tvLoginLink = findViewById(R.id.tvLoginLink);
		
		btnRegister.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final String name = etFullName.getText().toString().trim();
				final String email = etEmail.getText().toString().trim();
				String password = etPassword.getText().toString().trim();
				
				if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
					Toast.makeText(RegisterActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
					return;
				}
				
				// 1. Create User in Auth
				mAuth.createUserWithEmailAndPassword(email, password)
				.addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
					@Override
					public void onComplete(@NonNull Task<AuthResult> task) {
						if (task.isSuccessful()) {
							
							// Get the new User ID
							FirebaseUser user = mAuth.getCurrentUser();
							String userId = user.getUid();
							
							// Prepare Data
							HashMap<String, String> userMap = new HashMap<>();
							userMap.put("fullName", name);
							userMap.put("email", email);
							
							// 2. Save to Realtime Database
							mDatabase.child("Users").child(userId).setValue(userMap)
							.addOnCompleteListener(new OnCompleteListener<Void>() {
								@Override
								public void onComplete(@NonNull Task<Void> task) {
									if (task.isSuccessful()) {
										Toast.makeText(RegisterActivity.this, "Account Created!", Toast.LENGTH_LONG).show();
										finish(); // Go back to Login
										} else {
										Toast.makeText(RegisterActivity.this, "Database Error", Toast.LENGTH_SHORT).show();
									}
								}
							});
							
							} else {
							Toast.makeText(RegisterActivity.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
						}
					}
				});
			}
		});
		
		tvLoginLink.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}
}