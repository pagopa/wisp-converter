Feature: Common scenarios for nodoInviaCarrelloRPT

  Scenario: Send a nodoInviaCarrelloRPT request
    Given a valid nodoInviaCarrelloRPT request
    When the user sends a nodoInviaCarrelloRPT action
    Then the user receives the HTTP status code 200 
    And the response contains the field esitoComplessivoOperazione with value OK
    And the response contains the redirect URL

  # ===============================================================================================
  # ===============================================================================================

  Scenario: Execute redirect and complete payment from NodoInviaCarrelloRPT
    When the execution of "Execute NM1-to-NMU conversion in wisp-converter" was successful
    Then the execution of "Retrieve notice number from executed redirect" was successful
    And the execution of "Send a checkPosition request" was successful
    And the execution of "Send one or more activatePaymentNoticeV2 requests" was successful
    And the execution of "Check if WISP session timer was created" was successful
    And the execution of "Send a closePaymentV2 request" was successful
    And the execution of "Check if WISP session timer was deleted and RT was sent" was successful
    And the execution of "Check the paid payment position" was successful