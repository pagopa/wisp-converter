Feature: User pays a single payment from existing payment position on nodoInviaRPT

  Background:
    Given systems up
    And a new session

  # ===============================================================================================
  # ===============================================================================================

  # scenario: crea posizione debitoria (segcode = 48) e cerca di pagare
  # scenario: crea posizione debitoria (segcode = 48) in stato invalido e cerca di pagare
  # scenario: crea posizione debitoria simulando ACA (segcode != 48) in stato draft e cerca di pagare
  # scenario: crea posizione debitoria simulando ACA (segcode != 48) in stato valid e cerca di pagare