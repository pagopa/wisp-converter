package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.gen.wispconverter.client.cache.model.StationDto;
import it.gov.pagopa.gen.wispconverter.client.checkout.model.CartRequestDto;
import it.gov.pagopa.gen.wispconverter.client.checkout.model.CartResponseDto;
import it.gov.pagopa.gen.wispconverter.client.checkout.model.PaymentNoticeDto;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.repository.model.enumz.InternalStepStatus;
import it.gov.pagopa.wispconverter.service.mapper.CartMapper;
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import it.gov.pagopa.wispconverter.service.model.session.PaymentNoticeContentDTO;
import it.gov.pagopa.wispconverter.service.model.session.SessionDataDTO;
import it.gov.pagopa.wispconverter.util.ReUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.net.URISyntaxException;

@Service
@Slf4j
@RequiredArgsConstructor
public class CheckoutService {

    private final it.gov.pagopa.gen.wispconverter.client.checkout.invoker.ApiClient checkoutClient;

    private final ConfigCacheService configCacheService;

    private final ReService reService;

    private final CartMapper mapper;

    @Value("${wisp-converter.re-tracing.internal.checkout-interaction.enabled}")
    private Boolean isTracingOnREEnabled;

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
            generateRE(sessionData, cart, location);

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
        returnUrls.setReturnOkUrl(new URI(String.format("%s?idSession=%s&esito=OK",stationRedirectURL, sessionData.getCommonFields().getSessionId())));
        returnUrls.setReturnCancelUrl(new URI(String.format("%s?idSession=%s&esito=ERROR",stationRedirectURL, sessionData.getCommonFields().getSessionId())));
        returnUrls.setReturnErrorUrl(new URI(String.format("%s?idSession=%s&esito=ERROR",stationRedirectURL, sessionData.getCommonFields().getSessionId())));
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

    private void generateRE(SessionDataDTO sessionData, CartRequestDto cartRequest, String redirectUrl) {

        // creating event to be persisted for RE
        if (Boolean.TRUE.equals(isTracingOnREEnabled)) {
            for (PaymentNoticeDto paymentNoticeFromCart : cartRequest.getPaymentNotices()) {
                PaymentNoticeContentDTO paymentNotice = sessionData.getPaymentNoticeByNoticeNumber(paymentNoticeFromCart.getNoticeNumber());
                ReEventDto reEvent = ReUtil.getREBuilder()
                        .status(InternalStepStatus.SAVED_RPT_IN_CART_RECEIVED_REDIRECT_URL_FROM_CHECKOUT)
                        .iuv(paymentNotice.getIuv())
                        .noticeNumber(paymentNotice.getNoticeNumber())
                        .ccp(paymentNotice.getCcp())
                        .info(String.format("Redirect URL = [%s]", redirectUrl))
                        .build();
                reService.addRe(reEvent);
            }
        }
    }
}
