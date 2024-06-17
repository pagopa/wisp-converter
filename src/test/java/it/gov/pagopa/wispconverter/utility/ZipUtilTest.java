package it.gov.pagopa.wispconverter.utility;

import it.gov.pagopa.wispconverter.util.ZipUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ZipUtilTest {

    private static final String XML_STRING = "<soapenv:Envelope><soapenv:Header></soapenv:Header><soapenv:Body><ws:nodoInviaRPT></ws:nodoInviaRPT></soapenv:Body></soapenv:Envelope>";

    @Test
    void zip() throws IOException {
        byte[] byteString = ZipUtil.zip(XML_STRING);
        byte[] unzip = ZipUtil.unzip(byteString);
        String utf8Unzip = ZipUtil.getUtf8String(unzip);
        assertNotEquals(byteString, unzip);
        assertEquals(XML_STRING, utf8Unzip);
    }

}
