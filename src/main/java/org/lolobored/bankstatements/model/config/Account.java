package org.lolobored.bankstatements.model.config;

import lombok.Data;

@Data
public class Account {
  public static String DEBIT="DEBIT";
  public static String CREDIT="CREDIT";

  private String accountId;
  private String type;
  private String banktivitySuffix;
}
