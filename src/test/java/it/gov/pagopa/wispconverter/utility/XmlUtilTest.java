package it.gov.pagopa.wispconverter.utility;

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

    @Test
    public void toXMLGregoirianCalendar_withoutTimestamp() {
        Instant dateInMillis = Instant.ofEpochMilli(1577876400000L); // 2020-01-01T12:00:00 UTC
        String dateAsString = "2020-01-01T12:00:00.000";
        XMLGregorianCalendar xmlGregoirianCalendar = XmlUtil.toXMLGregoirianCalendar(dateInMillis);
        assertNotNull(xmlGregoirianCalendar);
        assertEquals(xmlGregoirianCalendar.toGregorianCalendar().getTimeInMillis(), dateInMillis.toEpochMilli());
        assertEquals(xmlGregoirianCalendar.toString(), dateAsString);
    }
}
