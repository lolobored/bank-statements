package org.lolobored.bankstatements.service.scrapers;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.config.Bank;
import org.openqa.selenium.WebDriver;

public interface BankGenericService {

  List<Statement> downloadStatements(WebDriver webDriver, Bank bank, String downloadDir)
      throws InterruptedException, IOException, ParseException, Exception;
}
