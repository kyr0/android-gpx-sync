package de.aron_homberg.gpxsync.entities;

import java.io.Serializable;

import de.aron_homberg.gpxsync.util.Helper;

public class GpxTrack implements Serializable {

    protected int id;
    protected String hash;
    protected boolean isSynchronized;
    protected String storagePath;
    protected String name;
    protected String markup;

    public GpxTrack(String storagePath, boolean isSynchronized, String markup) {
        init(storagePath, isSynchronized, markup);
    }

    public GpxTrack(int id, String storagePath, boolean isSynchronized, String markup) {
        setId(id);
        init(storagePath, isSynchronized, markup);
    }

    public void init(String storagePath, boolean isSynchronized, String markup) {
        setStoragePath(storagePath);
        setHash(Helper.md5(storagePath));
        setIsSynchronized(isSynchronized);
        setName(generateName(storagePath));
        setMarkup(markup);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMarkup() {
        return markup;
    }

    public void setMarkup(String markup) {
        this.markup = markup;
    }

    protected String generateName(String storagePath) {

        String[] parts = storagePath.split("/");
        return parts[parts.length-1];
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getIsSynchronized() {
        return isSynchronized;
    }

    public void setIsSynchronized(boolean isSynchronized) {
        this.isSynchronized = isSynchronized;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String toString() {

        return "[hash=" + getHash() + ", name=" + getName() + ", storagePath=" + getStoragePath() +
                ", isSynchronized=" + getIsSynchronized() + ", markup=" + getMarkup() + "]";
    }
}
