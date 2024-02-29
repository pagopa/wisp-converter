package it.gov.pagopa.wispconverter.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CacheRepository {

    @Autowired
    @Qualifier("object")
    private RedisTemplate<String, Object> template;

}