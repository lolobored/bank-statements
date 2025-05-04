package org.lolobored.bankstatements.model.config;

import lombok.Data;
import org.lolobored.bankstatements.utils.AccountUtils;

@Data
public class Account {
  public static String DEBIT="DEBIT";
  public static String CREDIT="CREDIT";

  private String accountName;
  private String accountId;
  private String type;
  private String banktivitySuffix;

  private void setAccountName(String accountName){
    this.accountName= AccountUtils.getCleanedAccount(accountName);
  }
}
