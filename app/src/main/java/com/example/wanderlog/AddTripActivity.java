package com.example.wanderlog;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import java.util.ArrayList;
import java.util.List;

public class AddTripActivity extends AppCompatActivity {
    private EditText tripTitle, tripDescription, tripLocation;
    private int editingTripIndex = -1;
    private final List<Uri> selectedImages = new ArrayList<>();
    private ImageView[] imageViews;
    private Button[] removeButtons;

    private final ActivityResultLauncher<PickVisualMediaRequest> imagePicker =
            registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(3), uris -> {
                if (uris == null) return;
                for (Uri uri : uris) {
                    if (selectedImages.size() >= 3) break;
                    if (!selectedImages.contains(uri)) selectedImages.add(uri);
                }
                updateImagePreviews();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_add_trip);
        tripTitle = findViewById(R.id.tripTitle);
        tripDescription = findViewById(R.id.tripDescription);
        tripLocation = findViewById(R.id.tripLocation);
        imageViews = new ImageView[]{findViewById(R.id.image1), findViewById(R.id.image2),
                findViewById(R.id.image3)};
        removeButtons = new Button[]{findViewById(R.id.remove1), findViewById(R.id.remove2),
                findViewById(R.id.remove3)};
        Button uploadButton = findViewById(R.id.uploadImageButton);
        Button saveButton = findViewById(R.id.saveTripButton);

        editingTripIndex = getIntent().getIntExtra(TripStore.EXTRA_TRIP_INDEX, -1);
        Trip existing = TripStore.getTrip(editingTripIndex);
        if (existing != null) {
            tripTitle.setText(existing.getTitle());
            tripDescription.setText(existing.getDescription());
            tripLocation.setText(existing.getLocation());
            selectedImages.addAll(existing.getPhotos());
            saveButton.setText(R.string.update_trip);
        } else {
            editingTripIndex = -1;
        }

        uploadButton.setOnClickListener(view -> {
            if (selectedImages.size() >= 3) {
                Toast.makeText(this, R.string.maximum_pictures, Toast.LENGTH_SHORT).show();
                return;
            }
            imagePicker.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
        });
        for (int i = 0; i < removeButtons.length; i++) {
            final int index = i;
            removeButtons[i].setOnClickListener(view -> {
                if (index < selectedImages.size()) {
                    selectedImages.remove(index);
                    updateImagePreviews();
                }
            });
        }
        saveButton.setOnClickListener(view -> saveTrip());
        updateImagePreviews();
    }

    private void updateImagePreviews() {
        for (int i = 0; i < imageViews.length; i++) {
            if (i < selectedImages.size()) {
                try { imageViews[i].setImageURI(selectedImages.get(i)); }
                catch (RuntimeException exception) {
                    imageViews[i].setImageResource(android.R.drawable.ic_menu_gallery);
                }
                removeButtons[i].setVisibility(View.VISIBLE);
            } else {
                imageViews[i].setImageDrawable(null);
                removeButtons[i].setVisibility(View.GONE);
            }
        }
    }

    private void saveTrip() {
        String title = tripTitle.getText().toString().trim();
        String description = tripDescription.getText().toString().trim();
        String location = tripLocation.getText().toString().trim();
        if (title.isEmpty()) {
            tripTitle.setError(getString(R.string.insert_trip_title));
            tripTitle.requestFocus();
            return;
        }
        if (description.isEmpty()) {
            tripDescription.setError(getString(R.string.insert_trip_description));
            tripDescription.requestFocus();
            return;
        }
        if (location.isEmpty()) {
            tripLocation.setError(getString(R.string.insert_trip_location));
            tripLocation.requestFocus();
            return;
        }
        if (selectedImages.isEmpty()) {
            Toast.makeText(this, R.string.add_at_least_one_picture, Toast.LENGTH_SHORT).show();
            return;
        }
        boolean updating = editingTripIndex >= 0;
        TripStore.saveTrip(editingTripIndex, new Trip(title, description, location, selectedImages));
        Toast.makeText(this, updating ? R.string.trip_updated : R.string.trip_added,
                Toast.LENGTH_SHORT).show();
        finish();
    }
}
