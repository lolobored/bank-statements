package org.lolobored.bankstatements;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import org.apache.commons.io.FileUtils;
import org.lolobored.bankstatements.model.Statement;
import org.lolobored.bankstatements.model.config.Account;
import org.lolobored.bankstatements.model.config.Bank;
import org.lolobored.bankstatements.service.bitwarden.BitwardenService;
import org.lolobored.bankstatements.service.filter.StatementsFilterService;
import org.lolobored.bankstatements.service.ofx.OfxConversionService;
import org.lolobored.bankstatements.service.scrapers.*;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BankStatementsApplication implements ApplicationRunner {

  @Autowired private MetroService metroService;
  @Autowired private AmexService amexService;
  @Autowired private CreditMutService creditMutService;
  @Autowired private RevolutService revolutService;
  @Autowired private CommBankService commBankService;
  @Autowired private WestpacService westpacService;
  @Autowired private UOBService uobService;
  @Autowired private OCBCService ocbcService;
  @Autowired private BitwardenService bitwardenService;
  @Autowired private StatementsFilterService statementsFilterService;
  @Autowired private OfxConversionService ofxConversionService;

  private static final String METRO = "metro";
  private static final String CREDIT_MUT = "credit mutuel";
  private static final String AMEX = "amex";
  private static final String REVOLUT = "revolut";
  private static final String COMM_BANK = "comm bank";
  private static final String WESTPAC = "westpac";
  private static final String UOB = "uob";
  private static final String OCBC = "ocbc";

  private static final int CHROME_BROWSER = 0;
  private static final int FIREFOX_BROWSER = 1;
  private static final int SAFARI_BROWSER = 2;

  private final Logger logger = LoggerFactory.getLogger(BankStatementsApplication.class);

  public static void main(String[] args) {
    SpringApplication.run(BankStatementsApplication.class, args);
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    Path downloads = Files.createTempDirectory("statements");
    Map<String, String> accountReplacements = new HashMap<>();
    Map<String, String> accountCurrencies = new HashMap<>();
    LocalDate startingDate = LocalDate.parse("1970-01-01");

    if (!args.containsOption("json")) {
      logger.error(
          "Option --json is mandatory and should contain the json config file path for the banks");
      System.exit(-1);
    }
    if (!args.containsOption("output")) {
      logger.error(
          "Option --output is mandatory and should contain the directory where the merged ofx file will be produced");
      System.exit(-1);
    }

    if (args.containsOption("monthly")) {
      LocalDate comparableDate = LocalDate.now().withDayOfMonth(1);
      if (comparableDate.isAfter(startingDate)) startingDate = comparableDate;
    }
    if (args.containsOption("days")) {
      LocalDate comparableDate =
          LocalDate.now().minusDays(Integer.parseInt(args.getOptionValues("days").get(0)));
      if (comparableDate.isAfter(startingDate)) startingDate = comparableDate;
    }
    if (args.containsOption("month")) {
      LocalDate comparableDate = LocalDate.now().minusDays(30);
      if (comparableDate.isAfter(startingDate)) startingDate = comparableDate;
    }
    if (args.containsOption("date")) {
      logger.info("Initial date [" + args.getOptionValues("date").get(0) + "]");
      try {
        LocalDate comparableDate = LocalDate.parse(args.getOptionValues("date").get(0));
        if (comparableDate.isAfter(startingDate)) startingDate = comparableDate;
      } catch (Exception err) {
        logger.error("Date parameter is supposed to be using the format yyyy-MM-dd");
        System.exit(-1);
      }
    }

    File outputDirectory = new File(args.getOptionValues("output").get(0));
    File screenshotsDirectory =
        args.containsOption("screenshots")
            ? new File(args.getOptionValues("screenshots").get(0))
            : new File(System.getProperty("user.home"), "Downloads");
    String jsonFilePath = args.getOptionValues("json").get(0);
    logger.info("JSON file [" + new File(jsonFilePath).getAbsolutePath() + "]");
    if (!new File(jsonFilePath).exists()) {
      logger.error("JSON file does not exists at [" + jsonFilePath + "]");
      System.exit(-1);
    }

    int browserType = CHROME_BROWSER;
    if (args.getOptionValues("browser") != null) {
      switch (args.getOptionValues("browser").get(0).toLowerCase().trim()) {
        case "firefox":
          browserType = FIREFOX_BROWSER;
          break;
        case "safari":
          browserType = SAFARI_BROWSER;
          break;
        case "chrome":
        case "":
          browserType = CHROME_BROWSER;
          break;
        default:
          throw new Exception(
              args.getOptionValues("browser").get(0).trim()
                  + " is not part of the supported browser");
      }
    }

    // Set up the driver binary once before spawning parallel tasks
    switch (browserType) {
      case CHROME_BROWSER:
        WebDriverManager.chromedriver().setup();
        break;
      case FIREFOX_BROWSER:
        WebDriverManager.firefoxdriver().setup();
        break;
    }

    try {
      ObjectMapper objectMapper = new ObjectMapper();
      List<Bank> banks =
          objectMapper.readValue(
              new String(Files.readAllBytes(Paths.get(jsonFilePath))),
              new TypeReference<List<Bank>>() {});

      // Collect per-account config from all accounts (including disabled banks)
      for (Bank bank : banks) {
        for (Account account : bank.getAccounts()) {
          if (account.getBanktivitySuffix() != null) {
            accountReplacements.put(account.getAccountId(), account.getBanktivitySuffix());
          }
          if (account.getCurrency() != null) {
            accountCurrencies.put(account.getAccountId(), account.getCurrency());
          }
        }
      }

      boolean anyBankUsesBitwarden =
          banks.stream()
              .anyMatch(
                  b -> b.getBitwardenItemName() != null && !b.getBitwardenItemName().isEmpty());
      if (anyBankUsesBitwarden) {
        bitwardenService.checkVaultAccess();
      }
      for (Bank bank : banks) {
        bitwardenService.resolveCredentials(bank);
      }

      // Build one task per enabled bank — each gets its own WebDriver and download subdirectory
      final int finalBrowserType = browserType;
      final File finalScreenshotsDirectory = screenshotsDirectory;
      List<Callable<List<Statement>>> tasks = new ArrayList<>();
      List<String> taskBankNames = new ArrayList<>();

      for (Bank bank : banks) {
        if (!bank.isEnabled()) {
          logger.info("Skipping disabled bank [{}]", bank.getName());
          continue;
        }
        BankGenericService service = resolveService(bank.getName());
        if (service == null) continue;

        Path bankDownloadDir = downloads.resolve(bank.getName().replaceAll("\\s+", "-"));
        Files.createDirectories(bankDownloadDir);

        taskBankNames.add(bank.getName());
        tasks.add(
            () -> {
              WebDriver driver = createWebDriver(finalBrowserType, bankDownloadDir);
              try {
                return service.downloadStatements(driver, bank, bankDownloadDir.toString());
              } catch (Exception e) {
                saveErrorScreenshot(driver, bank.getName(), finalScreenshotsDirectory);
                throw e;
              } finally {
                driver.close();
              }
            });
      }

      // Run all banks in parallel
      ExecutorService executor = Executors.newCachedThreadPool();
      List<Future<List<Statement>>> futures;
      try {
        futures = executor.invokeAll(tasks);
      } finally {
        executor.shutdown();
      }

      // Collect results — keep going even if some banks fail
      List<Statement> statements = new ArrayList<>();
      List<String> failures = new ArrayList<>();
      for (int i = 0; i < futures.size(); i++) {
        String bankName = taskBankNames.get(i);
        try {
          statements.addAll(futures.get(i).get());
        } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          String msg =
              cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
          failures.add(bankName + ": " + msg);
          logger.error("Bank [{}] failed", bankName, cause);
        }
      }
      if (!failures.isEmpty()) {
        logger.error("╔══════════════════════════════════════╗");
        logger.error("║         BANK SCRAPING FAILURES       ║");
        logger.error("╠══════════════════════════════════════╣");
        failures.forEach(f -> logger.error("║  ✗ {}", f));
        logger.error("╚══════════════════════════════════════╝");
      }
      if (statements.isEmpty() && !failures.isEmpty()) {
        throw new RuntimeException("All banks failed — no statements to process");
      }

      // Apply per-account currency overrides before filtering (filtering may modify account
      // numbers)
      statements.forEach(
          s -> {
            String currency = accountCurrencies.get(s.getAccountNumber());
            if (currency != null) s.setCurrency(currency);
          });

      statements =
          statementsFilterService.filterStatements(statements, startingDate, accountReplacements);
      String ofxResult = ofxConversionService.convertStatementsToOfx(statements);

      outputDirectory.mkdirs();
      File resultFile = new File(outputDirectory.getAbsolutePath() + "/downloaded.ofx");
      if (resultFile.exists()) resultFile.delete();
      FileUtils.writeStringToFile(resultFile, ofxResult, Charset.defaultCharset());

      outputDirectory = new File(outputDirectory.getAbsolutePath() + "/tx-compare");
      outputDirectory.mkdirs();
      String currentTime =
          LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
      resultFile =
          new File(outputDirectory.getAbsolutePath() + "/" + currentTime + "-downloaded.ofx");
      FileUtils.writeStringToFile(resultFile, ofxResult, Charset.defaultCharset());

      // Keep only the 30 most recent timestamped OFX files
      File[] txFiles = outputDirectory.listFiles((dir, name) -> name.endsWith("-downloaded.ofx"));
      if (txFiles != null && txFiles.length > 30) {
        Arrays.sort(txFiles, Comparator.comparing(File::getName));
        for (int i = 0; i < txFiles.length - 30; i++) {
          txFiles[i].delete();
        }
      }

    } finally {
      FileUtils.deleteDirectory(downloads.toFile());
    }
  }

  private BankGenericService resolveService(String bankName) {
    return switch (bankName) {
      case METRO -> metroService;
      case CREDIT_MUT -> creditMutService;
      case AMEX -> amexService;
      case REVOLUT -> revolutService;
      case COMM_BANK -> commBankService;
      case WESTPAC -> westpacService;
      case UOB -> uobService;
      case OCBC -> ocbcService;
      default -> null;
    };
  }

  private WebDriver createWebDriver(int browserType, Path downloadDir) {
    switch (browserType) {
      case CHROME_BROWSER:
        {
          ChromeOptions opts = new ChromeOptions();
          opts.addArguments(
              "--headless=new", "--window-size=1920,1080", "--disable-popup-blocking");
          Map<String, Object> prefs = new HashMap<>();
          prefs.put("profile.default_content_settings.popups", 0);
          prefs.put("download.default_directory", downloadDir.toAbsolutePath().toString());
          opts.setExperimentalOption("prefs", prefs);
          return new ChromeDriver(opts);
        }
      case FIREFOX_BROWSER:
        {
          FirefoxProfile profile = new FirefoxProfile();
          profile.setPreference("browser.download.folderList", 2);
          profile.setPreference("browser.download.manager.showWhenStarting", false);
          profile.setPreference("browser.download.dir", downloadDir.toAbsolutePath().toString());
          profile.setPreference("browser.helperApps.neverAsk.saveToDisk", "application/x-gzip");
          FirefoxOptions opts = new FirefoxOptions();
          opts.addArguments("--headless");
          opts.setProfile(profile);
          return new FirefoxDriver(opts);
        }
      default:
        return new SafariDriver(new SafariOptions());
    }
  }

  private void saveErrorScreenshot(WebDriver driver, String bankName, File screenshotsDir) {
    try {
      File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
      File dest = new File(screenshotsDir, "error-" + bankName + "-" + timestamp + ".png");
      FileUtils.copyFile(screenshot, dest);
      logger.error("Screenshot for [{}] saved to: {}", bankName, dest.getAbsolutePath());
    } catch (Exception e) {
      logger.warn("Could not take error screenshot for [{}]: {}", bankName, e.getMessage());
    }
  }
}
