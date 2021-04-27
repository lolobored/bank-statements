package org.lolobored.bankstatements.service.filter.impl;

import org.apache.commons.lang3.StringUtils;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.Transaction;
import org.lolobored.bankstatements.service.filter.StatementsFilterService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class StatementsFilterServiceImpl implements StatementsFilterService {
  @Override
  public List<Statement> filterStatements(List<Statement> currentStatements, LocalDate startingDate, Map<String, String> accountReplacements) {
    List<Statement> statements = new ArrayList<>();

    for (Statement currentStatement : currentStatements) {
      Statement statement = new Statement();
      statement.setCurrency(currentStatement.getCurrency());
      statement.setAccountType(currentStatement.getAccountType());
      String accountNumber = accountReplacements.get(currentStatement.getAccountNumber());
      statement.setAccountNumber(currentStatement.getAccountNumber());
      if (StringUtils.isNotEmpty(accountNumber)) {
        statement.setAccountNumber(statement.getAccountNumber() + "-" + accountNumber);
      }

      for (Transaction currentTransaction : currentStatement.getTransactions()) {
        LocalDate currentTransactionDate = new java.sql.Date(currentTransaction.getDate().getTime()).toLocalDate();
        if (currentTransactionDate.isAfter(startingDate) ||
                currentTransactionDate.isEqual(startingDate)) {
          statement.addTransaction(currentTransaction);
        }
      }
      statements.add(statement);
    }
    return statements;
  }
}
