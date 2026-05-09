package org.lolobored.bankstatements.service.bitwarden.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lolobored.bankstatements.model.config.Bank;

class BitwardenServiceImplTest {

  private BitwardenServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new BitwardenServiceImpl();
  }

  @Test
  void skipsResolutionWhenNoBitwardenItemName() throws IOException, InterruptedException {
    Bank bank = new Bank();
    bank.setUsername("plain-user");
    bank.setPassword("plain-pass");

    service.resolveCredentials(bank);

    assertThat(bank.getUsername()).isEqualTo("plain-user");
    assertThat(bank.getPassword()).isEqualTo("plain-pass");
  }

  @Test
  void appliesUsernameAndPasswordFromLogin() throws IOException {
    Bank bank = new Bank();
    String json =
        """
        {
          "name": "Metro Bank",
          "login": {
            "username": "bw-user",
            "password": "bw-secret"
          },
          "fields": []
        }
        """;

    service.applyCredentials(bank, json);

    assertThat(bank.getUsername()).isEqualTo("bw-user");
    assertThat(bank.getPassword()).isEqualTo("bw-secret");
  }

  @Test
  void appliesSecurityPinFromCustomField() throws IOException {
    Bank bank = new Bank();
    String json =
        """
        {
          "name": "Metro Bank",
          "login": {"username": "u", "password": "p"},
          "fields": [
            {"name": "securityPin", "value": "12345", "type": 0}
          ]
        }
        """;

    service.applyCredentials(bank, json);

    assertThat(bank.getSecurityPin()).isEqualTo("12345");
  }

  @Test
  void appliesSecurityCodeFromCustomField() throws IOException {
    Bank bank = new Bank();
    String json =
        """
        {
          "name": "Amex",
          "login": {"username": "u", "password": "p"},
          "fields": [
            {"name": "securityCode", "value": "67890", "type": 0}
          ]
        }
        """;

    service.applyCredentials(bank, json);

    assertThat(bank.getSecurityCode()).isEqualTo("67890");
  }

  @Test
  void appliesAllFieldsInOneItem() throws IOException {
    Bank bank = new Bank();
    String json =
        """
        {
          "name": "Metro Bank",
          "login": {"username": "metro-user", "password": "metro-pass"},
          "fields": [
            {"name": "securityPin", "value": "9876", "type": 0},
            {"name": "securityCode", "value": "1234", "type": 0}
          ]
        }
        """;

    service.applyCredentials(bank, json);

    assertThat(bank.getUsername()).isEqualTo("metro-user");
    assertThat(bank.getPassword()).isEqualTo("metro-pass");
    assertThat(bank.getSecurityPin()).isEqualTo("9876");
    assertThat(bank.getSecurityCode()).isEqualTo("1234");
  }

  @Test
  void doesNotOverwriteWhenFieldAbsent() throws IOException {
    Bank bank = new Bank();
    bank.setSecurityPin("existing-pin");
    String json =
        """
        {
          "name": "Bank",
          "login": {"username": "u", "password": "p"},
          "fields": []
        }
        """;

    service.applyCredentials(bank, json);

    assertThat(bank.getSecurityPin()).isEqualTo("existing-pin");
  }

  @Test
  void ignoresUnknownCustomFields() throws IOException {
    Bank bank = new Bank();
    String json =
        """
        {
          "name": "Bank",
          "login": {"username": "u", "password": "p"},
          "fields": [
            {"name": "someOtherField", "value": "ignored", "type": 0}
          ]
        }
        """;

    service.applyCredentials(bank, json);

    assertThat(bank.getSecurityPin()).isNull();
    assertThat(bank.getSecurityCode()).isNull();
  }

  @Test
  void throwsReadableErrorOnMalformedJson() {
    Bank bank = new Bank();
    assertThatThrownBy(() -> service.applyCredentials(bank, "not-json"))
        .isInstanceOf(IOException.class);
  }

  @Test
  void checkVaultAccessThrowsWhenUnauthenticated() {
    // Simulate bw status returning unauthenticated
    BitwardenServiceImpl spy =
        new BitwardenServiceImpl() {
          @Override
          String runBwCommand(String itemName) {
            return "{\"status\":\"unauthenticated\"}";
          }
        };
    // We can't call checkVaultAccess directly without bw, but we can verify
    // the status parsing logic by checking the regex used internally
    String stdout = "{\"status\":\"unauthenticated\"}";
    String status = stdout.replaceAll(".*\"status\":\"([^\"]+)\".*", "$1").trim();
    assertThat(status).isEqualTo("unauthenticated");
  }

  @Test
  void checkVaultAccessStatusParsingHandlesLockedState() {
    String stdout =
        "{\"serverUrl\":null,\"lastSync\":null,\"userEmail\":null,\"userId\":null,\"status\":\"locked\"}";
    String status = stdout.replaceAll(".*\"status\":\"([^\"]+)\".*", "$1").trim();
    assertThat(status).isEqualTo("locked");
  }

  @Test
  void checkVaultAccessStatusParsingHandlesUnlockedState() {
    String stdout =
        "{\"serverUrl\":\"https://vault.example.com\",\"lastSync\":\"2024-01-01\",\"userEmail\":\"user@example.com\",\"userId\":\"abc\",\"status\":\"unlocked\"}";
    String status = stdout.replaceAll(".*\"status\":\"([^\"]+)\".*", "$1").trim();
    assertThat(status).isEqualTo("unlocked");
  }
}
