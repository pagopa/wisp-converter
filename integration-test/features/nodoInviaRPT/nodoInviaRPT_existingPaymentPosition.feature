Feature: User pays a single payment from existing payment position on nodoInviaRPT

  Background:
    Given systems up
    And a new session

  # ===============================================================================================
  # ===============================================================================================

  @runnable @happy_path
  Scenario: User pays a single payment with single transfer and no stamp on nodoInviaRPT that exists already in GPD
    Given a single RPT of type BBT with 1 transfers of which 0 are stamps
    And an existing payment position related to first RPT with segregation code equals to 48 and state equals to VALID
    When the execution of "Send a nodoInviaRPT request" was successful
    Then the execution of "Execute redirect and complete payment from NodoInviaRPT" was successful
    And the execution of "Check if existing debt position was used" was successful

  @runnable @happy_path
  Scenario: User pays a single payment with no transfer and one stamp on nodoInviaRPT that exists already in GPD
    Given a single RPT of type BBT with 1 transfers of which 1 are stamps
    And an existing payment position related to first RPT with segregation code equals to 48 and state equals to VALID
    When the execution of "Send a nodoInviaRPT request" was successful
    Then the execution of "Execute redirect and complete payment from NodoInviaRPT" was successful
    And the execution of "Check if existing debt position was used" was successful

  @runnable @happy_path
  Scenario: User pays a single payment with single transfer and one stamp on nodoInviaRPT that exists already in GPD
    Given a single RPT of type BBT with 2 transfers of which 1 are stamps
    And an existing payment position related to first RPT with segregation code equals to 48 and state equals to VALID
    When the execution of "Send a nodoInviaRPT request" was successful
    Then the execution of "Execute redirect and complete payment from NodoInviaRPT" was successful
    And the execution of "Check if existing debt position was used" was successful

  @runnable @unhappy_path
  Scenario: User tries to pay a single payment with single transfer and no stamp on nodoInviaRPT that exists already in GPD in invalid state
    Given a single RPT of type BBT with 1 transfers of which 0 are stamps
    And an existing payment position related to first RPT with segregation code equals to 48 and state equals to VALID
    When the execution of "Send a nodoInviaRPT request" was successful
    Then the execution of "Fails on execute NM1-to-NMU conversion in wisp-converter" was successful
    And the execution of "Check if existing debt position was used" was successful

  #@runnable @unhappy_path
  #Scenario: User tries to pay a single payment on nodoInviaRPT that was inserted from ACA and is in valid state

  #@runnable @unhappy_path
  #Scenario: User tries to pay a single payment on nodoInviaRPT that was inserted from ACA and is in invalid state
  
  