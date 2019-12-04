package org.lolobored.bankstatements.model.csv;

import com.opencsv.bean.CsvBindByPosition;
import lombok.Data;

@Data
public class RevolutCSVLine {
    @CsvBindByPosition(position = 0)
    private String date;
    @CsvBindByPosition(position = 1)
    private String reference;
    @CsvBindByPosition(position = 2)
    private String amountOut;
    @CsvBindByPosition(position = 3)
    private String amountIn;
    @CsvBindByPosition(position = 4)
    private String exchangeOut;
    @CsvBindByPosition(position = 5)
    private String exchangeIn;
    @CsvBindByPosition(position = 6)
    private String balance;
    @CsvBindByPosition(position = 7)
    private String category;
}
