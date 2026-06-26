package com.waterpark.tickershow.controller;

import com.waterpark.tickershow.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/uploads")
@RequiredArgsConstructor
public class FileUploadController {

    private static final long MAX_IMAGE_SIZE = 2 * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    @PostMapping("/show-images")
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ResponseEntity<ApiResponse<?>> uploadShowImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Vui long chon anh"));
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(ApiResponse.error(413, "Anh phai nho hon 2MB"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Chi ho tro anh JPG, PNG hoac WEBP"));
        }

        try {
            String extension = getExtension(file.getOriginalFilename(), contentType);
            String fileName = UUID.randomUUID() + extension;
            Path uploadDir = Paths.get("uploads", "show-images").toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);

            Path destination = uploadDir.resolve(fileName).normalize();
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }

            return ResponseEntity.ok(ApiResponse.success("/api/uploads/show-images/" + fileName));
        } catch (IOException ex) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "Khong the luu anh"));
        }
    }

    private String getExtension(String originalFilename, String contentType) {
        if (originalFilename != null) {
            String cleaned = Paths.get(originalFilename).getFileName().toString();
            int dotIndex = cleaned.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < cleaned.length() - 1) {
                String extension = cleaned.substring(dotIndex).toLowerCase(Locale.ROOT);
                if (extension.equals(".jpg") || extension.equals(".jpeg") || extension.equals(".png") || extension.equals(".webp")) {
                    return extension;
                }
            }
        }

        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
