package org.lolobored.bankstatements.model.csv;

import com.opencsv.bean.CsvBindByPosition;
import lombok.Data;

@Data
public class CommBankCsvLine {
    @CsvBindByPosition(position = 0)
    private String date;
    @CsvBindByPosition(position = 1)
    private String amount;
    @CsvBindByPosition(position = 2)
    private String label;
    @CsvBindByPosition(position = 3)
    private String balance;
}
