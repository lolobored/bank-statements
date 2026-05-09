package org.lolobored.bankstatements.service.scrapers.pages.commbank;

public class CommBankAccountInfo {

  private final String accountNumber;
  private final String url;

  public CommBankAccountInfo(String accountNumber, String url) {
    this.accountNumber = accountNumber;
    this.url = url;
  }

  public String getAccountNumber() {
    return accountNumber;
  }

  public String getUrl() {
    return url;
  }
}
