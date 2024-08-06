Feature: User pays a single payment without stamps on nodoInviaRPT

  Background:
    Given systems up
    And a new session

  # ===============================================================================================
  # ===============================================================================================

  @runnable @happypath
  Scenario: User pays a single payment with single transfer and no stamp on nodoInviaRPT
    Given a single RPT with 1 transfers of which none are stamps
    When the user sends a nodoInviaRPT action
    Then the user receives the HTTP status code 200 
    And the response contains the field esito with value OK
    And the response contains the redirect URL
    And the execution of "Execute redirect and complete payment from NodoInviaRPT" was successful

  # ===============================================================================================
  # ===============================================================================================

  @runnable @happypath
  Scenario: User pays a single payment with two transfers and no stamp on nodoInviaRPT
    Given a single RPT with 2 transfers of which none are stamps
    When the user sends a nodoInviaRPT action
    Then the user receives the HTTP status code 200 
    And the response contains the field esito with value OK
    And the response contains the redirect URL
    And the execution of "Execute redirect and complete payment from NodoInviaRPT" was successful

  # ===============================================================================================
  # ===============================================================================================

  @runnable @happypath
  Scenario: User pays a single payment with three transfers and no stamp on nodoInviaRPT
    Given a single RPT with 3 transfers of which none are stamps
    When the user sends a nodoInviaRPT action
    Then the user receives the HTTP status code 200 
    And the response contains the field esito with value OK
    And the response contains the redirect URL
    And the execution of "Execute redirect and complete payment from NodoInviaRPT" was successful

  # ===============================================================================================
  # ===============================================================================================

  @runnable @happypath
  Scenario: User pays a single payment with four transfers and no stamp on nodoInviaRPT
    Given a single RPT with 4 transfers of which none are stamps
    When the user sends a nodoInviaRPT action
    Then the user receives the HTTP status code 200 
    And the response contains the field esito with value OK
    And the response contains the redirect URL
    And the execution of "Execute redirect and complete payment from NodoInviaRPT" was successful

  # ===============================================================================================
  # ===============================================================================================

  @runnable @happypath
  Scenario: User pays a single payment with five transfers and no stamp on nodoInviaRPT
    Given a single RPT with 5 transfers of which none are stamps
    When the user sends a nodoInviaRPT action
    Then the user receives the HTTP status code 200 
    And the response contains the field esito with value OK
    And the response contains the redirect URL
    And the execution of "Execute redirect and complete payment from NodoInviaRPT" was successful