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

    public <T> T read(String key, Class<T> clazz) {
        T result = null;
        try {
            Object value = this.redisSimpleTemplate.opsForValue().get(key);
            result = clazz.cast(value);
        } catch (ClassCastException e) {
            log.error(String.format("Cannot correctly parse the object retrieved with key [%s] in [%s] class", key, clazz.getCanonicalName()));
        }
        return result;
    }
}