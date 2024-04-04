package it.gov.pagopa.wispconverter.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;

public class TestUtils {

    public static String loadFileContent(String fileName) {
        String content = null;
        try {
            // Get the InputStream of the resource
            InputStream inputStream = TestUtils.class.getResourceAsStream(fileName);
            if (inputStream != null) {
                // Use Apache Commons IO to read the content from the InputStream
                content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            } else {
                System.err.println("File not found: " + fileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }


}
