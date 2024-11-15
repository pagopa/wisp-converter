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
import java.util.TimeZone;

public class XmlUtil {

    public static XMLGregorianCalendar toXMLGregorianCalendar(Instant instant) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTimeZone(TimeZone.getTimeZone("Europe/Rome"));
        gregorianCalendar.setTimeInMillis(instant.toEpochMilli());
        try {
            XMLGregorianCalendar xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
            xmlGregorianCalendar.setMillisecond(DatatypeConstants.FIELD_UNDEFINED); // removing info about milliseconds
            xmlGregorianCalendar.setTimezone(DatatypeConstants.FIELD_UNDEFINED); // removing all references about timezone
            return xmlGregorianCalendar;
        } catch (DatatypeConfigurationException e) {
            throw new AppException(e, AppErrorCodeMessageEnum.PARSING_INVALID_BODY, e.getMessage());
        }
    }

    public static XMLGregorianCalendar toXMLGregorianCalendar(XMLGregorianCalendar xmlGregorianCalendar) {
        xmlGregorianCalendar.setMillisecond(DatatypeConstants.FIELD_UNDEFINED); // removing info about milliseconds
        xmlGregorianCalendar.setTimezone(DatatypeConstants.FIELD_UNDEFINED); // removing all references about timezone
        return xmlGregorianCalendar;
    }

    public static BigDecimal toBigDecimalWithScale(BigDecimal target, int scale) {
        return target.setScale(scale, RoundingMode.UNNECESSARY);
    }
}
