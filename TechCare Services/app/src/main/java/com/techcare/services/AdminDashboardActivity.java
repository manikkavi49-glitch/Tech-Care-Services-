package com.techcare.services;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AdminDashboardActivity extends Activity {
	
	private ListView listView;
	private DatabaseReference mDatabase;
	private List<AdminBooking> bookingList;
	private AdminAdapter adapter;
	
	// ZOOM VARS
	private FrameLayout fullImageOverlay;
	private ImageView imgFullSize;
	private Button btnCloseFullImage, btnZoomIn, btnZoomOut;
	private ScaleGestureDetector scaleGestureDetector;
	private float mScaleFactor = 1.0f;
	private Matrix matrix = new Matrix();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_admin_dashboard);
		
		// 1. Initialize Views
		listView = findViewById(R.id.listViewAdmin);
		Button btnLogout = findViewById(R.id.btnAdminLogout);
		Button btnManageUsers = findViewById(R.id.btnManageUsers);
		
		fullImageOverlay = findViewById(R.id.fullImageOverlay);
		imgFullSize = findViewById(R.id.imgFullSize);
		btnCloseFullImage = findViewById(R.id.btnCloseFullImage);
		btnZoomIn = findViewById(R.id.btnZoomIn);
		btnZoomOut = findViewById(R.id.btnZoomOut);
		
		scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
		
		// 2. Setup List
		bookingList = new ArrayList<>();
		adapter = new AdminAdapter(this, bookingList);
		listView.setAdapter(adapter);
		
		// 3. Load Data
		mDatabase = FirebaseDatabase.getInstance("https://techcare-services-default-rtdb.firebaseio.com/").getReference();
		mDatabase.child("Bookings").addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				bookingList.clear();
				for (DataSnapshot postSnapshot : snapshot.getChildren()) {
					String id = postSnapshot.getKey();
					String device = postSnapshot.child("device").getValue(String.class);
					String issue = postSnapshot.child("issue").getValue(String.class);
					String status = postSnapshot.child("status").getValue(String.class);
					String date = postSnapshot.child("date").getValue(String.class);
					String time = postSnapshot.child("time").getValue(String.class);
					String address = postSnapshot.child("address").getValue(String.class);
					String serviceType = postSnapshot.child("serviceType").getValue(String.class);
					String method = postSnapshot.child("method").getValue(String.class);
					String photo = postSnapshot.child("photo").getValue(String.class);
					
					bookingList.add(new AdminBooking(id, device, issue, status, date, time, address, serviceType, method, photo));
				}
				adapter.notifyDataSetChanged();
			}
			@Override
			public void onCancelled(@NonNull DatabaseError error) { }
		});
		
		// 4. List Click Action
		listView.setOnItemClickListener((parent, view, position, id) -> {
			AdminBooking selected = bookingList.get(position);
			showUpdateDialog(selected.id);
		});
		
		// 5. Close Overlay
		btnCloseFullImage.setOnClickListener(v -> fullImageOverlay.setVisibility(View.GONE));
		
		// 6. ZOOM BUTTONS
		btnZoomIn.setOnClickListener(v -> applyZoom(1.5f)); // Zoom In
		btnZoomOut.setOnClickListener(v -> applyZoom(0.75f)); // Zoom Out
		
		// 7. TOUCH ZOOM (Pinch)
		imgFullSize.setOnTouchListener((v, event) -> {
			scaleGestureDetector.onTouchEvent(event);
			return true;
		});
		
		// 8. Navigation
		btnManageUsers.setOnClickListener(v -> startActivity(new Intent(AdminDashboardActivity.this, AdminUserListActivity.class)));
		btnLogout.setOnClickListener(v -> {
			startActivity(new Intent(AdminDashboardActivity.this, LoginActivity.class));
			finish();
		});
	}
	
	// --- ZOOM HELPER: Apply Zoom from Buttons ---
	private void applyZoom(float factor) {
		float oldScale = mScaleFactor;
		mScaleFactor *= factor;
		mScaleFactor = Math.max(1.0f, Math.min(mScaleFactor, 5.0f)); // Limit 1x to 5x
		
		float actualFactor = mScaleFactor / oldScale;
		matrix.postScale(actualFactor, actualFactor, imgFullSize.getWidth() / 2f, imgFullSize.getHeight() / 2f);
		
		centerImage();
		imgFullSize.setImageMatrix(matrix);
	}
	
	// --- ZOOM HELPER: Pinch Detection ---
	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			float scaleFactor = detector.getScaleFactor();
			float newScale = mScaleFactor * scaleFactor;
			
			// Prevent zooming out too much or in too much (1x to 5x)
			if (newScale >= 1.0f && newScale <= 5.0f) {
				mScaleFactor = newScale;
				matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
				centerImage();
				imgFullSize.setImageMatrix(matrix);
			}
			return true;
		}
	}
	
	// --- ZOOM HELPER: Keep Image Centered ---
	private void centerImage() {
		if (imgFullSize.getDrawable() == null) return;
		
		RectF drawableRect = new RectF(0, 0, imgFullSize.getDrawable().getIntrinsicWidth(), imgFullSize.getDrawable().getIntrinsicHeight());
		RectF viewRect = new RectF(0, 0, imgFullSize.getWidth(), imgFullSize.getHeight());
		matrix.mapRect(drawableRect);
		
		float deltaX = 0, deltaY = 0;
		
		// Horizontal Centering
		if (drawableRect.width() < viewRect.width()) {
			deltaX = viewRect.centerX() - drawableRect.centerX();
			} else {
			// Keep edges inside screen
			if (drawableRect.left > 0) deltaX = -drawableRect.left;
			if (drawableRect.right < viewRect.width()) deltaX = viewRect.width() - drawableRect.right;
		}
		
		// Vertical Centering
		if (drawableRect.height() < viewRect.height()) {
			deltaY = viewRect.centerY() - drawableRect.centerY();
			} else {
			if (drawableRect.top > 0) deltaY = -drawableRect.top;
			if (drawableRect.bottom < viewRect.height()) deltaY = viewRect.height() - drawableRect.bottom;
		}
		
		matrix.postTranslate(deltaX, deltaY);
	}
	
	// --- DIALOG: Manage Booking ---
	private void showUpdateDialog(final String bookingId) {
		final String[] options = {"Mark Pending", "Mark Approved", "Mark In Progress", "Mark Completed", "❌ DELETE"};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Manage Booking");
		builder.setItems(options, (dialog, which) -> {
			if (which == 4) {
				// Delete
				new AlertDialog.Builder(AdminDashboardActivity.this)
				.setTitle("Delete?")
				.setMessage("Permanently remove this booking?")
				.setPositiveButton("Yes", (d, w) -> {
					mDatabase.child("Bookings").child(bookingId).removeValue();
					Toast.makeText(AdminDashboardActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
				})
				.setNegativeButton("No", null).show();
				} else {
				// Update Status
				String[] statuses = {"Pending", "Approved", "In Progress", "Completed"};
				mDatabase.child("Bookings").child(bookingId).child("status").setValue(statuses[which]);
				Toast.makeText(AdminDashboardActivity.this, "Status Updated", Toast.LENGTH_SHORT).show();
			}
		});
		builder.setNeutralButton("Cancel", null);
		builder.show();
	}
	
	// --- DATA CLASS ---
	public static class AdminBooking {
		public String id, device, issue, status, date, time, address, serviceType, method, photo;
		public AdminBooking(String id, String d, String i, String s, String dt, String tm, String ad, String st, String m, String p) {
			this.id = id; device = d; issue = i; status = s; date = dt; time = tm; address = ad; serviceType = st; method = m; photo = p;
		}
	}
	
	// --- LIST ADAPTER ---
	public class AdminAdapter extends ArrayAdapter<AdminBooking> {
		public AdminAdapter(Context context, List<AdminBooking> list) { super(context, 0, list); }
		
		@NonNull
		@Override
		public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
			if (convertView == null) {
				convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_admin_booking, parent, false);
			}
			
			AdminBooking item = getItem(position);
			
			TextView tvDevice = convertView.findViewById(R.id.tvAdminDevice);
			TextView tvStatus = convertView.findViewById(R.id.tvAdminStatus);
			TextView tvService = convertView.findViewById(R.id.tvAdminService);
			TextView tvMethod = convertView.findViewById(R.id.tvAdminMethod);
			TextView tvDetails = convertView.findViewById(R.id.tvAdminDetails);
			TextView tvIssue = convertView.findViewById(R.id.tvAdminIssue);
			ImageView imgPhoto = convertView.findViewById(R.id.imgAdminPhoto);
			
			tvDevice.setText(item.device);
			tvService.setText("Service: " + (item.serviceType != null ? item.serviceType : "General"));
			tvMethod.setText("Method: " + (item.method != null ? item.method : "Pickup"));
			String addr = (item.address != null && !item.address.isEmpty()) ? item.address : "N/A";
			tvDetails.setText("📅 " + item.date + " @ " + item.time + "\n📍 " + addr);
			tvIssue.setText("Issue: " + item.issue);
			
			// Status Color
			tvStatus.setText(item.status);
			if ("Completed".equals(item.status)) tvStatus.setBackgroundColor(Color.parseColor("#4CAF50"));
			else if ("Approved".equals(item.status)) tvStatus.setBackgroundColor(Color.parseColor("#2196F3"));
			else tvStatus.setBackgroundColor(Color.parseColor("#FF9800"));
			
			if ("Drop-off".equals(item.method)) tvMethod.setTextColor(Color.parseColor("#D32F2F"));
			else tvMethod.setTextColor(Color.parseColor("#4CAF50"));
			
			// --- PHOTO LOGIC (FIXED) ---
			if (item.photo != null && !item.photo.isEmpty()) {
				try {
					byte[] decodedString = Base64.decode(item.photo, Base64.DEFAULT);
					final Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
					imgPhoto.setImageBitmap(decodedByte);
					imgPhoto.setVisibility(View.VISIBLE);
					
					// CLICK TO OPEN POPUP
					imgPhoto.setOnClickListener(v -> {
						imgFullSize.setImageBitmap(decodedByte);
						fullImageOverlay.setVisibility(View.VISIBLE);
						
						// *** CRITICAL FIX: WAIT FOR LAYOUT TO CENTER IMAGE ***
						imgFullSize.post(() -> {
							mScaleFactor = 1.0f;
							matrix.reset();
							
							float viewW = imgFullSize.getWidth();
							float viewH = imgFullSize.getHeight();
							float imgW = decodedByte.getWidth();
							float imgH = decodedByte.getHeight();
							
							// Calculate Scale to Fit
							float scale = Math.min(viewW / imgW, viewH / imgH);
							scale = Math.min(scale, 1.0f); // Don't zoom in initially
							
							// Calculate Center Position
							float scaledW = imgW * scale;
							float scaledH = imgH * scale;
							float dx = (viewW - scaledW) / 2;
							float dy = (viewH - scaledH) / 2;
							
							matrix.postScale(scale, scale);
							matrix.postTranslate(dx, dy);
							imgFullSize.setImageMatrix(matrix);
						});
					});
					
					} catch (Exception e) {
					imgPhoto.setVisibility(View.GONE);
				}
				} else {
				imgPhoto.setVisibility(View.GONE);
			}
			
			return convertView;
		}
	}
}