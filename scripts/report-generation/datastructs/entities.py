from datastructs.report import CIsReportInfo, CompletedPaymentsReportInfo, NotCompletedPaymentsReportInfo, ReceiptDetailStatistics, TriggerPrimitiveReportInfo
from utility.constants import Constants

class ReportEntity:

    def __init__(self, 
                 id, 
                 date, 
                 total_payments_on_ndp,
                 creditor_institutions: CIsReportInfo,
                 trigger_primitive: TriggerPrimitiveReportInfo,
                 completed_payments: CompletedPaymentsReportInfo,
                 not_completed_payments: NotCompletedPaymentsReportInfo):
        # generating map from objects
        self.data = {
            "id": id,
            "date": date,
            "creditor_institution_info": {
                "total": creditor_institutions.cis_total,
                "on_wisp": creditor_institutions.cis_on_wisp,
            },
            "payments": {
                "total_on_ndp": total_payments_on_ndp,
                "total_on_wisp": trigger_primitive.carts_total + trigger_primitive.no_carts_total,
                "trigger_primitives": {
                    "total_carts": trigger_primitive.carts_total,
                    "total_no_carts": trigger_primitive.no_carts_total,
                    "carts_completed": trigger_primitive.carts_completed,
                    "no_carts_completed": trigger_primitive.no_carts_completed,
                    Constants.TRIGGER_PRIMITIVE_NOT_COMPLETED_MACROTAG: {
                        Constants.TRIGGER_PRIMITIVE_NOT_COMPLETED_RPT_TIMEOUT: trigger_primitive.not_completed_rpt_timeout,
                        Constants.TRIGGER_PRIMITIVE_NOT_COMPLETED_REDIRECT: trigger_primitive.not_completed_redirect,
                        Constants.TRIGGER_PRIMITIVE_NOT_COMPLETED_RECEIPT_KO: trigger_primitive.not_completed_receipt_ko,
                        Constants.TRIGGER_PRIMITIVE_NOT_COMPLETED_PAYMENTTOKEN_TIMEOUT: trigger_primitive.not_completed_paymenttoken_timeout,
                        Constants.TRIGGER_PRIMITIVE_NOT_COMPLETED_ECOMMERCE_TIMEOUT: trigger_primitive.not_completed_ecommerce_timeout,
                        Constants.TRIGGER_PRIMITIVE_NOT_COMPLETED_NO_STATE:  trigger_primitive.not_completed_no_state,
                    }
                },
                Constants.COMPLETED_MACROTAG: {
                    Constants.COMPLETED_OK_RECEIPT_TOTAL: completed_payments.closed_as_ok,
                    Constants.COMPLETED_KO_RECEIPT_TOTAL: completed_payments.closed_as_ko,
                    Constants.COMPLETED_OK_RECEIPT_SENT_BY_RETRY: completed_payments.with_ok_receipts_only_sent_after_retry,
                    Constants.COMPLETED_KO_RECEIPT_SENT_BY_RETRY: completed_payments.with_ko_receipts_only_sent_after_retry,
                },
                Constants.NOT_COMPLETED_MACROTAG: {
                    Constants.NOT_COMPLETED_REJECTED: not_completed_payments.rejected.to_dict(),
                    Constants.NOT_COMPLETED_NOT_SENT_END_RETRY: not_completed_payments.not_sent_end_retry.to_dict(),
                    Constants.NOT_COMPLETED_SCHEDULED: not_completed_payments.scheduled.to_dict(),
                    Constants.NOT_COMPLETED_NEVER_SENT: not_completed_payments.never_sent.to_dict()
                }
            }
        }


    def get_map(self):
        return self.data
    

    def from_db_item(item):

        # extract useful data from item map
        item_ci_info = item["creditor_institution_info"]
        payments_info = item["payments"]
        trigger_primitive_info = payments_info["trigger_primitives"]
        trigger_primitive_not_completed = trigger_primitive_info[Constants.TRIGGER_PRIMITIVE_NOT_COMPLETED_MACROTAG]
        completed_payment_info = payments_info[Constants.COMPLETED_MACROTAG]
        not_completed_payment_info = payments_info[Constants.NOT_COMPLETED_MACROTAG]
        rejected_payments_info = not_completed_payment_info[Constants.NOT_COMPLETED_REJECTED]
        never_sent_payments_info = not_completed_payment_info[Constants.NOT_COMPLETED_NOT_SENT_END_RETRY]
        scheduled_payments_info = not_completed_payment_info[Constants.NOT_COMPLETED_SCHEDULED]
        _never_sent__payments_info = not_completed_payment_info[Constants.NOT_COMPLETED_NEVER_SENT]
        
        # generate complex objects
        creditor_institutions = CIsReportInfo(cis_total=item_ci_info["total"], 
                                              cis_on_wisp=item_ci_info["on_wisp"])
        trigger_primitives = TriggerPrimitiveReportInfo(carts_total=trigger_primitive_info["total_carts"], 
                                                        no_carts_total=trigger_primitive_info["total_no_carts"],
                                                        carts_completed=trigger_primitive_info["carts_completed"],
                                                        no_carts_completed=trigger_primitive_info["no_carts_completed"],
                                                        not_completed_rpt_timeout=trigger_primitive_not_completed[Constants.TRIGGER_PRIMITIVE_NOT_COMPLETED_RPT_TIMEOUT],
                                                        not_completed_redirect=trigger_primitive_not_completed[Constants.TRIGGER_PRIMITIVE_NOT_COMPLETED_REDIRECT],
                                                        not_completed_receipt_ko=trigger_primitive_not_completed[Constants.TRIGGER_PRIMITIVE_NOT_COMPLETED_RECEIPT_KO],
                                                        not_completed_ecommerce_timeout=trigger_primitive_not_completed[Constants.TRIGGER_PRIMITIVE_NOT_COMPLETED_ECOMMERCE_TIMEOUT],
                                                        not_completed_paymenttoken_timeout=trigger_primitive_not_completed[Constants.TRIGGER_PRIMITIVE_NOT_COMPLETED_PAYMENTTOKEN_TIMEOUT],
                                                        not_completed_no_state=trigger_primitive_not_completed[Constants.TRIGGER_PRIMITIVE_NOT_COMPLETED_NO_STATE])
        completed_payment_info = CompletedPaymentsReportInfo(closed_as_ok=completed_payment_info[Constants.COMPLETED_OK_RECEIPT_TOTAL],
                                                            closed_as_ko=completed_payment_info[Constants.COMPLETED_KO_RECEIPT_TOTAL],
                                                            with_ok_receipts_only_sent_after_retry=completed_payment_info[Constants.COMPLETED_OK_RECEIPT_SENT_BY_RETRY],
                                                            with_ko_receipts_only_sent_after_retry=completed_payment_info[Constants.COMPLETED_KO_RECEIPT_SENT_BY_RETRY])
        not_completed_payment_info = NotCompletedPaymentsReportInfo(rejected=ReceiptDetailStatistics(receipt_ok_count=rejected_payments_info[Constants.RECEIPT_OK_COUNT],
                                                                                                     receipt_ko_count=rejected_payments_info[Constants.RECEIPT_KO_COUNT]), 
                                                                    not_sent_end_retry=ReceiptDetailStatistics(receipt_ok_count=never_sent_payments_info[Constants.RECEIPT_OK_COUNT],
                                                                                                       receipt_ko_count=never_sent_payments_info[Constants.RECEIPT_KO_COUNT]), 
                                                                    scheduled=ReceiptDetailStatistics(receipt_ok_count=scheduled_payments_info[Constants.RECEIPT_OK_COUNT],
                                                                                                      receipt_ko_count=scheduled_payments_info[Constants.RECEIPT_KO_COUNT]), 
                                                                    never_sent=ReceiptDetailStatistics(receipt_ok_count=_never_sent__payments_info[Constants.RECEIPT_OK_COUNT],
                                                                                                    receipt_ko_count=_never_sent__payments_info[Constants.RECEIPT_KO_COUNT]))

        # generate report entity 
        return ReportEntity(id=item["id"],
                            date=item["date"],
                            total_payments_on_ndp=payments_info["total_on_ndp"],
                            creditor_institutions=creditor_institutions,
                            trigger_primitive=trigger_primitives,
                            completed_payments=completed_payment_info,
                            not_completed_payments=not_completed_payment_info)


# ============================================================================
     
class ReceiptRTEntity:

    def __init__(self, id, status, type):
        self.id = id
        self.status = status
        self.type = type
    

    def from_db_item(item):
        return ReceiptRTEntity(item["id"], 
                               item["receiptStatus"], 
                               item["receiptType"])
    

# ============================================================================
     
class REEventEntity:

    def __init__(self, 
                 id, 
                 partition_key,
                 session_id,
                 business_process,
                 status, 
                 operation_id, 
                 event_category, 
                 event_subcategory):
        self.id = id
        self.partition_key = partition_key
        self.session_id = session_id
        self.business_process = business_process
        self.status = status
        self.operation_id = operation_id 
        self.event_category = event_category 
        self.event_subcategory = event_subcategory
    

    def from_db_item(item):
        return REEventEntity(id=item["id"],
                             partition_key=item["partitionKey"], 
                             session_id=item["sessionId"], 
                             business_process=item["businessProcess"],
                             status=item["status"],
                             operation_id=item["operationId"],
                             event_category=item["eventCategory"],
                             event_subcategory=item["eventSubcategory"])
 

# ============================================================================
     
class TriggerPrimitiveEntity:

    def __init__(self, session_id, primitive, payload):
        self.session_id = session_id
        self.primitive = primitive
        self.payload = payload
    

    def from_db_item(item):
        return TriggerPrimitiveEntity(item["id"], item["primitive"], item["payload"])
