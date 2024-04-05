package it.gov.pagopa.wispconverter.utils;


import io.micrometer.core.instrument.util.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class TestUtils {

    public static String loadFileContent(String fileName) {
        String content = null;
        InputStream inputStream = TestUtils.class.getResourceAsStream(fileName);
        if (inputStream != null) {
            content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } else {
            System.err.println("File not found: " + fileName);
        }
        return content;
    }

    public static it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigDataV1Dto configData(String stationCode){
        it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigDataV1Dto configDataV1 = new it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigDataV1Dto();
        configDataV1.setStations(new HashMap<>());
        it.gov.pagopa.gen.wispconverter.client.cache.model.StationDto station = new it.gov.pagopa.gen.wispconverter.client.cache.model.StationDto();
        station.setStationCode(stationCode);
        station.setRedirect(new it.gov.pagopa.gen.wispconverter.client.cache.model.RedirectDto());
        station.getRedirect().setIp("127.0.0.1");
        station.getRedirect().setPath("/redirect");
        station.getRedirect().setPort(8888l);
        station.getRedirect().setProtocol(it.gov.pagopa.gen.wispconverter.client.cache.model.RedirectDto.ProtocolEnum.HTTPS);
        station.getRedirect().setQueryString("param=1");
        configDataV1.getStations().put(station.getStationCode(), station);
        return configDataV1;
    }

    public static void setMock(it.gov.pagopa.gen.wispconverter.client.decouplercaching.invoker.ApiClient client, ResponseEntity response){
        when(client.invokeAPI(any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any())).thenReturn(response);
        when(client.parameterToMultiValueMap(any(),any(),any())).thenReturn(new HttpHeaders());
        when(client.parameterToString(any())).thenReturn("");
        when(client.selectHeaderAccept(any())).thenReturn(Arrays.asList());
        when(client.selectHeaderContentType(any())).thenReturn(MediaType.APPLICATION_JSON);
    }
    public static void setMock(it.gov.pagopa.gen.wispconverter.client.cache.invoker.ApiClient client,ResponseEntity response){
        when(client.invokeAPI(any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any())).thenReturn(response);
        when(client.parameterToMultiValueMap(any(),any(),any())).thenReturn(new HttpHeaders());
        when(client.parameterToString(any())).thenReturn("");
        when(client.selectHeaderAccept(any())).thenReturn(Arrays.asList());
        when(client.selectHeaderContentType(any())).thenReturn(MediaType.APPLICATION_JSON);
    }
    public static void setMock(it.gov.pagopa.gen.wispconverter.client.iuvgenerator.invoker.ApiClient client,ResponseEntity response){
        when(client.invokeAPI(any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any())).thenReturn(response);
        when(client.parameterToMultiValueMap(any(),any(),any())).thenReturn(new HttpHeaders());
        when(client.parameterToString(any())).thenReturn("");
        when(client.selectHeaderAccept(any())).thenReturn(Arrays.asList());
        when(client.selectHeaderContentType(any())).thenReturn(MediaType.APPLICATION_JSON);
    }
    public static void setMock(it.gov.pagopa.gen.wispconverter.client.gpd.invoker.ApiClient client,ResponseEntity response){
        when(client.invokeAPI(any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any())).thenReturn(response);
        when(client.parameterToMultiValueMap(any(),any(),any())).thenReturn(new HttpHeaders());
        when(client.parameterToString(any())).thenReturn("");
        when(client.selectHeaderAccept(any())).thenReturn(Arrays.asList());
        when(client.selectHeaderContentType(any())).thenReturn(MediaType.APPLICATION_JSON);
    }
    public static void setMock(it.gov.pagopa.gen.wispconverter.client.checkout.invoker.ApiClient client,ResponseEntity response){
        when(client.invokeAPI(any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any())).thenReturn(response);
        when(client.parameterToMultiValueMap(any(),any(),any())).thenReturn(new HttpHeaders());
        when(client.parameterToString(any())).thenReturn("");
        when(client.selectHeaderAccept(any())).thenReturn(Arrays.asList());
        when(client.selectHeaderContentType(any())).thenReturn(MediaType.APPLICATION_JSON);
    }


}
