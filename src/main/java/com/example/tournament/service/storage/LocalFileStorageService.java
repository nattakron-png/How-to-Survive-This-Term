package com.example.tournament.service.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.exception.ValidationException;

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final int MAX_LOGO_BYTES = 2 * 1024 * 1024;
    private static final Pattern LOGO_NAME = Pattern.compile("[0-9a-f-]{36}\\.(png|jpg)");

    private final Path logoDirectory;

    public LocalFileStorageService(@Value("${app.storage.logo-dir:uploads/logos}") String logoDirectory) {
        this.logoDirectory = Path.of(logoDirectory).toAbsolutePath().normalize();
    }

    @Override
    public String storeLogo(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_LOGO_BYTES) {
            throw new ValidationException("Logo must be a non-empty image of at most 2 MB");
        }

        try {
            byte[] content = file.getBytes();
            if (content.length > MAX_LOGO_BYTES) {
                throw new ValidationException("Logo must be a non-empty image of at most 2 MB");
            }
            String extension = extensionFor(file.getContentType(), content);
            validateImage(content);
            String filename = UUID.randomUUID() + extension;
            Files.createDirectories(logoDirectory);
            Files.write(logoDirectory.resolve(filename), content, StandardOpenOption.CREATE_NEW);
            return filename;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not store logo", exception);
        }
    }

    @Override
    public StoredFile loadLogo(String filename) {
        Path path = pathFor(filename);
        if (!Files.isRegularFile(path)) {
            throw new ResourceNotFoundException("Logo not found");
        }
        try {
            byte[] content = Files.readAllBytes(path);
            return new StoredFile(content, filename.endsWith(".png") ? "image/png" : "image/jpeg");
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read logo", exception);
        }
    }

    @Override
    public void deleteLogo(String filename) {
        try {
            Files.deleteIfExists(pathFor(filename));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not delete logo", exception);
        }
    }

    private Path pathFor(String filename) {
        if (!LOGO_NAME.matcher(filename).matches()) {
            throw new ResourceNotFoundException("Logo not found");
        }
        return logoDirectory.resolve(filename);
    }

    private String extensionFor(String contentType, byte[] content) {
        if ("image/png".equals(contentType) && isPng(content)) {
            return ".png";
        }
        if ("image/jpeg".equals(contentType) && isJpeg(content)) {
            return ".jpg";
        }
        throw new ValidationException("Logo must be a PNG or JPEG image");
    }

    private void validateImage(byte[] content) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
            if (image == null || image.getWidth() > 4096 || image.getHeight() > 4096) {
                throw new ValidationException("Logo image must be at most 4096 x 4096 pixels");
            }
        } catch (IOException exception) {
            throw new ValidationException("Logo is not a valid image");
        }
    }

    private boolean isPng(byte[] bytes) {
        byte[] signature = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        if (bytes.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (bytes[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 4 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8
                && (bytes[bytes.length - 2] & 0xff) == 0xff && (bytes[bytes.length - 1] & 0xff) == 0xd9;
    }
}
