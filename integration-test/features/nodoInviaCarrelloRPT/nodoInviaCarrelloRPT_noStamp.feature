Feature: User pays a payment carts without stamps on nodoInviaCarrelloRPT

  Background:
    Given systems up
    And a new session

  # ===============================================================================================
  # ===============================================================================================

#   @runnable @happy_path
#   Scenario: User pays a single payment with single transfer and no stamp on nodoInviaRPT
#     Given a cart of RPTs
#     And a single RPT of type BBT with 1 transfers of which none are stamps included in cart
#     When the user sends a nodoInviaCarrelloRPT action
#     Then the user receives the HTTP status code 200 
#     And the response contains the field esito with value OK
#     And the response contains the redirect URL
#     And the execution of "Execute redirect and complete payment from NodoInviaCarrelloRPT" was successful