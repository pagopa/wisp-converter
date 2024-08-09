Feature: Common scenarios for payment context

  Scenario: Execute NM1-to-NMU conversion in wisp-converter
    Given a valid session identifier to be redirected to WISP dismantling
    When the user continue the session in WISP dismantling
    Then the user receives the HTTP status code 302
    And the user can be redirected to Checkout

  # ===============================================================================================
  
  Scenario: Fails on execute NM1-to-NMU conversion in wisp-converter  
    Given a valid session identifier to be redirected to WISP dismantling
    When the user continue the session in WISP dismantling
    Then the user receives the HTTP status code 200
    And the user receives an HTML page with an error

  # ===============================================================================================
  
  Scenario: Send a checkPosition request
    Given a valid checkPosition request
    When the creditor institution sends a checkPosition action
    Then the creditor institution receives the HTTP status code 200
    And the response contains the field outcome with value OK
    And the response contains the field positionslist as not empty list

  # ===============================================================================================
  
  Scenario: Send an activatePaymentNoticeV2 request on first RPT
    Given a valid activatePaymentNoticeV2 request on first RPT
    When the creditor institution sends a activatePaymentNoticeV2 action
    Then the creditor institution receives the HTTP status code 200
    And the response contains the field outcome with value OK
    And the response contains the field paymentToken with non-null value
    And the payment token can be retrieved and associated to first RPT

  # ===============================================================================================
  
  Scenario: Send an activatePaymentNoticeV2 request on second RPT
    Given a valid activatePaymentNoticeV2 request on second RPT
    When the creditor institution sends a activatePaymentNoticeV2 action
    Then the creditor institution receives the HTTP status code 200
    And the response contains the field outcome with value OK
    And the response contains the field paymentToken with non-null value
    And the payment token can be retrieved and associated to second RPT

  # ===============================================================================================
  
  Scenario: Send a closePaymentV2 request
    Given a valid closePaymentV2 request with outcome OK
    When the creditor institution sends a closePaymentV2 action
    Then the creditor institution receives the HTTP status code 200
    And the response contains the field outcome with value OK