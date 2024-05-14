package it.gov.pagopa.wispconverter;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.azure.cosmos.CosmosClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.wispconverter.controller.model.ReceiptRequest;
import it.gov.pagopa.wispconverter.repository.CacheRepository;
import it.gov.pagopa.wispconverter.repository.RPTRequestRepository;
import it.gov.pagopa.wispconverter.repository.RTRequestRepository;
import it.gov.pagopa.wispconverter.repository.ReEventRepository;
import it.gov.pagopa.wispconverter.repository.model.RPTRequestEntity;
import it.gov.pagopa.wispconverter.service.ConfigCacheService;
import it.gov.pagopa.wispconverter.service.PaaInviaRTService;
import it.gov.pagopa.wispconverter.service.PaaInviaRTServiceBusService;
import it.gov.pagopa.wispconverter.service.model.ReceiptDto;
import it.gov.pagopa.wispconverter.utils.TestUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
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

@ActiveProfiles(profiles = "test")
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
class ReceiptTest {

    
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    private MockMvc mvc;
    @Autowired private ConfigCacheService configCacheService;

    @MockBean
    private ApplicationStartup applicationStartup;
    @MockBean
    private RPTRequestRepository rptRequestRepository;
    @MockBean
    private RTRequestRepository rtRequestRepository;
    @MockBean private it.gov.pagopa.gen.wispconverter.client.iuvgenerator.invoker.ApiClient iuveneratorClient;
    @MockBean private it.gov.pagopa.gen.wispconverter.client.gpd.invoker.ApiClient gpdClient;
    @MockBean private it.gov.pagopa.gen.wispconverter.client.checkout.invoker.ApiClient checkoutClient;
    @MockBean private it.gov.pagopa.gen.wispconverter.client.cache.invoker.ApiClient cacheClient;
    @MockBean private it.gov.pagopa.gen.wispconverter.client.decouplercaching.invoker.ApiClient decouplerCachingClient;
    @MockBean private PaaInviaRTService paaInviaRTService;
    @MockBean private PaaInviaRTServiceBusService paaInviaRTServiceBusService;
    @MockBean private ServiceBusSenderClient serviceBusSenderClient;

    @MockBean private CosmosClientBuilder cosmosClientBuilder;
    @Qualifier("redisSimpleTemplate")
    @MockBean private RedisTemplate<String, Object> redisSimpleTemplate;
    @MockBean
    private ReEventRepository reEventRepository;
    @MockBean private CacheRepository cacheRepository;

    private String getPaSendRTPayload(){
        String pasendrtv2 = TestUtils.loadFileContent("/requests/paSendRTV2.xml");
        return pasendrtv2;
    }

    private byte[] zip(byte[] uncompressed) throws IOException {
        ByteArrayOutputStream bais = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(bais);
        gzipOutputStream.write(uncompressed);
        gzipOutputStream.close();
        bais.close();
        return bais.toByteArray();
    }

    @Test
    void success_positive() throws Exception {
        String station = "mystation";
        org.springframework.test.util.ReflectionTestUtils.setField(configCacheService, "configData",TestUtils.configData(station));

        when(rptRequestRepository.findById(any())).thenReturn(
                Optional.of(
                        RPTRequestEntity.builder().primitive("nodoInviaRPT")
                                .payload(
                                        TestUtils.zipAndEncode(TestUtils.getRptPayload(false,station,"100.00","datispec"))
                                ).build()
                )
        );
        when(cacheRepository.read(any(),any())).thenReturn("asdsad");

        mvc.perform(MockMvcRequestBuilders.post("/receipt/ok")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReceiptRequest(getPaSendRTPayload()))))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andDo(
                        (result) -> {
                            assertNotNull(result);
                            assertNotNull(result.getResponse());
                        });


        verify(reEventRepository,times(3)).save(any());
    }

    @Test
    void success_negative() throws Exception {
        String station = "mystation";
        org.springframework.test.util.ReflectionTestUtils.setField(configCacheService, "configData",TestUtils.configData(station));

        when(rptRequestRepository.findById(any())).thenReturn(Optional.of(RPTRequestEntity
                .builder()
                        .id(UUID.randomUUID().toString())
                        .primitive("nodoInviaRPT")
                        .payload(TestUtils.zipAndEncode(TestUtils.getRptPayload(false,"mystation","10.00","dati")))
                .build()));
        when(cacheRepository.read(any(),any())).thenReturn("asdsad");

        ReceiptDto[] receiptDtos = {
                new ReceiptDto("token", "dominio", "iuv")
        };
        mvc.perform(MockMvcRequestBuilders.post("/receipt/ko")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReceiptRequest(objectMapper.writeValueAsString(receiptDtos)))))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andDo(
                        (result) -> {
                            assertNotNull(result);
                            assertNotNull(result.getResponse());
                        });

        verify(reEventRepository,times(3)).save(any());
    }
     
}
