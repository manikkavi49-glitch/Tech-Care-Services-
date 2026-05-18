package com.techcare.services;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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

public class AdminUserListActivity extends Activity {
	
	private ListView listView;
	private DatabaseReference mDatabase;
	private List<UserItem> userList;
	private UserAdapter adapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_admin_user_list);
		
		listView = findViewById(R.id.listViewUsers);
		userList = new ArrayList<>();
		adapter = new UserAdapter(this, userList);
		listView.setAdapter(adapter);
		
		mDatabase = FirebaseDatabase.getInstance("https://techcare-services-default-rtdb.firebaseio.com/").getReference();
		
		// Load Users
		mDatabase.child("Users").addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				userList.clear();
				for (DataSnapshot postSnapshot : snapshot.getChildren()) {
					String uid = postSnapshot.getKey();
					String name = postSnapshot.child("fullName").getValue(String.class);
					String phone = postSnapshot.child("phone").getValue(String.class);
					String email = postSnapshot.child("email").getValue(String.class);
					// 1. Get the photo string
					String photo = postSnapshot.child("profileImage").getValue(String.class);
					
					if (name == null) name = "Unknown User";
					if (phone == null) phone = "No Phone";
					if (email == null) email = "No Email";
					
					// 2. Add to list
					userList.add(new UserItem(uid, name, phone, email, photo));
				}
				adapter.notifyDataSetChanged();
			}
			@Override
			public void onCancelled(@NonNull DatabaseError error) { }
		});
		
		// Click to Delete
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				UserItem user = userList.get(position);
				
				new AlertDialog.Builder(AdminUserListActivity.this)
				.setTitle("Delete User?")
				.setMessage("Permanently remove " + user.name + "?")
				.setPositiveButton("DELETE", (dialog, which) -> {
					mDatabase.child("Users").child(user.uid).removeValue();
					Toast.makeText(AdminUserListActivity.this, "User Deleted", Toast.LENGTH_SHORT).show();
				})
				.setNegativeButton("Cancel", null)
				.show();
			}
		});
	}
	
	// --- DATA CLASS ---
	public static class UserItem {
		public String uid, name, phone, email, photo;
		
		public UserItem(String uid, String name, String phone, String email, String photo) {
			this.uid = uid;
			this.name = name;
			this.phone = phone;
			this.email = email;
			this.photo = photo;
		}
	}
	
	// --- ADAPTER ---
	public class UserAdapter extends ArrayAdapter<UserItem> {
		public UserAdapter(Context context, List<UserItem> list) {
			super(context, 0, list);
		}
		
		@NonNull
		@Override
		public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
			if (convertView == null) {
				convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_admin_user, parent, false);
			}
			
			UserItem user = getItem(position);
			
			TextView tvName = convertView.findViewById(R.id.tvUserName);
			TextView tvContact = convertView.findViewById(R.id.tvUserContact);
			ImageView imgPhoto = convertView.findViewById(R.id.imgUserPhoto);
			
			tvName.setText(user.name);
			tvContact.setText("📞 " + user.phone + "\n📧 " + user.email);
			
			// 3. Decode and Show Photo
			if (user.photo != null && !user.photo.isEmpty()) {
				try {
					byte[] decodedString = Base64.decode(user.photo, Base64.DEFAULT);
					Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
					imgPhoto.setImageBitmap(decodedByte);
					} catch (Exception e) {
					// If error, show default
					imgPhoto.setImageResource(android.R.drawable.sym_def_app_icon);
				}
				} else {
				// No photo, show default
				imgPhoto.setImageResource(android.R.drawable.sym_def_app_icon);
			}
			
			return convertView;
		}
	}
}