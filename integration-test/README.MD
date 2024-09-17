# Integration Tests

👀 Integration tests are configured as:

- `config` folder contains configuration files;
- `features` folder contains feature description files;
- `steps` folder contains code files;

See there for more information.

---

### ☁️ Pipeline execution

Launch the `integration-test` GitHub Action to execute remotely.
The reports will be published here:

🔗 [Report with Allure](https://pagopauweusharedtstdtsa.blob.core.windows.net/pagopa-wisp-converter/reports/index.html)

---

### 💻 Local execution

If you want to test your local branch, you should change configuration.

``` shell
sh run_test.sh
```

ℹ️ Set in your environment:

- `INTEGRATION_TEST_GPD_SUBSCRIPTION_KEY` to set the subscription-key for `GPD core` APIs
- `INTEGRATION_TEST_NODO_SUBSCRIPTION_KEY` to set the subscription-key for `Nodo` primitives
- `INTEGRATION_TEST_NUOVACONNETTIVITA_SUBSCRIPTION_KEY` to set the subscription-key for `node-forwarder` endpoint
- `INTEGRATION_TEST_TECHNICALSUPPORT_SUBSCRIPTION_KEY` to set the subscription-key for `technical-support` APIs
- `INTEGRATION_TEST_CHANNEL_WISP_PASSWORD` to set the password for channel used by WISP flow
- `INTEGRATION_TEST_CHANNEL_CHECKOUT_PASSWORD` to set the password for channel used by Checkout
- `INTEGRATION_TEST_CHANNEL_PAYMENT_PASSWORD` to set the password for channel used by payment flow
- `INTEGRATION_TEST_STATION_PASSWORD` to set the password for station
- `TAGS` to customize test you want execute. Default `runnable`;
- `JUNIT=true` to execute _behave_ with the junit option;

#### Things to know

The test platform contains some fundamental data structures from which the steps generate the data to be sent: they
are `test_data` and `flow_data`.

- `test_data` contains the definition of static common data set for environment. On this data, some variable values can
  be injected during suite start. The structure can be found in  `config/commondata.json` file.
- `flow_data` contains the data dynamically set and updated, extracted and re-generated from `test_data` in each
  executed scenario. The structure is defined as follows:

```json
{
  "flow_data": {
    "action": {
      "request": {
        "body": "<REQUEST_BODY>"
      },
      "response": {
        "status_code": "200 | 400 | ...",
        "body": "<RESPONSE_BODY>",
        "content_type": "XML | JSON"
      },
      "trigger_primitive": {
        "name": "nodoInviaRPT | nodoInviaCarrelloRPT"
      }
    },
    "common": {
      "session_id": "<SESSION_ID>",
      "iuvs": [
        "<IUV1>",
        "<IUV2>",
        null,
        null,
        null
      ],
      "payment_notices": [
        {
          "domain_id": "<CREDITOR_INSTITUTION_ID>",
          "iuv": "<IUV_N>",
          "notice_number": "<NAV_N>",
          "payment_token": "<PAYMENT_TOKEN_N>"
        }
      ],
      "cart": {
        "id": "<CART_ID>",
        "is_multibeneficiary": true,
        "iuv_for_multibeneficiary": "<SINGLE_IUV_IF_MULTIBENEFICIARY>"
      },
      "re": {
        "last_analyzed_event": "<EVENT_TYPE_FROM_RE>"
      },
      "debt_positions": [
        {
          "iupd": "<IUPD_FROM_GPD>",
          "type": "F",
          "payStandIn": false,
          "fiscalCode": "<PAYER_FISCAL_CODE>",
          "fullName": "test_data.payer.name",
          "streetName": "test_data.payer.address",
          "civicNumber": "test_data.payer.address_number",
          "postalCode": "test_data.payer.address_zipcode",
          "city": "test_data.payer.address_location",
          "province": "test_data.payer.address_province",
          "region": null,
          "country": "test_data.payer.address_nation",
          "email": "test_data.payer.email",
          "phone": null,
          "switchToExpired": false,
          "companyName": "flow_data.rpt[i].domain.name",
          "officeName": null,
          "validityDate": null,
          "paymentDate": null,
          "pull": false,
          "status": "DRAFT | VALID",
          "paymentOption": [
            {
              "nav": "<NAV_GENERATED_BY_IUVGENERATOR>",
              "iuv": "<IUV_FROM_RPT>",
              "amount": 123.45,
              "description": "/RFB/<IUV>/<TOTAL_AMOUNT>/TXT/DEBITORE/<PAYER_FISCAL_CODE>",
              "isPartialPayment": false,
              "dueDate": "2000-01-01T00:00:00.000000000",
              "retentionDate": null,
              "fee": 0,
              "notificationFee": null,
              "transfer": [
                {
                  "idTransfer": 1,
                  "amount": 12345,
                  "organizationFiscalCode": "flow_data.rpt[i].domain.id",
                  "remittanceInformation": "flow_data.rpt[i].payment_data.transfers[j].transfer_note",
                  "category": "flow_data.rpt[i].payment_data.transfers[j].taxonomy",
                  "iban": "flow_data.rpt[i].payment_data.transfers[j].creditor_iban",
                  "postalIban": "<POSTAL_IBAN>",
                  "stamp": {
                    "stampType": "flow_data.rpt[i].payment_data.transfers[j].stamp_type",
                    "hashDocument": "flow_data.rpt[i].payment_data.transfers[j].stamp_hash",
                    "provincialResidence": "flow_data.rpt[i].payment_data.transfers[j].stamp_province"
                  },
                  "transferMetadata": [
                    {
                      "key": "DatiSpecificiRiscossione",
                      "value": "flow_data.rpt[i].payment_data.transfers[j].taxonomy"
                    }
                  ]
                }
              ],
              "paymentOptionMetadata": null
            }
          ]
        }
      ],
      "rpts": [
        {
          "domain": {
            "id": "test_data.payee_institution_N.fiscal_code",
            "name": "test_data.payee_institution_N.name",
            "station": "test_data.station"
          },
          "date_time_request": "2000-01-01T00:00:00",
          "payer": "test_data.payer",
          "payer_delegate": "test_data.payer_delegate",
          "payee_institution": "test_data.payee_institution_N",
          "payment_data": {
            "iuv": "<IUV>",
            "ccp": "<CCP>",
            "payment_date": "2000-01-01",
            "total_amount": 123.45,
            "total_fee": 12.34,
            "payment_type": "BBT | PO | ...",
            "debtor_iban": "test_data.payer_delegate.iban",
            "debtor_bic": "test_data.payer_delegate.bic",
            "payment_note": "/RFB/<IUV>/<TOTAL_AMOUNT>/TXT/DEBITORE/<PAYER_FISCAL_CODE>",
            "transfers": [
              {
                "iuv": "<IUV>",
                "amount": 16,
                "fee": 0.12,
                "stamp_hash": "cXVlc3RhIMOoIHVuYSBtYXJjYSBkYSBib2xsbw==",
                "stamp_type": "01",
                "stamp_province": "RM",
                "payer_info": "CP1.1 - MBD for transfer {i}",
                "taxonomy": "9/0301116TS/9/24B0060000000017",
                "transfer_note": "/RFB/<IUV>/<TRANSFER_AMOUNT>/TXT/DEBITORE/<PAYER_FISCAL_CODE>",
                "is_mbd": true
              },
              {
                "iuv": "<IUV>",
                "amount": 123.45,
                "fee": 1.23,
                "creditor_iban": "test_data.payee_institution_N.iban",
                "creditor_bic": "test_data.payee_institution_N.bic",
                "payer_info": "CP1.1 - Transfer {i}",
                "taxonomy": "9/0301109AP",
                "transfer_note": "/RFB/<IUV>/<TRANSFER_AMOUNT>/TXT/DEBITORE/<PAYER_FISCAL_CODE>",
                "is_mbd": false
              }
            ]
          }
        }
      ]
    }
  }
}
```

