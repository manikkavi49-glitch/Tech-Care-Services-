package com.techcare.services;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends Activity {
	
	private ListView listView;
	private DatabaseReference mDatabase;
	private List<Booking> bookingList;
	private BookingAdapter adapter;
	
	// POPUP VIEWS
	private FrameLayout fullImageOverlay;
	private ImageView imgFullSize;
	private Button btnCloseFullImage;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_history);
		
		// 1. Init List
		listView = findViewById(R.id.listViewHistory);
		bookingList = new ArrayList<>();
		adapter = new BookingAdapter(this, bookingList);
		listView.setAdapter(adapter);
		
		// 2. Init Popup Views
		fullImageOverlay = findViewById(R.id.fullImageOverlay);
		imgFullSize = findViewById(R.id.imgFullSize);
		btnCloseFullImage = findViewById(R.id.btnCloseFullImage);
		
		// 3. Close Popup Logic
		btnCloseFullImage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				fullImageOverlay.setVisibility(View.GONE);
			}
		});
		
		// Also close if clicking the dark background
		fullImageOverlay.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				fullImageOverlay.setVisibility(View.GONE);
			}
		});
		
		// 4. Load Data
		mDatabase = FirebaseDatabase.getInstance("https://techcare-services-default-rtdb.firebaseio.com/").getReference();
		String currentUserId = "";
		if (FirebaseAuth.getInstance().getCurrentUser() != null) {
			currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
		}
		
		if (!currentUserId.isEmpty()) {
			mDatabase.child("Bookings").orderByChild("userId").equalTo(currentUserId)
			.addValueEventListener(new ValueEventListener() {
				@Override
				public void onDataChange(@NonNull DataSnapshot snapshot) {
					bookingList.clear();
					for (DataSnapshot postSnapshot : snapshot.getChildren()) {
						String device = postSnapshot.child("device").getValue(String.class);
						String serviceType = postSnapshot.child("serviceType").getValue(String.class);
						String issue = postSnapshot.child("issue").getValue(String.class);
						String address = postSnapshot.child("address").getValue(String.class);
						String method = postSnapshot.child("method").getValue(String.class);
						String date = postSnapshot.child("date").getValue(String.class);
						String time = postSnapshot.child("time").getValue(String.class);
						String status = postSnapshot.child("status").getValue(String.class);
						String photo = postSnapshot.child("photo").getValue(String.class);
						
						bookingList.add(new Booking(device, serviceType, issue, address, method, date, time, status, photo));
					}
					adapter.notifyDataSetChanged();
				}
				@Override
				public void onCancelled(@NonNull DatabaseError error) { }
			});
		}
	}
	
	// --- Data Model ---
	public static class Booking {
		public String device, serviceType, issue, address, method, date, time, status, photo;
		
		public Booking(String device, String serviceType, String issue, String address, String method, String date, String time, String status, String photo) {
			this.device = device;
			this.serviceType = serviceType;
			this.issue = issue;
			this.address = address;
			this.method = method;
			this.date = date;
			this.time = time;
			this.status = status;
			this.photo = photo;
		}
	}
	
	// --- Adapter ---
	public class BookingAdapter extends ArrayAdapter<Booking> {
		public BookingAdapter(Context context, List<Booking> bookings) {
			super(context, 0, bookings);
		}
		
		@NonNull
		@Override
		public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
			if (convertView == null) {
				convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_booking, parent, false);
			}
			
			final Booking booking = getItem(position);
			
			TextView tvDevice = convertView.findViewById(R.id.tvHistoryDevice);
			TextView tvStatus = convertView.findViewById(R.id.tvHistoryStatus);
			TextView tvServiceType = convertView.findViewById(R.id.tvHistoryServiceType);
			TextView tvDate = convertView.findViewById(R.id.tvHistoryDate);
			TextView tvMethod = convertView.findViewById(R.id.tvHistoryMethod);
			TextView tvAddress = convertView.findViewById(R.id.tvHistoryAddress);
			TextView tvIssue = convertView.findViewById(R.id.tvHistoryIssue);
			ImageView imgPhoto = convertView.findViewById(R.id.imgHistoryPhoto);
			
			tvDevice.setText(booking.device);
			tvServiceType.setText("Service: " + booking.serviceType);
			tvDate.setText("📅 " + booking.date + " at " + booking.time);
			tvMethod.setText("📍 Method: " + booking.method);
			tvIssue.setText(booking.issue);
			
			if ("Drop-off".equals(booking.method)) {
				tvAddress.setVisibility(View.GONE);
				} else {
				tvAddress.setVisibility(View.VISIBLE);
				tvAddress.setText("🏠 " + booking.address);
			}
			
			// --- PHOTO LOGIC ---
			if (booking.photo != null && !booking.photo.isEmpty()) {
				try {
					byte[] decodedString = Base64.decode(booking.photo, Base64.DEFAULT);
					final Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
					
					imgPhoto.setImageBitmap(decodedByte);
					imgPhoto.setVisibility(View.VISIBLE);
					
					// ON CLICK: Show Full Image
					imgPhoto.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							imgFullSize.setImageBitmap(decodedByte);
							fullImageOverlay.setVisibility(View.VISIBLE);
						}
					});
					
					} catch (Exception e) {
					imgPhoto.setVisibility(View.GONE);
				}
				} else {
				imgPhoto.setVisibility(View.GONE);
			}
			
			tvStatus.setText(booking.status);
			if ("Completed".equals(booking.status)) {
				tvStatus.setTextColor(Color.parseColor("#4CAF50"));
				tvStatus.setBackgroundColor(Color.parseColor("#E8F5E9"));
				} else if ("Approved".equals(booking.status)) {
				tvStatus.setTextColor(Color.parseColor("#2196F3"));
				tvStatus.setBackgroundColor(Color.parseColor("#E3F2FD"));
				} else {
				tvStatus.setTextColor(Color.parseColor("#FF9800"));
				tvStatus.setBackgroundColor(Color.parseColor("#FFF3E0"));
			}
			
			return convertView;
		}
	}
}