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

        configDataV1.setConfigurations(new HashMap<>());
        configDataV1.getConfigurations().put("GLOBAL-istitutoAttestante.identificativoUnivocoAttestante.tipoIdentificativoUnivoco", new it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigurationKeyDto().value("G"));
        configDataV1.getConfigurations().put("GLOBAL-istitutoAttestante.identificativoUnivocoAttestante.codiceIdentificativoUnivoco", new it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigurationKeyDto().value("codiceIdentificativoUnivoco"));
        configDataV1.getConfigurations().put("GLOBAL-istitutoAttestante.denominazioneAttestante", new it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigurationKeyDto().value("denominazioneAttestante"));
        configDataV1.getConfigurations().put("GLOBAL-istitutoAttestante.codiceUnitOperAttestante", new it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigurationKeyDto().value("codiceUnitOperAttestante"));
        configDataV1.getConfigurations().put("GLOBAL-istitutoAttestante.denomUnitOperAttestante", new it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigurationKeyDto().value("denomUnitOperAttestante"));
        configDataV1.getConfigurations().put("GLOBAL-istitutoAttestante.indirizzoAttestante", new it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigurationKeyDto().value("indirizzoAttestante"));
        configDataV1.getConfigurations().put("GLOBAL-istitutoAttestante.civicoAttestante", new it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigurationKeyDto().value("civicoAttestante"));
        configDataV1.getConfigurations().put("GLOBAL-istitutoAttestante.capAttestante", new it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigurationKeyDto().value("capAttestante"));
        configDataV1.getConfigurations().put("GLOBAL-istitutoAttestante.localitaAttestante", new it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigurationKeyDto().value("localitaAttestante"));
        configDataV1.getConfigurations().put("GLOBAL-istitutoAttestante.provinciaAttestante", new it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigurationKeyDto().value("provinciaAttestante"));
        configDataV1.getConfigurations().put("GLOBAL-istitutoAttestante.nazioneAttestante", new it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigurationKeyDto().value("nazioneAttestante"));
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
