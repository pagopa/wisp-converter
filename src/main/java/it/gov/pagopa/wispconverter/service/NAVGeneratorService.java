package it.gov.pagopa.wispconverter.service;

import feign.FeignException;
import it.gov.pagopa.wispconverter.client.iuvgenerator.IUVGeneratorClient;
import it.gov.pagopa.wispconverter.client.iuvgenerator.model.IUVGeneratorRequest;
import it.gov.pagopa.wispconverter.client.iuvgenerator.model.IUVGeneratorResponse;
import it.gov.pagopa.wispconverter.exception.AppClientException;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NAVGeneratorService {

    private final IUVGeneratorClient iuvGeneratorClient;

    @Value("${wisp-converter.aux-digit}")
    private String auxDigit;

    @Value("${wisp-converter.segregation-code}")
    private String segregationCode;

    public String getNAVCodeFromIUVGenerator(String creditorInstitutionCode) {
        // generating request body
        IUVGeneratorRequest request = IUVGeneratorRequest.builder()
                .auxDigit(this.auxDigit)
                .segregationCode(this.segregationCode)
                .build();
        // communicating with IUV Generator service in order to retrieve response
        String navCode;
        try {
            IUVGeneratorResponse response = this.iuvGeneratorClient.generate(creditorInstitutionCode, request);
            if (response == null) {
                throw new AppException(AppErrorCodeMessageEnum.CLIENT_, "IUVGeneratorResponse is null");
            }
            navCode = response.getIuv();
        } catch (FeignException e) {
            throw new AppClientException(e, e.status(), AppErrorCodeMessageEnum.CLIENT_, e.getMessage());
        }
        return navCode;
    }
}
