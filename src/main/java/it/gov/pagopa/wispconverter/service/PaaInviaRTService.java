package it.gov.pagopa.wispconverter.service;

import gov.telematici.pagamenti.ws.papernodo.PaaInviaRTRisposta;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaaInviaRTService {

    public void send(String url, String payload) {
        try {
            RestClient restClient = RestClient.builder()
                    .requestFactory(new HttpComponentsClientHttpRequestFactory())
                    .baseUrl(url)
                    .build();

            ResponseEntity<PaaInviaRTRisposta> response = restClient.post().body(payload).retrieve().toEntity(PaaInviaRTRisposta.class);

            if( response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError() ) {
                //TODO: capire se gestire errore client specifico
                throw new AppException(AppErrorCodeMessageEnum.ERROR);
            }

            //TODO: check su esito risposta PA
            if( response.getBody() != null &&
                    response.getBody().getPaaInviaRTRisposta().getEsito() != null && response.getBody().getPaaInviaRTRisposta().getEsito().equals(Constants.KO)) {
                //TODO: capire se gestire errore client specifico
                throw new AppException(AppErrorCodeMessageEnum.ERROR);
            }
        } catch (Exception e) {
            throw new AppException(AppErrorCodeMessageEnum.ERROR);
        }
    }

}
