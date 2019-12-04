package org.lolobored.bankstatements.model.csv;

import com.opencsv.bean.CsvBindByPosition;
import lombok.Data;

@Data
public class CreditMutCsvLine {
    @CsvBindByPosition(position = 0)
    private String date;
    @CsvBindByPosition(position = 1)
    private String valueDate;
    @CsvBindByPosition(position = 2)
    private String moneyOut;
    @CsvBindByPosition(position = 3)
    private String moneyIn;
    @CsvBindByPosition(position = 4)
    private String label;
    @CsvBindByPosition(position = 5)
    private String balance;

    public void setMoneyIn(String moneyIn){
        this.moneyIn= moneyIn.replace(",", ".");
    }
    public void setMoneyOut(String moneyOut){
        this.moneyOut= moneyOut.replace(",", ".");
    }
}
