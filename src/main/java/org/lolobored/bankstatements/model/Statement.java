package org.lolobored.bankstatements.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Statement {

    public static String DEBIT_ACCOUNT= "DEBIT";
    public static String CREDIT_CARD= "CREDIT CARD";

    private String accountNumber;
    private String currency;
    private String accountType;
    private List<Transaction> transactions= new ArrayList<>();

    public void addTransaction(Transaction transaction){
        transactions.add(transaction);
    }

}
