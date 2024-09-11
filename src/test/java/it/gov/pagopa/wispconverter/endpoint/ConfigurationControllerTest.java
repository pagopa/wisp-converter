package it.gov.pagopa.wispconverter.endpoint;

import com.azure.cosmos.CosmosClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.wispconverter.Application;
import it.gov.pagopa.wispconverter.controller.model.ConfigurationModel;
import it.gov.pagopa.wispconverter.repository.*;
import it.gov.pagopa.wispconverter.service.*;
import it.gov.pagopa.wispconverter.servicebus.ECommerceHangTimeoutConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.client.RestClient;

@ActiveProfiles(profiles = "test")
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
class ConfigurationControllerTest {

    @MockBean
    ConfigurationService configurationService;

    @Autowired
    private MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    private ApplicationStartup applicationStartup;
    @MockBean
    private RPTRequestRepository rptRequestRepository;
    @MockBean
    private RTRetryRepository rtRetryRepository;
    @MockBean
    private RTRepository rtRepository;
    @MockBean
    private IdempotencyKeyRepository idempotencyKeyRepository;
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
    private PaaInviaRTSenderService paaInviaRTSenderService;
    @MockBean
    private ServiceBusService serviceBusService;
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
    private ReceiptTimerService receiptTimerService;
    @MockBean
    RtReceiptCosmosService rtReceiptCosmosService;
    @MockBean
    ECommerceHangTimerService eCommerceHangTimerService;
    @MockBean
    ECommerceHangTimeoutConsumer eCommerceHangTimeoutConsumer;

//    @Test
//    void getCreditorInstitutions() {
//    }
//
//    @Test
//    void getStations() {
//    }

    @Test
    void createCreditorInstitutionsConfiguration() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/whitelist/cis")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConfigurationModel())))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
    }

    @Test
    void createStationsConfiguration() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/whitelist/cis")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConfigurationModel())))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
    }
}