package com.multichunk.demo.controllers;

import com.multichunk.demo.services.MultiChunkUploadService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@RestController
@RequestMapping("/files")
public class FilesController {

    private final MultiChunkUploadService uploadService;

    public FilesController(MultiChunkUploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        System.out.println("Content-Type recibido: " + request.getContentType());
        var startTime = System.currentTimeMillis();
        try(InputStream inputStream = file.getInputStream()){
            var url = uploadService.uploadFileInChunks(file.getOriginalFilename(), inputStream, file.getSize(), 100).join();
            var endTime = System.currentTimeMillis();
            System.out.println("Uploaded in " + (endTime - startTime) + " ms");
            return ResponseEntity.ok("File upload started successfully. File URL: " + url);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("File upload failed: " + e.getMessage());
        }
    }
}
