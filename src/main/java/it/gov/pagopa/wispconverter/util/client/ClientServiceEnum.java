package it.gov.pagopa.wispconverter.util.client;

public enum ClientServiceEnum {

    GPD("GPD"),
    DECOUPLER("DECOUPLER"),
    IUV_GENERATOR("IUV GENERATOR"),
    CHECKOUT("CHECKOUT"),
    API_CONFIG_CACHE("API CONFIG CACHE");

    public final String label;

    ClientServiceEnum(String label) {
        this.label = label;
    }

}
