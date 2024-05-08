package it.gov.pagopa.wispconverter.service.model.re;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;

import java.time.Instant;

@Data
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonPropertyOrder()
//@Accessors(fluent = true,chain = true)
public class ReEventDto {
    //// START KEY
    private String id;
    //// END KEY

    //// START LOGICAL REF
    private String requestId;           //gruppo, tutte le chiamate fatte con lo stesso X-Request-ID
    private String operationId;         //id operation associato a un requestId
    private String clientOperationId;   //id client operation associato a un operationId
    private ComponenteEnum componente;  //componente che scrive l'evento
    private Instant insertedTimestamp;  //ora di inserimento evento
    //// END LOGICAL REF

    //// START FIELD FOR INTERFACE AND INTERN CHANGE
    private CategoriaEventoEnum categoriaEvento;
    private SottoTipoEventoEnum sottoTipoEvento;
    //// END FIELD FOR INTERFACE AND INTERN CHANGE

    //// START FIELD FOR INTERFACE
    private CallTypeEnum callType;

    private String fruitore;
    private String fruitoreDescr;
    private String erogatore;
    private String erogatoreDescr;

    private EsitoEnum esito;

    private String httpMethod;
    private String httpUri;
    private String httpHeaders;
    private String httpCallRemoteAddress;

    private Integer httpStatusCode;
    private Long executionTimeMs;

    private String compressedPayload;       //zip+Base64
    private Integer compressedPayloadLength;

    private String businessProcess;

    private String operationStatus;       //dettaglio response in uscita
    private String operationErrorTitle;   //dettaglio response in uscita
    private String operationErrorDetail;  //dettaglio response in uscita
    private String operationErrorCode;    //dettaglio response in uscita
    //// END FIELD FOR INTERFACE

    //// START FIELD FOR INTERN CHANGE
    private String idDominio;
    private String cartId;
    private String iuv;
    private String noticeNumber;
    private String ccp;
    private String psp;
    //private String tipoVersamento; TODO remove this comment
    private String tipoEvento;
    private String stazione;
    private String canale;
    //private String parametriSpecificiInterfaccia; TODO remove this comment
    private String status;
    private String info;

    //private String pspDescr; TODO remove this comment
    //private String creditorReferenceId; TODO remove this comment
    private String paymentToken;
    private String sessionIdOriginal;
    // private Boolean standIn; TODO remove this comment
    //// END FIELD FOR INTERN CHANGE

}
