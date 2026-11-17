package com.example.wanderlog;

import java.util.ArrayList;
import java.util.List;

/** In-memory storage for the current app process; trips are not persisted. */
public final class TripStore {
    public static final String EXTRA_TRIP_INDEX = "com.example.wanderlog.TRIP_INDEX";
    private static final List<Trip> trips = new ArrayList<>();

    private TripStore() {}
    public static List<Trip> getTrips() { return new ArrayList<>(trips); }
    public static Trip getTrip(int index) {
        return index >= 0 && index < trips.size() ? trips.get(index) : null;
    }
    public static void saveTrip(int index, Trip trip) {
        if (index >= 0 && index < trips.size()) trips.set(index, trip);
        else trips.add(trip);
    }
    public static void deleteTrip(int index) {
        if (index >= 0 && index < trips.size()) trips.remove(index);
    }
}
