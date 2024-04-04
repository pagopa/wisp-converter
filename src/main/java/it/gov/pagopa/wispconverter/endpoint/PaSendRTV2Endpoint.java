//package it.gov.pagopa.wispconverter.endpoint;
//
//import it.gov.pagopa.pagopa_api.pa.pafornode.ObjectFactory;
//import it.gov.pagopa.pagopa_api.pa.pafornode.PaSendRTV2Request;
//import it.gov.pagopa.pagopa_api.pa.pafornode.PaSendRTV2Response;
//import it.gov.pagopa.pagopa_api.xsd.common_types.v1_0.StOutcome;
//import jakarta.xml.bind.JAXBElement;
//import lombok.NoArgsConstructor;
//import org.springframework.ws.server.endpoint.annotation.Endpoint;
//import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
//import org.springframework.ws.server.endpoint.annotation.RequestPayload;
//import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
//
//@Endpoint
//@NoArgsConstructor
//public class PaSendRTV2Endpoint {
//
//    private static final String NAMESPACE_URI = "http://pagopa-api.pagopa.gov.it/pa/paForNode.xsd";
//    private static final String LOCAL_PART = "paSendRTV2Request";
//
//    @PayloadRoot(namespace = NAMESPACE_URI, localPart = LOCAL_PART)
//    @ResponsePayload
//    public JAXBElement<PaSendRTV2Response> paSendRTV2(@RequestPayload JAXBElement<PaSendRTV2Request> request) {
//        ObjectFactory objectFactory = new ObjectFactory();
//        PaSendRTV2Response paSendRTV2Response = objectFactory.createPaSendRTV2Response();
//        paSendRTV2Response.setOutcome(StOutcome.OK);
//        JAXBElement<PaSendRTV2Response> response = objectFactory.createPaSendRTV2Response(paSendRTV2Response);
//
//        PaSendRTV2Request paSendRTV2Request = request.getValue();
//
//        return response;
//    }
//
//
//}
