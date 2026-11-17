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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int MENU_EDIT = 1;
    private static final int MENU_DELETE = 2;
    private LinearLayout tripListContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_main);
        View main = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(main, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        tripListContainer = findViewById(R.id.tripListContainer);
        refreshTrips();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tripListContainer != null) refreshTrips();
    }

    private void refreshTrips() {
        tripListContainer.removeAllViews();
        List<Trip> trips = TripStore.getTrips();
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
            final int index = i;
            Trip trip = trips.get(i);
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
            row.setOnClickListener(view -> openTripDetails(index));
            row.findViewById(R.id.tripMenuButton).setOnClickListener(
                    view -> showTripMenu(view, index));
            tripListContainer.addView(row);
        }
    }

    private void showTripMenu(View anchor, int index) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, MENU_EDIT, 0, R.string.edit_trip);
        popup.getMenu().add(0, MENU_DELETE, 1, R.string.delete_trip);
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == MENU_EDIT) {
                Intent intent = new Intent(this, AddTripActivity.class);
                intent.putExtra(TripStore.EXTRA_TRIP_INDEX, index);
                startActivity(intent);
                return true;
            }
            if (item.getItemId() == MENU_DELETE) {
                confirmDeleteTrip(index);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void confirmDeleteTrip(int index) {
        Trip trip = TripStore.getTrip(index);
        if (trip == null) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_trip_title)
                .setMessage(getString(R.string.delete_trip_confirmation, trip.getTitle()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete_trip, (dialog, which) -> {
                    TripStore.deleteTrip(index);
                    refreshTrips();
                })
                .show();
    }

    private void openTripDetails(int index) {
        Intent intent = new Intent(this, TripDetailActivity.class);
        intent.putExtra(TripStore.EXTRA_TRIP_INDEX, index);
        startActivity(intent);
    }

    public void GoToAddTripActivity(View view) {
        startActivity(new Intent(this, AddTripActivity.class));
    }

    public void GoToProfileActivity(View view) {
        startActivity(new Intent(this, ProfileActivity.class));
    }
}
