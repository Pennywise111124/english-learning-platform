package com.example.englishlearningplatform.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String storeImage(MultipartFile file, String subDirectory);

    void deleteFile(String relativeUrl);
}