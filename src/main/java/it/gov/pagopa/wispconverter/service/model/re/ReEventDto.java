package it.gov.pagopa.wispconverter.service.model.re;

import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReEventDto {
  private String requestId;
  private Instant insertedTimestamp;
  private ComponenteEnum componente;
  private CategoriaEventoEnum categoriaEvento;
  private SottoTipoEventoEnum sottoTipoEvento;
  private String idDominio;
  private String iuv;
  private String ccp;
  private String psp;
  private String tipoVersamento;
  private String tipoEvento;
  private String fruitore;
  private String erogatore;
  private String stazione;
  private String canale;
  private String parametriSpecificiInterfaccia;
  private EsitoEnum esito;
  private String operationId;
  private String status;

  private String info;
  private String businessProcess;
  private String fruitoreDescr;
  private String erogatoreDescr;
  private String pspDescr;
  private String noticeNumber;
  private String creditorReferenceId;
  private String paymentToken;
  private String sessionIdOriginal;
  private Boolean standIn;

  private String compressedPayload;
  private Integer compressedPayloadPayloadLength;


  private String httpMethod;
  private String httpUri;
  private String httpHeaders;
  private String httpCallRemoteAddress;

  private Integer httpStatusCode;
  private String executionTimeMs;

  private String clientOperationId;

}
