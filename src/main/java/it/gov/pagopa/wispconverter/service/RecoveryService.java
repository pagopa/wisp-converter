package it.gov.pagopa.wispconverter.service;

import it.gov.pagopa.wispconverter.controller.model.RecoveryReceiptResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecoveryService {

    public RecoveryReceiptResponse recoverReceiptKOForCreditorInstitution(String creditorInstitution, String dateFrom, String dateTo) {

        /* TODO
         * la chiamata deve essere ASYNC!!!
         *
         * - fare query su receipt-rt basata su id startswith "<creditorInstitution>_", rt=null e (_ts >= lower_limit && _ts <= upper_limit)
         *   * lower_limit: passabile custom ma non può essere minore di 3-09-2024
         *   * upper_limit: passabile custom ma se è uguale ad oggi non può essere maggiore di now-1h
         * - per ogni entità estratta da receipt-rt si ricavano i valori di iuv e ccp
         * - ritorna sincronamente la lista di iuv che si sta riconciliando, da qui in avanti si continua asincronamente
         * - per ogni entità:
         *      - fare query su RE wisp basata su domainId=<creditorInstitution>, iuv=<iuv>, ccp=<ccp> e status=GENERATED_CACHE_ABOUT_RPT_FOR_RT_GENERATION
         *      - si avranno 0 o più entità:
         *          * se 0, non si può elaborare [da valutare]
         *          * se 1, si prendono il sessionId e NAV
         *          * se piu di 1, si ordina per tempo e si prendono il sessionId e NAV dell'ultima in ordine temporale
         *      - si generano le key da memorizzare su redis (da sovrascrivere sempre!!!!)
         *          * la prima key deve avere il formato 'wisp_nav2iuv_<creditorInstitution>_<nav>' con valore 'wisp_<creditorInstitution>_<iuv>'
         *          * la seconda key deve avere il formato 'wisp_<creditorInstitution>_<iuv>' con valore 'sessionId'
         *      - si genera la richiesta come segue: { "fiscalCode": "<creditorInstitution>", "noticeNumber": "<nav>", "paymentToken": null }
         *      - si chiama l'endpoint di receipt/ko con la richiesta
         */
    }
}
