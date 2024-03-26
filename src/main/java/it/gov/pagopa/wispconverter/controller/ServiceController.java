package it.gov.pagopa.wispconverter.controller;

import jakarta.xml.bind.JAXBElement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.springframework.ws.soap.server.endpoint.annotation.SoapAction;


@Endpoint
@Slf4j
public class ServiceController {

    @SoapAction("nodoInviaRPT")
    @PayloadRoot(localPart = "nodoInviaRPTReq")
    @ResponsePayload
    public JAXBElement<Void> nodoInviaRPT(@RequestPayload JAXBElement<Void> request) {
        return null;
    }

    @SoapAction("nodoInviaCarrelloRPT")
    @PayloadRoot(localPart = "nodoInviaCarrelloRPTReq")
    @ResponsePayload
    public JAXBElement<Void> nodoInviaCarrelloRPT(@RequestPayload JAXBElement<Void> request) {
        return null;
    }

}
