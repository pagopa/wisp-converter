package it.gov.pagopa.wispconverter.utility;

import gov.telematici.pagamenti.ws.nodoperpa.NodoInviaCarrelloRPT;
import gov.telematici.pagamenti.ws.nodoperpa.NodoInviaRPT;
import gov.telematici.pagamenti.ws.nodoperpa.ObjectFactory;
import gov.telematici.pagamenti.ws.nodoperpa.ppthead.IntestazioneCarrelloPPT;
import gov.telematici.pagamenti.ws.nodoperpa.ppthead.IntestazionePPT;
import it.gov.digitpa.schemas._2011.pagamenti.CtRichiestaPagamentoTelematico;
import it.gov.pagopa.wispconverter.util.JaxbElementUtil;
import it.gov.pagopa.wispconverter.utils.TestUtils;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.xmlsoap.schemas.soap.envelope.Body;
import org.xmlsoap.schemas.soap.envelope.Envelope;
import org.xmlsoap.schemas.soap.envelope.Header;

import javax.xml.parsers.ParserConfigurationException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BaseTest {

    @Test
    @SneakyThrows
    public void testSoapCarrello() throws ParserConfigurationException {
        String rptpayload = TestUtils.getCarrelloPayload(1, "station", "100.00", false, "{idCarrello}");
        JaxbElementUtil jaxbElementUtil = new JaxbElementUtil();
        SOAPMessage message = jaxbElementUtil.getMessage(rptpayload);
        IntestazioneCarrelloPPT intestazionePPT2 = jaxbElementUtil.convertToBean(message.getSOAPHeader().extractAllHeaderElements().next(), IntestazioneCarrelloPPT.class);
        NodoInviaCarrelloRPT nodoInviaCarrelloRPT = jaxbElementUtil.convertToBean(message.getSOAPBody().extractContentAsDocument(), NodoInviaCarrelloRPT.class);
        assertTrue(nodoInviaCarrelloRPT.getIdentificativoPSP().contains("{psp}"));
    }

    @SneakyThrows
    @Test
    public void testSoapRPT() throws ParserConfigurationException {
        String rptpayload = TestUtils.getRptPayload(false, "station", "100.00", null);
        JaxbElementUtil jaxbElementUtil = new JaxbElementUtil();
        SOAPMessage message = jaxbElementUtil.getMessage(rptpayload);
        IntestazionePPT intestazionePPT2 = jaxbElementUtil.convertToBean(message.getSOAPHeader().extractAllHeaderElements().next(), IntestazionePPT.class);
        NodoInviaRPT nodoInviaRPT2 = jaxbElementUtil.convertToBean(message.getSOAPBody().extractContentAsDocument(), NodoInviaRPT.class);
        assertTrue(nodoInviaRPT2.getIdentificativoPSP().contains("{psp}"));
    }

    @SneakyThrows
    @Test
    public void testRPT() throws ParserConfigurationException {
        String rptpayload = TestUtils.getInnerRptPayload(false, "100.00", null);
        JaxbElementUtil jaxbElementUtil = new JaxbElementUtil();
        CtRichiestaPagamentoTelematico rpt = jaxbElementUtil.convertToBean(rptpayload, CtRichiestaPagamentoTelematico.class);
        assertTrue(rpt.getDominio().getIdentificativoDominio().contains("{pa}"));

        CtRichiestaPagamentoTelematico ctRichiestaPagamentoTelematico = new it.gov.digitpa.schemas._2011.pagamenti.ObjectFactory().createCtRichiestaPagamentoTelematico();
        JAXBElement<CtRichiestaPagamentoTelematico> rpt1 = new it.gov.digitpa.schemas._2011.pagamenti.ObjectFactory().createRPT(ctRichiestaPagamentoTelematico);
        String string = jaxbElementUtil.objectToString(rpt1);
        assertTrue(true);

    }

    @SneakyThrows
    @Test
    public void testInte() throws ParserConfigurationException {
        IntestazionePPT intestazionePPT = new gov.telematici.pagamenti.ws.nodoperpa.ppthead.ObjectFactory().createIntestazionePPT();
        JaxbElementUtil jaxbElementUtil = new JaxbElementUtil();
        String string = jaxbElementUtil.objectToString(intestazionePPT);
        assertTrue(true);

    }

    @SneakyThrows
    @Test
    public void testGeneraXml() throws ParserConfigurationException, SOAPException {

        JaxbElementUtil jaxbElementUtil = new JaxbElementUtil();

        ObjectFactory objectFactory = new ObjectFactory();
        NodoInviaRPT nodoInviaRPT = objectFactory.createNodoInviaRPT();
        nodoInviaRPT.setIdentificativoCanale("mycanale");
        JAXBElement<NodoInviaRPT> nodoinviarpt = objectFactory.createNodoInviaRPT(nodoInviaRPT);
        IntestazionePPT intestazionePPT = new gov.telematici.pagamenti.ws.nodoperpa.ppthead.ObjectFactory().createIntestazionePPT();

        org.xmlsoap.schemas.soap.envelope.ObjectFactory objectFactoryEnvelope = new org.xmlsoap.schemas.soap.envelope.ObjectFactory();
        Envelope envelope = objectFactoryEnvelope.createEnvelope();
        Body body = objectFactoryEnvelope.createBody();
        body.getAny().add(nodoinviarpt);
        Header header = objectFactoryEnvelope.createHeader();
        header.getAny().add(intestazionePPT);
        envelope.setBody(body);
        envelope.setHeader(header);


        SOAPMessage message = jaxbElementUtil.newMessage();
        jaxbElementUtil.addBody(message, nodoinviarpt, NodoInviaRPT.class);
        jaxbElementUtil.addHeader(message, intestazionePPT, IntestazionePPT.class);

        String ss = jaxbElementUtil.toString(message);
        assertTrue(ss.contains("mycanale"));
    }
}
