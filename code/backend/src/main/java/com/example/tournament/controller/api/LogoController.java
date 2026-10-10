package com.example.tournament.controller.api;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.tournament.service.storage.FileStorageService;
import com.example.tournament.service.storage.StoredFile;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/logos")
public class LogoController {

    private final FileStorageService files;

    public LogoController(FileStorageService files) {
        this.files = files;
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<byte[]> get(@PathVariable String filename) {
        StoredFile file = files.loadLogo(filename);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(file.content());
    }
}
