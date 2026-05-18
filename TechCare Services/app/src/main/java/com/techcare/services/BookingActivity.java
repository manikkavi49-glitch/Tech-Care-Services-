package com.techcare.services;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class BookingActivity extends Activity {
	
	// UI Variables
	private EditText etDevice, etIssue, etAddress, etDate, etTime;
	private Spinner spServiceType;
	private RadioGroup rgServiceMethod;
	private RadioButton rbPickup, rbDropoff;
	private ImageView imgPreview;
	private Button btnUpload, btnSubmit;
	
	// Logic Variables
	private DatabaseReference mDatabase;
	private String userId;
	private String encodedImage = "";
	private static final int PICK_IMAGE_REQUEST = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Link to the new professional XML
		setContentView(R.layout.activity_booking);
		
		// --- 1. INITIALIZE VIEWS ---
		etDevice = findViewById(R.id.etDeviceType);
		etIssue = findViewById(R.id.etIssue);
		etAddress = findViewById(R.id.etAddress);
		etDate = findViewById(R.id.etDate);
		etTime = findViewById(R.id.etTime);
		spServiceType = findViewById(R.id.spServiceType);
		rgServiceMethod = findViewById(R.id.rgServiceMethod);
		rbPickup = findViewById(R.id.rbPickup);
		rbDropoff = findViewById(R.id.rbDropoff);
		imgPreview = findViewById(R.id.imgPreview);
		btnUpload = findViewById(R.id.btnUploadPhoto);
		btnSubmit = findViewById(R.id.btnSubmitBooking);
		
		// --- 2. SETUP DATA ---
		
		// Auto-fill Device Type from previous screen
		String incomingDevice = getIntent().getStringExtra("DEVICE_TYPE");
		if (incomingDevice != null) {
			etDevice.setText(incomingDevice);
		}
		
		// Setup Spinner
		String[] services = {"Diagnosis", "Screen Replacement", "Battery Replacement", "Software Issue", "Water Damage", "Other"};
		ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, services);
		spServiceType.setAdapter(adapter);
		
		// --- 3. LISTENERS ---
		
		// Date Picker
		etDate.setOnClickListener(v -> showDatePicker());
		
		// Time Picker
		etTime.setOnClickListener(v -> showTimePicker());
		
		// Photo Upload
		btnUpload.setOnClickListener(v -> openGallery());
		
		// Address Visibility Logic
		rgServiceMethod.setOnCheckedChangeListener((group, checkedId) -> {
			if (checkedId == R.id.rbDropoff) {
				etAddress.setVisibility(View.GONE);
				} else {
				etAddress.setVisibility(View.VISIBLE);
			}
		});
		
		// Submit Logic
		btnSubmit.setOnClickListener(v -> submitBooking());
		
		// --- 4. FIREBASE INIT ---
		mDatabase = FirebaseDatabase.getInstance("https://techcare-services-default-rtdb.firebaseio.com/").getReference();
		if (FirebaseAuth.getInstance().getCurrentUser() != null) {
			userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
		}
	}
	
	// --- PHOTO LOGIC ---
	private void openGallery() {
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
			try {
				Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
				imgPreview.setImageBitmap(bitmap);
				imgPreview.setVisibility(View.VISIBLE);
				
				Bitmap resized = Bitmap.createScaledBitmap(bitmap, 400, 400, true);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				resized.compress(Bitmap.CompressFormat.JPEG, 70, baos);
				encodedImage = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
				
				} catch (IOException e) {
				Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	// --- SUBMIT LOGIC ---
	private void submitBooking() {
		String device = etDevice.getText().toString();
		String serviceType = spServiceType.getSelectedItem().toString();
		String issue = etIssue.getText().toString();
		String date = etDate.getText().toString();
		String time = etTime.getText().toString();
		String address = etAddress.getText().toString();
		String method = "Pickup";
		
		if (rbDropoff.isChecked()) {
			method = "Drop-off";
			address = "Customer Drop-off (No Address)";
			} else {
			if (address.isEmpty()) {
				Toast.makeText(this, "Please enter pickup address", Toast.LENGTH_SHORT).show();
				return;
			}
		}
		
		if (issue.isEmpty() || date.isEmpty() || time.isEmpty()) {
			Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
			return;
		}
		
		Map<String, Object> booking = new HashMap<>();
		booking.put("device", device);
		booking.put("serviceType", serviceType);
		booking.put("issue", issue);
		booking.put("address", address);
		booking.put("method", method);
		booking.put("date", date);
		booking.put("time", time);
		booking.put("status", "Pending");
		booking.put("userId", userId);
		
		if (!encodedImage.isEmpty()) {
			booking.put("photo", encodedImage);
		}
		
		String bookingId = mDatabase.child("Bookings").push().getKey();
		
		if (bookingId != null) {
			mDatabase.child("Bookings").child(bookingId).setValue(booking, new DatabaseReference.CompletionListener() {
				@Override
				public void onComplete(DatabaseError error, @NonNull DatabaseReference ref) {
					if (error == null) {
						Toast.makeText(BookingActivity.this, "Booking Confirmed!", Toast.LENGTH_LONG).show();
						showNotification();
						finish();
						} else {
						Toast.makeText(BookingActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
					}
				}
			});
		}
	}
	
	// --- HELPERS ---
	private void showDatePicker() {
		final Calendar c = Calendar.getInstance();
		new DatePickerDialog(this, (view, year, month, dayOfMonth) ->
		etDate.setText(dayOfMonth + "/" + (month + 1) + "/" + year),
		c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
	}
	
	private void showTimePicker() {
		final Calendar c = Calendar.getInstance();
		new TimePickerDialog(this, (view, hourOfDay, minute) -> {
			String amPm = (hourOfDay >= 12) ? "PM" : "AM";
			int currentHour = (hourOfDay > 12) ? (hourOfDay - 12) : hourOfDay;
			if (currentHour == 0) currentHour = 12;
			String minStr = (minute < 10) ? "0" + minute : String.valueOf(minute);
			etTime.setText(currentHour + ":" + minStr + " " + amPm);
		}, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
	}
	
	private void showNotification() {
		String channelId = "booking_channel";
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			manager.createNotificationChannel(new NotificationChannel(channelId, "Booking Updates", NotificationManager.IMPORTANCE_HIGH));
		}
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
		.setSmallIcon(android.R.drawable.ic_dialog_info)
		.setContentTitle("Booking Received!")
		.setContentText("Your repair request has been sent.")
		.setPriority(NotificationCompat.PRIORITY_HIGH)
		.setAutoCancel(true);
		manager.notify(1, builder.build());
	}
}