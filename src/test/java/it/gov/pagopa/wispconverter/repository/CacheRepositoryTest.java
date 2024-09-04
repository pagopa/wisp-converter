package it.gov.pagopa.wispconverter.repository;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = CacheRepository.class)
class CacheRepositoryTest {

    @MockBean
    @Qualifier("redisSimpleTemplate")
    private RedisTemplate<String, Object> redisSimpleTemplate;


    @Autowired
            @InjectMocks
    CacheRepository cacheRepository;

    @Test
    void hasKey() {
        cacheRepository.hasKey("key");
        verify(redisSimpleTemplate, times(1)).hasKey(eq("key"));
    }
}