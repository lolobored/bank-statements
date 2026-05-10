# Bank Statements

## Origin

This Java application automatically scrapes account statements from bank websites, converts them to OFX format, and merges them into a single file ready to import into [Banktivity](https://www.banktivity.com).

This became a necessity when Banktivity and its provider (Yodlee) decided to **not** implement PSD2 and Open Banking standards, leaving users with no automated import option.

## Supported banks

| Bank | Region | Format |
|------|--------|--------|
| [Credit Mutuel](https://www.creditmutuel.fr) | FR | CSV (scraped) |
| [AMEX](https://www.americanexpress.com/uk) | UK | CSV (scraped) |
| [Metrobank](https://www.metrobankonline.co.uk) | UK | CSV (scraped) |
| [Comm Bank](https://www.commbank.com.au) | AU | CSV (scraped) |
| [Westpac](https://www.westpac.com.au) | AU | CSV (scraped) |
| [UOB](https://www.uob.com.sg) | SG | XLS (scraped) |
| [OCBC](https://www.ocbc.com) | SG | CSV (scraped) |
| [Revolut](https://www.revolut.com) | Multi | CSV (manual download) |

## Requirements

- **Java 17+** — any distribution works. [SDKMAN](https://sdkman.io) is recommended (a `.sdkmanrc` is provided); if installed, `download-ofx` automatically picks the latest Java version in SDKMAN. Otherwise it uses whatever `java` is on your `PATH`.
- **Google Chrome** — default browser for scraping (Firefox and Safari also supported)
- **[Bitwarden CLI](https://bitwarden.com/help/cli/)** (`bw`) — optional, required only if managing credentials via Bitwarden
- **[GitHub CLI](https://cli.github.com)** (`gh`) — optional, required only for `download-ofx --update`

## Configuration

The application is driven by a JSON file defining the banks and accounts to process. A complete `banktivity.json.sample` is provided at the root of the repository — copy it to your desired location and fill in your credentials.

### Bank-level parameters

| Name | Required | Description |
|------|----------|-------------|
| `name` | yes | Bank identifier. One of: `metro`, `amex`, `credit mutuel`, `comm bank`, `westpac`, `uob`, `ocbc`, `revolut` |
| `connectionUrl` | yes | Login page URL |
| `username` | no | Login username (omit if using Bitwarden) |
| `password` | no | Login password (omit if using Bitwarden) |
| `securityCode` | no | AMEX only — card security code required at login |
| `securityPin` | no | Metro only — 8-digit security PIN |
| `bitwardenItemName` | no | If set, credentials are fetched from this Bitwarden item instead of the fields above |
| `enabled` | no | Set to `false` to skip this bank without removing it from the config. Defaults to `true` |
| `statementsDirectory` | no | Revolut only — directory where manually downloaded Revolut CSVs are stored (files are deleted after processing) |
| `accounts` | yes | List of accounts. See below |

### Account-level parameters

| Name | Required | Description |
|------|----------|-------------|
| `accountId` | yes | Account number as it appears on the bank website |
| `accountName` | no | OCBC only — account name used to identify the account on the website |
| `type` | no | OCBC only — `DEBIT` or `CREDIT` |
| `banktivitySuffix` | no | 4-character suffix appended to the account number in the OFX. Banktivity shows only the last 4 characters of an account number, so this makes it easy to identify accounts at a glance |
| `currency` | no | ISO 4217 currency code (e.g. `EUR`, `CHF`, `SGD`). Overrides the bank's default currency. Set this when a bank holds accounts in multiple currencies |

### Sample configuration

```json
[
  {
    "name": "credit mutuel",
    "connectionUrl": "https://www.creditmutuel.fr/fr/authentification.html",
    "bitwardenItemName": "Credit Mutuel",
    "accounts": [
      { "accountId": "4352432423", "banktivitySuffix": "cmeu" },
      { "accountId": "3534643234", "currency": "CHF", "banktivitySuffix": "cmch" },
      { "accountId": "325346573",  "currency": "GBP", "banktivitySuffix": "cmgb" }
    ]
  },
  {
    "name": "ocbc",
    "connectionUrl": "https://internet.ocbc.com/internet-banking/digital/web/sg/cfo/login/login",
    "bitwardenItemName": "OCBC",
    "accounts": [
      { "accountId": "602-124109-001", "accountName": "360 Account",             "type": "DEBIT",  "banktivitySuffix": "ocbc" },
      { "accountId": "5413-8301-0026-1510", "accountName": "OCBC Cashback Card", "type": "CREDIT", "banktivitySuffix": "cash" }
    ]
  },
  {
    "name": "revolut",
    "statementsDirectory": "/Users/username/Downloads/revolut-statements",
    "accounts": [
      { "accountId": "revolut-gbp", "banktivitySuffix": "rgbp" },
      { "accountId": "revolut-eur", "banktivitySuffix": "reur" }
    ]
  }
]
```

### Currency defaults

Each bank has a hardcoded default currency. Override it per account with the `currency` field.

| Bank | Default | Notes |
|------|---------|-------|
| Metro | GBP | |
| AMEX | GBP | |
| Credit Mutuel | EUR | Override per account for CHF or GBP sub-accounts |
| Comm Bank | AUD | |
| Westpac | AUD | |
| UOB | SGD | |
| OCBC | SGD | Applies to both debit and credit accounts |
| Revolut | auto | Read from the CSV column header — no override needed |

## Bitwarden integration

Credentials can optionally be stored in Bitwarden instead of the JSON config file. Set `bitwardenItemName` on a bank entry to the name of the corresponding item in your Bitwarden vault. The following fields are mapped:

| Bitwarden field | Bank config field |
|-----------------|-------------------|
| Login → Username | `username` |
| Login → Password | `password` |
| Custom field `securityPin` | `securityPin` (Metro) |
| Custom field `securityCode` | `securityCode` (AMEX) |

Banks without `bitwardenItemName` continue to use plain credentials from the JSON file — the two approaches can be mixed freely.

### Self-hosted Bitwarden / Vaultwarden

If you run your own Bitwarden-compatible server (e.g. [Vaultwarden](https://github.com/dani-garcia/vaultwarden)), point the CLI at it before logging in:

```bash
bw config server https://your-vaultwarden-url.com
```

### First-time login

Run this once in your terminal. It requires an interactive session so it must be run directly in your terminal app, not through a script:

```bash
bw login
```

## `download-ofx` — setup and automation

`download-ofx` is a wrapper script that handles everything in one command: it unlocks Bitwarden if needed, picks the right Java, and runs the jar. It is the recommended way to run the application.

### First-time setup

Download the latest `bank-statements-<version>.jar` and `download-ofx` from the [Releases page](https://github.com/lolobored/bank-statements/releases) into the same directory, then:

```bash
# Make the script executable
chmod +x download-ofx

# Place your banks.json in the same directory
cp /path/to/your/banks.json ~/banktivity/banks.json
```

The script expects `banks.json` and the jar to live alongside it.

### Daily usage

```bash
# Current month — headless Chrome (default)
./download-ofx --monthly

# From a specific date
./download-ofx --date=2024-01-01

# Last 30 days
./download-ofx --month
```

### Browser selection

Chrome and Firefox run headless (no visible window). Safari runs in a visible window and is useful as a fallback when a bank's login flow doesn't behave correctly in headless mode.

```bash
# Headless Chrome (default)
./download-ofx --monthly

# Headless Firefox
./download-ofx --monthly --browser=firefox

# Visible Safari window — fallback
./download-ofx --monthly --browser=safari
```

### Keeping up to date

The script updates itself automatically on every run. If the [GitHub CLI](https://cli.github.com) (`gh`) is installed and authenticated, it checks for a newer jar on GitHub Releases, downloads it, removes the old one, and then proceeds with the run as normal. If `gh` is not available or there is no network, it silently skips the check and runs with whatever jar is already present.

### Automation

Because MFA approval is required (OCBC and UOB send push notifications to your phone), the script cannot run fully unattended. However, you can streamline the process by scheduling a reminder that pre-unlocks the vault and launches the script ready for you to approve.

**macOS — run manually on a schedule**

The simplest approach on macOS is a shell alias or a calendar reminder. When you want to run:

```bash
# Unlock vault once, then run
export BW_SESSION=$(bw unlock --raw)
./download-ofx --monthly
```

**macOS — launchd (runs at login or on a schedule)**

Create `~/Library/LaunchAgents/com.banktivity.download.plist`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key>
  <string>com.banktivity.download</string>
  <key>ProgramArguments</key>
  <array>
    <string>/Users/username/banktivity/download-ofx</string>
    <string>--monthly</string>
  </array>
  <!-- Run on the 1st of every month at 09:00 -->
  <key>StartCalendarInterval</key>
  <dict>
    <key>Day</key>
    <integer>1</integer>
    <key>Hour</key>
    <integer>9</integer>
    <key>Minute</key>
    <integer>0</integer>
  </dict>
  <key>StandardOutPath</key>
  <string>/tmp/banktivity.log</string>
  <key>StandardErrorPath</key>
  <string>/tmp/banktivity.log</string>
</dict>
</plist>
```

Load it with:

```bash
launchctl load ~/Library/LaunchAgents/com.banktivity.download.plist
```

> **Note:** The launchd job will prompt for your Bitwarden master password when it runs. Make sure your vault is unlocked beforehand, or run the script manually when you are at your machine to approve the MFA notifications on your phone.

## Running directly

For advanced use or scripting without the wrapper:

```bash
java -jar bank-statements-<VERSION>.jar --json=<path/to/config.json> --output=<output/dir> [options]
```

### Options

| Option | Description |
|--------|-------------|
| `--json=<path>` | Path to the JSON configuration file **(required)** |
| `--output=<dir>` | Directory where `downloaded.ofx` will be written **(required)** |
| `--monthly` | Only include transactions from the 1st of the current month |
| `--month` | Only include transactions from the last 30 days |
| `--days=<n>` | Only include transactions from the last `n` days |
| `--date=<yyyy-MM-dd>` | Only include transactions on or after this date |
| `--browser=<name>` | Browser to use: `chrome` (default, headless), `firefox` (headless), `safari` (visible window — useful as fallback) |
| `--screenshots=<dir>` | Directory where error screenshots are saved on scraping failure. Defaults to `~/Downloads` |
| `--history=<dir>` | Directory for the fuzzy-dedup history files. Defaults to `<output>/tx-history` |

If none of the date options are provided, all available transactions are downloaded.

### Fuzzy duplicate detection

Each time the app runs it checks new transactions against a persisted history (one JSON file per account under `tx-history/`). A transaction is suppressed if it matches a previously exported one on **all three** of:

- **Amount** — exact match
- **Date** — within ±5 days (configurable per account with `dateTolerance`)
- **Description** — the label is a substring of the historical one (or vice versa), **or** the Jaro-Winkler similarity exceeds 0.85 (configurable per account with `descriptionSimilarity`)

This prevents Banktivity duplicates when a bank initially posts a transaction with a pending description or an approximate date that later settles to a slightly different value.

Per-account tuning in your JSON config:

```json
{
  "accountId": "32432432",
  "banktivitySuffix": "metr",
  "dateTolerance": 7,
  "descriptionSimilarity": 0.80
}
```

To reset the history for an account (e.g. after a fresh Banktivity import), delete the corresponding `<accountNumber>.json` file inside the `tx-history/` directory.

### Examples

```bash
# All available transactions, headless Chrome
java -jar bank-statements-<VERSION>.jar --json=~/banks.json --output=~/Downloads

# Current month, headless Firefox
java -jar bank-statements-<VERSION>.jar --json=~/banks.json --output=~/Downloads --monthly --browser=firefox

# From a specific date, visible Safari window
java -jar bank-statements-<VERSION>.jar --json=~/banks.json --output=~/Downloads --date=2024-01-01 --browser=safari
```

## Building from source

```bash
./gradlew bootJar
```

This produces a self-contained jar in `build/libs/`.

## Code architecture

All bank scrapers use the **Page Object Model (POM)** pattern. Each logical page or workflow on a bank website has its own class under `service/scrapers/pages/<bank>/`. These classes:

- Declare all CSS selectors and XPaths as named `private static final By` constants
- Expose only high-level actions (`login()`, `downloadCsv()`, etc.)
- Contain no business logic — just DOM interaction

The service impls (`service/scrapers/impl/`) are thin orchestrators (~30-55 lines each) that wire page objects together and pass results to the conversion layer. When a bank changes its UI, only the relevant page object needs updating — the service impl stays untouched.

| Bank | Page objects |
|------|-------------|
| OCBC | `OCBCLoginPage`, `OCBCAccountsOverviewPage`, `OCBCDebitTransactionsPage`, `OCBCCreditTransactionsPage` |
| AMEX | `AmexLoginPage`, `AmexActivityPage` |
| CommBank | `CommBankLoginPage`, `CommBankAccountsPage`, `CommBankTransactionsPage` |
| Westpac | `WestpacLoginPage`, `WestpacExportPage` |
| CreditMut | `CreditMutLoginPage`, `CreditMutDownloadPage` |
| Metro | `MetroLoginPage`, `MetroSecurityPage`, `MetroAccountsPage` |
| UOB | `UOBLoginPage`, `UOBAccountsPage`, `UOBTransactionsPage` |

## Account detection

Account numbers and types are detected automatically during scraping. The `accounts` section in the JSON is only needed for `banktivitySuffix` (and for OCBC where the account name and type are needed to navigate the website).

| Bank | Account ID source | Account type |
|------|-------------------|--------------|
| Metrobank | Sort code + account number from the webpage | Detected from account icon (card = credit) |
| AMEX | From the download statement CSV page | Always `CREDIT CARD` |
| Revolut | From the CSV filename (e.g. `Revolut-GBP-Statement*.csv` → `revolut-gbp`) | Always `DEBIT` |
| Comm Bank | Account number from the webpage | Always `DEBIT` |
| Westpac | Account number from the webpage | Always `DEBIT` |
| Credit Mutuel | From the CSV filename | Always `DEBIT` |
| UOB | Single account, no multi-account support | Always `DEBIT` |
| OCBC | Matched against `accountName` in JSON config | Set by `type` in JSON config |
