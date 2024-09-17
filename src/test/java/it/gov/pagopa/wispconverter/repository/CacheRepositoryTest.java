package it.gov.pagopa.wispconverter.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.temporal.ChronoUnit;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = CacheRepository.class)
class CacheRepositoryTest {

    @MockBean
    @Qualifier("redisSimpleTemplate")
    private RedisTemplate<String, Object> redisSimpleTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Autowired
    @InjectMocks
    CacheRepository cacheRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisSimpleTemplate.opsForValue()).thenReturn(valueOperations); // Mock ValueOperations
    }

    @Test
    void hasKey() {
        cacheRepository.hasKey("key");
        verify(redisSimpleTemplate, times(1)).hasKey(eq("key"));
    }


    @Test
    void testInsertWhenDeleteIfAlreadyExistsTrueAndKeyExists() {
        // Arrange
        String key = "testKey";
        String value = "testValue";
        long ttl = 5;

        when(redisSimpleTemplate.hasKey(key)).thenReturn(true);

        // Act
        cacheRepository.insert(key, value, ttl, ChronoUnit.MINUTES, true);

        // Assert
        verify(redisSimpleTemplate).hasKey(key); // Ensure hasKey is called
        verify(redisSimpleTemplate).delete(key); // Ensure delete is called because deleteIfAlreadyExists is true and key exists
    }

    @Test
    void testInsertWhenDeleteIfAlreadyExistsFalse() {
        // Arrange
        String key = "testKey";
        String value = "testValue";
        long ttl = 5;

        when(redisSimpleTemplate.hasKey(key)).thenReturn(true);

        // Act
        cacheRepository.insert(key, value, ttl, ChronoUnit.MINUTES, false);

        // Assert
        verify(redisSimpleTemplate, never()).delete(key); // Ensure delete is NOT called
    }
}