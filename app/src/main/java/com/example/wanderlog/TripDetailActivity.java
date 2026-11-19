package com.example.wanderlog;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import java.util.List;

public class TripDetailActivity extends AppCompatActivity {
    private ViewPager2 pager;
    private TextView pageIndex;
    private int photoCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_trip_detail);
        View root = findViewById(R.id.tripDetailRoot);
        View back = findViewById(R.id.tripDetailBack);
        // The photo runs under the status bar, so only the back button moves down with it.
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, 0, bars.right, bars.bottom);
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) back.getLayoutParams();
            params.topMargin = dp(12) + bars.top;
            back.setLayoutParams(params);
            return insets;
        });

        String tripId = getIntent().getStringExtra(TripStore.EXTRA_TRIP_ID);
        Trip trip = TripStore.getTrip(tripId);
        if (trip == null) {
            finish();
            return;
        }
        back.setOnClickListener(view -> finish());
        ((TextView) findViewById(R.id.tripDetailTitle)).setText(trip.getTitle());
        ((TextView) findViewById(R.id.tripDetailLocation)).setText(trip.getLocation());
        ((TextView) findViewById(R.id.tripDetailDescription)).setText(trip.getDescription());
        ((TextView) findViewById(R.id.tripDetailPhotosLabel))
                .setText(getString(R.string.photos_count, trip.getPhotos().size()));

        List<Uri> photos = trip.getPhotos();
        photoCount = photos.size();
        pageIndex = findViewById(R.id.tripPhotoIndex);
        pager = findViewById(R.id.tripPhotoPager);
        pager.setAdapter(new PhotoPagerAdapter(photos));
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updatePageIndex(position);
            }
        });
        updatePageIndex(0);

        LinearLayout gallery = findViewById(R.id.tripPhotoGallery);
        int size = dp(96);
        int gap = dp(8);
        for (int i = 0; i < photos.size(); i++) {
            final int index = i;
            ImageView thumb = new ImageView(this);
            thumb.setBackgroundResource(R.color.wl_photo);
            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumb.setContentDescription(getString(R.string.trip_photo));
            setPhoto(thumb, photos.get(i));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            if (i < photos.size() - 1) params.setMarginEnd(gap);
            gallery.addView(thumb, params);
            thumb.setOnClickListener(view -> pager.setCurrentItem(index, true));
        }
    }

    private void updatePageIndex(int position) {
        pageIndex.setVisibility(photoCount > 1 ? View.VISIBLE : View.GONE);
        pageIndex.setText(getString(R.string.photo_position, position + 1, photoCount));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void setPhoto(ImageView image, Uri photo) {
        try { image.setImageURI(photo); }
        catch (RuntimeException exception) {
            image.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    private void showFullScreenPhoto(Uri photo) {
        Dialog dialog = new Dialog(this);
        ImageView image = new ImageView(this);
        image.setBackgroundColor(Color.BLACK);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setContentDescription(getString(R.string.trip_photo));
        setPhoto(image, photo);
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

    private class PhotoPagerAdapter extends RecyclerView.Adapter<PhotoPagerAdapter.Holder> {
        private final List<Uri> photos;

        PhotoPagerAdapter(List<Uri> photos) {
            this.photos = photos;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView image = new ImageView(parent.getContext());
            image.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setContentDescription(getString(R.string.trip_photo));
            return new Holder(image);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Uri photo = photos.get(position);
            setPhoto(holder.image, photo);
            holder.image.setOnClickListener(view -> showFullScreenPhoto(photo));
        }

        @Override
        public int getItemCount() {
            return photos.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final ImageView image;

            Holder(ImageView image) {
                super(image);
                this.image = image;
            }
        }
    }
}
