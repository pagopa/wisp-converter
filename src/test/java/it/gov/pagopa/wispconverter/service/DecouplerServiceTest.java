package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.CacheRepository;
import it.gov.pagopa.wispconverter.repository.ReceiptDeadLetterRepository;
import it.gov.pagopa.wispconverter.secondary.IdempotencyKeyRepositorySecondary;
import it.gov.pagopa.wispconverter.secondary.RTRepositorySecondary;
import it.gov.pagopa.wispconverter.secondary.ReEventRepositorySecondary;
import it.gov.pagopa.wispconverter.service.model.CachedKeysMapping;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static it.gov.pagopa.wispconverter.service.DecouplerService.MAP_CACHING_KEY_TEMPLATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles(profiles = "test")
@SpringBootTest
class DecouplerServiceTest {

    @Autowired
    private DecouplerService decouplerService;

    @MockBean
    private CacheRepository cacheRepository;

    @MockBean
    private ReceiptDeadLetterRepository receiptDeadLetterRepository;

    @MockBean
    private RTRepositorySecondary rtRepositorySecondary;

    @MockBean
    private ReEventRepositorySecondary reEventRepositorySecondary;

    @MockBean
    private IdempotencyKeyRepositorySecondary idempotencyKeyRepositorySecondary;

    private static final String domainId = "12345678910";

    private static final String nav = "350000000000000000";

    @ParameterizedTest
    @CsvSource({"wisp_nav2iuv_123456IUVMOCK1,123456IUVMOCK1", "wisp_nav2iuv_123456IUVMOCK1_123456IUVMOCK2,123456IUVMOCK1_123456IUVMOCK2"})
    @SneakyThrows
    void getCachedMappingFromNavToIuvTestOK(String nav2iuv, String iuv) {

        // mocking decoupler cached keys
        when(cacheRepository.read(anyString(), any())).thenReturn(nav2iuv);

        CachedKeysMapping result = decouplerService.getCachedMappingFromNavToIuv(domainId, nav);
        Assertions.assertEquals(iuv, result.getIuv());

    }

    @Test
    @SneakyThrows
    void getCachedMappingFromNavToIuvTestKONullKey() {

        String mappingKey = String.format(MAP_CACHING_KEY_TEMPLATE, domainId, nav);

        // mocking decoupler cached keys
        when(cacheRepository.read(anyString(), any())).thenReturn(null);

        try {
            decouplerService.getCachedMappingFromNavToIuv(domainId, nav);
            fail();
        } catch (AppException e) {
            assertEquals(e.getError(), AppErrorCodeMessageEnum.PERSISTENCE_REQUESTID_CACHING_ERROR);
        }
    }

    @Test
    @SneakyThrows
    void getCachedMappingFromNavToIuvTestKOWrongKey() {

        // mocking decoupler cached keys
        when(cacheRepository.read(anyString(), any())).thenReturn("wrong_string");

        try {
            decouplerService.getCachedMappingFromNavToIuv(domainId, nav);
            fail();
        } catch (AppException e) {
            assertEquals(e.getError(), AppErrorCodeMessageEnum.PERSISTENCE_MAPPING_NAV_TO_IUV_ERROR);
        }
    }
}