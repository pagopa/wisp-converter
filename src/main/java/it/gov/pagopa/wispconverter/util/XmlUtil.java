package it.gov.pagopa.wispconverter.util;

import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.Instant;
import java.util.GregorianCalendar;

public class XmlUtil {

    public static XMLGregorianCalendar toXMLGregoirianCalendar(Instant instant) {
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(instant.toEpochMilli());
        XMLGregorianCalendar xmlGc;
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
        } catch (DatatypeConfigurationException e) {
            throw new AppException(e, AppErrorCodeMessageEnum.PARSE_ERROR);
        }
    }

}
