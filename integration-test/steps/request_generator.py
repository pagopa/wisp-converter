import base64
import json
import logging
from constants import *
import utility.utils as utils


# ==============================================

def generate_nodoinviarpt(session_data):

    payment_index = 0
    payer = RPT_PAYER_STRUCTURE.format(
        payer_type=session_data['payer']['type'],
        payer_fiscal_code=session_data['payer']['fiscal_code'],
        payer_name=session_data['payer']['name'],
        payer_address=session_data['payer']['address'],
        payer_address_number=session_data['payer']['address_number'],
        payer_address_zipcode=session_data['payer']['address_zipcode'],
        payer_address_location=session_data['payer']['address_location'],
        payer_address_province=session_data['payer']['address_province'],
        payer_address_nation=session_data['payer']['address_nation'],
        payer_email=session_data['payer']['email'],
    )
    payer_delegate = RPT_PAYERDELEGATE_STRUCTURE.format(
        payer_delegate_type=session_data['payer_delegate']['type'],
        payer_delegate_fiscal_code=session_data['payer_delegate']['fiscal_code'],
        payer_delegate_name=session_data['payer_delegate']['name'],
        payer_delegate_address=session_data['payer_delegate']['address'],
        payer_delegate_address_number=session_data['payer_delegate']['address_number'],
        payer_delegate_address_zipcode=session_data['payer_delegate']['address_zipcode'],
        payer_delegate_address_location=session_data['payer_delegate']['address_location'],
        payer_delegate_address_province=session_data['payer_delegate']['address_province'],
        payer_delegate_address_nation=session_data['payer_delegate']['address_nation'],
        payer_delegate_email=session_data['payer_delegate']['email'],
    )
    payee_institution = RPT_PAYEEINSTITUTION_STRUCTURE.format(
        payee_institution_fiscal_code=session_data['payee_institutions_1']['fiscal_code'],
        payee_institution_name=session_data['payee_institutions_1']['name'],
        payee_institution_operative_code=session_data['payee_institutions_1']['operative_code'],
        payee_institution_operative_denomination=session_data['payee_institutions_1']['operative_denomination'],
        payee_institution_address=session_data['payee_institutions_1']['address'],
        payee_institution_address_number=session_data['payee_institutions_1']['address_number'],
        payee_institution_address_zipcode=session_data['payee_institutions_1']['address_zipcode'],
        payee_institution_address_location=session_data['payee_institutions_1']['address_location'],
        payee_institution_address_province=session_data['payee_institutions_1']['address_province'],
        payee_institution_address_nation=session_data['payee_institutions_1']['address_nation'],
    )
    rpt = RPT_STRUCTURE.format(
        creditor_institution=session_data['creditor_institution'],
        station=session_data['station'],
        current_date_time=utils.get_current_datetime(),
        payer_delegate=payer_delegate,
        payer=payer,
        payee_institution=payee_institution,
        payment=generate_transfers(session_data, payment_index)
    )
    request = NODOINVIARPT_STRUCTURE.format(
        creditor_institution_broker=session_data['creditor_institution_broker'],
        creditor_institution=session_data['creditor_institution'],
        station=session_data['station'],
        psp_broker=session_data['psp_broker_wisp'],
        psp=session_data['psp_wisp'],
        channel=session_data['channel_wisp'],
        password=session_data['station_password'],
        iuv=session_data['payments'][payment_index]['iuv'],
        ccp=session_data['payments'][payment_index]['ccp'],
        rpt=base64.b64encode(rpt.encode('utf-8')).decode('utf-8')
    )
    return request

# ==============================================

def generate_transfers(session_data, payment_index):

    transfers_content = ""
    payment = session_data['payments'][payment_index]
    
    for transfer in payment['transfers']:

        transfer_content = ""                
        if transfer['is_mbd'] == False:
            transfer_content = RPT_SINGLE_TRANSFER_STRUCTURE.format(
                payer_fiscal_code=session_data['payer']['fiscal_code'],
                transfer_iuv=transfer['iuv'],
                transfer_amount="{:.2f}".format(transfer['amount']),
                transfer_fee="{:.2f}".format(transfer['fee']),
                transfer_creditor_iban=transfer['creditor_iban'],
                transfer_creditor_bic=transfer['creditor_bic'],
                transfer_creditor_iban2=transfer['creditor_iban2'],
                transfer_creditor_bic2=transfer['creditor_bic2'],
                transfer_payer_info=transfer['payer_info'],
                transfer_taxonomy=transfer['taxonomy'],
            )
        else:
            transfer_content = RPT_SINGLE_MBD_TRANSFER_STRUCTURE.format(
                payer_fiscal_code=session_data['payer']['fiscal_code'],
                transfer_iuv=transfer['iuv'],
                transfer_amount="{:.2f}".format(transfer['amount']),
                transfer_fee="{:.2f}".format(transfer['fee']),
                transfer_payer_info=transfer['payer_info'],
                transfer_taxonomy=transfer['taxonomy'],
                transfer_stamp_type=transfer['stamp_type'],
                transfer_stamp_hash=transfer['stamp_hash'],
                transfer_stamp_province=transfer['stamp_province']
            )
        transfers_content += transfer_content
            
    return RPT_TRANSFER_SET_STRUCTURE.format(
        payment_payment_date=payment['payment_date'],
        payment_total_amount="{:.2f}".format(payment['total_amount']),
        payment_payment_type=payment['payment_type'],
        payment_iuv=payment['iuv'],
        payment_ccp=payment['ccp'],
        payment_debtor_iban=session_data['payer_delegate']['iban'],
        payment_debtor_bic=session_data['payer_delegate']['bic'],
        transfers=transfers_content
    )

# ==============================================

def create_payments(session_data, number_of_payments, number_of_transfers, multibeneficiary=False, number_of_mbd=0):

    session_data['payments'] = []
    for payment_index in range(number_of_payments):

        iuv = utils.generate_iuv()
        payer_info = "CP1.1"
        taxonomy = "9/0301109AP"
        transfers = []

        # generating transfer for multibeneficiary
        if multibeneficiary:
            transfers.append({
                'iuv': iuv,
                'amount': utils.generate_random_monetary_amount(10.00, 599.99),
                'fee': utils.generate_random_monetary_amount(0.10, 2.50),
                'creditor_iban': session_data['payee_institutions_1']['iban'],
                'creditor_bic': session_data['payee_institutions_1']['bic'],
                'creditor_iban2': session_data['payee_institutions_1']['iban'],
                'creditor_bic2': session_data['payee_institutions_1']['bic'],
                'payer_info': payer_info + f" - Rata {payment_index}",
                'taxonomy': taxonomy,
                'is_mbd': False
            })
            transfers.append({
                'iuv': iuv,
                'amount': utils.generate_random_monetary_amount(10.00, 599.99),
                'fee': utils.generate_random_monetary_amount(0.10, 2.50),
                'creditor_iban': session_data['payee_institutions_2']['iban'],
                'creditor_bic': session_data['payee_institutions_2']['bic'],
                'creditor_iban2': session_data['payee_institutions_2']['iban'],
                'creditor_bic2': session_data['payee_institutions_2']['bic'],
                'payer_info': payer_info + f" - Rata {payment_index}",
                'taxonomy': taxonomy,
                'is_mbd': False
            })
        
        # generating transfer for non-multibeneficiary
        else:
            no_mbd_transfers = number_of_transfers - number_of_mbd
            for i in range(number_of_transfers):
                # generating MBD transfer
                if no_mbd_transfers > 0:
                    transfers.append({
                        'iuv': iuv,
                        'amount': utils.generate_random_monetary_amount(10.00, 599.99),
                        'fee': utils.generate_random_monetary_amount(0.10, 2.50),
                        'creditor_iban': session_data['payee_institutions_1']['iban'],
                        'creditor_bic': session_data['payee_institutions_1']['bic'],
                        'creditor_iban2': session_data['payee_institutions_1']['iban'],
                        'creditor_bic2': session_data['payee_institutions_1']['bic'],
                        'payer_info': payer_info + f" - Rata {payment_index}",
                        'taxonomy': taxonomy,
                        'is_mbd': False
                    })
                    no_mbd_transfers -= 1
                
                else:
                    # generating MBD transfer
                    transfers.append({
                        'iuv': iuv,
                        'amount': 16.00,
                        'fee': utils.generate_random_monetary_amount(0.10, 0.50),
                        'stamp_hash': "cXVlc3RhIMOoIHVuYSBtYXJjYSBkYSBib2xsbw==",
                        'stamp_type': "01",
                        'stamp_province': "RM",
                        'payer_info': payer_info + f" - MBD {payment_index}",
                        'taxonomy': "9/0301116TS/9/24B0060000000017",
                        'is_mbd': True
                    })

        # populate payment common data
        payment = {
            'iuv': iuv,
            'ccp': utils.generate_ccp(),
            'payment_date': utils.get_current_date(),
            'total_amount': int(sum(transfer["amount"] for transfer in transfers) * 100) / 100,
            'total_fee': int(sum(transfer["fee"] for transfer in transfers) * 100) / 100,
            'payment_type': "BBT",
            'transfers': transfers        
        }
        session_data['payments'].append(payment)

    return session_data

# ==============================================

def generate_checkposition(payment_notices):

    checkposition = {"positionslist":[]}
    for payment_notice in payment_notices:
        checkposition["positionslist"].append({
            "fiscalCode": payment_notice['domain_id'],
            "noticeNumber": payment_notice['notice_number']
        })
    content = json.dumps(checkposition, separators=(',', ':'))
    return content

# ==============================================

def generate_activatepaymentnotice(test_data, payment_notices, payment):

    iuv = payment['iuv']
    total_amount = payment['total_amount']
    notice_number = None
    for payment_notice in payment_notices:
        if payment_notice['iuv'] == iuv:
            notice_number = payment_notice['notice_number']    
    idempotency_key = notice_number + '_' + utils.get_random_digit_string(10) 
    
    return ACTIVATE_PAYMENT_NOTICE.format(
        psp=test_data['psp_wisp'],
        psp_broker=test_data['psp_broker_wisp'],
        channel=test_data['channel_checkout'],
        password=test_data['channel_checkout_password'],
        idempotency_key=idempotency_key,
        fiscal_code=payment_notice['domain_id'],
        notice_number=notice_number,
        amount="{:.2f}".format(total_amount),
        payment_note="Integration test"
    )

def generate_closepayment(test_data, payment_notices, outcome):

    transactionId = utils.get_random_alphanumeric_string(32)
    amount = sum(payment['total_amount'] for payment in test_data['payments'])
    fees = sum(payment['total_fee'] for payment in test_data['payments'])
    grand_total = (amount + fees) * 100
    auth_code = utils.get_random_digit_string(6)
    rrn = utils.get_random_digit_string(12)
    now = utils.get_current_datetime() + ".000Z";

    closepayment = {
        "paymentTokens": [payment_notice['payment_token'] for payment_notice in payment_notices],
        "outcome": outcome,
        "idPSP": "BCITITMM",
        "idBrokerPSP": "00799960158",
        "idChannel": test_data['channel_payment'],
        "paymentMethod": "CP",
        "transactionId": transactionId,
        "totalAmount": grand_total / 100,
        "fee": fees,
        "timestampOperation": now,
        "transactionDetails": {
            "transaction": {
                "transactionId": transactionId,
                "transactionStatus": "Confermato",
                "creationDate": now,
                "grandTotal": int(grand_total),
                "amount": int(amount * 100),
                "fee": int(fees * 100),
                "authorizationCode": auth_code,
                "rrn": rrn,
                "psp": {
                    "idPsp": test_data['psp_payment'],
                    "idChannel": test_data['channel_payment'],
                    "businessName": test_data['psp_name'],
                    "brokerName": test_data['psp_broker_payment'],
                    "pspOnUs": False
                },
                "timestampOperation": now,
                "paymentGateway": "NPG"
            },
            "info": {
                "type": "CP",
                "brandLogo": "https://assets.cdn.platform.pagopa.it/creditcard/mastercard.png",
                "brand": "MC",
                "paymentMethodName": "CARDS",
                "clientId": "CHECKOUT"
            },
            "user": {
                "type": "GUEST"
            }
        },
        "additionalPaymentInformations": {
            "outcomePaymentGateway": outcome,
            "fee": "{:.2f}".format(fees),
            "totalAmount": "{:.2f}".format(grand_total / 100),
            "timestampOperation": now,
            "rrn": rrn,
            "authorizationCode": auth_code,
            "email": "test@mail.it"
        }
    }
    
    content = json.dumps(closepayment, separators=(',', ':'))
    return content