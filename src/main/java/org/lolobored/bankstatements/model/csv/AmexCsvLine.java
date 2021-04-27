package org.lolobored.bankstatements.model.csv;

import com.opencsv.bean.CsvBindByPosition;
import lombok.Data;

@Data
public class AmexCsvLine {
  @CsvBindByPosition(position = 0)
  private String date;
  @CsvBindByPosition(position = 1)
  private String description;
  @CsvBindByPosition(position = 2)
  private String amount;
  @CsvBindByPosition(position = 3)
  private String extendedDetails;
  @CsvBindByPosition(position = 4)
  private String appearsAs;
  @CsvBindByPosition(position = 5)
  private String address;
  @CsvBindByPosition(position = 6)
  private String city;
  @CsvBindByPosition(position = 7)
  private String postcode;
  @CsvBindByPosition(position = 8)
  private String country;
  @CsvBindByPosition(position = 9)
  private String reference;
}
