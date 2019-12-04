package org.lolobored.bankstatements.service.filter;

import org.lolobored.bankstatements.model.Statement;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface StatementsFilterService {
    List<Statement> filterStatements(List<Statement> statements, LocalDate startingDate, Map<String, String> accountReplacements);
}
