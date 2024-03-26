package it.gov.pagopa.wispconverter.util;

import gov.telematici.pagamenti.ws.NodoInviaCarrelloRPT;
import gov.telematici.pagamenti.ws.NodoInviaRPT;
import gov.telematici.pagamenti.ws.TipoElementoListaRPT;
import gov.telematici.pagamenti.ws.ppthead.IntestazioneCarrelloPPT;
import gov.telematici.pagamenti.ws.ppthead.IntestazionePPT;
import it.gov.pagopa.wispconverter.exception.conversion.ConversionException;
import it.gov.pagopa.wispconverter.model.unmarshall.RPTContent;
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
        // the request is a NodoInviaRPT, the CI code is included directly in header
        if (header instanceof IntestazionePPT intestazionePPT) {
            creditorInstitutionCode = intestazionePPT.getIdentificativoDominio();
        }
        // the request is a NodoInviaCarrelloRPT, the CI code is included directly in header
        else if (header instanceof IntestazioneCarrelloPPT intestazioneCarrelloPPT && rptRequest.getBody() instanceof NodoInviaCarrelloRPT nodoInviaCarrelloRPT) {

            // if cart is for 'multibeneficiario', the creditor institution code is surely included as first 11 characters in cart id
            if (Boolean.TRUE.equals(nodoInviaCarrelloRPT.isMultiBeneficiario())) {
                creditorInstitutionCode = intestazioneCarrelloPPT.getIdentificativoCarrello().substring(0, 11);
            }
            // if cart is not for 'multibeneficiario', get the first RPT from the list and get the creditor institution code from "identificativoDominio" field
            else {
                TipoElementoListaRPT tipoElementoListaRPT = nodoInviaCarrelloRPT.getListaRPT().getElementoListaRPT().stream()
                        .findFirst()
                        .orElseThrow(() -> new ConversionException("Unable to extract creditor institution code from request. The NodoInviaCarrelloRPT request does not contains an ElementoListaRPT from which extract the 'identificativoDominio' field."));
                creditorInstitutionCode = tipoElementoListaRPT.getIdentificativoDominio();
            }
        }
        // the request is not a valid type, throw an error
        else {
            throw new ConversionException("Unable to extract creditor institution code from request. The request type is not accepted for the creditor institution code extraction.");
        }
        return creditorInstitutionCode;
    }

    @SuppressWarnings({"rawtypes"})
    public static List<RPTContent> getAllRawRPTs(RPTRequest rptRequest) throws ConversionException {
        List<RPTContent> rawRPTs = new ArrayList<>();
        Object body = rptRequest.getBody();
        if (body instanceof NodoInviaRPT nodoInviaRPT) {
            rawRPTs.add(RPTContent.builder()
                    .wrappedRPT(nodoInviaRPT)
                    .build());
        } else if (body instanceof NodoInviaCarrelloRPT nodoInviaCarrelloRPT) {
            rawRPTs.addAll(nodoInviaCarrelloRPT.getListaRPT()
                    .getElementoListaRPT()
                    .stream()
                    .map(rpt -> RPTContent.builder()
                            .wrappedRPT(rpt)
                            .build())
                    .toList());
        } else {
            throw new ConversionException("Unable to extract the list of Base64-encoded RPTs. The request type is not accepted.");
        }
        return rawRPTs;
    }
}
