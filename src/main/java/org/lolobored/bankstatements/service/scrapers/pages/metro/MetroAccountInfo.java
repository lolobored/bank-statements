package org.lolobored.bankstatements.service.scrapers.pages.metro;

public class MetroAccountInfo {

  private final String accountNumber;
  private final String accountType;
  private final String linkId;

  public MetroAccountInfo(String accountNumber, String accountType, String linkId) {
    this.accountNumber = accountNumber;
    this.accountType = accountType;
    this.linkId = linkId;
  }

  public String getAccountNumber() {
    return accountNumber;
  }

  public String getAccountType() {
    return accountType;
  }

  public String getLinkId() {
    return linkId;
  }
}
