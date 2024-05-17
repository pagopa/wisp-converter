package it.gov.pagopa.wispconverter.service;

import gov.telematici.pagamenti.ws.papernodo.EsitoPaaInviaRT;
import gov.telematici.pagamenti.ws.papernodo.PaaInviaRTRisposta;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.exception.PaaInviaRTException;
import it.gov.pagopa.wispconverter.util.Constants;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class PaaInviaRTService {

    private final RestClient.Builder restClientBuilder;

    public void send(String url, String payload) {
        try {
            RestClient restClient = restClientBuilder.build();
            ResponseEntity<PaaInviaRTRisposta> response = restClient
                    .post()
                    .uri(URI.create(url))
                    .body(payload)
                    .retrieve()
                    .toEntity(PaaInviaRTRisposta.class);

            if( response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError() ) {
                throw new AppException(AppErrorCodeMessageEnum.CLIENT_PAAINVIART, "Error response: " + response.getStatusCode().value());
            }

            PaaInviaRTRisposta body = response.getBody();

            if(body == null){
                throw new AppException(AppErrorCodeMessageEnum.CLIENT_PAAINVIART, "Error response: body null");
            }
            if(body.getPaaInviaRTRisposta() == null){
                throw new AppException(AppErrorCodeMessageEnum.CLIENT_PAAINVIART, "Error response: paaInviaRTRisposta null");
            }
            EsitoPaaInviaRT esitoPaaInviaRT = body.getPaaInviaRTRisposta();
            if( esitoPaaInviaRT.getEsito() != null &&
                    esitoPaaInviaRT.getEsito().equals(Constants.KO) &&
                    esitoPaaInviaRT.getFault() != null ) {
                throw new PaaInviaRTException(
                        esitoPaaInviaRT.getFault().getFaultCode(),
                        esitoPaaInviaRT.getFault().getFaultString(),
                        esitoPaaInviaRT.getFault().getDescription());
            }
        } catch (PaaInviaRTException paaInviaRTException) {
            throw new AppException(AppErrorCodeMessageEnum.CLIENT_PAAINVIART, "Error response: " + paaInviaRTException.getErrorMessage());
        } catch (AppException appException) {
            throw appException;
        } catch (Exception exception) {
            throw new AppException(AppErrorCodeMessageEnum.ERROR, exception.getMessage());
        }
    }

}
