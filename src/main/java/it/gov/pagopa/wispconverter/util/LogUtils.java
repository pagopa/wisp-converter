package it.gov.pagopa.wispconverter.util;

import java.util.Base64;

public class LogUtils {

    /**
     * Encodes a given token to Base64.
     *
     * @param message the string to encode
     * @return the Base64 encoded message
     */
    public static String encodeToBase64(String message) {
        return Base64.getEncoder().encodeToString(message.getBytes());
    }
}
