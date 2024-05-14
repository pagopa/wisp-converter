package it.gov.pagopa.wispconverter.service;

import gov.telematici.pagamenti.ws.papernodo.EsitoPaaInviaRT;
import gov.telematici.pagamenti.ws.papernodo.PaaInviaRTRisposta;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.exception.PaaInviaRTException;
import it.gov.pagopa.wispconverter.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class PaaInviaRTService {

    public void send(String url, String payload) {
        try {
            RestClient restClient = RestClient.builder()
                    .requestFactory(new HttpComponentsClientHttpRequestFactory())
                    .baseUrl(url)
                    .build();

            ResponseEntity<PaaInviaRTRisposta> response = restClient
                    .post()
                    .body(payload)
                    .retrieve()
                    .toEntity(PaaInviaRTRisposta.class);

            if( response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError() ) {
                throw new AppException(AppErrorCodeMessageEnum.CLIENT_PAAINVIART, "Error response: " + response.getStatusCode().value());
            }

            if( response.getBody() != null ) {
                EsitoPaaInviaRT esitoPaaInviaRT = response.getBody().getPaaInviaRTRisposta();
                if( esitoPaaInviaRT.getEsito() != null &&
                        esitoPaaInviaRT.getEsito().equals(Constants.KO) &&
                        esitoPaaInviaRT.getFault() != null ) {
                    throw new PaaInviaRTException(
                            esitoPaaInviaRT.getFault().getFaultCode(),
                            esitoPaaInviaRT.getFault().getFaultString(),
                            esitoPaaInviaRT.getFault().getDescription());
                }
            } else {
                throw new AppException(AppErrorCodeMessageEnum.CLIENT_PAAINVIART, "Error response: body null");
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
