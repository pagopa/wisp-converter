package it.gov.pagopa.wispconverter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

@Configuration
public class UnmarshallerConfig {

    @Bean
    @Scope("prototype") // set lifecycle to "prototype" in order to generate a separate instance for each request
    public DocumentBuilderFactory documentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory;
    }

    @Bean
    public DocumentBuilder documentBuilder(DocumentBuilderFactory documentBuilderFactory) throws ParserConfigurationException {
        return documentBuilderFactory.newDocumentBuilder();
    }
}
