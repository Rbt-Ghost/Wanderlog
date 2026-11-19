package com.example.wanderlog;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ProfileActivity extends AppCompatActivity {
    private LinearLayout recentTripsContainer;
    private TextView activityStatus;
    private TextView tripCount;
    private TextView destinationCount;
    private TextView photoCount;
    private TextView avatarInitial;
    private ImageView avatarImage;

    private final ActivityResultLauncher<PickVisualMediaRequest> avatarPicker =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri == null) return;
                ProfilePhotoStore.save(this, uri, success -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (success) {
                        ProfilePhotoStore.load(this, this::showAvatar);
                    } else {
                        Toast.makeText(this, R.string.profile_photo_failed,
                                Toast.LENGTH_LONG).show();
                    }
                });
            });

    private final TripStore.TripsListener tripsListener = new TripStore.TripsListener() {
        @Override
        public void onTripsChanged(List<Trip> trips) {
            if (isFinishing() || isDestroyed()) return;
            renderActivity(trips);
        }

        @Override
        public void onError(Exception exception) {
            if (isFinishing() || isDestroyed()) return;
            activityStatus.setVisibility(View.VISIBLE);
            activityStatus.setText(R.string.profile_activity_load_failed);
            Toast.makeText(ProfileActivity.this, R.string.trips_load_failed,
                    Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            openLogin();
            return;
        }

        TextView username = findViewById(R.id.profileUsername);
        TextView email = findViewById(R.id.profileEmail);
        username.setText(user.getDisplayName() == null || user.getDisplayName().isEmpty()
                ? getString(R.string.profile_default_name) : user.getDisplayName());
        email.setText(user.getEmail() == null ? "" : user.getEmail());

        avatarInitial = findViewById(R.id.profileAvatar);
        avatarInitial.setText(UserInitial.of(user));
        avatarImage = findViewById(R.id.profileAvatarImage);
        findViewById(R.id.profileAvatarFrame).setOnClickListener(this::showAvatarMenu);
        findViewById(R.id.profileBack).setOnClickListener(view -> finish());
        tripCount = findViewById(R.id.profileTripCount);
        destinationCount = findViewById(R.id.profileDestinationCount);
        photoCount = findViewById(R.id.profilePhotoCount);
        activityStatus = findViewById(R.id.profileActivityStatus);
        recentTripsContainer = findViewById(R.id.profileRecentTrips);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (recentTripsContainer != null) {
            TripStore.startListening(this, tripsListener);
            ProfilePhotoStore.load(this, this::showAvatar);
        }
    }

    private void showAvatar(Bitmap photo) {
        if (isFinishing() || isDestroyed()) return;
        avatarImage.setVisibility(photo == null ? View.GONE : View.VISIBLE);
        avatarInitial.setVisibility(photo == null ? View.VISIBLE : View.GONE);
        if (photo != null) avatarImage.setImageBitmap(photo);
    }

    private void showAvatarMenu(View anchor) {
        if (avatarImage.getVisibility() != View.VISIBLE) {
            pickAvatar();
            return;
        }
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, R.string.choose_new_photo);
        popup.getMenu().add(0, 2, 1, R.string.remove_photo);
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                pickAvatar();
            } else {
                ProfilePhotoStore.remove(this, success -> showAvatar(null));
            }
            return true;
        });
        popup.show();
    }

    private void pickAvatar() {
        avatarPicker.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
    }

    @Override
    protected void onStop() {
        TripStore.stopListening(tripsListener);
        super.onStop();
    }

    private void renderActivity(List<Trip> trips) {
        Set<String> destinations = new HashSet<>();
        int photos = 0;
        for (Trip trip : trips) {
            String location = trip.getLocation();
            if (location != null && !location.trim().isEmpty()) {
                destinations.add(location.trim().toLowerCase(Locale.ROOT));
            }
            photos += trip.getPhotos().size();
        }

        tripCount.setText(String.valueOf(trips.size()));
        destinationCount.setText(String.valueOf(destinations.size()));
        photoCount.setText(String.valueOf(photos));
        recentTripsContainer.removeAllViews();

        if (trips.isEmpty()) {
            activityStatus.setVisibility(View.VISIBLE);
            activityStatus.setText(R.string.profile_activity_empty);
            return;
        }

        activityStatus.setVisibility(View.GONE);
        int recentCount = Math.min(3, trips.size());
        for (int i = 0; i < recentCount; i++) {
            Trip trip = trips.get(i);
            recentTripsContainer.addView(createRecentTripRow(trip));
        }
    }

    private View createRecentTripRow(Trip trip) {
        View row = LayoutInflater.from(this).inflate(
                R.layout.item_trip_compact, recentTripsContainer, false);
        ((TextView) row.findViewById(R.id.recentTitle)).setText(
                trip.getTitle() == null || trip.getTitle().trim().isEmpty()
                        ? getString(R.string.untitled_trip) : trip.getTitle());
        TextView location = row.findViewById(R.id.recentLocation);
        if (trip.getLocation() != null && !trip.getLocation().trim().isEmpty()) {
            location.setText(trip.getLocation().trim());
        } else {
            location.setVisibility(View.GONE);
        }
        ImageView image = row.findViewById(R.id.recentImage);
        if (!trip.getPhotos().isEmpty()) {
            try { image.setImageURI(trip.getPhotos().get(0)); }
            catch (RuntimeException exception) {
                image.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }
        row.setOnClickListener(view -> {
            Intent intent = new Intent(this, TripDetailActivity.class);
            intent.putExtra(TripStore.EXTRA_TRIP_ID, trip.getId());
            startActivity(intent);
        });
        return row;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    public void LogOutBtn(View view) {
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(this, R.string.logged_out, Toast.LENGTH_SHORT).show();
        openLogin();
    }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
