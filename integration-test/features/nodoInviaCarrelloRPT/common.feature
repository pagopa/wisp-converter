Feature: Common scenarios for nodoInviaCarrelloRPT

  Scenario: Send a nodoInviaCarrelloRPT request
    Given a valid nodoInviaCarrelloRPT request for WISP channel
    When the user sends a nodoInviaCarrelloRPT action
    Then the user receives the HTTP status code 200 
    And the response contains the field esitoComplessivoOperazione with value OK
    And the response contains the redirect URL

  # ===============================================================================================
  
  # Scenario: Check if WISP session timers were deleted and all RTs were sent
  #   Given a waiting time of 10 seconds to wait for Nodo to write RE events
  #   #
  #   Given the first IUV code of the sent RPTs
  #   When the user searches for flow steps by IUVs
  #   Then the user receives the HTTP status code 200
  #   And there is a timer-delete event with field operationStatus with value Success
  #   And there is a receipt-ok event with field operationStatus with value Success
  #   #
  #   Given the second IUV code of the sent RPTs
  #   When the user searches for flow steps by IUVs
  #   Then the user receives the HTTP status code 200
  #   And there is a timer-delete event with field operationStatus with value Success
  #   And there is a receipt-ok event with field operationStatus with value Success
  #   #
  #   Given the third IUV code of the sent RPTs
  #   When the user searches for flow steps by IUVs
  #   Then the user receives the HTTP status code 200
  #   And there is a timer-delete event with field operationStatus with value Success
  #   And there is a receipt-ok event with field operationStatus with value Success
  #   #
  #   Given the fourth IUV code of the sent RPTs
  #   When the user searches for flow steps by IUVs
  #   Then the user receives the HTTP status code 200
  #   And there is a timer-delete event with field operationStatus with value Success
  #   And there is a receipt-ok event with field operationStatus with value Success
  #   #
  #   Given the fifth IUV code of the sent RPTs
  #   When the user searches for flow steps by IUVs
  #   Then the user receives the HTTP status code 200
  #   And there is a timer-delete event with field operationStatus with value Success
  #   And there is a receipt-ok event with field operationStatus with value Success

  # ===============================================================================================
  # ===============================================================================================

  Scenario: Check if WISP session timers were deleted and all RTs were sent
    Given a waiting time of 10 seconds to wait for Nodo to write RE events
    And all the IUV codes of the sent RPTs
    When the user searches for flow steps by IUVs
    Then the user receives the HTTP status code 200
    Then there is a timer-delete event with field status with value RECEIPT_TIMER_GENERATION_DELETED_SCHEDULED_SEND
    And these events are related to each payment token
    Then there is a receipt-ok event with field status with value RT_SEND_SUCCESS
    And these events are related to each notice number

  # ===============================================================================================
  # ===============================================================================================

  Scenario: Execute redirect and complete payment from NodoInviaCarrelloRPT
    When the execution of "Execute NM1-to-NMU conversion in wisp-converter" was successful
    Then the execution of "Retrieve all related notice numbers from executed redirect" was successful
    And the execution of "Send a checkPosition request" was successful
    And the execution of "Send one or more activatePaymentNoticeV2 requests" was successful
    And the execution of "Check if WISP session timers were created" was successful
    And the execution of "Send a closePaymentV2 request" was successful
    And the execution of "Check if WISP session timers were deleted and all RTs were sent" was successful
    And the execution of "Check the paid payment positions" was successful