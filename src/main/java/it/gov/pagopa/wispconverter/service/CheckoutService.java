package it.gov.pagopa.wispconverter.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CheckoutService {

    private final it.gov.pagopa.checkoutclient.client.ApiClient checkoutClient;

    public String executeCall() {

        // execute mapping for Checkout carts invocation

        // call Checkout carts API, receive Checkout response and returns redirection URI

        // generate uri

        return null;
    }
}
