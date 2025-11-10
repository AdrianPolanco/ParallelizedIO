package com.multichunk.demo.helpers;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ProgressTracker {
    private final ConcurrentHashMap<String, AtomicLong> progressMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> totalSizeMap = new ConcurrentHashMap<>();

    public void start(String downloadId, long totalBytes) {
        progressMap.put(downloadId, new AtomicLong(0));
        totalSizeMap.put(downloadId, totalBytes);
    }

    public void update(String downloadId, long bytesRead) {
        progressMap.get(downloadId).addAndGet(bytesRead);
    }

    public int getPercentage(String downloadId) {
        long current = progressMap.getOrDefault(downloadId, new AtomicLong(0)).get();
        long total = totalSizeMap.getOrDefault(downloadId, 1L);
        return (int) ((current * 100) / total);
    }

    public void finish(String downloadId) {
        progressMap.remove(downloadId);
        totalSizeMap.remove(downloadId);
    }
}
