package org.xiaobai.prettylike.manager.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 缓存类
 */
@Slf4j
@Component
public class CacheManager {

    /**
     * 热键检测器
     */
    private TopK hotKeyDetector;

    private Cache<String,Object> localCache;

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    @Bean
    public TopK getHotKeyDetector() {
        // 检查 hotKeyDetector 是否已经初始化
        if (hotKeyDetector == null) {
            hotKeyDetector = new HeavyKeeper(
                    // 监控 Top 100 Key
                    100,
                    // 哈希表宽度
                    100000,
                    // 哈希表深度
                    5,
                    // 衰减系数
                    0.92,
                    // 最小出现 10 次才记录
                    10
            );
        }
        return hotKeyDetector;
    }

    /**
     * 获取本地缓存
     * @return 本地缓存
     */
    public Cache<String,Object> getLocalCache(){
        if(localCache != null){
            return localCache;
        }
        return localCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
    }

    // 辅助方法：构造复合 key
    private String buildCacheKey(String hashKey, String key) {
        return hashKey + ":" + key;
    }

    public Object get(String hashKey,String key){
        // 构造唯一的 composite key
        String compositeKey = buildCacheKey(hashKey, key);

        // 1.先查本地缓存
        Object value = localCache.getIfPresent(compositeKey);
        if (value != null) {
            log.info("本地缓存获取到数据 {} = {}",compositeKey,value);
            // 记录访问次数（每次访问计数 + 1）
            hotKeyDetector.add(compositeKey, 1);
            return value;
        }

        // 2.本地缓存未命中，查询 Redis
        Object redisValue = redisTemplate.opsForHash().get(hashKey, key);
        if(redisValue == null){
            return null;
        }

        // 3.记录访问（计数 + 1）
        AddResult addResult = hotKeyDetector.add(compositeKey, 1);

        // 4.如果是热 key 且不在本地缓存，则缓存数据
        if (addResult.isHotKey()) {
            localCache.put(compositeKey, redisValue);
            log.info("热 key 缓存到本地 {} = {}",compositeKey,redisValue);
        }

        return redisValue;
    }

    public void putIfPresent(String hashKey,String key,Object value){
        // 构造唯一的 composite key
        String compositeKey = buildCacheKey(hashKey, key);
        // 放入本地缓存
        Object obj = localCache.getIfPresent(compositeKey);
        if(obj == null){
            return;
        }
        localCache.put(compositeKey, value);
    }

    /**
     * 定时清理过期的热键
     * 每隔 20 秒清理一次过期的热键，衰减系数为 0.92，衰减时间为 5 分钟
     */
    @Scheduled(fixedRate = 20,timeUnit = TimeUnit.SECONDS)
    public void cleanHotKeys(){
        hotKeyDetector.fading();
    }
}
