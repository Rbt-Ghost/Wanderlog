package com.example.wanderlog;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class TripDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_trip_detail);
        View root = findViewById(R.id.tripDetailRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        int index = getIntent().getIntExtra(TripStore.EXTRA_TRIP_INDEX, -1);
        Trip trip = TripStore.getTrip(index);
        if (trip == null) {
            finish();
            return;
        }
        ((TextView) findViewById(R.id.tripDetailBack)).setOnClickListener(view -> finish());
        ((TextView) findViewById(R.id.tripDetailTitle)).setText(trip.getTitle());
        ((TextView) findViewById(R.id.tripDetailLocation)).setText(trip.getLocation());
        ((TextView) findViewById(R.id.tripDetailDescription)).setText(trip.getDescription());
        ((TextView) findViewById(R.id.tripDetailPhotosLabel))
                .setText(getString(R.string.photos_count, trip.getPhotos().size()));

        LinearLayout gallery = findViewById(R.id.tripPhotoGallery);
        int width = (int) (280 * getResources().getDisplayMetrics().density);
        int height = (int) (250 * getResources().getDisplayMetrics().density);
        int margin = (int) (12 * getResources().getDisplayMetrics().density);
        for (Uri photo : trip.getPhotos()) {
            ImageView image = new ImageView(this);
            image.setBackgroundColor(0xFFE8E8E8);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setContentDescription(getString(R.string.trip_photo));
            try { image.setImageURI(photo); }
            catch (RuntimeException exception) {
                image.setImageResource(android.R.drawable.ic_menu_gallery);
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
            params.setMarginEnd(margin);
            gallery.addView(image, params);
            image.setOnClickListener(view -> showFullScreenPhoto(photo));
        }
    }

    private void showFullScreenPhoto(Uri photo) {
        Dialog dialog = new Dialog(this);
        ImageView image = new ImageView(this);
        image.setBackgroundColor(Color.BLACK);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setContentDescription(getString(R.string.trip_photo));
        try { image.setImageURI(photo); }
        catch (RuntimeException exception) {
            image.setImageResource(android.R.drawable.ic_menu_gallery);
        }
        image.setOnClickListener(view -> dialog.dismiss());
        dialog.setContentView(image);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
        }
    }
}
