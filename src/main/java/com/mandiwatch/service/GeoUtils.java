package com.mandiwatch.service;

public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {
    }

    /** Great-circle (straight line) distance between two points, in km. */
    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
    }

    /**
     * Roads are never straight, so we scale the straight-line distance.
     * 1.3 is a common rule of thumb for Indian state highways; it's configurable.
     */
    public static double roadKm(double lat1, double lon1, double lat2, double lon2, double roadFactor) {
        return haversineKm(lat1, lon1, lat2, lon2) * roadFactor;
    }
}
