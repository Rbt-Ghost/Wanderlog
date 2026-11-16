package com.example.wanderlog;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import java.util.ArrayList;
import java.util.List;

public class AddTripActivity extends AppCompatActivity {

    private EditText tripTitle;
    private EditText tripDescription;
    private EditText tripLocation;

    private final List<Uri> selectedImages = new ArrayList<>();

    private ImageView[] imageViews;
    private Button[] removeButtons;

    private final ActivityResultLauncher<PickVisualMediaRequest>
            imagePicker = registerForActivityResult(
            new ActivityResultContracts.PickMultipleVisualMedia(3),
            uris -> {
                for (Uri uri : uris) {
                    if (selectedImages.size() >= 3) break;

                    if (!selectedImages.contains(uri)) {
                        selectedImages.add(uri);
                    }
                }

                updateImagePreviews();
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_add_trip);

        // Formular
        tripTitle = findViewById(R.id.tripTitle);
        tripDescription = findViewById(R.id.tripDescription);
        tripLocation = findViewById(R.id.tripLocation);

        // Imagini
        imageViews = new ImageView[]{
                findViewById(R.id.image1),
                findViewById(R.id.image2),
                findViewById(R.id.image3)
        };

        removeButtons = new Button[]{
                findViewById(R.id.remove1),
                findViewById(R.id.remove2),
                findViewById(R.id.remove3)
        };

        Button uploadButton =
                findViewById(R.id.uploadImageButton);

        Button saveButton =
                findViewById(R.id.saveTripButton);

        // Selectare fotografii
        uploadButton.setOnClickListener(v -> {
            if (selectedImages.size() >= 3) {
                Toast.makeText(
                        this,
                        "You can add MAX 3 pictures",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            imagePicker.launch(
                    new PickVisualMediaRequest.Builder()
                            .setMediaType(
                                    ActivityResultContracts
                                            .PickVisualMedia.ImageOnly
                                            .INSTANCE
                            )
                            .build()
            );
        });

        // Eliminare fotografii
        for (int i = 0; i < removeButtons.length; i++) {
            final int index = i;

            removeButtons[i].setOnClickListener(v -> {
                if (index < selectedImages.size()) {
                    selectedImages.remove(index);
                    updateImagePreviews();
                }
            });
        }

        // Salvare
        saveButton.setOnClickListener(v -> saveTrip());

        updateImagePreviews();
    }

    private void updateImagePreviews() {
        for (int i = 0; i < imageViews.length; i++) {
            if (i < selectedImages.size()) {
                imageViews[i].setImageURI(
                        selectedImages.get(i)
                );
                removeButtons[i].setVisibility(View.VISIBLE);
            } else {
                imageViews[i].setImageDrawable(null);
                removeButtons[i].setVisibility(View.GONE);
            }
        }
    }

    private void saveTrip() {
        String title = tripTitle.getText().toString().trim();
        String description =
                tripDescription.getText().toString().trim();
        String location =
                tripLocation.getText().toString().trim();

        if (title.isEmpty()) {
            tripTitle.setError("Insert Trip Title");
            tripTitle.requestFocus();
            return;
        }

        if (description.isEmpty()) {
            tripDescription.setError("Insert Trip Description");
            tripDescription.requestFocus();
            return;
        }

        if (location.isEmpty()) {
            tripLocation.setError("Insert trip location");
            tripLocation.requestFocus();
            return;
        }

        if (selectedImages.isEmpty()) {
            Toast.makeText(
                    this,
                    "Add at least one picture",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        // Aici vom integra Firebase ulterior.
        Toast.makeText(
                this,
                "Ready to upload",
                Toast.LENGTH_LONG
        ).show();
    }
}
