package com.multichunk.demo.controllers;

import com.multichunk.demo.services.MultiChunkUploadService;
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

@RestController
@RequestMapping("/files")
public class FilesController {

    private final MultiChunkUploadService uploadService;

    public FilesController(MultiChunkUploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
       /*
       System.out.println("Content-Type recibido: " + request.getContentType());
       var startTime = System.currentTimeMillis();
        try(InputStream inputStream = file.getInputStream()){
            var url = uploadService.uploadFileInChunks(file.getOriginalFilename(), inputStream, file.getSize(), 100).join();
            var endTime = System.currentTimeMillis();
            System.out.println("Uploaded in " + (endTime - startTime) + " ms");
            return ResponseEntity.ok("File upload started successfully. File URL: " + url);*/
        try{
            return ResponseEntity.ok(uploadService.generateUploadUrl(file.getOriginalFilename()));
        } catch (Exception e) {
            var response = Map.of("error", "File upload failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/upload/webhook")
    public ResponseEntity<String> uploadWebhook(@RequestBody String allParams) {
        System.out.println("Received webhook with params: " + allParams);
        return ResponseEntity.ok("Webhook received");
    }
}
