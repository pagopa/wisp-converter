# Feature: User pays a payment carts without stamps on nodoInviaCarrelloRPT

#   Background:
#     Given systems up
#     And a new session

#   # ===============================================================================================
#   # ===============================================================================================

#     @runnable @happy_path @new_scenario
#     Scenario: User pays a cart with single RPT with nodoInviaCarrelloRPT
#       Given a cart of RPTs
#       And a single RPT of type BBT with 1 transfers of which none are stamps
#       When the execution of "Send a nodoInviaCarrelloRPT request" was successful
#       Then the execution of "Execute redirect and complete payment from NodoInviaCarrelloRPT" was successful