package org.lolobored.bankstatements.service.ofx.impl;

import org.apache.commons.lang3.StringUtils;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.Transaction;
import org.lolobored.bankstatements.service.ofx.OfxConversionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Service
public class OfxConversionServiceImpl implements OfxConversionService {
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

    @Override
    public String convertStatementsToOfx(List<Statement> statements) {


        StringBuilder ofx = new StringBuilder();
        ofx.append(getHeader());

        // first get statements per credit or debit
        List<Statement> debitStatements = new ArrayList<>();
        List<Statement> creditStatements = new ArrayList<>();
        for (Statement statement : statements) {
            if (statement.getAccountType().equals(Statement.DEBIT_ACCOUNT)){
                debitStatements.add(statement);
            }
            else{
                creditStatements.add(statement);
            }
        }

        if (!debitStatements.isEmpty()) {
            ofx.append(getDebitHeader());
            for (Statement statement : debitStatements) {
                if (!statement.getTransactions().isEmpty()) {
                    ofx.append(getStatementDebitHeader(statement.getAccountNumber(), statement.getCurrency()));
                    generateTransactionsEntries(ofx, statement);
                    ofx.append(getStatementDebitTrailer());
                }
            }
        }

        if (!creditStatements.isEmpty()) {
            ofx.append(getCreditHeader());
            for (Statement statement : creditStatements) {
                if (!statement.getTransactions().isEmpty()) {
                    ofx.append(getStatementCreditHeader(statement.getAccountNumber(), statement.getCurrency()));
                    generateTransactionsEntries(ofx, statement);
                    ofx.append(getStatementCreditTrailer());
                }
            }
        }
        ofx.append(getTrailer());
        return ofx.toString();
    }

    private void generateTransactionsEntries(StringBuilder ofx, Statement statement) {
        for (Transaction transaction : statement.getTransactions()) {
            ofx.append("\n" +
                    "\t\t\t\t<STMTTRN>\n" +
                    "\t\t\t\t\t<TRNTYPE>");
            if (transaction.getAmount().compareTo(BigDecimal.ZERO) >= 0) {
                ofx.append("CREDIT").append("\n");
            } else {
                ofx.append("DEBIT").append("\n");
            }
            ofx.append("\t\t\t\t\t<DTPOSTED>").append(dateFormat.format(transaction.getDate())).append("\n");
            if (StringUtils.isNotEmpty(transaction.getReference())) {
                ofx.append("\t\t\t\t\t<FTID>").append(transaction.getReference()).append("\n");
                ofx.append("\t\t\t\t\t<REFNUM>").append(transaction.getReference()).append("\n");
            }
            ofx.append("\t\t\t\t\t<TRNAMT>").append(transaction.getAmount()).append("\n");
            ofx.append("\t\t\t\t\t<NAME>").append(transaction.getLabel()).append("\n");
            if (StringUtils.isNotEmpty(transaction.getAdditionalInformation())) {
                ofx.append("\t\t\t\t\t<MEMO>").append(transaction.getAdditionalInformation()).append("\n");
            }
            ofx.append("\t\t\t\t</STMTTRN>");
        }
    }

    private String getTrailer() {
        return "</OFX>";
    }
    
    private String getHeader() {

        StringBuilder header = new StringBuilder();
        header.append("OFXHEADER:100\n" +
                "DATA:OFXSGML\n" +
                "VERSION:102\n" +
                "SECURITY:NONE\n" +
                "ENCODING:USASCII\n" +
                "CHARSET:1252\n" +
                "COMPRESSION:NONE\n" +
                "OLDFILEUID:NONE\n" +
                "NEWFILEUID:NONE\n" +
                "<OFX>\n");
        return header.toString();
    }

    private String getDebitHeader(){
        return "<SIGNONMSGSRSV1>\n" +
                "\t<SONRS>\n" +
                "\t\t<STATUS>\n" +
                "\t\t\t<CODE>0\n" +
                "\t\t\t<SEVERITY>INFO\n" +
                "\t\t</STATUS>\n" +
                "\t\t<DTSERVER>20191203000000\n" +
                "\t\t<LANGUAGE>FRA\n" +
                "\t</SONRS>\n" +
                "</SIGNONMSGSRSV1>\n";
    }

    private String getStatementDebitHeader(String accountNumber, String currency){
        StringBuilder header= new StringBuilder();
        header.append("<BANKMSGSRSV1>\n"+
                "\t<STMTTRNRS>\n" +
                "\t\t<TRNUID>20191203000000\n" +
                "\t\t<STATUS>\n" +
                "\t\t\t<CODE>0\n" +
                "\t\t\t<SEVERITY>INFO\n" +
                "\t\t</STATUS>\n" +
                "\t\t<STMTRS>\n" +
                "\t\t\t<CURDEF>"+currency+"\n" +
                "\t\t\t<BANKACCTFROM>\n" +
                "\t\t\t\t<BANKID>10278\n" +
                "\t\t\t\t<BRANCHID>02409\n" +
                "\t\t\t\t<ACCTID>"+accountNumber+"\n" +
                "\t\t\t\t<ACCTTYPE>CHECKING\n" +
                "\t\t\t</BANKACCTFROM>\n" +
                "\t\t\t<BANKTRANLIST>");
        return header.toString();
    }

    private String getStatementDebitTrailer(){
        StringBuilder trailer= new StringBuilder();
        trailer.append("\n\t\t\t</BANKTRANLIST>\n" +
                "\t\t\t<LEDGERBAL>\n" +
                "\t\t\t\t<BALAMT>0\n" +
                "\t\t\t\t<DTASOF>20191121000000\n" +
                "\t\t\t</LEDGERBAL>\n" +
                "\t\t\t<AVAILBAL>\n" +
                "\t\t\t\t<BALAMT>0.00\n" +
                "\t\t\t\t<DTASOF>20191121000000\n" +
                "\t\t\t</AVAILBAL>\n" +
                "\t\t</STMTRS>\n" +
                "\t</STMTTRNRS>\n"+
                "</BANKMSGSRSV1>");
        return trailer.toString();
    }


    private String getCreditHeader(){
        return "<SIGNONMSGSRSV1>\n" +
                "\t<SONRS>\n" +
                "\t\t<STATUS>\n" +
                "\t\t\t<CODE>0\n" +
                "\t\t\t<SEVERITY>INFO\n" +
                "\t\t</STATUS>\n" +
                "\t\t<DTSERVER>20191203000000\n" +
                "\t\t<LANGUAGE>FRA\n" +
                "\t</SONRS>\n" +
                "</SIGNONMSGSRSV1>\n";
    }


    private String getStatementCreditHeader(String accountNumber, String currency){
        StringBuilder header= new StringBuilder();
        header.append( "\t<CREDITCARDMSGSRSV1>\n"+
                "\t\t<CCSTMTTRNRS>\n" +
                "\t\t\t<TRNUID>0\n" +
                "\t\t\t<STATUS>\n" +
                "\t\t\t\t<CODE>0\n" +
                "\t\t\t\t<SEVERITY>INFO\n" +
                "\t\t\t</STATUS>\n" +
                "\t\t\t<CCSTMTRS>\n" +
                "\t\t\t\t<CURDEF>"+currency+"\n" +
                "\t\t\t\t<CCACCTFROM>\n" +
                "\t\t\t\t\t<ACCTID>"+accountNumber+"\n" +
                "\t\t\t\t\t<AMEX.UNIVID>fbbe08704f3cbea802b8a2bbb414f7a9\n" +
                "\t\t\t\t</CCACCTFROM>\n" +
                "\t\t\t\t<BANKTRANLIST>\n" +
                "\t\t\t\t\t<DTSTART>20191117050000.000[-7:MST]\n" +
                "\t\t\t\t\t<DTEND>20191204050000.000[-7:MST]");
        return header.toString();
    }

    

    private String getStatementCreditTrailer(){
        StringBuilder trailer= new StringBuilder();
        trailer.append("\t\t\t\t</BANKTRANLIST>\n" +
                "\t\t\t\t<LEDGERBAL>\n" +
                "\t\t\t\t\t<BALAMT>-1403.26\n" +
                "\t\t\t\t\t<DTASOF>20191204050000.000[-7:MST]\n" +
                "\t\t\t\t</LEDGERBAL>\n" +
                "\t\t\t\t<CYCLECUT.INDICATOR>false\n" +
                "\t\t\t\t<PURGE.INDICATOR>false\n" +
                "\t\t\t\t<INTL.INDICATOR>false\n" +
                "\t\t\t</CCSTMTRS>"+
                "\t\t</CCSTMTTRNRS>\n" +
                "\t</CREDITCARDMSGSRSV1>\n");
        return trailer.toString();
    }
}
