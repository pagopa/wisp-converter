package it.gov.pagopa.wispconverter.entity;

public enum Primitive {

    NODO_INVIA_CARRELLO_RPT("nodoInviaCarrelloRPT"),
    NODO_INVIA_RPT("nodoInviaRPT");

    private final String value;

    Primitive(String value) {
        this.value = value;
    }

    public static Primitive fromString(String primitiveAsString) {
        for (Primitive primitive : Primitive.values()) {
            if (primitive.value.equals(primitiveAsString)) {
                return primitive;
            }
        }
        return null;
    }
}
