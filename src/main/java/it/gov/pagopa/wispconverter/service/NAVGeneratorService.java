package it.gov.pagopa.wispconverter.service;

import feign.FeignException;
import it.gov.pagopa.wispconverter.client.IUVGeneratorClient;
import it.gov.pagopa.wispconverter.exception.conversion.ConversionException;
import it.gov.pagopa.wispconverter.model.client.iuvgenerator.IUVGeneratorRequest;
import it.gov.pagopa.wispconverter.model.client.iuvgenerator.IUVGeneratorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NAVGeneratorService {

    private final IUVGeneratorClient iuvGeneratorClient;

    private final String auxDigit;

    private final String segregationCode;

    public NAVGeneratorService(@Autowired IUVGeneratorClient iuvGeneratorClient,
                               @Value("${wisp-converter.aux-digit}") String auxDigit,
                               @Value("${wisp-converter.segregation-code}") String segregationCode) {
        this.iuvGeneratorClient = iuvGeneratorClient;
        this.auxDigit = auxDigit;
        this.segregationCode = segregationCode;
    }

    public String getNAVCodeFromIUVGenerator(String creditorInstitutionCode) throws ConversionException {
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
                throw new ConversionException("Unable to retrieve NAV code from IUV Generator service. Retrieved null response.");
            }
            navCode = response.getIuv();
        } catch (FeignException e) {
            throw new ConversionException("Unable to retrieve NAV code from IUV Generator service. An error occurred during communication with service:", e);
        }
        return navCode;
    }
}
