package it.gov.pagopa.wispconverter.utility;

import it.gov.pagopa.wispconverter.util.XmlUtil;
import org.junit.Test;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class XmlUtilTest {

    @Test
    public void toXMLGregoirianCalendar() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        XMLGregorianCalendar xmlGregoirianCalendar = XmlUtil.toXMLGregorianCalendar(now);
        assertNotNull(xmlGregoirianCalendar);
        assertEquals(now.toEpochMilli(), xmlGregoirianCalendar.toGregorianCalendar().getTimeInMillis());
    }
    @Test
    public void toXMLGregoirianCalendar_withoutTimestamp() {
        Instant dateInMillis = Instant.ofEpochMilli(1577876400000L); // 2020-01-01T12:00:00 UTC
        String expectedDate = "2020-01-01T12:00:00";
        XMLGregorianCalendar xmlGregoirianCalendar = XmlUtil.toXMLGregorianCalendar(dateInMillis);
        assertNotNull(xmlGregoirianCalendar);
        assertEquals(dateInMillis.toEpochMilli(), xmlGregoirianCalendar.toGregorianCalendar().getTimeInMillis());
        assertEquals(expectedDate, xmlGregoirianCalendar.toString());
    }
}
