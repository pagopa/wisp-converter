package it.gov.pagopa.wispconverter;

import gov.telematici.pagamenti.ws.ObjectFactory;
import gov.telematici.pagamenti.ws.PaaInviaRT;
import gov.telematici.pagamenti.ws.ppthead.IntestazionePPT;
import it.gov.pagopa.wispconverter.util.JaxbElementUtil;
import jakarta.xml.bind.JAXBElement;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Test;
import org.xmlsoap.schemas.soap.envelope.Body;
import org.xmlsoap.schemas.soap.envelope.Envelope;
import org.xmlsoap.schemas.soap.envelope.Header;

public class BaseTest {

    public DocumentBuilderFactory documentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return dbf;
    }

    @Test
    public void testSoap() throws ParserConfigurationException {
        ObjectFactory objectFactory = new ObjectFactory();
        JAXBElement<PaaInviaRT> paaInviaRT = objectFactory.createPaaInviaRT(objectFactory.createPaaInviaRT());
        IntestazionePPT intestazionePPT = new gov.telematici.pagamenti.ws.ppthead.ObjectFactory().createIntestazionePPT();

        org.xmlsoap.schemas.soap.envelope.ObjectFactory objectFactoryEnvelope = new org.xmlsoap.schemas.soap.envelope.ObjectFactory();
        Envelope envelope = objectFactoryEnvelope.createEnvelope();
        Body body = objectFactoryEnvelope.createBody();
        body.getAny().add(paaInviaRT);
        Header header = objectFactoryEnvelope.createHeader();
        header.getAny().add(intestazionePPT);
        envelope.setBody(body);
        envelope.setHeader(header);

        JAXBElement<Envelope> envelope1 = objectFactoryEnvelope.createEnvelope(envelope);

        String s = new JaxbElementUtil(documentBuilderFactory()).convertToString(envelope1, Envelope.class);

        System.out.println(s);

    }
}
