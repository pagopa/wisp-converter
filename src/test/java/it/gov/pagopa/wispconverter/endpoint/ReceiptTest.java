package it.gov.pagopa.wispconverter.endpoint;

import com.azure.cosmos.CosmosClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.gen.wispconverter.client.cache.model.StationCreditorInstitutionDto;
import it.gov.pagopa.wispconverter.Application;
import it.gov.pagopa.wispconverter.controller.model.ReceiptRequest;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.CacheRepository;
import it.gov.pagopa.wispconverter.repository.RPTRequestRepository;
import it.gov.pagopa.wispconverter.repository.RTRequestRepository;
import it.gov.pagopa.wispconverter.repository.ReEventRepository;
import it.gov.pagopa.wispconverter.repository.model.RPTRequestEntity;
import it.gov.pagopa.wispconverter.service.ConfigCacheService;
import it.gov.pagopa.wispconverter.service.PaaInviaRTSenderService;
import it.gov.pagopa.wispconverter.service.ReceiptTimerService;
import it.gov.pagopa.wispconverter.service.ServiceBusService;
import it.gov.pagopa.wispconverter.service.model.ReceiptDto;
import it.gov.pagopa.wispconverter.utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles(profiles = "test")
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
class ReceiptTest {

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
    private RTRequestRepository rtRequestRepository;
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

    private String getPaSendRTPayload() {
        String pasendrtv2 = TestUtils.loadFileContent("/requests/paSendRTV2.xml");
        return pasendrtv2;
    }

    @Test
    void success_positive() throws Exception {
        String station = "mystation";
        StationCreditorInstitutionDto stationCreditorInstitutionDto = new StationCreditorInstitutionDto();
        stationCreditorInstitutionDto.setCreditorInstitutionCode("{pa}");
        stationCreditorInstitutionDto.setSegregationCode(11L);
        stationCreditorInstitutionDto.setStationCode(station);
        org.springframework.test.util.ReflectionTestUtils.setField(configCacheService, "configData", TestUtils.configDataCreditorInstitutionStations(stationCreditorInstitutionDto));


        when(rptRequestRepository.findById(anyString())).thenReturn(
                Optional.of(
                        RPTRequestEntity.builder()
                                .primitive("nodoInviaRPT")
                                .payload(TestUtils.zipAndEncode(TestUtils.getRptPayload(false, station, "100.00", "datispec")))
                                .build()
                )
        );
        when(cacheRepository.read(anyString(), any())).thenReturn("wisp_nav2iuv_dominio");

        mvc.perform(MockMvcRequestBuilders.post("/receipt/ok")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReceiptRequest(getPaSendRTPayload()))))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andDo(
                        result -> {
                            assertNotNull(result);
                            assertNotNull(result.getResponse());
                        });

    }

    @Test
    void success_negative() throws Exception {
        String station = "mystation";
        StationCreditorInstitutionDto stationCreditorInstitutionDto = new StationCreditorInstitutionDto();
        stationCreditorInstitutionDto.setCreditorInstitutionCode("{pa}");
        stationCreditorInstitutionDto.setSegregationCode(11L);
        stationCreditorInstitutionDto.setStationCode(station);
        org.springframework.test.util.ReflectionTestUtils.setField(configCacheService, "configData", TestUtils.configDataCreditorInstitutionStations(stationCreditorInstitutionDto));


        when(rptRequestRepository.findById(any()))
                .thenReturn(Optional.of(RPTRequestEntity.builder()
                        .id(UUID.randomUUID().toString())
                        .primitive("nodoInviaRPT")
                        .payload(TestUtils.zipAndEncode(TestUtils.getRptPayload(false, "mystation", "10.00", "dati")))
                        .build()
                ));
        when(cacheRepository.read(any(), any())).thenReturn("wisp_nav2iuv_dominio");

        ReceiptDto[] receiptDtos = {
                new ReceiptDto("token", "dominio", "iuv")
        };
        mvc.perform(MockMvcRequestBuilders.post("/receipt/ko")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReceiptRequest(objectMapper.writeValueAsString(receiptDtos)))))
                .andExpect(MockMvcResultMatchers.status().is4xxClientError())
                .andDo(
                        result -> {
                            assertNotNull(result);
                            assertNotNull(result.getResponse());
                        });
    }

    @Test
    void error_send_rt() throws Exception {
        String station = "mystation";
        StationCreditorInstitutionDto stationCreditorInstitutionDto = new StationCreditorInstitutionDto();
        stationCreditorInstitutionDto.setCreditorInstitutionCode("{pa}");
        stationCreditorInstitutionDto.setSegregationCode(11L);
        stationCreditorInstitutionDto.setStationCode(station);
        org.springframework.test.util.ReflectionTestUtils.setField(configCacheService, "configData", TestUtils.configDataCreditorInstitutionStations(stationCreditorInstitutionDto));

        when(rptRequestRepository.findById(any())).thenReturn(
                Optional.of(
                        RPTRequestEntity.builder().primitive("nodoInviaRPT")
                                .payload(
                                        TestUtils.zipAndEncode(TestUtils.getRptPayload(false, station, "100.00", "datispec"))
                                ).build()
                )
        );
        when(cacheRepository.read(any(), any())).thenReturn("wisp_nav2iuv_dominio");
        doThrow(new AppException(AppErrorCodeMessageEnum.RECEIPT_GENERATION_ERROR_RESPONSE_FROM_CREDITOR_INSTITUTION, "PAA_ERRORE_RESPONSE", "PAA_ERRORE_RESPONSE", "Errore PA")).when(paaInviaRTSenderService).sendToCreditorInstitution(anyString(), anyString());

        mvc.perform(MockMvcRequestBuilders.post("/receipt/ok")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReceiptRequest(getPaSendRTPayload()))))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andDo(
                        result -> {
                            assertNotNull(result);
                            assertNotNull(result.getResponse());
                        });

        verify(paaInviaRTSenderService, times(1)).sendToCreditorInstitution(anyString(), anyString());
    }

    @Test
    void error_send_rt2() throws Exception {
        String station = "mystation";
        StationCreditorInstitutionDto stationCreditorInstitutionDto = new StationCreditorInstitutionDto();
        stationCreditorInstitutionDto.setCreditorInstitutionCode("{pa}");
        stationCreditorInstitutionDto.setSegregationCode(11L);
        stationCreditorInstitutionDto.setStationCode(station);
        org.springframework.test.util.ReflectionTestUtils.setField(configCacheService, "configData", TestUtils.configDataCreditorInstitutionStations(stationCreditorInstitutionDto));

        when(rptRequestRepository.findById(any())).thenReturn(
                Optional.of(
                        RPTRequestEntity.builder().primitive("nodoInviaRPT")
                                .payload(
                                        TestUtils.zipAndEncode(TestUtils.getRptPayload(false, station, "100.00", "datispec"))
                                ).build()
                )
        );
        when(cacheRepository.read(any(), any())).thenReturn("wisp_nav2iuv_dominio");
        doAnswer(i -> new ResponseEntity<>(HttpStatusCode.valueOf(200))).when(paaInviaRTSenderService).sendToCreditorInstitution(anyString(), anyString());
        mvc.perform(MockMvcRequestBuilders.post("/receipt/ok")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReceiptRequest(getPaSendRTPayload()))))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andDo(
                        result -> {
                            assertNotNull(result);
                            assertNotNull(result.getResponse());
                        });

        verify(paaInviaRTSenderService, times(1)).sendToCreditorInstitution(anyString(), anyString());
    }

}
