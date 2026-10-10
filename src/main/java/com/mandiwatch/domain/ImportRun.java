package com.mandiwatch.domain;

import jakarta.persistence.*;

import java.time.Duration;
import java.time.LocalDateTime;

/** Audit record of one import (API pull, CSV upload or the startup sample loader). */
@Entity
@Table(name = "import_run")
public class ImportRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String source;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    private int rowsRead;
    private int rowsSaved;
    private int rowsSkipped;
    private int alertsSent;

    @Column(length = 1000)
    private String note;

    private boolean success;

    protected ImportRun() {
    }

    public ImportRun(String source) {
        this.source = source;
        this.startedAt = LocalDateTime.now();
    }

    public void finish(int read, int saved, int skipped, boolean success, String note) {
        this.rowsRead = read;
        this.rowsSaved = saved;
        this.rowsSkipped = skipped;
        this.success = success;
        this.note = note == null ? null : note.substring(0, Math.min(note.length(), 1000));
        this.finishedAt = LocalDateTime.now();
    }

    public long getDurationMillis() {
        if (startedAt == null || finishedAt == null) {
            return 0;
        }
        return Duration.between(startedAt, finishedAt).toMillis();
    }

    public Long getId() { return id; }
    public String getSource() { return source; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public int getRowsRead() { return rowsRead; }
    public int getRowsSaved() { return rowsSaved; }
    public int getRowsSkipped() { return rowsSkipped; }
    public int getAlertsSent() { return alertsSent; }
    public String getNote() { return note; }
    public boolean isSuccess() { return success; }

    public void setAlertsSent(int alertsSent) { this.alertsSent = alertsSent; }
}
