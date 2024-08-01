import base64
import logging
from constants import *
import utils


def generate_rpt(session_data):

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
        transferset=generate_transfers(session_data)
    )
    request = NODOINVIARPT_STRUCTURE.format(
        creditor_institution_broker=session_data['creditor_institution_broker'],
        creditor_institution=session_data['creditor_institution'],
        station=session_data['station'],
        psp_broker=session_data['psp_broker'],
        psp=session_data['psp'],
        channel=session_data['channel'],
        password=session_data['station_password'],
        iuv=session_data['transferset']['iuv'],
        ccp=session_data['transferset']['ccp'],
        rpt=base64.b64encode(rpt.encode('utf-8')).decode('utf-8')
    )
    return request


def generate_transfers(session_data):

    transfers_content = "";
    
    transfer_set = session_data['transferset']
    for transfer in transfer_set['transfers']:

        transfer_content = ""
            
        if transfer['is_mbd'] == False:
            transfer_content = RPT_SINGLE_TRANSFER_STRUCTURE.format(
                payer_fiscal_code=session_data['payer']['fiscal_code'],
                transfer_iuv=transfer['iuv'],
                transfer_amount=transfer['amount'],
                transfer_fee=transfer['fee'],
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
                transfer_amount=transfer['amount'],
                transfer_fee=transfer['fee'],
                transfer_payer_info=transfer['payer_info'],
                transfer_taxonomy=transfer['taxonomy'],
                transfer_stamp_type=transfer['stamp_type'],
                transfer_stamp_hash=transfer['stamp_hash'],
                transfer_stamp_province=transfer['stamp_province']
            )
        transfers_content += transfer_content
          
    return RPT_TRANSFER_SET_STRUCTURE.format(
        transferset_payment_date=transfer_set['payment_date'],
        transferset_total_amount=transfer_set['total_amount'],
        transferset_payment_type=transfer_set['payment_type'],
        transferset_iuv=transfer_set['iuv'],
        transferset_ccp=transfer_set['ccp'],
        transferset_debtor_iban=session_data['payer_delegate']['iban'],
        transferset_debtor_bic=session_data['payer_delegate']['bic'],
        transfers=transfers_content
    )







def create_transferset(session_data, number_of_transfers, multibeneficiary=False, number_of_mbd=0):

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
            'payer_info': payer_info,
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
            'payer_info': payer_info,
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
                    'payer_info': payer_info,
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
                    'payer_info': payer_info,
                    'taxonomy': "9/0301116TS/9/24B0060000000017",
                    'is_mbd': True
                })

    # populate transfer set
    transferset = {
        'iuv': iuv,
        'ccp': utils.generate_ccp(),
        'payment_date': utils.get_current_date(),
        'total_amount': sum(transfer["amount"] for transfer in transfers),
        'total_fee': sum(transfer["fee"] for transfer in transfers),
        'payment_type': "BBT",
        'transfers': transfers        
    }
    session_data['transferset'] = transferset

    return session_data


