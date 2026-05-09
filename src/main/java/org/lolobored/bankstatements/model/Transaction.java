package org.lolobored.bankstatements.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class Transaction {

  public static String CREDIT_TYPE = "CREDIT";
  public static String DEBIT_TYPE = "DEBIT";
  public static String XFER_TYPE = "XFER";

  private String reference;
  private String type = DEBIT_TYPE;
  private LocalDate date;
  private BigDecimal amount;
  private String label;
  private String additionalInformation;
}
