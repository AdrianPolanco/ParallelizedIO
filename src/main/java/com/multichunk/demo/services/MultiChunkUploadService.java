package com.multichunk.demo.services;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@ConfigurationProperties(prefix = "minio")
public class MultiChunkUploadService {

    private String bucket;
    private String uploadPath;
    private String uploadPathTemp;
    private final MinioClient minioClient;
    private final Executor uploadExecutor;

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getUploadPath() {
        return uploadPath;
    }

    public void setUploadPath(String uploadPath) {
        this.uploadPath = uploadPath;
    }

    public String getUploadPathTemp() {
        return uploadPathTemp;
    }

    public void setUploadPathTemp(String uploadPathTemp) {
        this.uploadPathTemp = uploadPathTemp;
    }

    public MultiChunkUploadService(MinioClient minioClient, Executor uploadExecutor) {
        this.minioClient = minioClient;
        this.uploadExecutor = uploadExecutor;
    }

    @Async("uploadExecutor")
    public CompletableFuture<String> uploadFileInChunks(String fileName, InputStream inputStream, long fileSize, int chunkCountMB) throws Exception {
        var chunkSize = chunkCountMB * 1024L * 1024L;
        byte[] buffer = new byte[(int)chunkSize];
        int bytesRead;
        //AtomicInteger partNumber = new AtomicInteger(0);
        int partNumber = 0;
        List<CompletableFuture<String>> futures = new ArrayList<>();

        while((bytesRead = inputStream.read(buffer)) != -1){
            byte[] chunkData = Arrays.copyOf(buffer, bytesRead);
            int currentPart = partNumber++;
            int finalBytesRead = bytesRead;
            int retries = 3;

            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                int attempts = 0;
                while (attempts < retries) {
                    try (InputStream chunkStream = new ByteArrayInputStream(chunkData)) {
                        String partName = uploadPathTemp + "/" + fileName + ".part" + currentPart;
                        minioClient.putObject(
                                PutObjectArgs.builder()
                                        .bucket(bucket)
                                        .object(partName)
                                        .stream(chunkStream, finalBytesRead, -1)
                                        .contentType("application/octet-stream")
                                        .build());
                        return partName;
                    } catch (Exception e) {
                        attempts++;
                        if (attempts >= retries) {
                            throw new RuntimeException("Fallo subiendo chunk tras " + retries + " intentos", e);
                        }
                        try {
                            // Backoff exponencial simple
                            Thread.sleep(1000L * attempts);
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
                throw new IllegalStateException("No debería llegar aquí");
            }, uploadExecutor);

            futures.add(future);
        }

        List<String> uploadedParts = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        String finalObjectName = uploadPath + "/" + fileName;
        composePartsParallel(finalObjectName, uploadedParts, uploadExecutor);

        String fileUrl = generateFileUrl(bucket, finalObjectName);
        return CompletableFuture.completedFuture(fileUrl);
    }

    private void composeParts(String fileName, List<String> partNames) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        List<ComposeSource> sources = partNames
                .stream().map(partName -> ComposeSource.builder().bucket(bucket).object(partName).build()).toList();

        minioClient
                .composeObject(ComposeObjectArgs.builder().bucket(bucket).sources(sources).object(fileName).build());

        for(String partName : partNames){
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(partName).build());
        }
    }

    private String generateFileUrl(String bucket, String objectName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .method(Method.GET)
                        .expiry(60 * 60 * 24) // 24 hours
                        .build()
        );
    }


    private String composePartsParallel(String fileName, List<String> partNames, Executor executor) throws Exception {
        // Componer en lotes de 100 partes para evitar límites de MinIO/S3
        int batchSize = 100;
        List<String> intermediateParts = new ArrayList<>();

        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < partNames.size(); i += batchSize) {
            batches.add(partNames.subList(i, Math.min(i + batchSize, partNames.size())));
        }

        // Componer cada lote en paralelo
        List<CompletableFuture<String>> batchFutures = batches.stream()
                .map(batch -> CompletableFuture.supplyAsync(() -> {
                    try {
                        String intermediateName = fileName + ".batch" + UUID.randomUUID();
                        List<ComposeSource> sources = batch.stream()
                                .map(p -> ComposeSource.builder().bucket(bucket).object(p).build())
                                .toList();

                        minioClient.composeObject(
                                ComposeObjectArgs.builder()
                                        .bucket(bucket)
                                        .object(intermediateName)
                                        .sources(sources)
                                        .build()
                        );
                        return intermediateName;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, executor))
                .toList();

        intermediateParts.addAll(batchFutures.stream().map(CompletableFuture::join).toList());

        // Componer el resultado final
        List<ComposeSource> finalSources = intermediateParts.stream()
                .map(p -> ComposeSource.builder().bucket(bucket).object(p).build())
                .toList();

        minioClient.composeObject(
                ComposeObjectArgs.builder()
                        .bucket(bucket)
                        .object(fileName)
                        .sources(finalSources)
                        .build()
        );

        // Limpiar partes intermedias
        for(String intermediatePart : intermediateParts){
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(intermediatePart).build());
        }

        return fileName;
    }

    public Map<String, String> generateUploadUrl(String fileName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        var objectName = uploadPath + "/" + fileName;

        var url = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs
                        .builder()
                        .bucket(bucket)
                        .object(objectName)
                        .method(Method.PUT)
                        .expiry(60 * 10) // 1 hour
                        .build()
        );

        return Map.of("url", url, "objectName", objectName);
    }
}
