package de.aron_homberg.gpxsync.entities;

import java.io.File;
import java.io.Serializable;

import de.aron_homberg.gpxsync.util.Helper;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class GpxTrack implements Serializable {

    private int id;
    private String hash;
    private boolean isSynchronized;
    private String storagePath;
    private String name;
    private String markup;

    public GpxTrack(String storagePath, boolean isSynchronized, String markup) {
        setStoragePath(storagePath);
        setHash(Helper.md5(storagePath));
        setSynchronized(isSynchronized);
        setName(generateName(storagePath));
        setMarkup(markup);
    }

    public GpxTrack(int id, String storagePath, boolean isSynchronized, String markup) {
        this(storagePath, isSynchronized, markup);
        setId(id);
    }

    private String generateName(String storagePath) {
        String[] parts = storagePath.split(File.separator);
        return parts[parts.length-1];
    }


}
