package com.multichunk.demo.services;

import com.multichunk.demo.components.ProgressTracker;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

@Service
public class MultiChunkDownloadService {
    private final MinioClient minioClient;
    private final ProgressTracker progressTracker;

    public MultiChunkDownloadService(MinioClient minioClient, ProgressTracker progressTracker) {
        this.minioClient = minioClient;
        this.progressTracker = progressTracker;
    }

    @Async("downloadExecutor")
    public CompletableFuture<Void> downloadFileInChunks(String downloadId, String bucketName, String objectName,  int chunkCount) {
        try{
            // Get object size in bytes
            long size = minioClient.statObject(
                    StatObjectArgs.builder().bucket(bucketName).object(objectName).build()
            ).size();

            // Initialize progress tracking
            progressTracker.start(downloadId, size);

            // Calculate chunk size
            // Example: 500 MB file / 5 chunks = 100 MB per chunk
            long chunkSize = size / chunkCount;

            // IntStram.range to create chunk download tasks according to chunk count
            // Example: chunkCount = 5, creates 5 tasks with index 0 to 4
            List<CompletableFuture<Void>> futures = IntStream.range(0, chunkCount)
                    .mapToObj(i -> CompletableFuture.runAsync(() -> {
                        // Calculate start and end byte range for each chunk
                        // Example: chunk 0 -> 0 to 99, chunk 1 -> 100 to 199, ..., chunk 4 -> 400 to 499
                        long start = i * chunkSize;
                        // If i is the last chunk, set end to size - 1 to include any remaining bytes
                        // Otherwise, set end to start + chunkSize - 1
                        // Example: chunk 0 end = (0 + 100 - 1) = 99, chunk 1 end = (100 + 100 - 1) = 199, ...
                        // chunk 4 end = 500 - 1 = 499
                        long end = (i == chunkCount - 1) ? size - 1 : (start + chunkSize - 1);
                        // Download the chunk
                        downloadChunk(downloadId, bucketName, objectName, start, end);
                    }))
                    .toList();

            // Wait for all chunk downloads to complete
            // Use new CompletableFuture[0] so Java can infer the type and create the array with correct size without needing to specify it explicitly
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Mark download as finished
            progressTracker.finish(downloadId);

            return CompletableFuture.completedFuture(null);
        }catch (Exception e){
            throw new RuntimeException("Error downloading file: " + e.getMessage(), e);
        }
    }

    private void downloadChunk(String downloadId, String bucketName, String objectName, long start, long end) {
        // Getting object with byte range using MinIO SDK
        try(InputStream is = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucketName).object(objectName).offset(start).length(end - start + 1).build())) {

            // Save chunk to temporary file: /tmp/{objectName}_chunk_{start}.part
            Path path = Path.of("/tmp/" + objectName + "_chunk_" + start + ".part");
            // Copy input stream to file
            long bytesCopied = is.transferTo(Files.newOutputStream(path));
            // Update progress tracker
            progressTracker.update(downloadId, bytesCopied);
        }catch (Exception e){
            throw new RuntimeException("Error downloading chunk: " + e.getMessage(), e);
        }
    }
}
