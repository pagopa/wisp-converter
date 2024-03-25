package it.gov.pagopa.wispconverter.util;

import it.gov.pagopa.wispconverter.exception.conversion.ConversionException;
import it.gov.pagopa.wispconverter.model.nodoperpa.*;
import it.gov.pagopa.wispconverter.model.unmarshall.RPTRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommonUtility {


    /**
     * @param value value to deNullify.
     * @return return empty string if value is null
     */
    public static String deNull(String value) {
        return Optional.ofNullable(value).orElse("");
    }

    /**
     * @param value value to deNullify.
     * @return return empty string if value is null
     */
    public static String deNull(Object value) {
        return Optional.ofNullable(value).orElse("").toString();
    }

    /**
     * @param value value to deNullify.
     * @return return false if value is null
     */
    public static Boolean deNull(Boolean value) {
        return Optional.ofNullable(value).orElse(false);
    }

    @SuppressWarnings({"rawtypes"})
    public static String getCreditorInstitutionCode(RPTRequest rptRequest) throws ConversionException {
        String creditorInstitutionCode;
        Object header = rptRequest.getHeader();
        if (header instanceof IntestazionePPT intestazionePPT) {
            creditorInstitutionCode = intestazionePPT.getIdentificativoDominio();
        } else if (header instanceof IntestazioneCarrelloPPT intestazioneCarrelloPPT) {
            creditorInstitutionCode = intestazioneCarrelloPPT.getIdentificativoCarrello();
        } else {
            throw new ConversionException("");
        }
        return creditorInstitutionCode;
    }

    @SuppressWarnings({"rawtypes"})
    public static List<byte[]> getAllRawRPTs(RPTRequest rptRequest) throws ConversionException {
        List<byte[]> rawRPTs = new ArrayList<>();
        Object body = rptRequest.getBody();
        if (body instanceof NodoInviaRPT nodoInviaRPT) {
            rawRPTs.add(nodoInviaRPT.getRpt());
        } else if (body instanceof NodoInviaCarrelloRPT nodoInviaCarrelloRPT) {
            rawRPTs.addAll(nodoInviaCarrelloRPT.getListaRPT().getElementoListaRPT().stream()
                    .map(TipoElementoListaRPT::getRpt)
                    .toList());
        } else {
            throw new ConversionException("");
        }
        return rawRPTs;
    }
}
