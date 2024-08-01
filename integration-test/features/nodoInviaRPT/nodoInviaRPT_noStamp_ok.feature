Feature: User pays a single payment on nodoInviaRPT

  Background:
    Given systems up
    And a new session

  @runnable @happypath
  Scenario: User pays a single payment on nodoInviaRPT

    # Start process, sending nodoInviaRPT to wisp-soap-converter 
    Given a single RPT with 1 transfers
    When the user sends a nodoInviaRPT action
    Then the user receives the HTTP status code 200 
    And the user receives a response with outcome OK
    And the user receives a response with the redirect URL
 
    # Executing NM1-to-NMU conversion in wisp-converter
    Given a valid session identifier to be redirected to WISP dismantling
    When the user continue the session in WISP dismantling
    Then the user receives the HTTP status code 302 
    And the user can be redirected to Checkout

    # # Simulating the step when Checkout uses notice number from sent cart
    Given the first IUV code of the sent RPTs
    When the user searches for flow steps by IUVs
    Then the user receives the HTTP status code 200 
    And the notice number can be retrieved 

    # # Send a checkPosition request as creditor institution
    # Given a valid checkPosition request
    # When the creditor institution sends a checkPosition action
    # Then the creditor institution receives the HTTP status code 200
    # And the response contains the field outcome with value OK
    # And the response contains the field positionslist as not empty list

    # # Send a activatePaymentNoticeV2 request as creditor institution
    # Given a valid activatePaymentNoticeV2 request
    # When the creditor institution sends a activatePaymentNotice action
    # Then the creditor institution receives the HTTP status code 200
    # And the response contains the field Body.activatePaymentNoticeV2Response.outcome with value OK
    # And the response contains the field Body.activatePaymentNoticeV2Response.paymentToken with non-null value 
    # And the field Body.activatePaymentNoticeV2Response.paymentToken is retrieved

    # # Checking if WISP session timer is created
    # Given the first IUV code of the sent RPTs
    # When the user searches for flow steps by IUV
    # Then the user receives the HTTP status code 200 
    # And there is a timer-set event with field status code 200 

    # # Send a closePaymentV2 request as creditor institution
    # Given a valid closePaymentV2 request with outcome OK
    # When the creditor institution sends a closePaymentV2 action
    # Then the creditor institution receives the HTTP status code 200
    # And the response contains the field outcome with value OK

    # # Checking if WISP session timer is deleted
    # Given the first IUV code of the sent RPTs
    # When the user searches for flow steps by IUV
    # Then the user receives the HTTP status code 200 
    # And there is a timer-delete event with field status code 200 

    # # Send a sendPaymentOutcomeV2 request as PSP
    # Given a valid sendPaymentOutcomeV2 request with outcome OK
    # When the PSP sends a sendPaymentOutcome action
    # Then the PSP receives the HTTP status code 200
    # And the response contains the field Body.sendPaymentOutcomeV2Response.outcome with value OK

    # # Checking if OK paaInviaRT request is sent to creditor institution
    # Given the first IUV code of the sent RPTs
    # When the user searches for flow steps by IUV
    # Then the user receives the HTTP status code 200 
    # And there is a receipt-ok event with field status code 200 