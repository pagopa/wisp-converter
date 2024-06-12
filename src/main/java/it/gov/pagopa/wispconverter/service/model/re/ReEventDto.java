package it.gov.pagopa.wispconverter.service.model.re;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import it.gov.pagopa.wispconverter.repository.model.enumz.CallTypeEnum;
import it.gov.pagopa.wispconverter.repository.model.enumz.*;
import lombok.*;

import java.time.Instant;

@Data
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonPropertyOrder()
public class ReEventDto {

    private String id;
    @Schema(title = "BOTH", description = "The identifier, set by X-Request-ID, from which the events can be grouped.")
    private String requestId;

    @Schema(title = "BOTH", description = "The identifier associated to a request identifier")
    private String operationId;

    @Schema(title = "BOTH", description = "The identifier that associate a client to an operation identifier.")
    private String clientOperationId;

    @Schema(title = "BOTH", description = "The applicative component from which the event is generated.<br>In NDP it is mapped with field 'componente'.")
    private ComponentEnum component;

    @Schema(title = "BOTH", description = "The time on which the event is inserted in RE storage")
    private Instant insertedTimestamp;

    @Schema(title = "BOTH", description = "The category on which the event can be grouped.<br>In NDP it is mapped with field 'categoriaEvento'.")
    private EventCategoryEnum eventCategory;

    @Schema(title = "BOTH", description = "The subcategory related to the specific nature of the event.<br>In NDP it is mapped with field 'sottoTipoEvento'.")
    private EventSubcategoryEnum eventSubcategory;

    @Schema(title = "INTERFACE", description = "The type of role that the application has in the communication with the remote endpoint.")
    private CallTypeEnum callType;

    @Schema(title = "INTERFACE", description = "The service that is consumer in the step execution.<br>In NDP it is mapped with field 'fruitore'.")
    private String consumer;

    @Schema(title = "INTERFACE", description = "The service that provide to the step execution.<br>In NDP it is mapped with field 'erogatore'.")
    private String provider;

    @Schema(title = "INTERFACE", description = "The outcome of the operation described by the event.<br>In NDP it is mapped with field 'esito'.")
    private OutcomeEnum outcome;

    @Schema(title = "INTERFACE", description = "The HTTP method of the endpoint related to the event.<br>This field is set only if the events that describe an HTTP communication with an external service.")
    private String httpMethod;

    @Schema(title = "INTERFACE", description = "The URI related to the called endpoint.<br>This field is set only if the events that describe an HTTP communication with an external service.")
    private String httpUri;

    @Schema(title = "INTERFACE", description = "The list of HTTP headers extracted from the request/response analyzed by the event.<br>This field is set only if the events that describe an HTTP communication with an external service.")
    private String httpHeaders;

    @Schema(title = "INTERFACE", description = "The remote IP address extracted from the called endpoint.<br>This field is set only if the events that describe an HTTP communication with an external service.")
    private String httpCallRemoteAddress;

    @Schema(title = "INTERFACE", description = "The status code extracted from the called endpoint.<br>This field is set only if the events that describe an HTTP communication with an external service.")
    private Integer httpStatusCode;

    @Schema(title = "INTERFACE", description = "The duration time of the invocation of the endpoint related to the event.<br>This field is set only if the events that describe an HTTP communication with an external service.")
    private Long executionTimeMs;

    @Schema(title = "INTERFACE", description = "The payload of the request/response analyzed by the event.<br>This value is zipped using GZip compression algorithm.")
    private String compressedPayload;

    @Schema(title = "INTERFACE", description = "The length (in number of characters) of the compressed payload.")
    private Integer compressedPayloadLength;

    @Schema(title = "INTERFACE", description = "The descriptive label associated to the endpoint called by user and related to the whole process.")
    private String businessProcess;

    @Schema(title = "INTERFACE", description = "The final status of the whole operation.<br>This is set only in the events that describe the response in output to user.")
    private String operationStatus;

    @Schema(title = "INTERFACE", description = "The error title extracted from the computation that refers to the error occurred during computation.<br>This is set only in the events that describe the response in output to user if there is an error.")
    private String operationErrorTitle;

    @Schema(title = "INTERFACE", description = "The error detail message extracted from the computation that refers to the error occurred during computation.<br>This is set only in the events that describe the response in output to user if there is an error.")
    private String operationErrorDetail;

    @Schema(title = "INTERFACE", description = "The error code extracted from the computation that refers to the error occurred during computation.<br>This is set only in the events that describe the response in output to user if there is an error.")
    private String operationErrorCode;

    @Schema(title = "INTERNAL", description = "The typology of primitive analyzed and tracked by the event.<br>In NDP it is mapped with field 'eventType'.")
    private String primitive;

    @Schema(title = "INTERNAL", description = "The session identifier generated by WISP SOAP Converter and used in the request.")
    private String sessionId;

    @Schema(title = "INTERNAL", description = "The cart identifier used in the request.")
    private String cartId;

    @Schema(title = "INTERNAL", description = "The 'identificativo univoco pagamento' used in the request.")
    private String iuv;

    @Schema(title = "INTERNAL", description = "The notice number (aka NAV code) used in the request.")
    private String noticeNumber;

    @Schema(title = "INTERNAL", description = "The creditor institution identifier used in the request.")
    private String domainId;

    @Schema(title = "INTERNAL", description = "The 'codice contesto pagamento' used in the request.")
    private String ccp;

    @Schema(title = "INTERNAL", description = "The payment service provider used in the request.")
    private String psp;

    @Schema(title = "INTERNAL", description = "The station used in the request.")
    private String station;

    @Schema(title = "INTERNAL", description = "The channel used in the request.")
    private String channel;

    @Schema(title = "INTERNAL", description = "The state of the internal step executed.")
    private String status;

    @Schema(title = "INTERNAL", description = "The other information that can be inserted for the tracing.")
    private String info;

    @Schema(title = "INTERNAL", description = "The payment token.")
    private String paymentToken;
}
