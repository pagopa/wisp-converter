Feature: Common scenarios for nodoInviaRPT

  Scenario: Send a nodoInviaRPT request
    Given a valid nodoInviaRPT request
    When the user sends a nodoInviaRPT action
    Then the user receives the HTTP status code 200 
    And the response contains the field esito with value OK
    And the response contains the redirect URL

  # ===============================================================================================
  
  Scenario: Retrieve notice number from executed redirect
    Given a waiting time of 2 seconds to wait for Nodo to write RE events
    And the first IUV code of the sent RPTs
    When the user searches for flow steps by IUVs
    Then the user receives the HTTP status code 200
    And the notice number can be retrieved

  # ===============================================================================================
  
  Scenario: Check if WISP session timer was created
    Given a waiting time of 5 seconds to wait for Nodo to write RE events
    And the first IUV code of the sent RPTs
    When the user searches for flow steps by IUVs
    Then the user receives the HTTP status code 200
    And there is a timer-set event with field operationStatus with value Success

  # ===============================================================================================
  
  Scenario: Check if WISP session timer was deleted and RT was sent
    Given a waiting time of 10 seconds to wait for Nodo to write RE events
    And the first IUV code of the sent RPTs
    When the user searches for flow steps by IUVs
    Then the user receives the HTTP status code 200
    And there is a timer-delete event with field operationStatus with value Success
    And there is a receipt-ok event with field operationStatus with value Success

  # ===============================================================================================
  
  Scenario: Check the paid payment position
    When the user searches for payment position in GPD by first IUV
    Then the user receives the HTTP status code 200
    And the response contains the field status with value PAID
    And the response contains a single payment option
    And the response contains the payment option correctly generated from first RPT
    And the response contains the status in PO_PAID for the payment option
    And the response contains the transfers correctly generated from first RPT in nodoInviaRPT

  # ===============================================================================================
  
  Scenario: Check if existing debt position was used
    Given a waiting time of 2 seconds to wait for Nodo to write RE events
    And the first IUV code of the sent RPTs
    When the user searches for flow steps by IUVs
    Then the user receives the HTTP status code 200
    And there is a redirect event with field status with value UPDATED_EXISTING_PAYMENT_POSITION_IN_GPD

  # ===============================================================================================
  
  Scenario: Check if existing debt position was invalid but has sent a KO receipt
    Given a waiting time of 2 seconds to wait for Nodo to write RE events
    And the first IUV code of the sent RPTs
    When the user searches for flow steps by IUVs
    Then the user receives the HTTP status code 200
    And there is a redirect event with field operationErrorCode with value WIC-1300
    And there is a redirect event with field status with value RT_SEND_SUCCESS

  # ===============================================================================================

  Scenario: Check if existing debt position was invalid from ACA but has sent a KO receipt
    Given a waiting time of 2 seconds to wait for Nodo to write RE events
    And the first IUV code of the sent RPTs
    When the user searches for flow steps by IUVs
    Then the user receives the HTTP status code 200
    And there is a redirect event with field operationErrorCode with value WIC-1205
    And there is a redirect event with field status with value RT_SEND_SUCCESS

  # ===============================================================================================
  # ===============================================================================================

  Scenario: Execute redirect and complete payment from NodoInviaRPT
    When the execution of "Execute NM1-to-NMU conversion in wisp-converter" was successful
    Then the execution of "Retrieve notice number from executed redirect" was successful
    And the execution of "Send a checkPosition request" was successful
    And the execution of "Send one or more activatePaymentNoticeV2 requests" was successful
    And the execution of "Check if WISP session timer was created" was successful
    And the execution of "Send a closePaymentV2 request" was successful
    And the execution of "Check if WISP session timer was deleted and RT was sent" was successful
    And the execution of "Check the paid payment position" was successful












