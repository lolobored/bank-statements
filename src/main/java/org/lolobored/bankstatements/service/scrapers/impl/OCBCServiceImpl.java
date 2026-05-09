package org.lolobored.bankstatements.service.scrapers.impl;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.config.Account;
import org.lolobored.bankstatements.model.config.Bank;
import org.lolobored.bankstatements.service.conversion.OCBCCSVConversionService;
import org.lolobored.bankstatements.service.conversion.OCBCCreditCSVConversionService;
import org.lolobored.bankstatements.service.scrapers.OCBCService;
import org.lolobored.bankstatements.service.scrapers.pages.ocbc.OCBCAccountsOverviewPage;
import org.lolobored.bankstatements.service.scrapers.pages.ocbc.OCBCCreditTransactionsPage;
import org.lolobored.bankstatements.service.scrapers.pages.ocbc.OCBCDebitTransactionsPage;
import org.lolobored.bankstatements.service.scrapers.pages.ocbc.OCBCLoginPage;
import org.lolobored.bankstatements.utils.FileUtility;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OCBCServiceImpl implements OCBCService {

  @Autowired private OCBCCSVConversionService ocbccsvConversionService;
  @Autowired private OCBCCreditCSVConversionService ocbcCreditCSVConversionService;

  @Override
  public List<Statement> downloadStatements(WebDriver webDriver, Bank bank, String downloadDir)
      throws Exception {
    List<Statement> statements = new ArrayList<>();
    WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(bank.getWaitTime()));
    File downloads = new File(downloadDir);

    OCBCLoginPage loginPage = new OCBCLoginPage(webDriver, wait);
    loginPage.login(bank.getConnectionUrl(), bank.getUsername(), bank.getPassword());

    OCBCAccountsOverviewPage overviewPage = new OCBCAccountsOverviewPage(webDriver, wait);
    OCBCDebitTransactionsPage debitPage = new OCBCDebitTransactionsPage(webDriver, wait);
    OCBCCreditTransactionsPage creditPage = new OCBCCreditTransactionsPage(webDriver, wait);

    for (Account account : bank.getAccounts()) {
      if (Account.DEBIT.equals(account.getType())) {
        FileUtils.deleteDirectory(downloads);
        downloads.mkdirs();

        overviewPage.openDebitAccount(account.getAccountName());
        debitPage.downloadCsv();
        String csv = FileUtility.readDownloadedFile(downloads, bank.getWaitTime());
        statements.add(
            ocbccsvConversionService.convertTableToTransactions(
                account.getAccountId(), Statement.DEBIT_ACCOUNT, csv));
      }
    }

    for (Account account : bank.getAccounts()) {
      if (Account.CREDIT.equals(account.getType())) {
        FileUtils.deleteDirectory(downloads);
        downloads.mkdirs();

        overviewPage.scrollToTop();
        overviewPage.navigateToOverview();
        overviewPage.openCreditAccount(account.getAccountName());
        creditPage.downloadCsv();
        String csv = FileUtility.readDownloadedFile(downloads, bank.getWaitTime());
        statements.add(
            ocbcCreditCSVConversionService.convertTableToTransactions(
                account.getAccountId(), Statement.CREDIT_CARD, csv));
      }
    }

    return statements;
  }
}
