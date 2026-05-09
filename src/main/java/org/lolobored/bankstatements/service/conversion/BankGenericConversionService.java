package org.lolobored.bankstatements.service.conversion;

import java.io.IOException;
import java.text.ParseException;
import org.lolobored.bankstatements.model.Statement;

public interface BankGenericConversionService {
  Statement convertTableToTransactions(
      String accountNumber, String accountType, String csvContentOrPath)
      throws ParseException, IOException;
}
