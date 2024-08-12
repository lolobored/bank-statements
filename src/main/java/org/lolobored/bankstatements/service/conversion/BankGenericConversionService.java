package org.lolobored.bankstatements.service.conversion;

import org.lolobored.bankstatements.model.Statement;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;

public interface BankGenericConversionService {
  Statement convertTableToTransactions(String accountNumber, String accountType, String csvContentOrPath) throws ParseException, IOException;
}
