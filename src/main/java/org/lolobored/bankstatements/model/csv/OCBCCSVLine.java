package org.lolobored.bankstatements.model.csv;

import com.opencsv.bean.CsvBindByPosition;
import lombok.Data;

@Data
public class OCBCCSVLine {
    @CsvBindByPosition(position = 0)
    private String date;
    @CsvBindByPosition(position = 1)
    private String valueDate;
    @CsvBindByPosition(position = 2)
    private String description;
    @CsvBindByPosition(position = 3)
    private String debitAmount;
    @CsvBindByPosition(position = 4)
    private String creditAmount;

}
