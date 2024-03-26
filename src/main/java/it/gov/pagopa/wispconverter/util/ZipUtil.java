package it.gov.pagopa.wispconverter.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

public class ZipUtil {

    public static byte[] unzip(byte[] compressed) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
        GZIPInputStream gzipInputStream = new GZIPInputStream(bais);
        return gzipInputStream.readAllBytes();
    }

    public static byte[] base64Decode(String base64Encoded){
        return Base64.getDecoder().decode(base64Encoded);
    }

    public static String getUtf8String(byte[] bytes){
        return new String(bytes, StandardCharsets.UTF_8);
    }

}
