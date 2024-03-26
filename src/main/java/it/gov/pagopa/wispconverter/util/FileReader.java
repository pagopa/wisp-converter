package it.gov.pagopa.wispconverter.util;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

@Service
public class FileReader {

    private final ResourceLoader resourceLoader;

    public FileReader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String readFileFromResources(String fileName) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:" + fileName);
        InputStream inputStream = resource.getInputStream();
        try (Scanner scanner = new Scanner(inputStream)) {
            StringBuilder content = new StringBuilder();
            while (scanner.hasNextLine()) {
                content.append(scanner.nextLine()).append("\n");
            }
            return content.toString();
        }
    }
}
