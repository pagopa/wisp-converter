package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.gen.wispconverter.client.checkout.model.CartResponseDto;
import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.service.mapper.CartMapper;
import it.gov.pagopa.wispconverter.service.model.CommonRPTFieldsDTO;
import it.gov.pagopa.wispconverter.service.model.re.EntityStatusEnum;
import it.gov.pagopa.wispconverter.service.model.re.ReEventDto;
import it.gov.pagopa.wispconverter.util.Constants;
import it.gov.pagopa.wispconverter.util.ReUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static it.gov.pagopa.wispconverter.util.Constants.NODO_DEI_PAGAMENTI_SPC;

@Service
@Slf4j
@RequiredArgsConstructor
public class CheckoutService {

    private final it.gov.pagopa.gen.wispconverter.client.checkout.invoker.ApiClient checkoutClient;

    private final ConfigCacheService configCacheService;

    private final ReService reService;

    private final CartMapper mapper;

    public String executeCall(CommonRPTFieldsDTO commonRPTFieldsDTO) {

        String location;

        try {
            // execute mapping for Checkout carts invocation
            it.gov.pagopa.gen.wispconverter.client.checkout.model.CartRequestDto cart = mapper.toCart(commonRPTFieldsDTO);
            String stationRedirectURL = getRedirectURL(commonRPTFieldsDTO.getStationId());
            it.gov.pagopa.gen.wispconverter.client.checkout.model.CartRequestReturnUrlsDto returnUrls = new it.gov.pagopa.gen.wispconverter.client.checkout.model.CartRequestReturnUrlsDto();
            returnUrls.setReturnOkUrl(new URI(stationRedirectURL + "/success.html"));
            returnUrls.setReturnCancelUrl(new URI(stationRedirectURL + "/cancel.html"));
            returnUrls.setReturnErrorUrl(new URI(stationRedirectURL + "/error.html"));
            cart.setReturnUrls(returnUrls);

            it.gov.pagopa.gen.wispconverter.client.checkout.api.DefaultApi apiInstance = new it.gov.pagopa.gen.wispconverter.client.checkout.api.DefaultApi(checkoutClient);
            CartResponseDto response = apiInstance.postCarts(cart);
            location = response.getCheckoutRedirectUrl().toString();

            // generate and save re events internal for change status
            reService.addRe(generateRE(location));

        } catch (RestClientException e) {
            throw new AppException(AppErrorCodeMessageEnum.CLIENT_CHECKOUT,
                    String.format("RestClientException ERROR [%s] - %s", e.getCause().getClass().getCanonicalName(), e.getMessage()));
        } catch (URISyntaxException e) {
            throw new AppException(AppErrorCodeMessageEnum.CONFIGURATION_INVALID_STATION_REDIRECT_URL);
        }

        return location;
    }

    private String getRedirectURL(String stationId) {
        it.gov.pagopa.gen.wispconverter.client.cache.model.ConfigDataV1Dto cache = configCacheService.getConfigData();
        if (cache == null) {
            throw new AppException(AppErrorCodeMessageEnum.CONFIGURATION_INVALID_CACHE);
        }
        Map<String, it.gov.pagopa.gen.wispconverter.client.cache.model.StationDto> stations = cache.getStations();
        it.gov.pagopa.gen.wispconverter.client.cache.model.StationDto station = stations.get(stationId);
        if (station == null) {
            throw new AppException(AppErrorCodeMessageEnum.CONFIGURATION_INVALID_STATION, stationId);
        }
        it.gov.pagopa.gen.wispconverter.client.cache.model.RedirectDto redirect = station.getRedirect();
        String protocol = redirect.getProtocol() == null ? "http" : redirect.getProtocol().getValue().toLowerCase();
        String url = redirect.getIp() + "/" + redirect.getPath();
        url = url.replace("//", "/");

        return protocol + "://" + url;
    }

    private ReEventDto generateRE(String redirectUrl) {
        return ReUtil.createBaseReInternal()
                .status(EntityStatusEnum.REDIRECT_DA_CHECKOUT_OK.name())
                .erogatore(NODO_DEI_PAGAMENTI_SPC)
                .erogatoreDescr(NODO_DEI_PAGAMENTI_SPC)
                .sessionIdOriginal(MDC.get(Constants.MDC_SESSION_ID))
                .tipoEvento(MDC.get(Constants.MDC_EVENT_TYPE))
                .cartId(MDC.get(Constants.MDC_CART_ID))
                .idDominio(MDC.get(Constants.MDC_DOMAIN_ID))
                .stazione(MDC.get(Constants.MDC_STATION_ID))
                .iuv(MDC.get(Constants.MDC_IUV)) // null if nodoInviaCarrelloRPT
                .noticeNumber(MDC.get(Constants.MDC_NOTICE_NUMBER)) // null if nodoInviaCarrelloRPT
                .info(String.format("Redirect URL = [%s]", redirectUrl))
                .build();
    }
}
