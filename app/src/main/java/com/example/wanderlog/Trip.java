package com.example.wanderlog;

import android.net.Uri;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Trip {
    private final String id;
    private final String title;
    private final String description;
    private final String location;
    private final List<Uri> photos;

    public Trip(String title, String description, String location, List<Uri> photos) {
        this(null, title, description, location, photos);
    }

    public Trip(String id, String title, String description, String location, List<Uri> photos) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.photos = new ArrayList<>(photos);
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public List<Uri> getPhotos() { return Collections.unmodifiableList(photos); }
}
