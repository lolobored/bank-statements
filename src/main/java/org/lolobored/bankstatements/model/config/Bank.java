package org.lolobored.bankstatements.model.config;

import lombok.Data;

import java.util.List;

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
  private int waitTime;
  // for manually downloaded revolut statements
  private String statementsDirectory;
  private List<Account> accounts;
  private Integer multiplier=1;
}
