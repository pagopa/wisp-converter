package it.gov.pagopa.wispconverter.service;

import gov.telematici.pagamenti.ws.NodoInviaCarrelloRPT;
import gov.telematici.pagamenti.ws.NodoInviaRPT;
import gov.telematici.pagamenti.ws.TipoElementoListaRPT;
import gov.telematici.pagamenti.ws.ppthead.IntestazioneCarrelloPPT;
import gov.telematici.pagamenti.ws.ppthead.IntestazionePPT;
import it.gov.digitpa.schemas._2011.pagamenti.CtRichiestaPagamentoTelematico;
import it.gov.pagopa.wispconverter.entity.Primitive;
import it.gov.pagopa.wispconverter.entity.RPTRequestEntity;
import it.gov.pagopa.wispconverter.exception.conversion.ConversionException;
import it.gov.pagopa.wispconverter.model.unmarshall.RPTContent;
import it.gov.pagopa.wispconverter.model.unmarshall.RPTRequest;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

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

    @SuppressWarnings({"rawtypes"})
    public CtRichiestaPagamentoTelematico unmarshall(RPTContent rptContent) throws ConversionException {
        CtRichiestaPagamentoTelematico rpt;
        try {
            // extracting byte array containing Base64 of RPT
            byte[] base64Content;
            Object wrapper = rptContent.getWrappedRPT();
            if (wrapper instanceof NodoInviaRPT nodoInviaRPT) {
                base64Content = nodoInviaRPT.getRpt();
            } else if (wrapper instanceof TipoElementoListaRPT tipoElementoListaRPT) {
                base64Content = tipoElementoListaRPT.getRpt();
            } else {
                throw new ConversionException("Unable to unmarshall RPT content for CtRichiestaPagamentoTelematico. Invalid class for RPT wrapper.");
            }
            // converting Base64 to XML string
            byte[] content = Base64.decode(new String(base64Content));
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
        RPTRequest<IntestazionePPT, NodoInviaRPT> rptRequest = null;
        /*
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
         */
        return rptRequest;
    }

    private RPTRequest<IntestazioneCarrelloPPT, NodoInviaCarrelloRPT> unmarshallNodoInviaCarrelloRPT(RPTRequestEntity entity) throws ConversionException {
        RPTRequest<IntestazioneCarrelloPPT, NodoInviaCarrelloRPT> rptRequest = null;
        /*
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
         */
        return rptRequest;
    }
}
