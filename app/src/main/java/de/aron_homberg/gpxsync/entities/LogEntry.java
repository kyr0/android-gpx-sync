package de.aron_homberg.gpxsync.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class LogEntry {
    private long id;
    private long gpxTrackId;
    private String message;
    private byte[] picture;
    private String time;
}
