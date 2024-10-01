package it.gov.pagopa.wispconverter.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import it.gov.pagopa.wispconverter.util.ReceiptRequestHandler;
import it.gov.pagopa.wispconverter.util.ReceiptRequestHandler.PaSendRTV2Request;

class ReceiptRequestHandlerTest {

	@Test
	void parsePaSendRTV2Request() throws ParserConfigurationException, SAXException, IOException {
		ReceiptRequestHandler receiptRequestHandler = new ReceiptRequestHandler();
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		saxParser.parse("src/test/resources/requests/paSendRTV2Request.xml", receiptRequestHandler);
		
		PaSendRTV2Request result = receiptRequestHandler.getPaSendRTV2Request();
		assertEquals("348172725623804858", result.getNoticeNumber());
		assertEquals("15376371009", result.getFiscalCode());
		assertEquals("863965926210520", result.getCreditorReferenceId());
	}
}
