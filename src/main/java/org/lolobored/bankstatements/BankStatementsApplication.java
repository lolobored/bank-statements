package org.lolobored.bankstatements;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.commons.io.FileUtils;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.config.Account;
import org.lolobored.bankstatements.model.config.Bank;
import org.lolobored.bankstatements.service.filter.StatementsFilterService;
import org.lolobored.bankstatements.service.ofx.OfxConversionService;
import org.lolobored.bankstatements.service.scrapers.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class BankStatementsApplication implements ApplicationRunner {

  @Autowired
  private MetroService metroService;
  @Autowired
  private AmexService amexService;
  @Autowired
  private CreditMutService creditMutService;
  @Autowired
  private RevolutService revolutService;
  @Autowired
  private CommBankService commBankService;
  @Autowired
  private WestpacService westpacService;
  @Autowired
  private UOBService uobService;
  @Autowired
  private OCBCService ocbcService;
  @Autowired
  private StatementsFilterService statementsFilterService;

  @Autowired
  private OfxConversionService ofxConversionService;

  private final static String METRO = "metro";
  private final static String CREDIT_MUT = "credit mutuel";
  private final static String AMEX = "amex";
  private final static String REVOLUT = "revolut";
  private final static String COMM_BANK = "comm bank";
  private final static String WESTPAC = "westpac";
  private final static String UOB = "uob";
  private final static String OCBC = "ocbc";

  private final static int CHROME_BROWSER=0;
  private final static int FIREFOX_BROWSER=1;
  private final static int SAFARI_BROWSER=2;


  private Logger logger = LoggerFactory.getLogger(BankStatementsApplication.class);

  public static void main(String[] args) {
    SpringApplication.run(BankStatementsApplication.class, args);

  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    Path downloads = Files.createTempDirectory("statements");
    Map<String, String> accountReplacements = new HashMap<>();
    LocalDate startingDate = LocalDate.parse("1970-01-01");

    /**
     * Check arguments
     */
    if (!args.containsOption("json")) {
      logger.error("Option --json is mandatory and should contain the json config file path for the banks");
      System.exit(-1);
    }
    if (!args.containsOption("output")) {
      logger.error("Option --output is mandatory and should contain the directory where the merged ofx file will be produced");
      System.exit(-1);
    }
    /**
     * if just for this month
     */
    if (args.containsOption("monthly")) {
      LocalDate comparableDate = LocalDate.now();
      comparableDate = comparableDate.withDayOfMonth(1);
      if (comparableDate.isAfter(startingDate)) {
        startingDate = comparableDate;
      }
    }
    if (args.containsOption("days")) {
      LocalDate comparableDate = LocalDate.now();
      comparableDate = comparableDate.minusDays(Integer.parseInt(args.getOptionValues("days").get(0)));
      if (comparableDate.isAfter(startingDate)) {
        startingDate = comparableDate;
      }
    }
    if (args.containsOption("month")) {
      LocalDate comparableDate = LocalDate.now();
      comparableDate = comparableDate.minusDays(30);
      if (comparableDate.isAfter(startingDate)) {
        startingDate = comparableDate;
      }
    }
    if (args.containsOption("date")) {
      logger.info("Initial date [" + args.getOptionValues("date").get(0) + "]");
      try {
        LocalDate comparableDate = LocalDate.parse(args.getOptionValues("date").get(0));
        if (comparableDate.isAfter(startingDate)) {
          startingDate = comparableDate;
        }
      } catch (Exception err) {
        logger.error("Date parameter is supposed to be using the format yyyy-MM-dd");
        System.exit(-1);
      }
    }

    File outputDirectory = new File(args.getOptionValues("output").get(0));
    String jsonFilePath = args.getOptionValues("json").get(0);
    logger.info("JSON file [" + new File(jsonFilePath).getAbsolutePath() + "]");
    if (!new File(jsonFilePath).exists()) {
      logger.error("JSON file does not exists at [" + jsonFilePath + "]");
      System.exit(-1);
    }

    int browserType=CHROME_BROWSER;
    // check if preferred web browser is selected
    if (args.getOptionValues("browser")!= null){
      String browserName = args.getOptionValues("browser").get(0);

      switch (browserName.toLowerCase().trim()){
        case "firefox":
          browserType= FIREFOX_BROWSER;
          break;
        case "chrome":
          browserType=CHROME_BROWSER;
          break;
        case "safari":
          browserType=SAFARI_BROWSER;
          break;
        case "":
          browserType=CHROME_BROWSER;
          break;
        default:
          throw new Exception(browserName.trim()+" is not part of the supported browser");
      }
    }

    try {

      WebDriver webDriver=null;
      switch (browserType) {
        case CHROME_BROWSER:
          WebDriverManager.chromedriver().setup();
          ChromeOptions chromeOptions = new ChromeOptions();
          chromeOptions.addArguments("--disable-popup-blocking");
          DesiredCapabilities capabilities = new DesiredCapabilities();

          Map<String, Object> chromePrefs = new HashMap<String, Object>();
          chromePrefs.put("profile.default_content_settings.popups", 0);
          chromePrefs.put("download.default_directory", downloads.toAbsolutePath().toString());
          chromeOptions.setExperimentalOption("prefs", chromePrefs);
          capabilities.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
          webDriver = new ChromeDriver(chromeOptions);
          break;
        case FIREFOX_BROWSER:
          WebDriverManager.firefoxdriver().setup();
          FirefoxProfile firefoxProfile = new FirefoxProfile();
          firefoxProfile.setPreference("browser.download.folderList", 2);
          firefoxProfile.setPreference("browser.download.manager.showWhenStarting", false);
          firefoxProfile.setPreference("browser.download.dir", downloads.toAbsolutePath().toString());
          firefoxProfile.setPreference("browser.helperApps.neverAsk.saveToDisk", "application/x-gzip");
          FirefoxOptions firefoxOptions= new FirefoxOptions();
          firefoxOptions.setProfile(firefoxProfile);
          webDriver = new FirefoxDriver(firefoxOptions);
          break;
    }

      /**
       * Read configuration for the banks
       */
      ObjectMapper objectMapper = new ObjectMapper();

      List<Bank> banks = objectMapper.readValue(new String(Files.readAllBytes(Paths.get(jsonFilePath))),
              new TypeReference<List<Bank>>() {
              });

      List<Statement> statements = new ArrayList<>();

      // find all the replacements
      // for an easier integration into banktivity
      for (Bank bank : banks) {
        for (Account account : bank.getAccounts()) {
          if (account.getBanktivitySuffix() != null) {
            accountReplacements.put(account.getAccountId(), account.getBanktivitySuffix());
          }
        }
        BankGenericService bankGenericService = null;

        switch (bank.getName()) {
          case METRO:
            bankGenericService = metroService;
            break;
          case CREDIT_MUT:
            bankGenericService = creditMutService;
            break;
          case AMEX:
            bankGenericService = amexService;
            break;
          case REVOLUT:
            bankGenericService = revolutService;
            break;
          case COMM_BANK:
            bankGenericService = commBankService;
            break;
          case WESTPAC:
            bankGenericService = westpacService;
            break;
          case UOB:
            bankGenericService = uobService;
            break;
          case OCBC:
            bankGenericService = ocbcService;
            break;
        }
        if (bankGenericService != null) {
          statements.addAll(bankGenericService.downloadStatements(webDriver, bank, downloads.toAbsolutePath().toString()));
        }
      }

      statements = statementsFilterService.filterStatements(statements, startingDate, accountReplacements);
      String ofxResult = ofxConversionService.convertStatementsToOfx(statements);


      outputDirectory.mkdirs();
      File resultFile = new File(outputDirectory.getAbsolutePath() + "/downloaded.ofx");
      if (resultFile.exists()) {
        resultFile.delete();
      }
      FileUtils.writeStringToFile(resultFile, ofxResult, Charset.defaultCharset());
      webDriver.close();

    } finally {
      FileUtils.deleteDirectory(downloads.toFile());
    }

  }
}
