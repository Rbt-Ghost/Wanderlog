package com.example.wanderlog;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;
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

        tripCount = findViewById(R.id.profileTripCount);
        destinationCount = findViewById(R.id.profileDestinationCount);
        photoCount = findViewById(R.id.profilePhotoCount);
        activityStatus = findViewById(R.id.profileActivityStatus);
        recentTripsContainer = findViewById(R.id.profileRecentTrips);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (recentTripsContainer != null) TripStore.startListening(this, tripsListener);
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
            MaterialCardView card = createRecentTripCard(trip);
            recentTripsContainer.addView(card);
        }
    }

    private MaterialCardView createRecentTripCard(Trip trip) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(10);
        card.setLayoutParams(cardParams);
        card.setClickable(true);
        card.setFocusable(true);
        card.setRadius(dp(16));
        card.setCardElevation(dp(1));
        card.setStrokeWidth(0);
        card.setOnClickListener(view -> {
            Intent intent = new Intent(this, TripDetailActivity.class);
            intent.putExtra(TripStore.EXTRA_TRIP_ID, trip.getId());
            startActivity(intent);
        });

        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setPadding(dp(16), dp(14), dp(16), dp(14));

        TextView title = new TextView(this);
        title.setText(trip.getTitle() == null || trip.getTitle().trim().isEmpty()
                ? getString(R.string.untitled_trip) : trip.getTitle());
        title.setTextSize(16);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        textContainer.addView(title);

        if (trip.getLocation() != null && !trip.getLocation().trim().isEmpty()) {
            TextView location = new TextView(this);
            location.setText(trip.getLocation().trim());
            location.setTextSize(14);
            LinearLayout.LayoutParams locationParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            locationParams.topMargin = dp(4);
            textContainer.addView(location, locationParams);
        }

        card.addView(textContainer);
        return card;
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
