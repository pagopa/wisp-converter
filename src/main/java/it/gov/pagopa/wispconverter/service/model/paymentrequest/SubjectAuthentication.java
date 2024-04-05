package it.gov.pagopa.wispconverter.service.model.paymentrequest;

import jakarta.xml.bind.annotation.XmlEnumValue;

public enum SubjectAuthentication {

    CNS("CNS"),
    USR("USR"),
    OTH("OTH"),
    @XmlEnumValue("N/A")
    N_A("N/A");
    private final String value;

    SubjectAuthentication(String v) {
        value = v;
    }

    public static SubjectAuthentication fromValue(String v) {
        for (SubjectAuthentication c : SubjectAuthentication.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

    public String value() {
        return value;
    }

}