package it.gov.pagopa.wispconverter.exception.conversion;

import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.validation.annotation.Validated;

/**
 * Exception made in order to handle all conversion errors
 */
@EqualsAndHashCode(callSuper = true)
@Value
@Validated
public class ConversionException extends Exception {

    public ConversionException(@NotNull String message) {
        super(message);
    }

    public ConversionException(@NotNull String message, Throwable cause) {
        super(message, cause);
    }
}
