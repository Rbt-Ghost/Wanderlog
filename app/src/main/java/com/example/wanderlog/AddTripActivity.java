package com.example.wanderlog;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.location.LocationManagerCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddTripActivity extends AppCompatActivity {
    private EditText tripTitle, tripDescription, tripLocation;
    private Button saveTripButton;
    private View currentLocationButton;
    private ProgressBar currentLocationProgress;
    private TextView currentLocationStatus;
    private String editingTripId;
    private final List<Uri> selectedImages = new ArrayList<>();
    private ImageView[] imageViews;
    private Button[] removeButtons;
    private final ExecutorService geocoderExecutor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String[]> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (hasLocationPermission()) {
                    getCurrentCity();
                } else {
                    setLocationLoading(false);
                    Toast.makeText(this, R.string.location_permission_required, Toast.LENGTH_LONG).show();
                }
            });

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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.addTripRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        findViewById(R.id.addTripBack).setOnClickListener(view -> finish());
        tripTitle = findViewById(R.id.tripTitle);
        tripDescription = findViewById(R.id.tripDescription);
        tripLocation = findViewById(R.id.tripLocation);
        currentLocationButton = findViewById(R.id.currentLocationButton);
        currentLocationProgress = findViewById(R.id.currentLocationProgress);
        currentLocationStatus = findViewById(R.id.currentLocationStatus);
        imageViews = new ImageView[]{findViewById(R.id.image1), findViewById(R.id.image2),
                findViewById(R.id.image3)};
        removeButtons = new Button[]{findViewById(R.id.remove1), findViewById(R.id.remove2),
                findViewById(R.id.remove3)};
        Button uploadButton = findViewById(R.id.uploadImageButton);
        saveTripButton = findViewById(R.id.saveTripButton);

        editingTripId = getIntent().getStringExtra(TripStore.EXTRA_TRIP_ID);
        Trip existing = TripStore.getTrip(editingTripId);
        if (existing != null) {
            populateTrip(existing);
        } else if (editingTripId != null) {
            saveTripButton.setEnabled(false);
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
        saveTripButton.setOnClickListener(view -> saveTrip());
        currentLocationButton.setOnClickListener(view -> requestCurrentCity());
        updateImagePreviews();

        if (existing == null && editingTripId != null) {
            TripStore.loadTrip(this, editingTripId, (trip, exception) -> runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (exception != null || trip == null) {
                    Toast.makeText(this, R.string.trips_load_failed, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                populateTrip(trip);
            }));
        }
    }

    private void requestCurrentCity() {
        if (hasLocationPermission()) {
            getCurrentCity();
        } else {
            locationPermissionRequest.launch(new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            });
        }
    }

    private boolean hasLocationPermission() {
        return hasFineLocationPermission() || hasCoarseLocationPermission();
    }

    private boolean hasFineLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasCoarseLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void getCurrentCity() {
        setLocationLoading(true);
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null) {
            setLocationLoading(false);
            Toast.makeText(this, R.string.current_location_unavailable, Toast.LENGTH_LONG).show();
            return;
        }
        boolean hasFinePermission = hasFineLocationPermission();
        String provider = hasFinePermission
                ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;
        String fallbackProvider = hasFinePermission && hasCoarseLocationPermission()
                ? LocationManager.NETWORK_PROVIDER : null;

        if (!isProviderEnabled(locationManager, provider)) {
            if (fallbackProvider != null && isProviderEnabled(locationManager, fallbackProvider)) {
                provider = fallbackProvider;
                fallbackProvider = null;
            } else {
                setLocationLoading(false);
                Toast.makeText(this, R.string.location_services_disabled, Toast.LENGTH_LONG).show();
                return;
            }
        }

        requestLocation(locationManager, provider, fallbackProvider);
    }

    private boolean isProviderEnabled(LocationManager locationManager, String provider) {
        try {
            return locationManager.getAllProviders().contains(provider)
                    && locationManager.isProviderEnabled(provider);
        } catch (SecurityException | IllegalArgumentException exception) {
            return false;
        }
    }

    private void requestLocation(LocationManager locationManager, String provider, String fallbackProvider) {
        try {
            LocationManagerCompat.getCurrentLocation(
                    locationManager,
                    provider,
                    new CancellationSignal(),
                    ContextCompat.getMainExecutor(this),
                    location -> {
                        if (isFinishing() || isDestroyed()) return;
                        if (location != null) {
                            findCity(location);
                        } else if (fallbackProvider != null
                                && hasCoarseLocationPermission()
                                && isProviderEnabled(locationManager, fallbackProvider)) {
                            requestLocation(locationManager, fallbackProvider, null);
                        } else {
                            setLocationLoading(false);
                            Toast.makeText(this, R.string.current_location_unavailable, Toast.LENGTH_LONG).show();
                        }
                    });
        } catch (SecurityException | IllegalArgumentException exception) {
            setLocationLoading(false);
            Toast.makeText(this, R.string.current_location_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    private void findCity(Location location) {
        if (!Geocoder.isPresent()) {
            setLocationLoading(false);
            Toast.makeText(this, R.string.city_lookup_unavailable, Toast.LENGTH_LONG).show();
            return;
        }

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1,
                    new Geocoder.GeocodeListener() {
                        @Override
                        public void onGeocode(List<Address> addresses) {
                            runOnUiThread(() -> applyCity(addresses));
                        }

                        @Override
                        public void onError(String errorMessage) {
                            runOnUiThread(() -> showCityNotFound());
                        }
                    });
        } else {
            geocoderExecutor.execute(() -> {
                List<Address> addresses = null;
                try {
                    // The synchronous Geocoder API is deprecated on Android 13+, so it is only used below API 33.
                    addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                } catch (IOException ignored) {
                    // The UI callback below presents the same recovery message for an unavailable geocoder.
                }
                List<Address> result = addresses;
                runOnUiThread(() -> applyCity(result));
            });
        }
    }

    private void applyCity(List<Address> addresses) {
        if (isFinishing() || isDestroyed()) return;
        String city = extractCity(addresses);
        if (city == null) {
            showCityNotFound();
            return;
        }
        tripLocation.setText(city);
        tripLocation.setSelection(city.length());
        setLocationLoading(false);
        Toast.makeText(this, R.string.current_city_added, Toast.LENGTH_SHORT).show();
    }

    private String extractCity(List<Address> addresses) {
        if (addresses == null || addresses.isEmpty()) return null;
        Address address = addresses.get(0);
        String[] candidates = {
                address.getLocality(),
                address.getSubLocality(),
                address.getSubAdminArea(),
                address.getAdminArea()
        };
        for (String candidate : candidates) {
            if (candidate != null && !candidate.trim().isEmpty()) return candidate.trim();
        }
        return null;
    }

    private void showCityNotFound() {
        if (isFinishing() || isDestroyed()) return;
        setLocationLoading(false);
        Toast.makeText(this, R.string.city_not_found, Toast.LENGTH_LONG).show();
    }

    private void setLocationLoading(boolean loading) {
        currentLocationButton.setVisibility(loading ? View.GONE : View.VISIBLE);
        currentLocationButton.setEnabled(!loading);
        currentLocationProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        currentLocationStatus.setVisibility(loading ? View.VISIBLE : View.GONE);
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

    private void populateTrip(Trip trip) {
        tripTitle.setText(trip.getTitle());
        tripDescription.setText(trip.getDescription());
        tripLocation.setText(trip.getLocation());
        selectedImages.clear();
        selectedImages.addAll(trip.getPhotos());
        ((TextView) findViewById(R.id.addTripTitle)).setText(R.string.edit_trip_title);
        saveTripButton.setText(R.string.update_trip);
        saveTripButton.setEnabled(true);
        updateImagePreviews();
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
        boolean updating = editingTripId != null;
        saveTripButton.setEnabled(false);
        Trip trip = new Trip(editingTripId, title, description, location, selectedImages);
        TripStore.saveTrip(this, trip, exception -> runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            saveTripButton.setEnabled(true);
            if (exception != null) {
                Toast.makeText(this, R.string.trip_save_failed, Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(this, updating ? R.string.trip_updated : R.string.trip_added,
                    Toast.LENGTH_SHORT).show();
            finish();
        }));
    }

    @Override
    protected void onDestroy() {
        geocoderExecutor.shutdownNow();
        super.onDestroy();
    }
}
