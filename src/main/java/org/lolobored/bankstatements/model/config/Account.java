package org.lolobored.bankstatements.model.config;

import lombok.Data;
import org.lolobored.bankstatements.utils.AccountUtils;

@Data
public class Account {
  public static String DEBIT = "DEBIT";
  public static String CREDIT = "CREDIT";

  private String accountName;
  private String accountId;
  private String type;
  private String banktivitySuffix;
  // ISO 4217 currency code — overrides the hardcoded default in the conversion service
  private String currency;
  // set to true to enable fuzzy duplicate detection; disabled by default
  private Boolean dedup;
  // fuzzy dedup: max days a re-downloaded transaction's date may differ (default: 5)
  private Integer dateTolerance;
  // fuzzy dedup: minimum Jaro-Winkler similarity for label match (default: 0.85)
  private Double descriptionSimilarity;

  private void setAccountName(String accountName) {
    this.accountName = AccountUtils.getCleanedAccount(accountName);
  }
}
