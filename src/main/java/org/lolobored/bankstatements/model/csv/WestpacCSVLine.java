package org.lolobored.bankstatements.model.csv;

import com.opencsv.bean.CsvBindByPosition;
import lombok.Data;

@Data
public class WestpacCSVLine {
    @CsvBindByPosition(position = 0)
    private String bankAccount;
    @CsvBindByPosition(position = 1)
    private String date;
    @CsvBindByPosition(position = 2)
    private String narrative;
    @CsvBindByPosition(position = 3)
    private String debitAmount;
    @CsvBindByPosition(position = 4)
    private String creditAmount;
    @CsvBindByPosition(position = 5)
    private String balance;
    @CsvBindByPosition(position = 6)
    private String categories;
    @CsvBindByPosition(position = 7)
    private String serial;

}
