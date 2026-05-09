package org.lolobored.bankstatements.service.ofx;

import java.util.List;
import org.lolobored.bankstatements.model.Statement;

public interface OfxConversionService {
  String convertStatementsToOfx(List<Statement> statements);
}
