package com.example.wanderlog;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Profile picture for the signed-in user. The picture is always kept on the device so it works
 * immediately; it is also copied to the user's Firestore document so it follows them to another
 * device. A failed cloud copy never blocks or undoes the local picture.
 */
final class ProfilePhotoStore {
    private static final String FIELD_AVATAR = "avatar";
    private static final int SIZE = 384;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler main = new Handler(Looper.getMainLooper());

    private ProfilePhotoStore() {}

    interface PhotoCallback {
        void onPhoto(@Nullable Bitmap photo);
    }

    interface DoneCallback {
        void onDone(boolean success);
    }

    /** Delivers the cached picture right away, then the cloud one if this device has none yet. */
    static void load(Context context, PhotoCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onPhoto(null);
            return;
        }
        Context app = context.getApplicationContext();
        File file = cacheFile(app, user.getUid());
        executor.execute(() -> {
            Bitmap cached = file.isFile() ? BitmapFactory.decodeFile(file.getPath()) : null;
            main.post(() -> callback.onPhoto(cached));
            if (cached != null) return;
            userDoc(user.getUid()).get().addOnSuccessListener(snapshot -> {
                Object value = snapshot.get(FIELD_AVATAR);
                if (!(value instanceof Blob)) return;
                byte[] bytes = ((Blob) value).toBytes();
                executor.execute(() -> {
                    try { write(file, bytes); } catch (IOException ignored) { }
                    Bitmap remote = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    main.post(() -> callback.onPhoto(remote));
                });
            });
        });
    }

    static void save(Context context, Uri source, DoneCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onDone(false);
            return;
        }
        Context app = context.getApplicationContext();
        File file = cacheFile(app, user.getUid());
        executor.execute(() -> {
            try {
                byte[] bytes = squareJpeg(app, source);
                write(file, bytes);
                main.post(() -> callback.onDone(true));
                Map<String, Object> data = new HashMap<>();
                data.put(FIELD_AVATAR, Blob.fromBytes(bytes));
                userDoc(user.getUid()).set(data, SetOptions.merge());
            } catch (IOException | RuntimeException exception) {
                main.post(() -> callback.onDone(false));
            }
        });
    }

    static void remove(Context context, DoneCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onDone(false);
            return;
        }
        File file = cacheFile(context.getApplicationContext(), user.getUid());
        executor.execute(() -> {
            boolean gone = !file.exists() || file.delete();
            main.post(() -> callback.onDone(gone));
            Map<String, Object> data = new HashMap<>();
            data.put(FIELD_AVATAR, FieldValue.delete());
            userDoc(user.getUid()).set(data, SetOptions.merge());
        });
    }

    private static DocumentReference userDoc(String uid) {
        return FirebaseFirestore.getInstance().collection("users").document(uid);
    }

    private static File cacheFile(Context context, String uid) {
        return new File(context.getFilesDir(), "avatar_" + uid + ".jpg");
    }

    private static void write(File file, byte[] bytes) throws IOException {
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(bytes);
        }
    }

    /** Center-crops the picked image to a square and compresses it to a small JPEG. */
    private static byte[] squareJpeg(Context context, Uri source) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = context.getContentResolver().openInputStream(source)) {
            if (input == null) throw new IOException("Unable to open the picture.");
            BitmapFactory.decodeStream(input, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IOException("Unreadable picture.");
        BitmapFactory.Options options = new BitmapFactory.Options();
        int sample = 1;
        while (Math.min(bounds.outWidth, bounds.outHeight) / (sample * 2) >= SIZE) sample *= 2;
        options.inSampleSize = sample;
        Bitmap decoded;
        try (InputStream input = context.getContentResolver().openInputStream(source)) {
            if (input == null) throw new IOException("Unable to open the picture.");
            decoded = BitmapFactory.decodeStream(input, null, options);
        }
        if (decoded == null) throw new IOException("Unable to decode the picture.");
        int edge = Math.min(decoded.getWidth(), decoded.getHeight());
        Bitmap square = Bitmap.createBitmap(decoded,
                (decoded.getWidth() - edge) / 2, (decoded.getHeight() - edge) / 2, edge, edge);
        Bitmap scaled = Bitmap.createScaledBitmap(square, SIZE, SIZE, true);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, output);
        return output.toByteArray();
    }
}
