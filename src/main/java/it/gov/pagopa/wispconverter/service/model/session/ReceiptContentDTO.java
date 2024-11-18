package it.gov.pagopa.wispconverter.service.model.session;

import lombok.*;


@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReceiptContentDTO {
    private String paaInviaRTPayload;
    private String rtPayload;
}
