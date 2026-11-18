package com.example.wanderlog;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int MENU_EDIT = 1;
    private static final int MENU_DELETE = 2;
    private LinearLayout tripListContainer;
    private final TripStore.TripsListener tripsListener = new TripStore.TripsListener() {
        @Override
        public void onTripsChanged(List<Trip> trips) {
            if (tripListContainer != null) renderTrips(trips);
        }

        @Override
        public void onError(Exception exception) {
            if (tripListContainer != null) {
                Toast.makeText(MainActivity.this, R.string.trips_load_failed,
                        Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_main);
        View main = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(main, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        tripListContainer = findViewById(R.id.tripListContainer);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (tripListContainer != null) TripStore.startListening(this, tripsListener);
    }

    @Override
    protected void onStop() {
        TripStore.stopListening(tripsListener);
        super.onStop();
    }

    private void renderTrips(List<Trip> trips) {
        tripListContainer.removeAllViews();
        if (trips.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.trip_list_empty);
            empty.setTextSize(16);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(24, 56, 24, 24);
            tripListContainer.addView(empty, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < trips.size(); i++) {
            Trip trip = trips.get(i);
            String tripId = trip.getId();
            MaterialCardView row = (MaterialCardView) inflater.inflate(
                    R.layout.item_trip, tripListContainer, false);
            TextView title = row.findViewById(R.id.tripItemTitle);
            TextView location = row.findViewById(R.id.tripItemLocation);
            ImageView image = row.findViewById(R.id.tripItemImage);
            title.setText(trip.getTitle());
            location.setText(trip.getLocation());
            if (!trip.getPhotos().isEmpty()) {
                Uri firstPhoto = trip.getPhotos().get(0);
                try { image.setImageURI(firstPhoto); }
                catch (RuntimeException exception) {
                    image.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            }
            row.setOnClickListener(view -> openTripDetails(tripId));
            row.findViewById(R.id.tripMenuButton).setOnClickListener(
                    view -> showTripMenu(view, tripId));
            tripListContainer.addView(row);
        }
    }

    private void showTripMenu(View anchor, String tripId) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, MENU_EDIT, 0, R.string.edit_trip);
        popup.getMenu().add(0, MENU_DELETE, 1, R.string.delete_trip);
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == MENU_EDIT) {
                Intent intent = new Intent(this, AddTripActivity.class);
                intent.putExtra(TripStore.EXTRA_TRIP_ID, tripId);
                startActivity(intent);
                return true;
            }
            if (item.getItemId() == MENU_DELETE) {
                confirmDeleteTrip(tripId);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void confirmDeleteTrip(String tripId) {
        Trip trip = TripStore.getTrip(tripId);
        if (trip == null) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_trip_title)
                .setMessage(getString(R.string.delete_trip_confirmation, trip.getTitle()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete_trip, (dialog, which) -> {
                    TripStore.deleteTrip(tripId, exception -> {
                        if (exception != null) {
                            if (!isFinishing() && !isDestroyed()) {
                                Toast.makeText(this, R.string.trip_delete_failed,
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                })
                .show();
    }

    private void openTripDetails(String tripId) {
        Intent intent = new Intent(this, TripDetailActivity.class);
        intent.putExtra(TripStore.EXTRA_TRIP_ID, tripId);
        startActivity(intent);
    }

    public void GoToAddTripActivity(View view) {
        startActivity(new Intent(this, AddTripActivity.class));
    }

    public void GoToProfileActivity(View view) {
        startActivity(new Intent(this, ProfileActivity.class));
    }
}
