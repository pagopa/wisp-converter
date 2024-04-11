package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.wispconverter.exception.AppErrorCodeMessageEnum;
import it.gov.pagopa.wispconverter.exception.AppException;
import it.gov.pagopa.wispconverter.service.mapper.CartMapper;
import it.gov.pagopa.wispconverter.service.model.CommonRPTFieldsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class CheckoutService {

    private final it.gov.pagopa.gen.wispconverter.client.checkout.invoker.ApiClient checkoutClient;

    private final ConfigCacheService configCacheService;

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

            it.gov.pagopa.gen.wispconverter.client.checkout.api.PaymentRequestsApi apiInstance = new it.gov.pagopa.gen.wispconverter.client.checkout.api.PaymentRequestsApi(checkoutClient);
            ResponseEntity<Void> response = apiInstance.postCartsWithHttpInfo(cart);

            HttpStatusCode status = response.getStatusCode();
            if (status.value() != 302) {
                throw new AppException(AppErrorCodeMessageEnum.CLIENT_CHECKOUT, "The response retrieved from Checkout is not '302 Found'.");
            }
            Collection<String> locationHeader = response.getHeaders().get("location");
            if (locationHeader == null) {
                throw new AppException(AppErrorCodeMessageEnum.CLIENT_CHECKOUT_NO_REDIRECT_LOCATION);
            }
            location = locationHeader.stream().findFirst().orElseThrow(() -> new AppException(AppErrorCodeMessageEnum.CLIENT_CHECKOUT_INVALID_REDIRECT_LOCATION));

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
}
