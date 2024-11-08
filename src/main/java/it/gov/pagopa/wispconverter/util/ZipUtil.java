package it.gov.pagopa.wispconverter.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ZipUtil {

    private ZipUtil() {
    }

    public static byte[] zip(String str) throws IOException {
        byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream bais = new ByteArrayOutputStream(strBytes.length);
        GZIPOutputStream gzipOut = new GZIPOutputStream(bais);
        gzipOut.write(strBytes);
        gzipOut.close();
        byte[] compressed = bais.toByteArray();
        bais.close();
        return compressed;
    }

    public static byte[] unzip(byte[] compressed) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
        GZIPInputStream gzipInputStream = new GZIPInputStream(bais);
        return gzipInputStream.readAllBytes();
    }

    public static byte[] base64Decode(String base64Encoded) {
        return Base64.getDecoder().decode(base64Encoded);
    }

    public static String getUtf8String(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

}
