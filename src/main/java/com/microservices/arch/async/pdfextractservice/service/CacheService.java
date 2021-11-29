package com.microservices.arch.async.pdfextractservice.service;

public interface CacheService {
    public String put(String key, String value);
    public String get(String key);
    public String putIfAbsent(String key, String value);
    public String remove(String key);

    void clearCache();
}
