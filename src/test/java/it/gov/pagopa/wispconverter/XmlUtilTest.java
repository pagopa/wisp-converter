package it.gov.pagopa.wispconverter;

import it.gov.pagopa.wispconverter.util.XmlUtil;
import org.junit.Test;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.Instant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class XmlUtilTest {

    @Test
    public void toXMLGregoirianCalendar() {
        Instant now = Instant.now();
        XMLGregorianCalendar xmlGregoirianCalendar = XmlUtil.toXMLGregoirianCalendar(now);
        assertNotNull(xmlGregoirianCalendar);
        assertEquals(xmlGregoirianCalendar.toGregorianCalendar().getTimeInMillis(), now.toEpochMilli());
    }
}
