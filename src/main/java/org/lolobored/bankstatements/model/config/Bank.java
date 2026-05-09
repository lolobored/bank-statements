package org.lolobored.bankstatements.model.config;

import java.util.List;
import lombok.Data;

@Data
public class Bank {
  private String name;
  private String username;
  private String password;
  // used only in the case of Amex
  private String securityCode;
  // used only in the case of Metro
  private String securityPin;
  private String connectionUrl;
  private int waitTime = 60;
  // for manually downloaded revolut statements
  private String statementsDirectory;
  private List<Account> accounts;
  private Integer multiplier = 1;
  // if set, credentials are fetched from Bitwarden instead of the fields above
  private String bitwardenItemName;
  private boolean enabled = true;
}
