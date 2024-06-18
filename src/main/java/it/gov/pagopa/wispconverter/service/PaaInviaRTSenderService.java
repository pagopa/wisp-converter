package it.gov.pagopa.wispconverter.service;

import gov.telematici.pagamenti.ws.papernodo.EsitoPaaInviaRT;
import gov.telematici.pagamenti.ws.papernodo.FaultBean;
import gov.telematici.pagamenti.ws.papernodo.PaaInviaRTRisposta;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.util.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class PaaInviaRTSenderService {

    private final RestClient.Builder restClientBuilder;

    public void sendToCreditorInstitution(String url, String payload) {

        try {

            // Generating the REST client and send the passed request payload to the passed URL
            RestClient client = restClientBuilder.build();
            ResponseEntity<PaaInviaRTRisposta> response = client.post()
                    .uri(URI.create(url))
                    .header("SOAPAction", "paaInviaRT")
                    .body(payload)
                    .retrieve()
                    .toEntity(PaaInviaRTRisposta.class);

            // check SOAP response and extract body if it is valid
            PaaInviaRTRisposta body = checkResponseValidity(response);

            // check the response and if the outcome is KO, throw an exception
            EsitoPaaInviaRT esitoPaaInviaRT = body.getPaaInviaRTRisposta();
            if (Constants.KO.equals(esitoPaaInviaRT.getEsito())) {
                FaultBean fault = esitoPaaInviaRT.getFault();
                String faultCode = "ND";
                String faultString = "ND";
                String faultDescr = "ND";
                if (fault != null) {
                    faultCode = fault.getFaultCode();
                    faultString = fault.getFaultString();
                    faultDescr = fault.getDescription();
                }
                throw new AppException(AppErrorCodeMessageEnum.RECEIPT_GENERATION_ERROR_RESPONSE_FROM_CREDITOR_INSTITUTION, faultCode, faultString, faultDescr);
            }

        } catch (AppException e) {

            throw e;

        } catch (Exception e) {

            throw new AppException(AppErrorCodeMessageEnum.RECEIPT_GENERATION_GENERIC_ERROR, e);
        }
    }


    private PaaInviaRTRisposta checkResponseValidity(ResponseEntity<PaaInviaRTRisposta> response) {

        PaaInviaRTRisposta body = response.getBody();

        // check the response received and, if is a 4xx or a 5xx HTTP error code throw an exception
        if (response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()) {
            throw new AppException(AppErrorCodeMessageEnum.CLIENT_PAAINVIART, "Error response: " + response.getStatusCode().value());
        }
        // validating the response body and, if something is null, throw an exception
        if (body == null) {
            throw new AppException(AppErrorCodeMessageEnum.RECEIPT_GENERATION_WRONG_RESPONSE_FROM_CREDITOR_INSTITUTION, "Passed null body");
        }
        if (body.getPaaInviaRTRisposta() == null) {
            throw new AppException(AppErrorCodeMessageEnum.RECEIPT_GENERATION_WRONG_RESPONSE_FROM_CREDITOR_INSTITUTION, "Passed null paaInviaRTRisposta tag");
        }

        return body;
    }

}
