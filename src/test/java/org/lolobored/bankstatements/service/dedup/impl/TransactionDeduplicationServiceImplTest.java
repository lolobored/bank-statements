package org.lolobored.bankstatements.service.dedup.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.Transaction;
import org.lolobored.bankstatements.model.config.Account;

class TransactionDeduplicationServiceImplTest {

  private TransactionDeduplicationServiceImpl service;

  @TempDir File tempDir;

  @BeforeEach
  void setUp() {
    service = new TransactionDeduplicationServiceImpl();
  }

  // --- unit tests for the matching logic ---

  @Test
  void exactMatchIsNearDuplicate() {
    Transaction tx = tx(LocalDate.of(2026, 1, 1), "-50.00", "UBER EATS");
    var historical = historical("2026-01-01", "-50.00", "uber eats");
    assertThat(service.isNearDuplicate(tx, historical, 5, 0.85)).isTrue();
  }

  @Test
  void differentAmountIsNotDuplicate() {
    Transaction tx = tx(LocalDate.of(2026, 1, 1), "-50.00", "UBER EATS");
    var historical = historical("2026-01-01", "-49.00", "uber eats");
    assertThat(service.isNearDuplicate(tx, historical, 5, 0.85)).isFalse();
  }

  @Test
  void dateWithinToleranceIsNearDuplicate() {
    Transaction tx = tx(LocalDate.of(2026, 1, 4), "-50.00", "UBER EATS");
    var historical = historical("2026-01-01", "-50.00", "uber eats");
    assertThat(service.isNearDuplicate(tx, historical, 5, 0.85)).isTrue();
  }

  @Test
  void dateBeyondToleranceIsNotDuplicate() {
    Transaction tx = tx(LocalDate.of(2026, 1, 7), "-50.00", "UBER EATS");
    var historical = historical("2026-01-01", "-50.00", "uber eats");
    assertThat(service.isNearDuplicate(tx, historical, 5, 0.85)).isFalse();
  }

  @Test
  void descriptionSubstringIsNearDuplicate() {
    Transaction tx = tx(LocalDate.of(2026, 1, 1), "-50.00", "UBER EATS SINGAPORE PTE LTD");
    var historical = historical("2026-01-01", "-50.00", "uber eats");
    assertThat(service.isNearDuplicate(tx, historical, 5, 0.85)).isTrue();
  }

  @Test
  void similarDescriptionIsNearDuplicate() {
    Transaction tx = tx(LocalDate.of(2026, 1, 1), "-50.00", "STARBUCKS COFFEE");
    var historical = historical("2026-01-01", "-50.00", "starbucks coffe");
    assertThat(service.isNearDuplicate(tx, historical, 5, 0.85)).isTrue();
  }

  @Test
  void differentDescriptionIsNotDuplicate() {
    Transaction tx = tx(LocalDate.of(2026, 1, 1), "-50.00", "STARBUCKS");
    var historical = historical("2026-01-01", "-50.00", "mcdonalds");
    assertThat(service.isNearDuplicate(tx, historical, 5, 0.85)).isFalse();
  }

  // --- integration tests for deduplicateAndUpdate ---

  @Test
  void firstRunAddsAllToHistory() {
    Statement stmt = statement("ACC1", tx(LocalDate.of(2026, 1, 1), "-50.00", "UBER EATS"));

    List<Statement> result =
        service.deduplicateAndUpdate(List.of(stmt), tempDir, withDedup("ACC1"));

    assertThat(result.get(0).getTransactions()).hasSize(1);
    assertThat(new File(tempDir, "ACC1.json")).exists();
  }

  @Test
  void dedupDisabledPassesThroughAllTransactions() {
    // With dedup disabled: transactions pass through and no history file is written
    Statement run1 = statement("ACC1", tx(LocalDate.of(2026, 1, 1), "-50.00", "UBER EATS"));
    List<Statement> result =
        service.deduplicateAndUpdate(List.of(run1), tempDir, Collections.emptyMap());

    assertThat(result.get(0).getTransactions()).hasSize(1);
    assertThat(result.get(0).getTransactions().get(0).getLabel()).isEqualTo("UBER EATS");
    assertThat(new File(tempDir, "ACC1.json")).doesNotExist();
  }

  @Test
  void secondRunUsesOriginalDataForExactDuplicate() {
    Statement stmt1 = statement("ACC1", tx(LocalDate.of(2026, 1, 1), "-50.00", "UBER EATS"));
    service.deduplicateAndUpdate(List.of(stmt1), tempDir, withDedup("ACC1"));

    Statement stmt2 = statement("ACC1", tx(LocalDate.of(2026, 1, 1), "-50.00", "UBER EATS"));
    List<Statement> result =
        service.deduplicateAndUpdate(List.of(stmt2), tempDir, withDedup("ACC1"));

    assertThat(result.get(0).getTransactions()).hasSize(1);
    Transaction tx = result.get(0).getTransactions().get(0);
    assertThat(tx.getDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    assertThat(tx.getLabel()).isEqualTo("UBER EATS");
  }

  @Test
  void secondRunUsesOriginalDataForNearDuplicateWithShiftedDateAndChangedLabel() {
    Statement stmt1 = statement("ACC1", tx(LocalDate.of(2026, 1, 1), "-50.00", "MERCHANT"));
    service.deduplicateAndUpdate(List.of(stmt1), tempDir, withDedup("ACC1"));

    Statement stmt2 = statement("ACC1", tx(LocalDate.of(2026, 1, 3), "-50.00", "MERCHANT PTE LTD"));
    List<Statement> result =
        service.deduplicateAndUpdate(List.of(stmt2), tempDir, withDedup("ACC1"));

    assertThat(result.get(0).getTransactions()).hasSize(1);
    Transaction tx = result.get(0).getTransactions().get(0);
    // original date and label must be preserved
    assertThat(tx.getDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    assertThat(tx.getLabel()).isEqualTo("MERCHANT");
  }

  @Test
  void genuinelyNewTransactionIsKept() {
    Statement stmt1 = statement("ACC1", tx(LocalDate.of(2026, 1, 1), "-50.00", "UBER EATS"));
    service.deduplicateAndUpdate(List.of(stmt1), tempDir, Collections.emptyMap());

    Statement stmt2 = statement("ACC1", tx(LocalDate.of(2026, 1, 1), "-30.00", "GRAB"));
    List<Statement> result =
        service.deduplicateAndUpdate(List.of(stmt2), tempDir, Collections.emptyMap());

    assertThat(result.get(0).getTransactions()).hasSize(1);
  }

  @Test
  void perAccountDateToleranceIsRespected() {
    Statement stmt1 = statement("ACC1", tx(LocalDate.of(2026, 1, 1), "-50.00", "MERCHANT"));
    service.deduplicateAndUpdate(List.of(stmt1), tempDir, Collections.emptyMap());

    // Default tolerance is 5 days. With tolerance=0 a 3-day shift should NOT match.
    Account account = new Account();
    account.setDedup(true);
    account.setDateTolerance(0);
    Map<String, Account> settings = new HashMap<>();
    settings.put("ACC1", account);

    Statement stmt2 = statement("ACC1", tx(LocalDate.of(2026, 1, 4), "-50.00", "MERCHANT"));
    List<Statement> result = service.deduplicateAndUpdate(List.of(stmt2), tempDir, settings);

    assertThat(result.get(0).getTransactions()).hasSize(1);
  }

  @Test
  void tier2PicksClosestDateWhenMultipleCandidatesMatch() {
    // History has two transactions with the same label/amount but different dates
    Statement run1 =
        statement(
            "ACC1",
            tx(LocalDate.of(2026, 1, 1), "-50.00", "MERCHANT"),
            tx(LocalDate.of(2026, 1, 5), "-50.00", "MERCHANT"));
    service.deduplicateAndUpdate(List.of(run1), tempDir, withDedup("ACC1"));

    // Downloaded transaction has a date of Jan 4 — closest history entry is Jan 5 (1 day away),
    // not Jan 1 (3 days away); so the Jan 5 original should be emitted
    Statement run2 = statement("ACC1", tx(LocalDate.of(2026, 1, 4), "-50.00", "MERCHANT NEW"));
    List<Statement> result =
        service.deduplicateAndUpdate(List.of(run2), tempDir, withDedup("ACC1"));

    assertThat(result.get(0).getTransactions()).hasSize(1);
    Transaction matched = result.get(0).getTransactions().get(0);
    assertThat(matched.getDate()).isEqualTo(LocalDate.of(2026, 1, 5));
    assertThat(matched.getLabel()).isEqualTo("MERCHANT");
  }

  @Test
  void twoIdenticalTransactionsSameDayBothPreservedAcrossRuns() {
    // Two coffees on the same day in run 1
    Statement run1 =
        statement(
            "ACC1",
            tx(LocalDate.of(2026, 1, 1), "-5.00", "COFFEE SHOP"),
            tx(LocalDate.of(2026, 1, 1), "-5.00", "COFFEE SHOP"));
    service.deduplicateAndUpdate(List.of(run1), tempDir, withDedup("ACC1"));

    // Same two coffees re-downloaded in run 2
    Statement run2 =
        statement(
            "ACC1",
            tx(LocalDate.of(2026, 1, 1), "-5.00", "COFFEE SHOP"),
            tx(LocalDate.of(2026, 1, 1), "-5.00", "COFFEE SHOP"));
    List<Statement> result =
        service.deduplicateAndUpdate(List.of(run2), tempDir, withDedup("ACC1"));

    assertThat(result.get(0).getTransactions()).hasSize(2);
  }

  @Test
  void tier1ExactDateFuzzyDescriptionMatches() {
    Statement run1 = statement("ACC1", tx(LocalDate.of(2026, 1, 1), "-50.00", "MERCHANT"));
    service.deduplicateAndUpdate(List.of(run1), tempDir, withDedup("ACC1"));

    // Same date, description expanded — should match via Tier 1
    Statement run2 = statement("ACC1", tx(LocalDate.of(2026, 1, 1), "-50.00", "MERCHANT PTE LTD"));
    List<Statement> result =
        service.deduplicateAndUpdate(List.of(run2), tempDir, withDedup("ACC1"));

    assertThat(result.get(0).getTransactions()).hasSize(1);
    assertThat(result.get(0).getTransactions().get(0).getLabel()).isEqualTo("MERCHANT");
    assertThat(result.get(0).getTransactions().get(0).getDate())
        .isEqualTo(LocalDate.of(2026, 1, 1));
  }

  @Test
  void tier2FuzzyDateFuzzyDescriptionFallbackMatches() {
    Statement run1 = statement("ACC1", tx(LocalDate.of(2026, 1, 1), "-50.00", "MERCHANT"));
    service.deduplicateAndUpdate(List.of(run1), tempDir, withDedup("ACC1"));

    // Date shifted AND description changed — Tier 1 misses, Tier 2 catches it
    Statement run2 = statement("ACC1", tx(LocalDate.of(2026, 1, 4), "-50.00", "MERCHANT PTE LTD"));
    List<Statement> result =
        service.deduplicateAndUpdate(List.of(run2), tempDir, withDedup("ACC1"));

    assertThat(result.get(0).getTransactions()).hasSize(1);
    assertThat(result.get(0).getTransactions().get(0).getLabel()).isEqualTo("MERCHANT");
    assertThat(result.get(0).getTransactions().get(0).getDate())
        .isEqualTo(LocalDate.of(2026, 1, 1));
  }

  @Test
  void threeSameMerchantConsecutiveDaysAllPreservedAcrossRuns() {
    // Three visits to the same coffee shop on consecutive days
    Statement run1 =
        statement(
            "ACC1",
            tx(LocalDate.of(2026, 5, 5), "-6.90", "DIMBULAH MBFC"),
            tx(LocalDate.of(2026, 5, 6), "-6.90", "DIMBULAH MBFC"),
            tx(LocalDate.of(2026, 5, 7), "-6.90", "DIMBULAH MBFC"));
    service.deduplicateAndUpdate(List.of(run1), tempDir, withDedup("ACC1"));

    Statement run2 =
        statement(
            "ACC1",
            tx(LocalDate.of(2026, 5, 5), "-6.90", "DIMBULAH MBFC"),
            tx(LocalDate.of(2026, 5, 6), "-6.90", "DIMBULAH MBFC"),
            tx(LocalDate.of(2026, 5, 7), "-6.90", "DIMBULAH MBFC"));
    List<Statement> result =
        service.deduplicateAndUpdate(List.of(run2), tempDir, withDedup("ACC1"));

    assertThat(result.get(0).getTransactions()).hasSize(3);
  }

  @Test
  void historyIsReplacedEachRun() {
    // Run 1: transaction A
    Statement run1 = statement("ACC1", tx(LocalDate.of(2026, 1, 1), "-50.00", "MERCHANT A"));
    service.deduplicateAndUpdate(List.of(run1), tempDir, Collections.emptyMap());

    // Run 2: transaction B only (A is gone from this download period)
    Statement run2 = statement("ACC1", tx(LocalDate.of(2026, 1, 15), "-30.00", "MERCHANT B"));
    service.deduplicateAndUpdate(List.of(run2), tempDir, Collections.emptyMap());

    // Run 3: transaction A reappears — history only has run 2, so A is not a duplicate
    Statement run3 = statement("ACC1", tx(LocalDate.of(2026, 1, 1), "-50.00", "MERCHANT A"));
    List<Statement> result =
        service.deduplicateAndUpdate(List.of(run3), tempDir, Collections.emptyMap());

    assertThat(result.get(0).getTransactions()).hasSize(1);
  }

  @Test
  void exactMatchIsNotStolenByNewerFuzzyCandidate() {
    // History has May 13. CSV arrives newest-first: May 14, May 13.
    // Without two-pass, May 14 would steal the May 13 history entry via Tier 2 (1-day diff),
    // leaving the real May 13 with no match and creating two May 13 entries.
    Statement run1 = statement("ACC1", tx(LocalDate.of(2026, 5, 13), "-15.90", "PAUL MBLM"));
    service.deduplicateAndUpdate(List.of(run1), tempDir, withDedup("ACC1"));

    // Newest-first order as OCBC delivers
    Statement run2 =
        statement(
            "ACC1",
            tx(LocalDate.of(2026, 5, 14), "-15.90", "PAUL MBLM"),
            tx(LocalDate.of(2026, 5, 13), "-15.90", "PAUL MBLM"));
    List<Statement> result =
        service.deduplicateAndUpdate(List.of(run2), tempDir, withDedup("ACC1"));

    assertThat(result.get(0).getTransactions()).hasSize(2);
    // May 13 must keep its original date (exact match from history)
    assertThat(result.get(0).getTransactions().get(0).getDate())
        .isEqualTo(LocalDate.of(2026, 5, 14));
    assertThat(result.get(0).getTransactions().get(1).getDate())
        .isEqualTo(LocalDate.of(2026, 5, 13));
  }

  @Test
  void olderUnmatchedTransactionWinsFuzzyOverNewerOne() {
    // History has Jan 1. CSV arrives newest-first: Jan 4 (new), Jan 3 (new).
    // Both are unmatched in Pass 1. In Pass 2, Jan 3 (older) should win the fuzzy
    // match against history Jan 1 (2 days away); Jan 4 (3 days away) should be new.
    Statement run1 = statement("ACC1", tx(LocalDate.of(2026, 1, 1), "-50.00", "MERCHANT"));
    service.deduplicateAndUpdate(List.of(run1), tempDir, withDedup("ACC1"));

    Statement run2 =
        statement(
            "ACC1",
            tx(LocalDate.of(2026, 1, 4), "-50.00", "MERCHANT"),
            tx(LocalDate.of(2026, 1, 3), "-50.00", "MERCHANT"));
    List<Statement> result =
        service.deduplicateAndUpdate(List.of(run2), tempDir, withDedup("ACC1"));

    assertThat(result.get(0).getTransactions()).hasSize(2);
    // Jan 3 matched history Jan 1 — uses original date Jan 1
    assertThat(result.get(0).getTransactions().get(0).getDate())
        .isEqualTo(LocalDate.of(2026, 1, 4));
    assertThat(result.get(0).getTransactions().get(1).getDate())
        .isEqualTo(LocalDate.of(2026, 1, 1));
  }

  // --- normalisation ---

  @Test
  void normalizesLabel() {
    assertThat(service.normalize("  UBER  EATS!! ")).isEqualTo("uber eats");
    assertThat(service.normalize(null)).isEqualTo("");
    assertThat(service.normalize("Starbucks #42")).isEqualTo("starbucks 42");
  }

  // --- helpers ---

  private Map<String, Account> withDedup(String... accountNumbers) {
    Map<String, Account> map = new HashMap<>();
    for (String id : accountNumbers) {
      Account acc = new Account();
      acc.setDedup(true);
      map.put(id, acc);
    }
    return map;
  }

  private Transaction tx(LocalDate date, String amount, String label) {
    Transaction tx = new Transaction();
    tx.setDate(date);
    tx.setAmount(new BigDecimal(amount));
    tx.setLabel(label);
    tx.setType(Transaction.DEBIT_TYPE);
    return tx;
  }

  private org.lolobored.bankstatements.model.HistoricalTransaction historical(
      String date, String amount, String normalizedLabel) {
    return new org.lolobored.bankstatements.model.HistoricalTransaction(
        date,
        amount,
        normalizedLabel,
        "2026-01-01",
        normalizedLabel,
        Transaction.DEBIT_TYPE,
        null,
        null);
  }

  private Statement statement(String accountNumber, Transaction... txs) {
    Statement stmt = new Statement();
    stmt.setAccountNumber(accountNumber);
    stmt.setCurrency("SGD");
    stmt.setAccountType(Statement.DEBIT_ACCOUNT);
    for (Transaction tx : txs) stmt.addTransaction(tx);
    return stmt;
  }
}
