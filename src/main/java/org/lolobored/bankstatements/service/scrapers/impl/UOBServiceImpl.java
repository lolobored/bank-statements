package org.lolobored.bankstatements.service.scrapers.impl;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.config.Account;
import org.lolobored.bankstatements.model.config.Bank;
import org.lolobored.bankstatements.service.UOBXLSConversionService;
import org.lolobored.bankstatements.service.scrapers.UOBService;
import org.lolobored.bankstatements.service.scrapers.pages.uob.UOBAccountsPage;
import org.lolobored.bankstatements.service.scrapers.pages.uob.UOBLoginPage;
import org.lolobored.bankstatements.service.scrapers.pages.uob.UOBTransactionsPage;
import org.lolobored.bankstatements.utils.FileUtility;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UOBServiceImpl implements UOBService {

  @Autowired private UOBXLSConversionService uobxlsConversionService;

  @Override
  public List<Statement> downloadStatements(WebDriver webDriver, Bank bank, String downloadDir)
      throws InterruptedException, IOException, ParseException {
    List<Statement> statements = new ArrayList<>();
    WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(bank.getWaitTime()));

    File downloads = new File(downloadDir);
    FileUtils.deleteDirectory(downloads);
    downloads.mkdirs();

    UOBLoginPage loginPage = new UOBLoginPage(webDriver, wait);
    loginPage.login(bank.getConnectionUrl(), bank.getUsername(), bank.getPassword());

    UOBAccountsPage accountsPage = new UOBAccountsPage(webDriver, wait);
    UOBTransactionsPage transactionsPage = new UOBTransactionsPage(webDriver, wait);

    for (Account bankAccount : bank.getAccounts()) {
      accountsPage.openAccount(bankAccount.getAccountId());

      transactionsPage.downloadCurrentMonth();
      String currentFile = FileUtility.getDownloadedFilename(downloads, bank.getWaitTime());
      Statement current =
          uobxlsConversionService.convertTableToTransactions(
              bankAccount.getAccountId(),
              Statement.DEBIT_ACCOUNT,
              downloads.getAbsolutePath() + "/" + currentFile);
      FileUtils.deleteDirectory(downloads);
      downloads.mkdirs();

      transactionsPage.downloadPreviousMonth();
      String prevFile = FileUtility.getDownloadedFilename(downloads, bank.getWaitTime());
      Statement previous =
          uobxlsConversionService.convertTableToTransactions(
              bankAccount.getAccountId(),
              Statement.DEBIT_ACCOUNT,
              downloads.getAbsolutePath() + "/" + prevFile);
      current.getTransactions().addAll(previous.getTransactions());
      statements.add(current);
      FileUtils.deleteDirectory(downloads);
      downloads.mkdirs();

      accountsPage.goBackToDashboard();
    }

    return statements;
  }
}
