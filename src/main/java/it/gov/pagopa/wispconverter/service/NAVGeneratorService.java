package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.wispconverter.util.client.iuvgenerator.IuvGeneratorClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NAVGeneratorService {

    private final IuvGeneratorClient iuvGeneratorClient;

    @Value("${wisp-converter.aux-digit}")
    private String auxDigit;

    @Value("${wisp-converter.segregation-code}")
    private String segregationCode;

    public String getNAVCodeFromIUVGenerator(String creditorInstitutionCode) {
        it.gov.pagopa.iuvgeneratorclient.api.IuvGeneratorApiApi apiInstance = new it.gov.pagopa.iuvgeneratorclient.api.IuvGeneratorApiApi(iuvGeneratorClient);
        it.gov.pagopa.iuvgeneratorclient.model.IuvGenerationModelDto iuvGenerationModelDto = new it.gov.pagopa.iuvgeneratorclient.model.IuvGenerationModelDto();
        iuvGenerationModelDto.setAuxDigit(Long.valueOf(this.auxDigit));
        iuvGenerationModelDto.setSegregationCode(Long.valueOf(this.segregationCode));

        it.gov.pagopa.iuvgeneratorclient.model.IuvGenerationModelResponseDto result = apiInstance.generateIUV(creditorInstitutionCode, iuvGenerationModelDto);
        return result.getIuv();
        //FIXME gestire errori di connessione
    }
}
