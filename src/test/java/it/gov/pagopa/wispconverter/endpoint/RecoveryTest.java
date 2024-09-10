package it.gov.pagopa.wispconverter.endpoint;

import com.azure.cosmos.CosmosClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.wispconverter.Application;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.*;
import it.gov.pagopa.wispconverter.service.*;
import it.gov.pagopa.wispconverter.servicebus.ECommerceHangTimeoutConsumer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles(profiles = "test")
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
public class RecoveryTest {
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ConfigCacheService configCacheService;
    @MockBean
    private ApplicationStartup applicationStartup;
    @MockBean
    private RPTRequestRepository rptRequestRepository;
    @MockBean
    private RTRetryRepository rtRetryRepository;
    @MockBean
    private RTRepository rtRepository;
    @MockBean
    private it.gov.pagopa.gen.wispconverter.client.iuvgenerator.invoker.ApiClient iuveneratorClient;
    @MockBean
    private it.gov.pagopa.gen.wispconverter.client.gpd.invoker.ApiClient gpdClient;
    @MockBean
    private it.gov.pagopa.gen.wispconverter.client.checkout.invoker.ApiClient checkoutClient;
    @MockBean
    private it.gov.pagopa.gen.wispconverter.client.cache.invoker.ApiClient cacheClient;
    @MockBean
    private it.gov.pagopa.gen.wispconverter.client.decouplercaching.invoker.ApiClient decouplerCachingClient;
    @MockBean
    private PaaInviaRTSenderService paaInviaRTService;
    @MockBean
    private ServiceBusService paaInviaRTServiceBusService;
    @MockBean
    private ServiceBusSenderClient serviceBusSenderClient;
    @MockBean
    private RestClient.Builder restClientBuilder;
    @MockBean
    private CosmosClientBuilder cosmosClientBuilder;
    @Qualifier("redisSimpleTemplate")
    @MockBean
    private RedisTemplate<String, Object> redisSimpleTemplate;
    @MockBean
    private ReEventRepository reEventRepository;
    @MockBean
    private CacheRepository cacheRepository;
    @MockBean
    private IdempotencyKeyRepository idempotencyKeyRepository;
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private RecoveryService recoveryService;
    @MockBean
    private ReceiptTimerService receiptTimerService;
    @MockBean
    private ReceiptService receiptService;
    @MockBean
    ECommerceHangTimerService eCommerceHangTimerService;
    @MockBean
    ECommerceHangTimeoutConsumer eCommerceHangTimeoutConsumer;

    @Test
    public void testRecoverReceiptKOForCreditorInstitution() throws Exception {
        String ci = "77777777777";
        String dateFrom = "2024-09-03";
        String dateTo = "2024-09-09";

        mockMvc.perform(post("/recovery/{creditor_institution}/receipt-ko", ci)
                                .queryParam("date_from", dateFrom)
                                .queryParam("date_to", dateTo))
                .andExpect(status().isOk());

        Mockito.verify(recoveryService, times(1)).recoverReceiptKOForCreditorInstitution(eq(ci), any(), any());
    }

    @Test
    public void testRecoverReceiptKOForCreditorInstitution_500() throws Exception {
        String ci = "77777777777";
        String dateFrom = "2024-09-03";
        String dateTo = "2024-09-09";

        Mockito.when(recoveryService.recoverReceiptKOForCreditorInstitution(anyString(), any(), any()))
                .thenThrow(new RuntimeException("Test exception"));

        mockMvc.perform(post("/recovery/{creditor_institution}/receipt-ko", ci)
                                .queryParam("date_from", dateFrom)
                                .queryParam("date_to", dateTo))
                .andExpect(status().isInternalServerError());

        Mockito.verify(recoveryService, times(1)).recoverReceiptKOForCreditorInstitution(eq(ci), any(), any());
    }
}
