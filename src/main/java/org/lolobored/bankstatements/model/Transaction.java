package org.lolobored.bankstatements.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Transaction {
  private String reference;
  private Date date;
  private BigDecimal amount;
  private String label;
  private String additionalInformation;
}
