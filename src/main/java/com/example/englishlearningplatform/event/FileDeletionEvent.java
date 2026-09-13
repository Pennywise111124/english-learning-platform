package com.example.englishlearningplatform.event;

public class FileDeletionEvent {

    private final String fileUrl;

    public FileDeletionEvent(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileUrl() {
        return fileUrl;
    }
}