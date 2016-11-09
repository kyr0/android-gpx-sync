package de.aron_homberg.gpxsync.entities;

public class LogEntry {

    protected long id;
    protected long gpxTrackId;
    protected String message;
    protected byte[] picture;
    protected String time;
    protected double lat;
    protected double lng;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public byte[] getPicture() {
        return picture;
    }

    public void setPicture(byte[] pictureBuffer) {
        this.picture = pictureBuffer;
    }

    public long getGpxTrackId() {
        return gpxTrackId;
    }

    public void setGpxTrackId(long gpxTrackId) {
        this.gpxTrackId = gpxTrackId;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }
}
