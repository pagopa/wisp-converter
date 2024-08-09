import base64
from decimal import Decimal
import json
import logging
import uuid
import constants as constants
import utility.session as session
import utility.utils as utils


# ==============================================

def generate_nodoinviarpt(test_data, payment):

    payer = constants.RPT_PAYER_STRUCTURE.format(
        payer_type=test_data['payer']['type'],
        payer_fiscal_code=test_data['payer']['fiscal_code'],
        payer_name=test_data['payer']['name'],
        payer_address=test_data['payer']['address'],
        payer_address_number=test_data['payer']['address_number'],
        payer_address_zipcode=test_data['payer']['address_zipcode'],
        payer_address_location=test_data['payer']['address_location'],
        payer_address_province=test_data['payer']['address_province'],
        payer_address_nation=test_data['payer']['address_nation'],
        payer_email=test_data['payer']['email'],
    )
    payer_delegate = constants.RPT_PAYERDELEGATE_STRUCTURE.format(
        payer_delegate_type=test_data['payer_delegate']['type'],
        payer_delegate_fiscal_code=test_data['payer_delegate']['fiscal_code'],
        payer_delegate_name=test_data['payer_delegate']['name'],
        payer_delegate_address=test_data['payer_delegate']['address'],
        payer_delegate_address_number=test_data['payer_delegate']['address_number'],
        payer_delegate_address_zipcode=test_data['payer_delegate']['address_zipcode'],
        payer_delegate_address_location=test_data['payer_delegate']['address_location'],
        payer_delegate_address_province=test_data['payer_delegate']['address_province'],
        payer_delegate_address_nation=test_data['payer_delegate']['address_nation'],
        payer_delegate_email=test_data['payer_delegate']['email'],
    )
    payee_institution = constants.RPT_PAYEEINSTITUTION_STRUCTURE.format(
        payee_institution_fiscal_code=test_data['payee_institutions_1']['fiscal_code'],
        payee_institution_name=test_data['payee_institutions_1']['name'],
        payee_institution_operative_code=test_data['payee_institutions_1']['operative_code'],
        payee_institution_operative_denomination=test_data['payee_institutions_1']['operative_denomination'],
        payee_institution_address=test_data['payee_institutions_1']['address'],
        payee_institution_address_number=test_data['payee_institutions_1']['address_number'],
        payee_institution_address_zipcode=test_data['payee_institutions_1']['address_zipcode'],
        payee_institution_address_location=test_data['payee_institutions_1']['address_location'],
        payee_institution_address_province=test_data['payee_institutions_1']['address_province'],
        payee_institution_address_nation=test_data['payee_institutions_1']['address_nation'],
    )
    rpt = constants.RPT_STRUCTURE.format(
        creditor_institution=test_data['creditor_institution'],
        station=test_data['station'],
        current_date_time=utils.get_current_datetime(),
        payer_delegate=payer_delegate,
        payer=payer,
        payee_institution=payee_institution,
        payment=generate_transfers(test_data, payment)
    )
    request = constants.NODOINVIARPT_STRUCTURE.format(
        creditor_institution_broker=test_data['creditor_institution_broker'],
        creditor_institution=test_data['creditor_institution'],
        station=test_data['station'],
        psp_broker=test_data['psp_broker_wisp'],
        psp=test_data['psp_wisp'],
        channel=test_data['channel_wisp'],
        password=test_data['station_password'],
        iuv=payment['iuv'],
        ccp=payment['ccp'],
        rpt=base64.b64encode(rpt.encode('utf-8')).decode('utf-8')
    )
    return request

# ==============================================

def generate_nodoinviacarrellorpt(test_data, payments):
    # TODO 
    1==1

# ==============================================

def generate_transfers(test_data, payment):

    transfers_content = ""
    for transfer in payment['transfers']:

        transfer_content = ""                
        if transfer['is_mbd'] == False:
            transfer_content = constants.RPT_SINGLE_TRANSFER_STRUCTURE.format(
                payer_fiscal_code=test_data['payer']['fiscal_code'],
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
            transfer_content = constants.RPT_SINGLE_MBD_TRANSFER_STRUCTURE.format(
                payer_fiscal_code=test_data['payer']['fiscal_code'],
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
            
    return constants.RPT_TRANSFER_SET_STRUCTURE.format(
        payment_payment_date=payment['payment_date'],
        payment_total_amount="{:.2f}".format(payment['total_amount']),
        payment_payment_type=payment['payment_type'],
        payment_iuv=payment['iuv'],
        payment_ccp=payment['ccp'],
        payment_debtor_iban=test_data['payer_delegate']['iban'],
        payment_debtor_bic=test_data['payer_delegate']['bic'],
        transfers=transfers_content
    )

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

def generate_activatepaymentnotice(test_data, payment_notices, payment, session_id):

    iuv = payment['iuv']
    total_amount = payment['total_amount']
    notice_number = None
    for payment_notice in payment_notices:
        if payment_notice['iuv'] == iuv:
            notice_number = payment_notice['notice_number']    
    idempotency_key = notice_number + '_' + utils.get_random_digit_string(10) 
    
    return constants.ACTIVATE_PAYMENT_NOTICE.format(
        psp=test_data['psp_wisp'],
        psp_broker=test_data['psp_broker_wisp'],
        channel=test_data['channel_checkout'],
        password=test_data['channel_checkout_password'],
        idempotency_key=idempotency_key,
        fiscal_code=payment_notice['domain_id'],
        notice_number=notice_number,
        amount="{:.2f}".format(total_amount),
        payment_note=session_id
    )

# ==============================================

def generate_closepayment(test_data, payment_notices, outcome):

    transactionId = utils.get_random_alphanumeric_string(32)
    amount = round(sum(payment['total_amount'] for payment in test_data['payments']), 2)
    fees = round(sum(payment['total_fee'] for payment in test_data['payments']), 2)
    grand_total = round((amount + fees) * 100, 2)
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
        "totalAmount": round(grand_total / 100, 2),
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

# ==============================================

def generate_paymentposition(context, rpt, segregation_code, payment_status):

    payment_positions = session.get_flow_data(context, constants.SESSION_DATA_DEBT_POSITIONS)
    if payment_positions is None:
        payment_positions = []

    iuv = rpt.find(".//identificativoUnivocoVersamento").text
    total_amount = rpt.find(".//importoTotaleDaVersare").text
    domain_id = rpt.find(".//dominio").find(".//identificativoDominio").text
    fiscal_code = rpt.find(".//identificativoUnivocoPagatore").find(".//codiceIdentificativoUnivoco").text
    nav = utils.generate_nav(segregation_code)
    
    extracted_transfers = list(rpt.findall(".//datiSingoloVersamento"))
    transfers = []
    transfer_index = 1
    for extracted_transfer in extracted_transfers:
        iban = extracted_transfer.find(".//ibanAccredito").text if extracted_transfer.find(".//ibanAccredito") is not None else None
        stamp = None if iban is not None else {
            "stampType": rpt.find(".//tipoBollo").text,
            "hashDocument": rpt.find(".//hashDocumento").text,
            "provincialResidence": rpt.find(".//provinciaResidenza").text,
        }
        transfers.append({
            "idTransfer": transfer_index,
            "amount": round(Decimal(extracted_transfer.find(".//importoSingoloVersamento").text) * 100),
            "organizationFiscalCode": domain_id,
            "remittanceInformation": rpt.find(".//causaleVersamento").text,
            "category": extracted_transfer.find(".//datiSpecificiRiscossione").text,
            "iban": iban,
            "postalIban": iban if iban is not None and iban[5:10] == "07601" else None,
            "stamp": stamp,
            "transferMetadata": [
                {
                    "key": "DatiSpecificiRiscossione",
                    "value": extracted_transfer.find(".//datiSpecificiRiscossione").text
                }
            ]
        })
        transfer_index += 1

    payment_position = {
        "iupd": f'wisp_{domain_id}_{uuid.uuid4()}',
        "type": "F",
        "payStandIn": False,
        "fiscalCode": fiscal_code,
        "fullName": rpt.find(".//anagraficaPagatore").text,
        "streetName": rpt.find(".//indirizzoPagatore").text,
        "civicNumber": rpt.find(".//civicoPagatore").text,
        "postalCode": rpt.find(".//capPagatore").text,
        "city": rpt.find(".//localitaPagatore").text,
        "province": rpt.find(".//provinciaPagatore").text,
        "region": None,
        "country": rpt.find(".//nazionePagatore").text,
        "email": rpt.find(".//e-mailPagatore").text,
        "phone": None,
        "switchToExpired": False,
        "companyName": rpt.find(".//denominazioneBeneficiario").text,
        "officeName": None,
        "validityDate": None,
        "paymentDate": None,
        "pull": False,
        "status": f"{payment_status}",
        "paymentOption": [
            {
                "nav": nav,
                "iuv": iuv,
                "amount": round(Decimal(total_amount) * 100),
                "description": f"/RFB/{iuv}/{total_amount}/TXT/DEBITORE/{fiscal_code}",
                "isPartialPayment": False,
                "dueDate": f'{utils.get_tomorrow_datetime()}.000000000',
                "retentionDate": None,
                "fee": 0,
                "notificationFee": None,
                "transfer": transfers,
                "paymentOptionMetadata": None
            }
        ]
    }
    payment_positions.append(payment_position)
    request = {"paymentPositions": payment_positions}
    content = json.dumps(request, separators=(',', ':'))
    return content

# ==============================================

def create_payment(session_data, payment_type, number_of_transfers, number_of_mbd=0):

    iuv = utils.generate_iuv()
    payer_info = "CP1.1"
    taxonomy = "9/0301109AP"
    transfers = []

    no_mbd_transfers = number_of_transfers - number_of_mbd
    for i in range(number_of_transfers):
        
        # generating simple transfer
        if no_mbd_transfers > 0:
            transfers.append({
                'iuv': iuv,
                'amount': utils.generate_random_monetary_amount(10.00, 599.99),
                'fee': utils.generate_random_monetary_amount(0.10, 2.50),
                'creditor_iban': session_data['payee_institutions_1']['iban'],
                'creditor_bic': session_data['payee_institutions_1']['bic'],
                'creditor_iban2': session_data['payee_institutions_1']['iban'],
                'creditor_bic2': session_data['payee_institutions_1']['bic'],
                'payer_info': payer_info + f" - Transfer {i}",
                'taxonomy': taxonomy,
                'is_mbd': False
            })
            no_mbd_transfers -= 1

        # generating MBD transfer    
        else:
            transfers.append({
                'iuv': iuv,
                'amount': 16.00,
                'fee': utils.generate_random_monetary_amount(0.10, 0.50),
                'stamp_hash': "cXVlc3RhIMOoIHVuYSBtYXJjYSBkYSBib2xsbw==",
                'stamp_type': "01",
                'stamp_province': "RM",
                'payer_info': payer_info + f" - MBD for transfer {i}",
                'taxonomy': "9/0301116TS/9/24B0060000000017",
                'is_mbd': True
            })

        # populate payment common data
        payment = {
            'iuv': iuv,
            'ccp': utils.generate_ccp(),
            'payment_date': utils.get_current_date(),
            'total_amount': round(sum(transfer["amount"] for transfer in transfers), 2),
            'total_fee': round(sum(transfer["fee"] for transfer in transfers), 2),
            'payment_type': payment_type,
            'transfers': transfers        
        }

    return payment
