package it.gov.pagopa.wispconverter.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Slf4j
public class CacheRepository {

    @Autowired
    @Qualifier("redisSimpleTemplate")
    private RedisTemplate<String, Object> redisSimpleTemplate;

    public void insert(String key, String value, long ttlInMinutes) {
        this.redisSimpleTemplate.opsForValue().set(key, value, Duration.ofMinutes(ttlInMinutes));
    }
}