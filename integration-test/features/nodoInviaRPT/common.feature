Feature: Common scenarios for nodoInviaRPT
  
  Scenario: Execute NM1-to-NMU conversion in wisp-converter    
    Given a valid session identifier to be redirected to WISP dismantling
    When the user continue the session in WISP dismantling
    Then the user receives the HTTP status code 302 
    And the user can be redirected to Checkout

  Scenario: Retrieve notice number from executed redirect
    Given a waiting time of 5 seconds to wait for Nodo to write RE events
    And the first IUV code of the sent RPTs
    When the user searches for flow steps by IUVs
    Then the user receives the HTTP status code 200 
    And the notice number can be retrieved 

  Scenario: Send a checkPosition request
    Given a valid checkPosition request
    When the creditor institution sends a checkPosition action
    Then the creditor institution receives the HTTP status code 200
    And the response contains the field outcome with value OK
    And the response contains the field positionslist as not empty list

  Scenario: Send an activatePaymentNoticeV2 request
    Given a valid activatePaymentNoticeV2 request on first RPT
    When the creditor institution sends a activatePaymentNoticeV2 action
    Then the creditor institution receives the HTTP status code 200
    And the response contains the field outcome with value OK
    And the response contains the field paymentToken with non-null value 
    And the payment token can be retrieved and associated to first RPT

  Scenario: Send a closePaymentV2 request
    Given a valid closePaymentV2 request with outcome OK
    When the creditor institution sends a closePaymentV2 action
    Then the creditor institution receives the HTTP status code 200
    And the response contains the field outcome with value OK

  Scenario: Check if WISP session timer was created
    Given a waiting time of 5 seconds to wait for Nodo to write RE events
    And the first IUV code of the sent RPTs
    When the user searches for flow steps by IUVs
    Then the user receives the HTTP status code 200 
    And there is a timer-set event with field operationStatus with value Success 

  Scenario: Check if WISP session timer was deleted and RT was sent
    Given a waiting time of 5 seconds to wait for Nodo to write RE events
    And the first IUV code of the sent RPTs
    When the user searches for flow steps by IUVs
    Then the user receives the HTTP status code 200 
    And there is a timer-delete event with field operationStatus with value Success 
    And there is a receipt-ok event with field operationStatus with value Success 

  Scenario: Execute redirect and complete payment from NodoInviaRPT
    When the execution of "Execute NM1-to-NMU conversion in wisp-converter" was successful
    Then the execution of "Retrieve notice number from executed redirect" was successful
    And the execution of "Send a checkPosition request" was successful
    And the execution of "Send an activatePaymentNoticeV2 request" was successful
    And the execution of "Check if WISP session timer was created" was successful
    And the execution of "Send a closePaymentV2 request" was successful
    And the execution of "Check if WISP session timer was deleted and RT was sent" was successful

    



    






