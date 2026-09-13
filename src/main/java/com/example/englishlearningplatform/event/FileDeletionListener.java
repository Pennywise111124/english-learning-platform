package com.example.englishlearningplatform.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.englishlearningplatform.service.FileStorageService;

@Component
public class FileDeletionListener {

    private final FileStorageService fileStorageService;

    public FileDeletionListener(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFileDeletion(FileDeletionEvent event) {
        fileStorageService.deleteFile(event.getFileUrl());
    }
}