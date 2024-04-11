package it.gov.pagopa.wispconverter;

import static it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum.VALIDATION_INVALID_MULTIBENEFICIARY_CART;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.gen.wispconverter.client.iuvgenerator.model.IuvGenerationModelResponseDto;
import it.gov.pagopa.wispconverter.repository.RPTRequestRepository;
import it.gov.pagopa.wispconverter.repository.RTRequestRepository;
import it.gov.pagopa.wispconverter.repository.ReEventRepository;
import it.gov.pagopa.wispconverter.repository.model.RPTRequestEntity;
import it.gov.pagopa.wispconverter.service.ConfigCacheService;
import it.gov.pagopa.wispconverter.utils.TestUtils;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ActiveProfiles(profiles = "test")
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
class CarrelloTest {


    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    private MockMvc mvc;
    @Autowired private ConfigCacheService configCacheService;

    @MockBean
    private RPTRequestRepository rptRequestRepository;
    @MockBean
    private RTRequestRepository rtRequestRepository;
    @MockBean private it.gov.pagopa.gen.wispconverter.client.iuvgenerator.invoker.ApiClient iuveneratorClient;
    @MockBean private it.gov.pagopa.gen.wispconverter.client.gpd.invoker.ApiClient gpdClient;
    @MockBean private it.gov.pagopa.gen.wispconverter.client.checkout.invoker.ApiClient checkoutClient;
    @MockBean private it.gov.pagopa.gen.wispconverter.client.cache.invoker.ApiClient cacheClient;
    @MockBean private it.gov.pagopa.gen.wispconverter.client.decouplercaching.invoker.ApiClient decouplerCachingClient;
    @Qualifier("redisSimpleTemplate")
    @MockBean private RedisTemplate<String, Object> redisSimpleTemplate;
    @MockBean
    private ReEventRepository reEventRepository;

    @Test
    void success() throws Exception {
        String station = "mystation";
        TestUtils.setMock(cacheClient,ResponseEntity.ok().body(TestUtils.configData(station)));

        org.springframework.test.util.ReflectionTestUtils.setField(configCacheService, "configCacheClient",cacheClient);
        HttpHeaders headers = new HttpHeaders();
        headers.add("location","locationheader");
        TestUtils.setMock(checkoutClient, ResponseEntity.status(HttpStatus.FOUND).headers(headers).build());

        IuvGenerationModelResponseDto iuvGenerationModelResponseDto = new IuvGenerationModelResponseDto();
        iuvGenerationModelResponseDto.setIuv("00000000");
        TestUtils.setMock(iuveneratorClient,ResponseEntity.ok().body(iuvGenerationModelResponseDto));
        TestUtils.setMock(gpdClient,ResponseEntity.ok().build());
        TestUtils.setMock(decouplerCachingClient,ResponseEntity.ok().build());
        when(rptRequestRepository.findById(any())).thenReturn(
                Optional.of(
                        RPTRequestEntity.builder().primitive("nodoInviaCarrelloRPT")
                                .payload(
                                        TestUtils.zipAndEncode(TestUtils.getCarrelloPayload(2,station,
                                                        "100.00",true
                                                ))
                                ).build()
                )
        );
        when(redisSimpleTemplate.opsForValue()).thenReturn(mock(ValueOperations.class));



        mvc.perform(MockMvcRequestBuilders.get("/redirect?sessionId=aaaaaaaaaaaa").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andDo(
                        (result) -> {
                            assertNotNull(result);
                            assertNotNull(result.getResponse());
                        });

        verify(checkoutClient,times(1)).invokeAPI(any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any());
        verify(iuveneratorClient,times(1)).invokeAPI(any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any());
        verify(gpdClient,times(1)).invokeAPI(any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any());
        verify(decouplerCachingClient,times(1)).invokeAPI(any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any());
    }

    @Test
    void success_multibeneficiario() throws Exception {
        String station = "mystation";
        TestUtils.setMock(cacheClient,ResponseEntity.ok().body(TestUtils.configData(station)));
        org.springframework.test.util.ReflectionTestUtils.setField(configCacheService, "configCacheClient",cacheClient);
        HttpHeaders headers = new HttpHeaders();
        headers.add("location","locationheader");
        TestUtils.setMock(checkoutClient, ResponseEntity.status(HttpStatus.FOUND).headers(headers).build());

        IuvGenerationModelResponseDto iuvGenerationModelResponseDto = new IuvGenerationModelResponseDto();
        iuvGenerationModelResponseDto.setIuv("00000000");
        TestUtils.setMock(iuveneratorClient,ResponseEntity.ok().body(iuvGenerationModelResponseDto));
        TestUtils.setMock(gpdClient,ResponseEntity.ok().build());
        TestUtils.setMock(decouplerCachingClient,ResponseEntity.ok().build());
        when(rptRequestRepository.findById(any())).thenReturn(
                Optional.of(
                        RPTRequestEntity.builder().primitive("nodoInviaCarrelloRPT")
                                .payload(
                                        TestUtils.zipAndEncode(TestUtils.getCarrelloPayload(2,station,
                                                        "100.00",true
                                                ))
                                ).build()
                )
        );
        when(redisSimpleTemplate.opsForValue()).thenReturn(mock(ValueOperations.class));



        mvc.perform(MockMvcRequestBuilders.get("/redirect?sessionId=aaaaaaaaaaaa").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andDo(
                        (result) -> {
                            assertNotNull(result);
                            assertNotNull(result.getResponse());
                        });

        verify(checkoutClient,times(1)).invokeAPI(any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any());
        verify(iuveneratorClient,times(1)).invokeAPI(any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any());
        verify(gpdClient,times(1)).invokeAPI(any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any());
        verify(decouplerCachingClient,times(1)).invokeAPI(any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any());
    }

    @Test
    void fail_multibeneficiario_less_rpts() throws Exception {
        String station = "mystation";
        TestUtils.setMock(cacheClient,ResponseEntity.ok().body(TestUtils.configData(station)));
        org.springframework.test.util.ReflectionTestUtils.setField(configCacheService, "configCacheClient",cacheClient);
        HttpHeaders headers = new HttpHeaders();
        headers.add("location","locationheader");
        TestUtils.setMock(checkoutClient, ResponseEntity.status(HttpStatus.FOUND).headers(headers).build());

        IuvGenerationModelResponseDto iuvGenerationModelResponseDto = new IuvGenerationModelResponseDto();
        iuvGenerationModelResponseDto.setIuv("00000000");
        TestUtils.setMock(iuveneratorClient,ResponseEntity.ok().body(iuvGenerationModelResponseDto));
        TestUtils.setMock(gpdClient,ResponseEntity.ok().build());
        TestUtils.setMock(decouplerCachingClient,ResponseEntity.ok().build());
        when(rptRequestRepository.findById(any())).thenReturn(
                Optional.of(
                        RPTRequestEntity.builder().primitive("nodoInviaCarrelloRPT")
                                .payload(
                                        TestUtils.zipAndEncode(
                                                TestUtils.getCarrelloPayload(1,station,
                                                        "100.00",true
                                                ))
                                ).build()
                )
        );
        when(redisSimpleTemplate.opsForValue()).thenReturn(mock(ValueOperations.class));



        mvc.perform(MockMvcRequestBuilders.get("/redirect?sessionId=aaaaaaaaaaaa").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().is4xxClientError())
                .andDo(
                        (result) -> {
                            assertNotNull(result);
                            assertNotNull(result.getResponse());
                            assertTrue(result.getResponse().getContentAsString().contains(VALIDATION_INVALID_MULTIBENEFICIARY_CART.getDetail()));
                        });

        verify(checkoutClient,times(0)).invokeAPI(any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any());
        verify(iuveneratorClient,times(0)).invokeAPI(any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any());
        verify(gpdClient,times(0)).invokeAPI(any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any());
        verify(decouplerCachingClient,times(0)).invokeAPI(any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any());
    }

}
