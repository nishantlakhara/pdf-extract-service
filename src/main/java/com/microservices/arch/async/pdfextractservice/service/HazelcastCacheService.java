package com.microservices.arch.async.pdfextractservice.service;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class HazelcastCacheService implements CacheService {
    IMap<String, String> map;

    public HazelcastCacheService() {
        HazelcastInstance hz = HazelcastClient.newHazelcastClient();
        // Get the Distributed Map from Cluster.
        this.map = hz.getMap("my-distributed-map");
    }

    @Override
    public String put(String key, String value) {
        return map.put(key, value);
    }

    @Override
    public String get(String key) {
        return map.get(key);
    }

    @Override
    public String putIfAbsent(String key, String value) {
        return map.putIfAbsent(key, value);
    }

    @Override
    public String remove(String key) {
        return map.remove(key);
    }

    public Map<String, String> getMap() {
        return map;
    }

    @Override
    public void clearCache() {
        map.clear();
    }
}
