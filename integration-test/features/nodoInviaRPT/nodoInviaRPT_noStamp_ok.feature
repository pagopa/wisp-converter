Feature: User pays a single payment on nodoInviaRPT

  Background:
    Given systems up
    And a new session

  # ===============================================================================================

  @runnable @happypath
  Scenario: User pays a single payment with single transfer on nodoInviaRPT

    # Start process, sending nodoInviaRPT to wisp-soap-converter 
    Given a single RPT with 1 transfers
    When the user sends a nodoInviaRPT action
    Then the user receives the HTTP status code 200 
    And the response contains the field esito with value OK
    And the response contains the redirect URL
 
    # Executing NM1-to-NMU conversion in wisp-converter
    Given a valid session identifier to be redirected to WISP dismantling
    When the user continue the session in WISP dismantling
    Then the user receives the HTTP status code 302 
    And the user can be redirected to Checkout

    # Simulating the step when Checkout uses notice number from sent cart
    Given a waiting time of 5 seconds to wait for Nodo to write RE events
    And the first IUV code of the sent RPTs
    When the user searches for flow steps by IUVs
    Then the user receives the HTTP status code 200 
    And the notice number can be retrieved 

    # Send a checkPosition request as creditor institution, simulating Checkout behavior
    Given a valid checkPosition request
    When the creditor institution sends a checkPosition action
    Then the creditor institution receives the HTTP status code 200
    And the response contains the field outcome with value OK
    And the response contains the field positionslist as not empty list

    # # Send a activatePaymentNoticeV2 request as creditor institution
    Given a valid activatePaymentNoticeV2 request on first RPT
    When the creditor institution sends a activatePaymentNoticeV2 action
    Then the creditor institution receives the HTTP status code 200
    And the response contains the field outcome with value OK
    And the response contains the field paymentToken with non-null value 
    And the payment token can be retrieved and associated to first RPT

    # Checking if WISP session timer is created
    Given a waiting time of 5 seconds to wait for Nodo to write RE events
    And the first IUV code of the sent RPTs
    When the user searches for flow steps by IUVs
    Then the user receives the HTTP status code 200 
    And there is a timer-set event with field operationStatus with value Success 

    # # Send a closePaymentV2 request as creditor institution
    Given a valid closePaymentV2 request with outcome OK
    When the creditor institution sends a closePaymentV2 action
    Then the creditor institution receives the HTTP status code 200
    And the response contains the field outcome with value OK

    # Checking if WISP session timer is deleted
    Given a waiting time of 5 seconds to wait for Nodo to write RE events
    And the first IUV code of the sent RPTs
    When the user searches for flow steps by IUVs
    Then the user receives the HTTP status code 200 
    And there is a timer-delete event with field operationStatus with value Success 

    # Checking if OK paaInviaRT request is sent to creditor institution
    Given a waiting time of 5 seconds to wait for Nodo to write RE events
    And the first IUV code of the sent RPTs
    When the user searches for flow steps by IUVs
    Then the user receives the HTTP status code 200 
    And there is a receipt-ok event with field operationStatus with value Success 

  # ===============================================================================================
  # ===============================================================================================

  @runnable @happypath
  Scenario: User pays a single payment with two transfers on nodoInviaRPT

    # Start process, sending nodoInviaRPT to wisp-soap-converter 
    Given a single RPT with 2 transfers
    When the user sends a nodoInviaRPT action
    Then the user receives the HTTP status code 200 
    And the response contains the field esito with value OK
    And the response contains the redirect URL
 
    # Executing NM1-to-NMU conversion in wisp-converter
    Given a valid session identifier to be redirected to WISP dismantling
    When the user continue the session in WISP dismantling
    Then the user receives the HTTP status code 302 
    And the user can be redirected to Checkout

    # Simulating the step when Checkout uses notice number from sent cart
    Given a waiting time of 5 seconds to wait for Nodo to write RE events
    And the first IUV code of the sent RPTs
    When the user searches for flow steps by IUVs
    Then the user receives the HTTP status code 200 
    And the notice number can be retrieved 

    # Send a checkPosition request as creditor institution, simulating Checkout behavior
    Given a valid checkPosition request
    When the creditor institution sends a checkPosition action
    Then the creditor institution receives the HTTP status code 200
    And the response contains the field outcome with value OK
    And the response contains the field positionslist as not empty list

    # # Send a activatePaymentNoticeV2 request as creditor institution
    Given a valid activatePaymentNoticeV2 request on first RPT
    When the creditor institution sends a activatePaymentNoticeV2 action
    Then the creditor institution receives the HTTP status code 200
    And the response contains the field outcome with value OK
    And the response contains the field paymentToken with non-null value 
    And the payment token can be retrieved and associated to first RPT

    # Checking if WISP session timer is created
    Given a waiting time of 5 seconds to wait for Nodo to write RE events
    And the first IUV code of the sent RPTs
    When the user searches for flow steps by IUVs
    Then the user receives the HTTP status code 200 
    And there is a timer-set event with field operationStatus with value Success 

    # # Send a closePaymentV2 request as creditor institution
    Given a valid closePaymentV2 request with outcome OK
    When the creditor institution sends a closePaymentV2 action
    Then the creditor institution receives the HTTP status code 200
    And the response contains the field outcome with value OK

    # Checking if WISP session timer is deleted
    Given a waiting time of 5 seconds to wait for Nodo to write RE events
    And the first IUV code of the sent RPTs
    When the user searches for flow steps by IUVs
    Then the user receives the HTTP status code 200 
    And there is a timer-delete event with field operationStatus with value Success 

    # Checking if OK paaInviaRT request is sent to creditor institution
    Given a waiting time of 5 seconds to wait for Nodo to write RE events
    And the first IUV code of the sent RPTs
    When the user searches for flow steps by IUVs
    Then the user receives the HTTP status code 200 
    And there is a receipt-ok event with field operationStatus with value Success 
