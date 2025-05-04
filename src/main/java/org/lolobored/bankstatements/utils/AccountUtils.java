package org.lolobored.bankstatements.utils;

public class AccountUtils {
    public static String getCleanedAccount(String accountName){
        return accountName.replace("-","").replace(" ", "").toLowerCase();
    }
}
