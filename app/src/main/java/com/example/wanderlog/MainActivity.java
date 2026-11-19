package com.example.wanderlog;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
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
import com.google.firebase.auth.FirebaseAuth;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int MENU_EDIT = 1;
    private static final int MENU_DELETE = 2;
    private LinearLayout tripListContainer;
    private TextView tripCountLabel;
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
        tripCountLabel = findViewById(R.id.tripCountLabel);
        ((TextView) findViewById(R.id.profileButton)).setText(
                UserInitial.of(FirebaseAuth.getInstance().getCurrentUser()));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (tripListContainer != null) {
            TripStore.startListening(this, tripsListener);
            ProfilePhotoStore.load(this, this::showAvatar);
        }
    }

    private void showAvatar(Bitmap photo) {
        if (isFinishing() || isDestroyed()) return;
        ImageView image = findViewById(R.id.profileButtonImage);
        image.setVisibility(photo == null ? View.GONE : View.VISIBLE);
        if (photo != null) image.setImageBitmap(photo);
    }

    @Override
    protected void onStop() {
        TripStore.stopListening(tripsListener);
        super.onStop();
    }

    private void renderTrips(List<Trip> trips) {
        tripListContainer.removeAllViews();
        tripCountLabel.setText(getResources().getQuantityString(
                R.plurals.trip_count, trips.size(), trips.size()));
        if (trips.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.trip_list_empty);
            empty.setTextAppearance(R.style.TextAppearance_Wl_Body);
            empty.setPadding(dp(20), dp(24), dp(20), dp(24));
            tripListContainer.addView(empty, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < trips.size(); i++) {
            Trip trip = trips.get(i);
            String tripId = trip.getId();
            View row = inflater.inflate(R.layout.item_trip, tripListContainer, false);
            View divider = row.findViewById(R.id.tripItemDivider);
            if (divider != null && i == trips.size() - 1) divider.setVisibility(View.GONE);
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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
