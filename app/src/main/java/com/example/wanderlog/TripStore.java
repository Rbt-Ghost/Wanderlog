package com.example.wanderlog;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** User-scoped Firestore storage for trips and their photo data. */
public final class TripStore {
    public static final String EXTRA_TRIP_ID = "com.example.wanderlog.TRIP_ID";

    private static final String FIELD_TITLE = "title";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_LOCATION = "location";
    private static final String FIELD_PHOTOS = "photos";
    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String FIELD_UPDATED_AT = "updatedAt";
    private static final int MAX_PHOTO_BYTES = 250 * 1024;
    private static final int MAX_PHOTO_DIMENSION = 1280;
    private static final ExecutorService photoExecutor = Executors.newSingleThreadExecutor();
    private static final List<Trip> trips = new ArrayList<>();
    private static ListenerRegistration registration;
    private static TripsListener activeListener;
    private static String listeningUserId;

    private TripStore() {}

    public interface TripsListener {
        void onTripsChanged(List<Trip> trips);
        void onError(Exception exception);
    }

    public interface CompletionListener {
        void onComplete(Exception exception);
    }

    public interface TripListener {
        void onTripLoaded(Trip trip, Exception exception);
    }

    public static synchronized List<Trip> getTrips() {
        return new ArrayList<>(trips);
    }

    public static synchronized Trip getTrip(String id) {
        if (id == null) return null;
        for (Trip trip : trips) {
            if (id.equals(trip.getId())) return trip;
        }
        return null;
    }

    public static void loadTrip(Context context, String id, TripListener listener) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            listener.onTripLoaded(null, new IllegalStateException("A signed-in user is required."));
            return;
        }
        Context appContext = context.getApplicationContext();
        userTrips(auth.getCurrentUser().getUid()).document(id).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                listener.onTripLoaded(null, task.getException());
                return;
            }
            DocumentSnapshot document = task.getResult();
            if (document == null || !document.exists()) {
                listener.onTripLoaded(null, new IllegalStateException("The trip no longer exists."));
                return;
            }
            try {
                Trip trip = readTrip(appContext, document);
                synchronized (TripStore.class) {
                    trips.removeIf(existing -> id.equals(existing.getId()));
                    trips.add(trip);
                }
                listener.onTripLoaded(trip, null);
            } catch (IOException exception) {
                listener.onTripLoaded(null, exception);
            }
        });
    }

    public static void startListening(Context context, TripsListener listener) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() == null
                ? null : FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userId == null) {
            listener.onError(new IllegalStateException("A signed-in user is required."));
            return;
        }

        synchronized (TripStore.class) {
            if (registration != null) registration.remove();
            registration = null;
            activeListener = listener;
            listeningUserId = userId;
            trips.clear();
        }

        Context appContext = context.getApplicationContext();
        registration = userTrips(userId).orderBy(FIELD_UPDATED_AT, Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    if (snapshot == null) return;

                    List<Trip> loadedTrips = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        try {
                            loadedTrips.add(readTrip(appContext, document));
                        } catch (IOException exception) {
                            listener.onError(exception);
                        }
                    }
                    synchronized (TripStore.class) {
                        if (activeListener != listener || !userId.equals(listeningUserId)) return;
                        trips.clear();
                        trips.addAll(loadedTrips);
                    }
                    listener.onTripsChanged(new ArrayList<>(loadedTrips));
                });
    }

    public static synchronized void stopListening(TripsListener listener) {
        if (activeListener != listener) return;
        if (registration != null) registration.remove();
        registration = null;
        activeListener = null;
        listeningUserId = null;
    }

    public static void saveTrip(Context context, Trip trip, CompletionListener listener) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            listener.onComplete(new IllegalStateException("A signed-in user is required."));
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        CollectionReference collection = userTrips(userId);
        String tripId = trip.getId() == null ? collection.document().getId() : trip.getId();
        Context appContext = context.getApplicationContext();

        photoExecutor.execute(() -> {
            try {
                List<Blob> photos = new ArrayList<>();
                for (int i = 0; i < trip.getPhotos().size(); i++) {
                    photos.add(Blob.fromBytes(readPhotoBytes(appContext, trip.getPhotos().get(i))));
                }
                Map<String, Object> data = new HashMap<>();
                data.put(FIELD_TITLE, trip.getTitle());
                data.put(FIELD_DESCRIPTION, trip.getDescription());
                data.put(FIELD_LOCATION, trip.getLocation());
                data.put(FIELD_PHOTOS, photos);
                data.put(FIELD_UPDATED_AT, com.google.firebase.firestore.FieldValue.serverTimestamp());
                if (trip.getId() == null) {
                    data.put(FIELD_CREATED_AT,
                            com.google.firebase.firestore.FieldValue.serverTimestamp());
                }

                collection.document(tripId).set(data, SetOptions.merge())
                        .addOnCompleteListener(task -> listener.onComplete(
                                task.isSuccessful() ? null : task.getException()));
            } catch (Exception exception) {
                listener.onComplete(exception);
            }
        });
    }

    public static void deleteTrip(String id, CompletionListener listener) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            listener.onComplete(new IllegalStateException("A signed-in user is required."));
            return;
        }
        userTrips(auth.getCurrentUser().getUid()).document(id).delete()
                .addOnCompleteListener(task -> listener.onComplete(
                        task.isSuccessful() ? null : task.getException()));
    }

    private static CollectionReference userTrips(String userId) {
        return FirebaseFirestore.getInstance().collection("users").document(userId)
                .collection("trips");
    }

    private static Trip readTrip(Context context, DocumentSnapshot document) throws IOException {
        List<Uri> photoUris = new ArrayList<>();
        Object photoValue = document.get(FIELD_PHOTOS);
        if (photoValue instanceof List<?>) {
            File photoDirectory = new File(context.getCacheDir(), "trip_photos");
            if (!photoDirectory.exists() && !photoDirectory.mkdirs()) {
                throw new IOException("Unable to create the trip photo cache.");
            }
            int photoIndex = 0;
            for (Object photo : (List<?>) photoValue) {
                if (!(photo instanceof Blob)) continue;
                File photoFile = new File(photoDirectory,
                        document.getId() + "_" + photoIndex++ + ".jpg");
                try (FileOutputStream output = new FileOutputStream(photoFile)) {
                    output.write(((Blob) photo).toBytes());
                }
                photoUris.add(Uri.fromFile(photoFile));
            }
        }
        return new Trip(document.getId(), document.getString(FIELD_TITLE),
                document.getString(FIELD_DESCRIPTION), document.getString(FIELD_LOCATION), photoUris);
    }

    private static byte[] readPhotoBytes(Context context, Uri uri) throws IOException {
        if ("file".equals(uri.getScheme())) {
            String path = uri.getPath();
            if (path != null && path.contains(File.separator + "trip_photos" + File.separator)) {
                File cachedPhoto = new File(path);
                if (cachedPhoto.isFile()) {
                    try (InputStream input = context.getContentResolver().openInputStream(uri)) {
                        return readAllBytes(input);
                    }
                }
            }
        }

        ContentResolver resolver = context.getContentResolver();
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = resolver.openInputStream(uri)) {
            if (input == null) throw new IOException("Unable to open the selected photo.");
            BitmapFactory.decodeStream(input, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("The selected photo could not be read.");
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        while (bounds.outWidth / options.inSampleSize > MAX_PHOTO_DIMENSION
                || bounds.outHeight / options.inSampleSize > MAX_PHOTO_DIMENSION) {
            options.inSampleSize *= 2;
        }
        Bitmap bitmap;
        try (InputStream input = resolver.openInputStream(uri)) {
            if (input == null) throw new IOException("Unable to open the selected photo.");
            bitmap = BitmapFactory.decodeStream(input, null, options);
        }
        if (bitmap == null) throw new IOException("The selected photo could not be decoded.");

        try {
            while (true) {
                for (int quality : new int[]{82, 72, 62, 52}) {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output);
                    if (output.size() <= MAX_PHOTO_BYTES) return output.toByteArray();
                }
                int width = Math.max(1, (int) (bitmap.getWidth() * 0.75f));
                int height = Math.max(1, (int) (bitmap.getHeight() * 0.75f));
                if (width == bitmap.getWidth() && height == bitmap.getHeight()) {
                    throw new IOException("A trip photo is too large to store in Firestore.");
                }
                Bitmap smaller = Bitmap.createScaledBitmap(bitmap, width, height, true);
                bitmap.recycle();
                bitmap = smaller;
            }
        } finally {
            if (!bitmap.isRecycled()) bitmap.recycle();
        }
    }

    private static byte[] readAllBytes(InputStream input) throws IOException {
        if (input == null) throw new IOException("Unable to read a cached trip photo.");
        try (InputStream source = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = source.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toByteArray();
        }
    }
}
