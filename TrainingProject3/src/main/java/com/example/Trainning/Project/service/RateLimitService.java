package com.example.Trainning.Project.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimitService {

    private final ConcurrentHashMap<String, Long> lastRequestTime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> requestCount = new ConcurrentHashMap<>();
    private final long TIME_PERIOD_MILLIS = TimeUnit.SECONDS.toMillis(10); // 10 giây
    private final int MAX_REQUESTS = 10; // 5 yêu cầu

    public boolean allowRequest(String key) {
        long currentTime = System.currentTimeMillis();

        lastRequestTime.compute(key, (k, oldTime) -> {
            if (oldTime == null || currentTime - oldTime > TIME_PERIOD_MILLIS) {
                requestCount.put(k, 1); // Reset count
                return currentTime; // Update last request time
            } else {
                requestCount.merge(k, 1, Integer::sum); // Increment count
                return oldTime; // Keep old time if within period
            }
        });

        return requestCount.get(key) <= MAX_REQUESTS;
    }
}