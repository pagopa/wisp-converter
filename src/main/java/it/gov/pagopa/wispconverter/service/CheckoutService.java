package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.wispconverter.client.checkout.model.Cart;
import it.gov.pagopa.wispconverter.service.mapper.CartMapper;
import it.gov.pagopa.wispconverter.service.model.CommonRPTFieldsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CheckoutService {

    private CartMapper mapper;

    public String executeCall(CommonRPTFieldsDTO commonRPTFieldsDTO) {

        // get station info from cached configuration
        Cart cart = mapper.toCart(commonRPTFieldsDTO);

        // execute mapping for Checkout carts invocation

        // call Checkout carts API, receive Checkout response and returns redirection URI

        // generate uri

        return null;
    }
}
