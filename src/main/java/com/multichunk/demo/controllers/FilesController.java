package com.multichunk.demo.controllers;

import com.multichunk.demo.services.MultiChunkUploadService;
import com.multichunk.demo.services.WebhookStreamProducer;
import io.minio.errors.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/files")
public class FilesController {

    private final MultiChunkUploadService uploadService;
    private final Logger logger = Logger.getLogger(FilesController.class.getName());
    private final WebhookStreamProducer webhookStreamProducer;

    public FilesController(MultiChunkUploadService uploadService, WebhookStreamProducer webhookStreamProducer) {
        this.uploadService = uploadService;
        this.webhookStreamProducer = webhookStreamProducer;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try{
            return ResponseEntity.ok(uploadService.generateUploadUrl(file.getOriginalFilename()));
        } catch (Exception e) {
            var response = Map.of("error", "File upload failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/upload/webhook")
    public ResponseEntity<String> uploadWebhook(@RequestBody Map<String, Object> payload) {
        logger.info("Received webhook with payload: " + payload);
        webhookStreamProducer.produceEvent(payload);
        return ResponseEntity.ok("Webhook received");
    }
}
