package org.lolobored.bankstatements.service.ofx;

import org.lolobored.bankstatements.model.Statement;

import java.util.List;

public interface OfxConversionService {
  String convertStatementsToOfx(List<Statement> statements);
}
