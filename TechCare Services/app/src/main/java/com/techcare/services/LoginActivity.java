package com.techcare.services;

import android.app.Activity;
import android.content.Intent;
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

public class LoginActivity extends Activity {
	
	private FirebaseAuth mAuth;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		
		// Initialize Firebase Auth
		mAuth = FirebaseAuth.getInstance();
		
		// Bind UI Elements
		final EditText etEmail = findViewById(R.id.etEmail);
		final EditText etPassword = findViewById(R.id.etPassword);
		Button btnLogin = findViewById(R.id.btnLogin);
		TextView tvCreateAccount = findViewById(R.id.tvCreateAccount);
		// REMOVED: TextView tvAdminLogin...
		
		// 1. UNIFIED LOGIN BUTTON LOGIC
		btnLogin.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String email = etEmail.getText().toString().trim();
				String password = etPassword.getText().toString().trim();
				
				if (email.isEmpty() || password.isEmpty()) {
					Toast.makeText(LoginActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
					return;
				}
				
				// --- A. CHECK FOR ADMIN CREDENTIALS FIRST ---
				if (email.equals("admin@techcare.com") && password.equals("admin123")) {
					Toast.makeText(LoginActivity.this, "Welcome Admin!", Toast.LENGTH_SHORT).show();
					
					// Redirect to Admin Dashboard
					Intent intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
					// Clear history so Admin can't go back to login
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
					startActivity(intent);
					finish();
					return; // Stop here! Do not check Firebase.
				}
				
				// --- B. REGULAR USER LOGIN (FIREBASE) ---
				mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(LoginActivity.this,
				new OnCompleteListener<AuthResult>() {
					@Override
					public void onComplete(@NonNull Task<AuthResult> task) {
						if (task.isSuccessful()) {
							Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
							
							// Redirect to User Home
							Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
							intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
							startActivity(intent);
							finish();
							} else {
							Toast.makeText(LoginActivity.this,
							"Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT)
							.show();
						}
					}
				});
			}
		});
		
		// 2. CREATE ACCOUNT LINK LOGIC
		tvCreateAccount.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
				startActivity(intent);
			}
		});
	}
}