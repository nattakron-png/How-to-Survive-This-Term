package com.example.tournament.service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String storeLogo(MultipartFile file);

    StoredFile loadLogo(String filename);

    void deleteLogo(String filename);
}
