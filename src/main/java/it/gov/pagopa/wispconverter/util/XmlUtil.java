package it.gov.pagopa.wispconverter.util;

import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.GregorianCalendar;

public class XmlUtil {

    public static XMLGregorianCalendar toXMLGregoirianCalendar(Instant instant) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTimeInMillis(instant.toEpochMilli());
        try {
            XMLGregorianCalendar xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
            xmlGregorianCalendar.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
            return xmlGregorianCalendar;
        } catch (DatatypeConfigurationException e) {
            throw new AppException(e, AppErrorCodeMessageEnum.PARSING_INVALID_BODY);
        }
    }

    public static BigDecimal toBigDecimalWithScale(BigDecimal target, int scale) {
        return target.setScale(scale, RoundingMode.UNNECESSARY);
    }
}
