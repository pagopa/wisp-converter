Feature: User pays a payment carts without stamps on nodoInviaCarrelloRPT

  Background:
    Given systems up
    And a new session

  # ===============================================================================================
  # ===============================================================================================

  @runnable @happy_path
  Scenario: User pays a cart with single RPT with one transfer via nodoInviaCarrelloRPT
    Given a cart of RPTs non-multibeneficiary
    And a single RPT of type BBT with 1 transfers of which none are stamps
    When the execution of "Send a nodoInviaCarrelloRPT request" was successful
    Then the execution of "Execute redirect and complete payment from NodoInviaCarrelloRPT" was successful

  # ===============================================================================================
  # ===============================================================================================

  @runnable @happy_path
  Scenario: User pays a cart with single RPT with two transfers via nodoInviaCarrelloRPT
    Given a cart of RPTs non-multibeneficiary
    And a single RPT of type BBT with 2 transfers of which none are stamps
    When the execution of "Send a nodoInviaCarrelloRPT request" was successful
    Then the execution of "Execute redirect and complete payment from NodoInviaCarrelloRPT" was successful

  # ===============================================================================================
  # ===============================================================================================

  @runnable @happy_path
  Scenario: User pays a cart with single RPT with three transfers via nodoInviaCarrelloRPT
    Given a cart of RPTs non-multibeneficiary
    And a single RPT of type BBT with 3 transfers of which none are stamps
    When the execution of "Send a nodoInviaCarrelloRPT request" was successful
    Then the execution of "Execute redirect and complete payment from NodoInviaCarrelloRPT" was successful

  # ===============================================================================================
  # ===============================================================================================

  @runnable @happy_path
  Scenario: User pays a cart with single RPT with four transfers via nodoInviaCarrelloRPT
    Given a cart of RPTs non-multibeneficiary
    And a single RPT of type BBT with 4 transfers of which none are stamps
    When the execution of "Send a nodoInviaCarrelloRPT request" was successful
    Then the execution of "Execute redirect and complete payment from NodoInviaCarrelloRPT" was successful

  # ===============================================================================================
  # ===============================================================================================

  @runnable @happy_path
  Scenario: User pays a cart with single RPT with five transfers via nodoInviaCarrelloRPT
    Given a cart of RPTs non-multibeneficiary
    And a single RPT of type BBT with 5 transfers of which none are stamps
    When the execution of "Send a nodoInviaCarrelloRPT request" was successful
    Then the execution of "Execute redirect and complete payment from NodoInviaCarrelloRPT" was successful

  # ===============================================================================================
  # ===============================================================================================
