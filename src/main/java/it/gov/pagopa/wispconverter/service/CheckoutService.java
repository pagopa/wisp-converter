package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.gen.wispconverter.client.cache.model.StationDto;
import it.gov.pagopa.gen.wispconverter.client.checkout.model.CartRequestDto;
import it.gov.pagopa.gen.wispconverter.client.checkout.model.CartResponseDto;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.model.enumz.EntityStatusEnum;
import it.gov.pagopa.wispconverter.service.mapper.CartMapper;
import it.gov.pagopa.wispconverter.service.model.session.SessionDataDTO;
import it.gov.pagopa.wispconverter.util.Constants;
import it.gov.pagopa.wispconverter.util.ReUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.net.URISyntaxException;

import static it.gov.pagopa.wispconverter.util.Constants.NODO_DEI_PAGAMENTI_SPC;

@Service
@Slf4j
@RequiredArgsConstructor
public class CheckoutService {

    private final it.gov.pagopa.gen.wispconverter.client.checkout.invoker.ApiClient checkoutClient;

    private final ConfigCacheService configCacheService;

    private final ReService reService;

    private final CartMapper mapper;

    public String executeCall(SessionDataDTO sessionData) {

        String location;

        try {
            // execute mapping for Checkout carts invocation
            CartRequestDto cart = extractCart(sessionData);

            // communicating with Checkout for cart creation and retrieving location from response
            it.gov.pagopa.gen.wispconverter.client.checkout.api.DefaultApi apiInstance = new it.gov.pagopa.gen.wispconverter.client.checkout.api.DefaultApi(checkoutClient);
            CartResponseDto response = apiInstance.postCarts(cart);
            location = response.getCheckoutRedirectUrl().toString();

            // generate and save re events internal for change status
            generateRE(location);

        } catch (RestClientException e) {
            throw new AppException(AppErrorCodeMessageEnum.CLIENT_CHECKOUT,
                    String.format("RestClientException ERROR [%s] - %s", e.getCause().getClass().getCanonicalName(), e.getMessage()));
        } catch (URISyntaxException e) {
            throw new AppException(AppErrorCodeMessageEnum.CONFIGURATION_INVALID_STATION_REDIRECT_URL);
        }

        return location;
    }

    private CartRequestDto extractCart(SessionDataDTO sessionData) throws URISyntaxException {

        // execute main mapping for the cart to be sent to Checkout
        CartRequestDto cart = mapper.toCart(sessionData);
        cart.setPaymentNotices(sessionData.getAllPaymentNotices().stream()
                .map(mapper::toPaymentNotice)
                .toList());

        // retrieving URL for redirect from station
        String stationRedirectURL = getRedirectURL(sessionData.getCommonFields().getStationId());

        // explicitly set all URLs for object
        it.gov.pagopa.gen.wispconverter.client.checkout.model.CartRequestReturnUrlsDto returnUrls = new it.gov.pagopa.gen.wispconverter.client.checkout.model.CartRequestReturnUrlsDto();
        returnUrls.setReturnOkUrl(new URI(stationRedirectURL + "/success.html"));
        returnUrls.setReturnCancelUrl(new URI(stationRedirectURL + "/cancel.html"));
        returnUrls.setReturnErrorUrl(new URI(stationRedirectURL + "/error.html"));
        cart.setReturnUrls(returnUrls);

        return cart;
    }

    private String getRedirectURL(String stationId) {

        // retrieving station by station identifier
        StationDto station = configCacheService.getStationByIdFromCache(stationId);

        // extracting redirect URL using protocol, IP and path
        it.gov.pagopa.gen.wispconverter.client.cache.model.RedirectDto redirect = station.getRedirect();
        String protocol = redirect.getProtocol() == null ? "http" : redirect.getProtocol().getValue().toLowerCase();
        String url = redirect.getIp() + "/" + redirect.getPath();
        url = url.replace("//", "/");

        return protocol + "://" + url;
    }

    private void generateRE(String redirectUrl) {

        reService.addRe(ReUtil.createBaseReInternal()
                .status(EntityStatusEnum.REDIRECT_DA_CHECKOUT_OK.name())
                .provider(NODO_DEI_PAGAMENTI_SPC)
                .sessionId(MDC.get(Constants.MDC_SESSION_ID))
                .primitive(MDC.get(Constants.MDC_PRIMITIVE))
                .cartId(MDC.get(Constants.MDC_CART_ID))
                .domainId(MDC.get(Constants.MDC_DOMAIN_ID))
                .station(MDC.get(Constants.MDC_STATION_ID))
                .iuv(MDC.get(Constants.MDC_IUV)) // null if nodoInviaCarrelloRPT
                .noticeNumber(MDC.get(Constants.MDC_NOTICE_NUMBER)) // null if nodoInviaCarrelloRPT
                .info(String.format("Redirect URL = [%s]", redirectUrl))
                .build());
    }
}
