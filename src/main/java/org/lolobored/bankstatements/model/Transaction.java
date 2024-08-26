package org.lolobored.bankstatements.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Transaction {

  public static String CREDIT_TYPE="CREDIT";
  public static String DEBIT_TYPE="DEBIT";
  public static String XFER_TYPE="XFER";

  private String reference;
  private String type=DEBIT_TYPE;
  private Date date;
  private BigDecimal amount;
  private String label;
  private String additionalInformation;
}
