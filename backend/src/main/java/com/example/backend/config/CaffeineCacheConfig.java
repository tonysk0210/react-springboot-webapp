package com.example.backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
public class CaffeineCacheConfig {
    /**
     * 建立 CaffeineCacheManager 來管理 CaffeineCache
     */
    @Bean
    public CacheManager caffeineCacheManager() {
        // 1. 建立 products Cache
        // recordStats()：讓 Actuator / Micrometer 能輸出命中率等快取指標，
        // 未開啟時啟動階段會出現 CaffeineCacheMetrics 警告，且僅能取得 cache.size。
        CaffeineCache productsCache = new CaffeineCache("products",
                Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .maximumSize(1000) // 限制此 cache 最多保留 1000 個 cache entries；目前 getProducts() 無參數，通常只會使用其中 1 個 entry
                        .recordStats()
                        .build());

        // 2. 建立 roles Cache
        CaffeineCache rolesCache = new CaffeineCache("roles",
                Caffeine.newBuilder()
                        .expireAfterWrite(1, TimeUnit.DAYS)
                        .maximumSize(10)
                        .recordStats()
                        .build());

        // 3. 將 productsCache 與 rolesCache 加入 CacheManager
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(Arrays.asList(productsCache, rolesCache));
        return manager;


    } // 建立 CaffeineCacheManager 來管理 CaffeineCache
}