package it.gov.pagopa.wispconverter;

import it.gov.pagopa.wispconverter.util.ZipUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ZipUtilTest {

    private static final String XML_STRING = "<soapenv:Envelope><soapenv:Header></soapenv:Header><soapenv:Body><ws:nodoInviaRPT></ws:nodoInviaRPT></soapenv:Body></soapenv:Envelope>";

    @Test
    public void zip() throws IOException {
        byte[] byteString = ZipUtil.zip(XML_STRING);
        byte[] unzip = ZipUtil.unzip(byteString);
        String utf8String = ZipUtil.getUtf8String(byteString);
        String utf8Unzip = ZipUtil.getUtf8String(unzip);
        assertNotEquals(byteString, unzip);
        assertEquals(XML_STRING, utf8Unzip);
        String s = new String(byteString);
//        assertEquals(XML_STRING, s);
    }

}
