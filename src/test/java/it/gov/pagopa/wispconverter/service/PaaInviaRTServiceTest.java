package it.gov.pagopa.wispconverter.service;

import gov.telematici.pagamenti.ws.papernodo.EsitoPaaInviaRT;
import gov.telematici.pagamenti.ws.papernodo.FaultBean;
import gov.telematici.pagamenti.ws.papernodo.PaaInviaRTRisposta;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.service.PaaInviaRTService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaaInviaRTServiceTest {

    @Test
    void esitoOK(){
        RestClient.Builder builder = mock(RestClient.Builder.class);
        RestClient client = mock(RestClient.class);
        when(builder.build()).thenReturn(client);
        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        when(client.post()).thenReturn(requestBodyUriSpec);
        RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
        when(requestBodyUriSpec.uri(any(URI.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);

        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        EsitoPaaInviaRT esitoPaaInviaRT = new EsitoPaaInviaRT();
        esitoPaaInviaRT.setEsito("OK");
        PaaInviaRTRisposta paaInviaRTRisposta = new PaaInviaRTRisposta();
        paaInviaRTRisposta.setPaaInviaRTRisposta(esitoPaaInviaRT);
        when(responseSpec.toEntity(PaaInviaRTRisposta.class))
        .thenReturn(ResponseEntity.<PaaInviaRTRisposta>ok().body(paaInviaRTRisposta));

        PaaInviaRTService p = new PaaInviaRTService(builder);
        p.send("","");
        assertTrue(true);
    }

    @Test
    void esitoKO(){
        RestClient.Builder builder = mock(RestClient.Builder.class);
        RestClient client = mock(RestClient.class);
        when(builder.build()).thenReturn(client);
        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        when(client.post()).thenReturn(requestBodyUriSpec);
        RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
        when(requestBodyUriSpec.uri(any(URI.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);

        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        FaultBean faultBean = new FaultBean();
        EsitoPaaInviaRT esitoPaaInviaRT = new EsitoPaaInviaRT();
        esitoPaaInviaRT.setEsito("KO");
        esitoPaaInviaRT.setFault(faultBean);
        PaaInviaRTRisposta paaInviaRTRisposta = new PaaInviaRTRisposta();
        paaInviaRTRisposta.setPaaInviaRTRisposta(esitoPaaInviaRT);
        when(responseSpec.toEntity(PaaInviaRTRisposta.class))
                .thenReturn(ResponseEntity.<PaaInviaRTRisposta>ok().body(paaInviaRTRisposta));

        PaaInviaRTService p = new PaaInviaRTService(builder);
        try{
            p.send("","");
            assertTrue(false);
        }catch (AppException e){
            assertTrue(true);
        }
    }
}
