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

- **Java 17+** — via [SDKMAN](https://sdkman.io) (a `.sdkmanrc` is provided)
- **Gradle 8.9+** — via SDKMAN or the included Gradle wrapper
- **Google Chrome** — default browser for scraping (Firefox and Safari also supported)
- **[Bitwarden CLI](https://bitwarden.com/help/cli/)** (`bw`) — optional, required only if managing credentials via Bitwarden

Install the Bitwarden CLI via Homebrew:

```bash
brew install bitwarden-cli
```

## Building

```bash
./gradlew clean build
```

This produces a self-contained jar in `build/libs/`.

## Configuration

The application is driven by a JSON file defining the banks and accounts to process. A sample is provided at `src/main/resources/sample.json`.

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
| `waitTime` | yes | Selenium timeout in seconds. 5 is usually enough; increase for slow connections |
| `statementsDirectory` | no | Revolut only — directory where manually downloaded Revolut CSVs are stored (files are deleted after processing) |
| `accounts` | yes | List of accounts. See below |

### Account-level parameters

| Name | Required | Description |
|------|----------|-------------|
| `accountId` | yes | Account number as it appears on the bank website |
| `accountName` | no | OCBC only — account name used to identify the account on the website |
| `type` | no | OCBC only — `DEBIT` or `CREDIT` |
| `banktivitySuffix` | no | 4-character suffix appended to the account number in the OFX. Banktivity shows only the last 4 characters of an account number, so this makes it easy to identify accounts at a glance |

### Sample configuration

```json
[
  {
    "name": "metro",
    "connectionUrl": "https://personal.metrobankonline.co.uk/MetroBankRetail",
    "bitwardenItemName": "Metro Bank",
    "waitTime": 5,
    "accounts": [
      { "accountId": "32432432", "banktivitySuffix": "metr" }
    ]
  },
  {
    "name": "amex",
    "connectionUrl": "https://global.americanexpress.com/login?inav=iNavLnkLog",
    "username": "fake.user",
    "password": "fake.password",
    "securityCode": "0000",
    "waitTime": 5,
    "accounts": [
      { "accountId": "XXX-16241", "type": "CREDIT", "banktivitySuffix": "amex" }
    ]
  },
  {
    "name": "ocbc",
    "connectionUrl": "https://internet.ocbc.com/internet-banking/digital/web/sg/cfo/login/login",
    "bitwardenItemName": "OCBC",
    "waitTime": 60,
    "accounts": [
      { "accountId": "602-124109-001", "accountName": "360 Account", "type": "DEBIT", "banktivitySuffix": "ocbc" },
      { "accountId": "5413-8301-0026-1510", "accountName": "OCBC INFINITY Cashback Card", "type": "CREDIT", "banktivitySuffix": "cash" }
    ]
  },
  {
    "name": "revolut",
    "statementsDirectory": "/Users/username/Downloads/revolut-statements",
    "waitTime": 5,
    "accounts": [
      { "accountId": "revolut-gbp", "banktivitySuffix": "rgbp" },
      { "accountId": "revolut-eur", "banktivitySuffix": "reur" }
    ]
  }
]
```

## Bitwarden integration

Credentials can optionally be stored in Bitwarden instead of the JSON config file. Set `bitwardenItemName` on a bank entry to the name of the corresponding item in your Bitwarden vault. The following fields are mapped:

| Bitwarden field | Bank config field |
|-----------------|-------------------|
| Login → Username | `username` |
| Login → Password | `password` |
| Custom field `securityPin` | `securityPin` (Metro) |
| Custom field `securityCode` | `securityCode` (AMEX) |

Banks without `bitwardenItemName` continue to use plain credentials from the JSON file — the two approaches can be mixed freely.

### Installation

```bash
brew install bitwarden-cli
```

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

### Each session

Before running the application, unlock your vault and export the session token:

```bash
export BW_SESSION=$(bw unlock --raw)
```

The `download-ofx` wrapper script handles this automatically — it detects the vault state and prompts for your master password if needed.

## Running

### Using the wrapper script (recommended)

A `download-ofx` wrapper script is provided alongside the jar. It handles Bitwarden unlocking automatically and always picks the latest jar in the directory:

```bash
# Current month only
./download-ofx --monthly

# From a specific date
./download-ofx --date=2024-01-01
```

The script behaviour on startup:
- **Vault already unlocked** — proceeds immediately
- **Vault locked** — prompts for your master password, unlocks, then runs
- **Not logged in** — prints a message asking you to run `bw login` (one-time setup only)

### Running directly

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
| `--browser=<name>` | Browser to use: `chrome` (default), `firefox`, `safari` |

If none of the date options are provided, all available transactions are downloaded.

A timestamped backup of each generated OFX is also saved under `<output>/tx-compare/` for easy reconciliation.

### Examples

```bash
# All available transactions
java -jar bank-statements-<VERSION>.jar --json=~/banks.json --output=~/Downloads

# Current month only
java -jar bank-statements-<VERSION>.jar --json=~/banks.json --output=~/Downloads --monthly

# Last 30 days
java -jar bank-statements-<VERSION>.jar --json=~/banks.json --output=~/Downloads --month

# From a specific date
java -jar bank-statements-<VERSION>.jar --json=~/banks.json --output=~/Downloads --date=2024-01-01

# Using Firefox
java -jar bank-statements-<VERSION>.jar --json=~/banks.json --output=~/Downloads --monthly --browser=firefox
```

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
