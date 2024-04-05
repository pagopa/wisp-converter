package it.gov.pagopa.wispconverter;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.gen.wispconverter.client.iuvgenerator.model.IuvGenerationModelResponseDto;
import it.gov.pagopa.wispconverter.repository.RPTRequestRepository;
import it.gov.pagopa.wispconverter.repository.model.RPTRequestEntity;
import it.gov.pagopa.wispconverter.service.ConfigCacheService;
import it.gov.pagopa.wispconverter.utils.TestUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPOutputStream;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
class RptTest {

    
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    private MockMvc mvc;
    @Autowired private ConfigCacheService configCacheService;

    @MockBean
    private RPTRequestRepository rptRequestRepository;
    @MockBean private it.gov.pagopa.gen.wispconverter.client.iuvgenerator.invoker.ApiClient iuveneratorClient;
    @MockBean private it.gov.pagopa.gen.wispconverter.client.gpd.invoker.ApiClient gpdClient;
    @MockBean private it.gov.pagopa.gen.wispconverter.client.checkout.invoker.ApiClient checkoutClient;
    @MockBean private it.gov.pagopa.gen.wispconverter.client.cache.invoker.ApiClient cacheClient;
    @MockBean private it.gov.pagopa.gen.wispconverter.client.decouplercaching.invoker.ApiClient decouplerCachingClient;
    @Qualifier("redisSimpleTemplate")
    @MockBean private RedisTemplate<String, Object> redisSimpleTemplate;

    private String getRptPayload(String station,String amount,String datiSpecificiRiscossione){
        if(datiSpecificiRiscossione==null){
            datiSpecificiRiscossione = "9/tipodovuto_7/datospecifico";
        }
        String rpt = TestUtils.loadFileContent("/requests/rpt.xml");
        String rptreplace = rpt
                .replace("{datiSpecificiRiscossione}",datiSpecificiRiscossione)
                .replaceAll("\\{amount\\}", amount);
        String nodoInviaRPT = TestUtils.loadFileContent("/requests/nodoInviaRPT.xml");
        return nodoInviaRPT
                .replace("{station}",station)
                .replace("{rpt}", Base64.getEncoder().encodeToString(rptreplace.getBytes(StandardCharsets.UTF_8)));
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
                        RPTRequestEntity.builder().primitive("nodoInviaRPT")
                                .payload(
                                        new String(Base64.getEncoder().encode(zip(
                                                getRptPayload(station,"100.00",null).getBytes(StandardCharsets.UTF_8))),StandardCharsets.UTF_8)
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
    void success_tassonomia() throws Exception {
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
                        RPTRequestEntity.builder().primitive("nodoInviaRPT")
                                .payload(
                                        new String(Base64.getEncoder().encode(zip(
                                                getRptPayload(station,"100.00","datispec").getBytes(StandardCharsets.UTF_8))),StandardCharsets.UTF_8)
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
     
}
