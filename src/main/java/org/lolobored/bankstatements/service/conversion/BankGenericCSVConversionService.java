package org.lolobored.bankstatements.service.conversion;

import org.lolobored.bankstatements.model.Statement;

import java.text.ParseException;

public interface BankGenericCSVConversionService {
    Statement convertCSVToTransactions(String accountNumber, String accountType, String csv) throws ParseException;
}
