package it.gov.pagopa.wispconverter.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class AppBase64Util {

    public static String base64Encode(byte[] compressed){
        return getUtf8String(Base64.getEncoder().encode(compressed));
    }
    public static byte[] base64Decode(String base64Encoded){
        return Base64.getDecoder().decode(base64Encoded);
    }
    public static String getUtf8String(byte[] bytes){
        return new String(bytes, StandardCharsets.UTF_8);
    }

}
