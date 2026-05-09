package org.lolobored.bankstatements.service.bitwarden.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.lolobored.bankstatements.model.config.Bank;
import org.lolobored.bankstatements.service.bitwarden.BitwardenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class BitwardenServiceImpl implements BitwardenService {

  private static final Logger logger = LoggerFactory.getLogger(BitwardenServiceImpl.class);

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void checkVaultAccess() throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder("bw", "status");
    Process process = pb.start();
    String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    process.waitFor();

    String status = stdout.contains("\"status\"") ?
      stdout.replaceAll(".*\"status\":\"([^\"]+)\".*", "$1").trim() : "unknown";

    switch (status) {
      case "unauthenticated" -> throw new RuntimeException(
        "You are not logged in to Bitwarden. Please run 'bw login' in your terminal first."
      );
      case "locked" -> throw new RuntimeException(
        "Your Bitwarden vault is locked. Please run 'export BW_SESSION=$(bw unlock --raw)' before starting the application."
      );
      case "unlocked" -> logger.info("Bitwarden vault is unlocked.");
      default -> logger.warn("Could not determine Bitwarden vault status — proceeding anyway.");
    }
  }

  @Override
  public void resolveCredentials(Bank bank) throws IOException, InterruptedException {
    if (StringUtils.isEmpty(bank.getBitwardenItemName())) {
      return;
    }
    logger.info("Fetching credentials for [{}] from Bitwarden item [{}]", bank.getName(), bank.getBitwardenItemName());
    String json = runBwCommand(bank.getBitwardenItemName());
    applyCredentials(bank, json);
  }

  String runBwCommand(String itemName) throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder("bw", "get", "item", itemName);
    Process process = pb.start();
    String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new RuntimeException(
        "Bitwarden CLI failed for item [" + itemName + "]: " + stderr.trim() +
        ". Ensure 'bw' is installed and the vault is unlocked (BW_SESSION env var must be set)."
      );
    }
    return stdout;
  }

  void applyCredentials(Bank bank, String json) throws IOException {
    JsonNode root = objectMapper.readTree(json);

    JsonNode login = root.get("login");
    if (login != null) {
      JsonNode username = login.get("username");
      if (username != null && !username.isNull()) {
        bank.setUsername(username.asText());
      }
      JsonNode password = login.get("password");
      if (password != null && !password.isNull()) {
        bank.setPassword(password.asText());
      }
    }

    JsonNode fields = root.get("fields");
    if (fields != null && fields.isArray()) {
      for (JsonNode field : fields) {
        String name = field.path("name").asText();
        String value = field.path("value").asText();
        switch (name) {
          case "securityPin"  -> bank.setSecurityPin(value);
          case "securityCode" -> bank.setSecurityCode(value);
        }
      }
    }
  }
}
