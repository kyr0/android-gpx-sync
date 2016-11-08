package de.aron_homberg.gpxsync.entities;

public class LogEntry {

    protected long id;
    protected int nr;
    protected long gpxTrackId;
    protected String message;
    protected String message_en;
    protected byte[] picture;
    protected String origin;
    protected String time;
    protected String type;
    protected double lat;
    protected double lng;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getNr() {
        return nr;
    }

    public void setNr(int nr) {
        this.nr = nr;
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

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public String getMessage_en() {
        return message_en;
    }

    public void setMessage_en(String message_en) {
        this.message_en = message_en;
    }
}
