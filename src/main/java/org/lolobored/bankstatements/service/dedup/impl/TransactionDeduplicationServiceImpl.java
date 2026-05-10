package org.lolobored.bankstatements.service.dedup.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.lolobored.bankstatements.model.HistoricalTransaction;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.Transaction;
import org.lolobored.bankstatements.model.config.Account;
import org.lolobored.bankstatements.service.dedup.TransactionDeduplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TransactionDeduplicationServiceImpl implements TransactionDeduplicationService {

  static final int DEFAULT_DATE_TOLERANCE = 5;
  static final double DEFAULT_SIMILARITY_THRESHOLD = 0.85;

  private final Logger logger = LoggerFactory.getLogger(TransactionDeduplicationServiceImpl.class);
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final JaroWinklerSimilarity jaroWinkler = new JaroWinklerSimilarity();

  @Override
  public List<Statement> deduplicateAndUpdate(
      List<Statement> statements, File historyDir, Map<String, Account> accountSettings) {

    historyDir.mkdirs();
    List<Statement> result = new ArrayList<>();

    for (Statement statement : statements) {
      String accountNumber = statement.getAccountNumber();
      Account accountConfig = accountSettings.get(accountNumber);

      int dateTolerance =
          (accountConfig != null && accountConfig.getDateTolerance() != null)
              ? accountConfig.getDateTolerance()
              : DEFAULT_DATE_TOLERANCE;
      double similarityThreshold =
          (accountConfig != null && accountConfig.getDescriptionSimilarity() != null)
              ? accountConfig.getDescriptionSimilarity()
              : DEFAULT_SIMILARITY_THRESHOLD;

      List<HistoricalTransaction> previousHistory = loadHistory(historyDir, accountNumber);

      Statement deduped = new Statement();
      deduped.setCurrency(statement.getCurrency());
      deduped.setAccountType(statement.getAccountType());
      deduped.setAccountNumber(accountNumber);

      List<HistoricalTransaction> currentHistory = new ArrayList<>();
      for (Transaction tx : statement.getTransactions()) {
        HistoricalTransaction match =
            previousHistory.stream()
                .filter(h -> isNearDuplicate(tx, h, dateTolerance, similarityThreshold))
                .findFirst()
                .orElse(null);
        if (match != null) {
          logger.info(
              "Near-duplicate for account [{}]: {} {} \"{}\" — using original version",
              accountNumber,
              tx.getDate(),
              tx.getAmount(),
              tx.getLabel());
          currentHistory.add(match);
          deduped.addTransaction(toTransaction(match));
        } else {
          currentHistory.add(toHistorical(tx));
          deduped.addTransaction(tx);
        }
      }

      saveHistory(historyDir, accountNumber, currentHistory);
      result.add(deduped);
    }

    return result;
  }

  boolean isNearDuplicate(
      Transaction tx,
      HistoricalTransaction historical,
      int dateTolerance,
      double similarityThreshold) {

    if (tx.getAmount().compareTo(new BigDecimal(historical.getAmount())) != 0) return false;

    LocalDate txDate = tx.getDate();
    LocalDate histDate = LocalDate.parse(historical.getDate());
    if (Math.abs(ChronoUnit.DAYS.between(txDate, histDate)) > dateTolerance) return false;

    String normalizedNew = normalize(tx.getLabel());
    String normalizedHist = historical.getNormalizedLabel();

    if (normalizedNew.contains(normalizedHist) || normalizedHist.contains(normalizedNew))
      return true;

    return jaroWinkler.apply(normalizedNew, normalizedHist) >= similarityThreshold;
  }

  String normalize(String label) {
    if (label == null) return "";
    return label.toLowerCase().replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
  }

  private HistoricalTransaction toHistorical(Transaction tx) {
    return new HistoricalTransaction(
        tx.getDate().toString(),
        tx.getAmount().toPlainString(),
        normalize(tx.getLabel()),
        LocalDate.now().toString(),
        tx.getLabel(),
        tx.getType(),
        tx.getAdditionalInformation(),
        tx.getReference());
  }

  private Transaction toTransaction(HistoricalTransaction h) {
    Transaction tx = new Transaction();
    tx.setDate(LocalDate.parse(h.getDate()));
    tx.setAmount(new BigDecimal(h.getAmount()));
    tx.setLabel(h.getOriginalLabel() != null ? h.getOriginalLabel() : h.getNormalizedLabel());
    tx.setType(h.getType() != null ? h.getType() : Transaction.DEBIT_TYPE);
    tx.setAdditionalInformation(h.getAdditionalInformation());
    tx.setReference(h.getReference());
    return tx;
  }

  private List<HistoricalTransaction> loadHistory(File historyDir, String accountNumber) {
    File file = historyFile(historyDir, accountNumber);
    if (!file.exists()) return new ArrayList<>();
    try {
      return objectMapper.readValue(file, new TypeReference<List<HistoricalTransaction>>() {});
    } catch (IOException e) {
      logger.warn(
          "Could not read history for account [{}], starting fresh: {}",
          accountNumber,
          e.getMessage());
      return new ArrayList<>();
    }
  }

  private void saveHistory(
      File historyDir, String accountNumber, List<HistoricalTransaction> history) {
    try {
      objectMapper
          .writerWithDefaultPrettyPrinter()
          .writeValue(historyFile(historyDir, accountNumber), history);
    } catch (IOException e) {
      logger.error("Could not save history for account [{}]: {}", accountNumber, e.getMessage());
    }
  }

  private File historyFile(File historyDir, String accountNumber) {
    String safeName = accountNumber.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    return new File(historyDir, safeName + ".json");
  }
}
