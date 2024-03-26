package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.wispconverter.entity.Primitive;
import it.gov.pagopa.wispconverter.entity.RPTRequestEntity;
import it.gov.pagopa.wispconverter.exception.conversion.ConversionException;
import it.gov.pagopa.wispconverter.model.nodoperpa.IntestazioneCarrelloPPT;
import it.gov.pagopa.wispconverter.model.nodoperpa.IntestazionePPT;
import it.gov.pagopa.wispconverter.model.nodoperpa.NodoInviaCarrelloRPT;
import it.gov.pagopa.wispconverter.model.nodoperpa.NodoInviaRPT;
import it.gov.pagopa.wispconverter.model.rpt.CtRichiestaPagamentoTelematico;
import it.gov.pagopa.wispconverter.model.unmarshall.RPTRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

@Service
public class UnmarshallerService {

    private final DocumentBuilder docBuilder;
    private final JAXBContext nodoInviaRPTBodyContext;
    private final JAXBContext nodoInviaCarrelloRPTBodyContext;
    private final JAXBContext intestazionePPTContext;
    private final JAXBContext intestazioneCarrelloPPTContext;
    private final JAXBContext rptContext;


    public UnmarshallerService(@Autowired DocumentBuilder documentBuilder) throws JAXBException {
        this.intestazionePPTContext = JAXBContext.newInstance(IntestazionePPT.class);
        this.intestazioneCarrelloPPTContext = JAXBContext.newInstance(IntestazioneCarrelloPPT.class);
        this.nodoInviaRPTBodyContext = JAXBContext.newInstance(NodoInviaRPT.class);
        this.nodoInviaCarrelloRPTBodyContext = JAXBContext.newInstance(NodoInviaCarrelloRPT.class);
        this.rptContext = JAXBContext.newInstance(CtRichiestaPagamentoTelematico.class);
        this.docBuilder = documentBuilder;
    }

    @SuppressWarnings({"rawtypes"})
    public RPTRequest unmarshall(RPTRequestEntity entity) throws ConversionException {
        Primitive primitive = Primitive.fromString(entity.getPrimitive());
        if (primitive == null) {
            throw new ConversionException(String.format("Unable to unmarshall RPT header or body. The string object refers to a primitive that is not handled by this service. Use one of the following: [%s]", Arrays.asList(Primitive.values())));
        }

        RPTRequest response;
        switch (primitive) {
            case NODO_INVIA_RPT -> response = unmarshallNodoInviaRPT(entity);
            case NODO_INVIA_CARRELLO_RPT -> response = unmarshallNodoInviaCarrelloRPT(entity);
            default ->
                    throw new ConversionException(String.format("Unable to unmarshall RPT header or body. No valid parsing process was defined for the primitive [%s].", primitive));
        }
        return response;
    }

    public CtRichiestaPagamentoTelematico unmarshall(byte[] content) throws ConversionException {
        CtRichiestaPagamentoTelematico rpt;
        try {
            // unmarshalling header content
            Unmarshaller rptUnmarshaller = rptContext.createUnmarshaller();
            Document rptDoc = this.docBuilder.parse(new ByteArrayInputStream(content));
            rpt = rptUnmarshaller.unmarshal(rptDoc, CtRichiestaPagamentoTelematico.class).getValue();
        } catch (JAXBException | IOException | SAXException e) {
            throw new ConversionException("Unable to unmarshall RPT content for CtRichiestaPagamentoTelematico. ", e);
        }
        return rpt;
    }

    private RPTRequest<IntestazionePPT, NodoInviaRPT> unmarshallNodoInviaRPT(RPTRequestEntity entity) throws ConversionException {
        RPTRequest<IntestazionePPT, NodoInviaRPT> rptRequest;
        try {
            // unmarshalling header content
            Unmarshaller intestazionePPTUnmarshaller = intestazionePPTContext.createUnmarshaller();
            Document intestazionePPTDoc = this.docBuilder.parse(new ByteArrayInputStream(entity.getHeader().getBytes()));
            IntestazionePPT header = intestazionePPTUnmarshaller.unmarshal(intestazionePPTDoc, IntestazionePPT.class).getValue();

            // unmarshalling body content
            Unmarshaller nodoInviaRPTBodyUnmarshaller = nodoInviaRPTBodyContext.createUnmarshaller();
            Document nodoInviaRPTBodyDoc = this.docBuilder.parse(new ByteArrayInputStream(entity.getBody().getBytes()));
            NodoInviaRPT body = nodoInviaRPTBodyUnmarshaller.unmarshal(nodoInviaRPTBodyDoc, NodoInviaRPT.class).getValue();

            // generating complete response
            rptRequest = RPTRequest.<IntestazionePPT, NodoInviaRPT>builder()
                    .body(body)
                    .header(header)
                    .build();
        } catch (JAXBException | IOException | SAXException e) {
            throw new ConversionException("Unable to unmarshall RPT header or body for NodoInviaRPT. ", e);
        }
        return rptRequest;
    }

    private RPTRequest<IntestazioneCarrelloPPT, NodoInviaCarrelloRPT> unmarshallNodoInviaCarrelloRPT(RPTRequestEntity entity) throws ConversionException {
        RPTRequest<IntestazioneCarrelloPPT, NodoInviaCarrelloRPT> rptRequest;
        try {
            // unmarshalling header content
            Unmarshaller intestazioneCarrelloPPTUnmarshaller = intestazioneCarrelloPPTContext.createUnmarshaller();
            Document intestazioneCarrelloPPTDoc = this.docBuilder.parse(new ByteArrayInputStream(entity.getHeader().getBytes()));
            IntestazioneCarrelloPPT header = intestazioneCarrelloPPTUnmarshaller.unmarshal(intestazioneCarrelloPPTDoc, IntestazioneCarrelloPPT.class).getValue();

            // unmarshalling body content
            Unmarshaller nodoInviaCarrelloRPTBodyUnmarshaller = nodoInviaCarrelloRPTBodyContext.createUnmarshaller();
            Document nodoInviaCarrelloRPTBodyDoc = this.docBuilder.parse(new ByteArrayInputStream(entity.getBody().getBytes()));
            NodoInviaCarrelloRPT body = nodoInviaCarrelloRPTBodyUnmarshaller.unmarshal(nodoInviaCarrelloRPTBodyDoc, NodoInviaCarrelloRPT.class).getValue();

            // generating complete response
            rptRequest = RPTRequest.<IntestazioneCarrelloPPT, NodoInviaCarrelloRPT>builder()
                    .body(body)
                    .header(header)
                    .build();
        } catch (JAXBException | IOException | SAXException e) {
            throw new ConversionException("Unable to unmarshall RPT header or body for NodoInviaCarrelloRPT. ", e);
        }
        return rptRequest;
    }
}
