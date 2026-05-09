package org.lolobored.bankstatements.service.scrapers.impl;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.config.Bank;
import org.lolobored.bankstatements.service.conversion.CommBankCSVConversionService;
import org.lolobored.bankstatements.service.scrapers.CommBankService;
import org.lolobored.bankstatements.service.scrapers.pages.commbank.CommBankAccountInfo;
import org.lolobored.bankstatements.service.scrapers.pages.commbank.CommBankAccountsPage;
import org.lolobored.bankstatements.service.scrapers.pages.commbank.CommBankLoginPage;
import org.lolobored.bankstatements.service.scrapers.pages.commbank.CommBankTransactionsPage;
import org.lolobored.bankstatements.utils.FileUtility;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CommBankServiceImpl implements CommBankService {

  @Autowired private CommBankCSVConversionService commBankCSVConversionService;

  @Override
  public List<Statement> downloadStatements(WebDriver webDriver, Bank bank, String downloadDir)
      throws InterruptedException, IOException, ParseException {
    List<Statement> statements = new ArrayList<>();
    WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(bank.getWaitTime()));

    File downloads = new File(downloadDir);
    FileUtils.deleteDirectory(downloads);
    downloads.mkdirs();

    CommBankLoginPage loginPage = new CommBankLoginPage(webDriver, wait);
    loginPage.login(bank.getConnectionUrl(), bank.getUsername(), bank.getPassword());

    CommBankAccountsPage accountsPage = new CommBankAccountsPage(webDriver);
    List<CommBankAccountInfo> accounts = accountsPage.collectAccounts();

    CommBankTransactionsPage transactionsPage = new CommBankTransactionsPage(webDriver, wait);
    for (CommBankAccountInfo account : accounts) {
      transactionsPage.navigateTo(account.getUrl());
      transactionsPage.downloadCsv();
      String csv = FileUtility.readDownloadedFile(downloads, bank.getWaitTime());
      statements.add(
          commBankCSVConversionService.convertTableToTransactions(
              account.getAccountNumber(), Statement.DEBIT_ACCOUNT, csv));
    }

    return statements;
  }
}
