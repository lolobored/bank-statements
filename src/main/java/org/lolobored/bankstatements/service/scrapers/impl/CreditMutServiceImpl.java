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
import org.lolobored.bankstatements.service.conversion.CreditMutCSVConversionService;
import org.lolobored.bankstatements.service.scrapers.CreditMutService;
import org.lolobored.bankstatements.service.scrapers.pages.creditmut.CreditMutDownloadPage;
import org.lolobored.bankstatements.service.scrapers.pages.creditmut.CreditMutLoginPage;
import org.lolobored.bankstatements.utils.FileUtility;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CreditMutServiceImpl implements CreditMutService {

  @Autowired private CreditMutCSVConversionService creditMutCSVConversionService;

  @Override
  public List<Statement> downloadStatements(WebDriver webDriver, Bank bank, String downloadDir)
      throws IOException, InterruptedException, ParseException {
    List<Statement> statements = new ArrayList<>();
    WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(bank.getWaitTime()));

    File downloads = new File(downloadDir);
    FileUtils.deleteDirectory(downloads);
    downloads.mkdirs();

    CreditMutLoginPage loginPage = new CreditMutLoginPage(webDriver, wait);
    loginPage.login(bank.getConnectionUrl(), bank.getUsername(), bank.getPassword());

    CreditMutDownloadPage downloadPage = new CreditMutDownloadPage(webDriver, wait);
    downloadPage.navigateToDownload();
    downloadPage.selectCsvFormat();

    int i = 0;
    WebElement previousCheckbox = null;
    while (true) {
      WebElement checkbox = downloadPage.findAccountCheckbox(i);
      if (checkbox == null) break;

      if (previousCheckbox != null) previousCheckbox.click();
      checkbox.click();
      downloadPage.clickDownload();

      String filename = FileUtility.getDownloadedFilename(downloads, bank.getWaitTime());
      String accountNumber = filename.replace(".csv", "");
      String csv = FileUtility.readDownloadedFile(downloads, bank.getWaitTime());
      statements.add(
          creditMutCSVConversionService.convertTableToTransactions(
              accountNumber, Statement.DEBIT_ACCOUNT, csv));

      previousCheckbox = checkbox;
      i++;
    }

    return statements;
  }
}
