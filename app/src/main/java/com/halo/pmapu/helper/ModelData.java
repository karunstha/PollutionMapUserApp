package com.halo.pmapu.helper;

public class ModelData {

    String id, locationName, location, time, value;

    public ModelData(String id, String locationName, String location, String time, String value) {
        this.id = id;
        this.locationName = locationName;
        this.location = location;
        this.time = time;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}