package it.gov.pagopa.wispconverter.utils;


import io.micrometer.core.instrument.util.IOUtils;
import it.gov.pagopa.gen.wispconverter.client.cache.model.ConnectionDto;
import it.gov.pagopa.gen.wispconverter.client.cache.model.ServiceDto;
import it.gov.pagopa.gen.wispconverter.client.cache.model.StationCreditorInstitutionDto;
import it.gov.pagopa.gen.wispconverter.client.gpd.model.PaymentOptionModelDto;
import it.gov.pagopa.gen.wispconverter.client.gpd.model.PaymentOptionModelResponseDto;
import it.gov.pagopa.gen.wispconverter.client.gpd.model.PaymentPositionModelBaseResponseDto;
import it.gov.pagopa.gen.wispconverter.client.gpd.model.PaymentPositionModelDto;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    public static it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigDataV1Dto configData(String stationCode) {
        it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigDataV1Dto configDataV1 = new it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigDataV1Dto();
        configDataV1.setStations(new HashMap<>());
        it.gov.pagopa.gen.wispconverter.client.cache.model.StationDto station = new it.gov.pagopa.gen.wispconverter.client.cache.model.StationDto();
        station.setStationCode(stationCode);
        station.setConnection(new ConnectionDto());
        station.getConnection().setIp("127.0.0.1");
        station.getConnection().setPort(8888L);
        station.getConnection().setProtocol(ConnectionDto.ProtocolEnum.HTTP);
        station.setService(new ServiceDto());
        station.getService().setPath("/path");
        station.setRedirect(new it.gov.pagopa.gen.wispconverter.client.cache.model.RedirectDto());
        station.getRedirect().setIp("127.0.0.1");
        station.getRedirect().setPath("/redirect");
        station.getRedirect().setPort(8888L);
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

    public static it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigDataV1Dto configDataCreditorInstitutionStations
            (StationCreditorInstitutionDto stationCreditorInstitutionDto) {
        it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigDataV1Dto configDataV1 = new it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigDataV1Dto();
        configDataV1.setCreditorInstitutionStations(new HashMap<>());
        configDataV1.getCreditorInstitutionStations().put(stationCreditorInstitutionDto.getCreditorInstitutionCode(), stationCreditorInstitutionDto);
        configDataV1.setStations(new HashMap<>());
        it.gov.pagopa.gen.wispconverter.client.cache.model.StationDto station = new it.gov.pagopa.gen.wispconverter.client.cache.model.StationDto();
        station.setStationCode(stationCreditorInstitutionDto.getStationCode());
        station.setConnection(new ConnectionDto());
        station.getConnection().setIp("127.0.0.1");
        station.getConnection().setPort(8888L);
        station.getConnection().setProtocol(ConnectionDto.ProtocolEnum.HTTP);
        station.setService(new ServiceDto());
        station.getService().setPath("/path");
        station.setRedirect(new it.gov.pagopa.gen.wispconverter.client.cache.model.RedirectDto());
        station.getRedirect().setIp("127.0.0.1");
        station.getRedirect().setPath("/redirect");
        station.getRedirect().setPort(8888L);
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

    public static void setMock(it.gov.pagopa.gen.wispconverter.client.decouplercaching.invoker.ApiClient client, ResponseEntity response) {
        when(client.invokeAPI(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(response);
        when(client.parameterToMultiValueMap(any(), any(), any())).thenReturn(new HttpHeaders());
        when(client.parameterToString(any())).thenReturn("");
        when(client.selectHeaderAccept(any())).thenReturn(List.of());
        when(client.selectHeaderContentType(any())).thenReturn(MediaType.APPLICATION_JSON);
    }

    public static void setMock(it.gov.pagopa.gen.wispconverter.client.cache.invoker.ApiClient client, ResponseEntity response) {
        when(client.invokeAPI(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(response);
        when(client.parameterToMultiValueMap(any(), any(), any())).thenReturn(new HttpHeaders());
        when(client.parameterToString(any())).thenReturn("");
        when(client.selectHeaderAccept(any())).thenReturn(List.of());
        when(client.selectHeaderContentType(any())).thenReturn(MediaType.APPLICATION_JSON);
    }

    public static void setMock(it.gov.pagopa.gen.wispconverter.client.iuvgenerator.invoker.ApiClient client, ResponseEntity response) {
        when(client.invokeAPI(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(response);
        when(client.parameterToMultiValueMap(any(), any(), any())).thenReturn(new HttpHeaders());
        when(client.parameterToString(any())).thenReturn("");
        when(client.selectHeaderAccept(any())).thenReturn(List.of());
        when(client.selectHeaderContentType(any())).thenReturn(MediaType.APPLICATION_JSON);
    }

    public static void setMock(it.gov.pagopa.gen.wispconverter.client.gpd.invoker.ApiClient client, ResponseEntity response) {
        when(client.invokeAPI(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(response);
        when(client.parameterToMultiValueMap(any(), any(), any())).thenReturn(new HttpHeaders());
        when(client.parameterToString(any())).thenReturn("");
        when(client.selectHeaderAccept(any())).thenReturn(List.of());
        when(client.selectHeaderContentType(any())).thenReturn(MediaType.APPLICATION_JSON);
    }

    public static void setMockGet(it.gov.pagopa.gen.wispconverter.client.gpd.invoker.ApiClient client, ResponseEntity response) {
        when(client.invokeAPI(any(), eq(HttpMethod.GET), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(response);
        when(client.parameterToMultiValueMap(any(), any(), any())).thenReturn(new HttpHeaders());
        when(client.parameterToString(any())).thenReturn("");
        when(client.selectHeaderAccept(any())).thenReturn(List.of());
        when(client.selectHeaderContentType(any())).thenReturn(MediaType.APPLICATION_JSON);
    }

    public static void setMockGetExceptionNotFound(it.gov.pagopa.gen.wispconverter.client.gpd.invoker.ApiClient client) {
        when(client.invokeAPI(any(), eq(HttpMethod.GET), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new AppException(new HttpClientErrorException(HttpStatus.NOT_FOUND), AppErrorCodeMessageEnum.CLIENT_GPD));
    }

    public static void setMockGetExceptionBadRequest(it.gov.pagopa.gen.wispconverter.client.gpd.invoker.ApiClient client) {
        when(client.invokeAPI(any(), eq(HttpMethod.GET), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new AppException(new HttpClientErrorException(HttpStatus.BAD_REQUEST), AppErrorCodeMessageEnum.CLIENT_GPD));
    }

    public static void setMockPut(it.gov.pagopa.gen.wispconverter.client.gpd.invoker.ApiClient client, ResponseEntity response) {
        when(client.invokeAPI(any(), eq(HttpMethod.PUT), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(response);
        when(client.parameterToMultiValueMap(any(), any(), any())).thenReturn(new HttpHeaders());
        when(client.parameterToString(any())).thenReturn("");
        when(client.selectHeaderAccept(any())).thenReturn(List.of());
        when(client.selectHeaderContentType(any())).thenReturn(MediaType.APPLICATION_JSON);
    }

    public static void setMockPost(it.gov.pagopa.gen.wispconverter.client.gpd.invoker.ApiClient client, ResponseEntity response) {
        when(client.invokeAPI(any(), eq(HttpMethod.POST), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(response);
        when(client.parameterToMultiValueMap(any(), any(), any())).thenReturn(new HttpHeaders());
        when(client.parameterToString(any())).thenReturn("");
        when(client.selectHeaderAccept(any())).thenReturn(List.of());
        when(client.selectHeaderContentType(any())).thenReturn(MediaType.APPLICATION_JSON);
    }

    public static void setMock(it.gov.pagopa.gen.wispconverter.client.checkout.invoker.ApiClient client, ResponseEntity response) {
        when(client.invokeAPI(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(response);
        when(client.parameterToMultiValueMap(any(), any(), any())).thenReturn(new HttpHeaders());
        when(client.parameterToString(any())).thenReturn("");
        when(client.selectHeaderAccept(any())).thenReturn(List.of());
        when(client.selectHeaderContentType(any())).thenReturn(MediaType.APPLICATION_JSON);
    }

    public static String getInnerRptPayload(boolean bollo, String amount, String datiSpecificiRiscossione) {
        if (datiSpecificiRiscossione == null) {
            datiSpecificiRiscossione = "9/tipodovuto_7/datospecifico";
        }
        String rpt = TestUtils.loadFileContent(bollo ? "/requests/rptBollo.xml" : "/requests/rpt.xml");
        return rpt
                .replace("{datiSpecificiRiscossione}", datiSpecificiRiscossione)
                .replaceAll("\\{amount}", amount);
    }

    public static String getRptPayload(boolean bollo, String station, String amount, String datiSpecificiRiscossione) {
        if (datiSpecificiRiscossione == null) {
            datiSpecificiRiscossione = "9/tipodovuto_7/datospecifico";
        }
        String rpt = TestUtils.loadFileContent(bollo ? "/requests/rptBollo.xml" : "/requests/rpt.xml");
        String rptreplace = rpt
                .replace("{datiSpecificiRiscossione}", datiSpecificiRiscossione)
                .replaceAll("\\{amount}", amount);
        String nodoInviaRPT = TestUtils.loadFileContent("/requests/nodoInviaRPT.xml");
        return nodoInviaRPT
                .replace("{station}", station)
                .replace("{rpt}", Base64.getEncoder().encodeToString(rptreplace.getBytes(StandardCharsets.UTF_8)));
    }

    public static String getRptNullIbanPayload(String station, String amount, String datiSpecificiRiscossione) {
        if (datiSpecificiRiscossione == null) {
            datiSpecificiRiscossione = "9/tipodovuto_7/datospecifico";
        }
        String rpt = TestUtils.loadFileContent("/requests/rptNullIban.xml");
        String rptreplace = rpt
                .replace("{datiSpecificiRiscossione}", datiSpecificiRiscossione)
                .replaceAll("\\{amount}", amount);
        String nodoInviaRPT = TestUtils.loadFileContent("/requests/nodoInviaRPT.xml");
        return nodoInviaRPT
                .replace("{station}", station)
                .replace("{rpt}", Base64.getEncoder().encodeToString(rptreplace.getBytes(StandardCharsets.UTF_8)));
    }

    public static String getCarrelloPayload(int numofrpt, String station, String amount, boolean multibeneficiario) {
        String rpt = TestUtils.loadFileContent("/requests/rptCart.xml");
        String rptReplaceAmount = rpt.replaceAll("\\{amount}", amount);
        String iuv = "123456IUVMOCK%s";
        StringBuilder listaRpt = new StringBuilder();
        for (int i = 0; i < numofrpt; i++) {
            String rptReplaceIuv = rptReplaceAmount.replaceAll("\\{iuv}", String.format(iuv, i));
            listaRpt.append(
                    ("<elementoListaRPT>" +
                            "<identificativoDominio></identificativoDominio>" +
                            "<identificativoUnivocoVersamento></identificativoUnivocoVersamento>" +
                            "<codiceContestoPagamento></codiceContestoPagamento>" +
                            "<tipoFirma></tipoFirma>" +
                            "<rpt>{rpt}</rpt>" +
                            "</elementoListaRPT>").replace("{rpt}", Base64.getEncoder().encodeToString(rptReplaceIuv.getBytes(StandardCharsets.UTF_8)))
            );
        }

        String carrello = TestUtils.loadFileContent("/requests/nodoInviaCarrelloRPT.xml");
        return carrello
                .replace("{station}", station)
                .replace("{multi}", multibeneficiario ? "<multiBeneficiario>true</multiBeneficiario>" : "")
                .replace("{elementiRpt}", listaRpt.toString());
    }

    public static byte[] zip(byte[] uncompressed) throws IOException {
        ByteArrayOutputStream bais = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(bais);
        gzipOutputStream.write(uncompressed);
        gzipOutputStream.close();
        bais.close();
        return bais.toByteArray();
    }

    public static String zipAndEncode(String p) {
        try {
            return new String(Base64.getEncoder().encode(zip(p.getBytes(StandardCharsets.UTF_8))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static PaymentPositionModelBaseResponseDto getValidPaymentPositionModelBaseResponseDto() {
        PaymentPositionModelBaseResponseDto paymentPositionModelBaseResponseDto = new PaymentPositionModelBaseResponseDto();
        paymentPositionModelBaseResponseDto.setOrganizationFiscalCode("org12345678");
        paymentPositionModelBaseResponseDto.setIupd("abcd-efgh-ilmn-opqr");
        paymentPositionModelBaseResponseDto.setStatus(PaymentPositionModelBaseResponseDto.StatusEnum.VALID);
        paymentPositionModelBaseResponseDto.setPaymentOption(List.of(getPaymentOptionModelResponseDto()));
        return paymentPositionModelBaseResponseDto;
    }

    public static PaymentPositionModelBaseResponseDto getInvalidPaymentPositionModelBaseResponseDto() {
        PaymentPositionModelBaseResponseDto paymentPositionModelBaseResponseDto = new PaymentPositionModelBaseResponseDto();
        paymentPositionModelBaseResponseDto.setOrganizationFiscalCode("org12345678");
        paymentPositionModelBaseResponseDto.setIupd("abcd-efgh-ilmn-opqr");
        paymentPositionModelBaseResponseDto.setStatus(PaymentPositionModelBaseResponseDto.StatusEnum.INVALID);
        paymentPositionModelBaseResponseDto.setPaymentOption(List.of(getPaymentOptionModelResponseDto()));
        return paymentPositionModelBaseResponseDto;
    }

    public static PaymentOptionModelResponseDto getPaymentOptionModelResponseDto() {
        PaymentOptionModelResponseDto paymentOptionModelResponseDto = new PaymentOptionModelResponseDto();
        paymentOptionModelResponseDto.setIuv("123456IUVMOCK1");
        paymentOptionModelResponseDto.setAmount(200L);
        paymentOptionModelResponseDto.setNav("3123456IUVMOCK1");
        return paymentOptionModelResponseDto;
    }

    public static PaymentPositionModelDto getPaymentPositionModelDto() {
        PaymentPositionModelDto paymentPositionModelDto = new PaymentPositionModelDto();
        paymentPositionModelDto.setIupd("abcd-efgh-ilmn-opqr");
        paymentPositionModelDto.setPaymentOption(List.of(getPaymentOptionModelDto()));
        return paymentPositionModelDto;
    }

    public static PaymentOptionModelDto getPaymentOptionModelDto() {
        PaymentOptionModelDto paymentOptionModelDto = new PaymentOptionModelDto();
        paymentOptionModelDto.setIuv("123456IUVMOCK1");
        paymentOptionModelDto.setAmount(200L);
        paymentOptionModelDto.setNav("3123456IUVMOCK1");
        return paymentOptionModelDto;
    }

}
