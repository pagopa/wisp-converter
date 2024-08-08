Feature: User pays a single payment from existing payment position on nodoInviaRPT

  Background:
    Given systems up
    And a new session

  # ===============================================================================================
  # ===============================================================================================

  #@runnable @happy_path
  #Scenario: User pays a single payment with single transfer and no stamp on nodoInviaRPT that exists already in GPD
    # Given an existing payment position with segregation code equals to 48, state equals to VALID, one simple transfer and no stamp
    # And a single RPT of type BBT with 1 transfers of which none are stamps related to existing payment position
    # When the user sends a nodoInviaRPT action
    # Then the user receives the HTTP status code 200 
    # And the response contains the field esito with value OK
    # And the response contains the redirect URL
    # And the execution of "Execute redirect and complete payment from NodoInviaRPT" was successful

  #@runnable @happy_path
  #Scenario: User pays a single payment with no transfer and one stamp on nodoInviaRPT that exists already in GPD

  #@runnable @happy_path
  #Scenario: User pays a single payment with single transfer and one stamp on nodoInviaRPT that exists already in GPD

  #@runnable @unhappy_path
  #Scenario: User tries to pay a single payment with single transfer and no stamp on nodoInviaRPT that exists already in GPD in invalid state

  #@runnable @unhappy_path
  #Scenario: User tries to pay a single payment on nodoInviaRPT that was inserted from ACA and is in valid state

  #@runnable @unhappy_path
  #Scenario: User tries to pay a single payment on nodoInviaRPT that was inserted from ACA and is in invalid state
  
  