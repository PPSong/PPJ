package com.penn.ppj.model;

/**
 * Created by penn on 23/04/2017.
 */

public class Geo {
    public double lon;
    public double lat;

    public Geo(double lon, double lat) {
        this.lon = lon;
        this.lat = lat;
    }

    public static String getDefaultGeoString() {
        return "121.52619934082031,31.216968536376953";
    }
}
