package org.lolobored.bankstatements.service.dedup;

import java.io.File;
import java.util.List;
import java.util.Map;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.config.Account;

public interface TransactionDeduplicationService {

  /**
   * Compares each statement's transactions against a persisted history, drops near-duplicates, and
   * updates the history with newly-seen transactions.
   *
   * @param statements filtered statements from this run
   * @param historyDir directory where per-account history JSON files are stored
   * @param accountSettings map of post-filter account number → Account config (for per-account
   *     tolerances)
   */
  List<Statement> deduplicateAndUpdate(
      List<Statement> statements, File historyDir, Map<String, Account> accountSettings);
}
