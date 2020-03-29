package org.lolobored.bankstatements.service.scrapers.impl;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.lolobored.bankstatements.model.config.Bank;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.service.conversion.RevolutCSVConversionService;
import org.lolobored.bankstatements.service.scrapers.RevolutService;
import org.lolobored.bankstatements.utils.FileUtility;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class RevolutServiceImpl implements RevolutService {

    @Autowired
    private RevolutCSVConversionService revolutCSVConversionService;

    @Override
    public List<Statement> downloadStatements(WebDriver webDriver, Bank bank, String downloadDir) throws InterruptedException, IOException, ParseException {

        /**
         * Delete the download directory
         */
        File downloads= new File(downloadDir);
        FileUtils.deleteDirectory(downloads);
        downloads.mkdirs();

        List<Statement> statements= new ArrayList<>();
        String statementsDirectoryPath = bank.getStatementsDirectory();
        File statementsDirectory= new File(statementsDirectoryPath);
        File tempDirectory = new File(downloadDir);

        if (statementsDirectory.exists()){

            // list the files
            Collection<File> statementFiles = FileUtils.listFiles(statementsDirectory, new String[]{"csv"}, false);
            for (File statement : statementFiles) {
                if (statement.getName().startsWith("Revolut-")) {
                    FileUtils.moveFileToDirectory(statement, tempDirectory, true);
                    String fileName = FileUtility.getDownloadedFilename(tempDirectory, bank.getWaitTime());
                    String csv = FileUtility.readDownloadedFile(tempDirectory, bank.getWaitTime());
                    String accountName = StringUtils.substringBefore(fileName, "-Statement").toLowerCase();
                    statements.add(revolutCSVConversionService.convertCSVToTransactions(accountName, Statement.DEBIT_ACCOUNT, csv));
                }
            }
        }
        return statements;
    }
}
