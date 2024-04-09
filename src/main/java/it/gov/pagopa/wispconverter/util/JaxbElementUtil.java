package it.gov.pagopa.wispconverter.util;

import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import jakarta.xml.bind.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import org.springframework.xml.transform.StringResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlsoap.schemas.soap.envelope.Body;
import org.xmlsoap.schemas.soap.envelope.Envelope;
import org.xmlsoap.schemas.soap.envelope.Header;

@Slf4j
@Component
@RequiredArgsConstructor
public class JaxbElementUtil {

    private static final String ENVELOPE_NAMESPACE_URI = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String ENVELOPE_LOCAL_NAME = "Envelope";


    private static final String RPT_NAMESPACE_URI = "http://www.digitpa.gov.it/schemas/2011/Pagamenti/";
    private static final String RPT_LOCAL_NAME = "RPT";

    private final DocumentBuilderFactory documentBuilderFactory;

    private Element convertToElement(InputSource inputSource, String nameSpaceUri, String localName) {
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(inputSource);
            NodeList nodeList = document.getElementsByTagNameNS(nameSpaceUri, localName);
            if (nodeList.getLength() == 0) {
                throw new AppException(AppErrorCodeMessageEnum.PARSING_INVALID_XML_NODES);
            }
            return (Element) nodeList.item(0);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new AppException(e, AppErrorCodeMessageEnum.PARSING_GENERIC_ERROR, e.getMessage());
        }
    }

    public <T> T convertToBean(Element element, Class<T> targetType) {
        try {
            JAXBContext context = JAXBContext.newInstance(targetType);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            JAXBElement<T> jaxbElement = unmarshaller.unmarshal(element, targetType);
            return jaxbElement.getValue();
        } catch (JAXBException e) {
            throw new AppException(e, AppErrorCodeMessageEnum.PARSING_GENERIC_ERROR, e.getMessage());
        }
    }
    
    public <T> String convertToString(Object object, Class<T> targetType) {
            Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
            marshaller.setPackagesToScan(new String[]{
                    "org.xmlsoap.schemas.soap.envelope",
                    "gov.telematici.pagamenti.ws.ppthead",
                    "gov.telematici.pagamenti.ws"
            });
            StringResult ss = new StringResult();
            marshaller.marshal(object, ss);
            return ss.toString();
    }

    public Element convertToEnvelopeElement(byte[] source) {
        return convertToElement(new InputSource(new ByteArrayInputStream(source)), ENVELOPE_NAMESPACE_URI, ENVELOPE_LOCAL_NAME);
    }

    public Element convertToRPTElement(byte[] source) {
        return convertToElement(new InputSource(new ByteArrayInputStream(source)), RPT_NAMESPACE_URI, RPT_LOCAL_NAME);
    }


    public <T> T getSoapHeader(Envelope envelope, Class<T> targetType) {
        Header header = envelope.getHeader();
        if (header == null) {
            throw new AppException(AppErrorCodeMessageEnum.PARSING_INVALID_HEADER, "header is null");
        }

        List<Object> list = header.getAny();
        if (list == null || list.isEmpty()) {
            throw new AppException(AppErrorCodeMessageEnum.PARSING_INVALID_HEADER, "headerValue is null or is empty");
        }
        Element element = (Element) list.get(0);
        return convertToBean(element, targetType);
    }

    public <T> T getSoapBody(Envelope envelope, Class<T> targetType) {
        Body body = envelope.getBody();
        if (body == null) {
            throw new AppException(AppErrorCodeMessageEnum.PARSING_INVALID_BODY, "body is null");
        }

        List<Object> list = body.getAny();
        if (list == null || list.isEmpty()) {
            throw new AppException(AppErrorCodeMessageEnum.PARSING_INVALID_BODY, "bodyValue is null or is empty");
        }
        Element element = (Element) list.get(0);
        return convertToBean(element, targetType);
    }
}
