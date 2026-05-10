package org.lolobored.bankstatements.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalTransaction {
  private String date;
  private String amount;
  private String normalizedLabel;
  private String firstSeen;
  // original fields preserved so we can replay the exact same transaction on future runs
  private String originalLabel;
  private String type;
  private String additionalInformation;
  private String reference;
}
