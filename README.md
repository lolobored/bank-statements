# Bank Statements

## Origin

This Java application was designed for automatically scraping my account statements from the following websites:
* Credit Mutuel (FR): https://www.creditmutuel.fr
* AMEX (UK): https://www.americanexpress.com/uk
* Metrobank (UK): https://www.metrobankonline.co.uk

In addition it automatically converts as well Revolut (https://www.revolut.com) statements.

This became a necessity when Banktivity and its provider (Yodlee) decided without any common sense to **not** implement PSD2 and Open Banking standards in an account management software.

## Basic features

The configuration of the application relies on a JSON file that defines the site and the accounts statements to download.
The download will be done using Selenium Chrome driver (appropriate driver automatically downloaded when running the application).

It will go through these website and download CSVs files.

Once downloaded it will convert and merge all of these in one single OFX file ready to be imported.

## Running the software

A self-running jar file is created when building using gradle:
```
gradle clean build
```

Running the software only requires to run the jar file with the appropriate options:
* json: defines where to find the json file defining the banks website to scrape
* output: defines where the merged OFX file will be created. The merged file will be named "downloaded.ofx"
* monthly: an optional parameter allowing to use only transactions whose value date is from the start of the current month
* month: an optional parameter allowing to use only transactions whose value date is 30-days earlier than today
* date: an optional parameter allowing to download all the transactions available from that date using a pattern yyyy-MM-dd

If none of the last parameter is used, all the transactions will be downloaded.

Example of usage:
* Downloading all the transactions available into the user Downloads directory:
```
java -jar bank-statements-<VERSION>.jar --json=<path to the json file> --output=~/Downloads
```
* Downloading all the transactions available from the 1st of this month into the user Downloads directory:
```
java -jar bank-statements-<VERSION>.jar --json=<path to the json file> --monthly --output=~/Downloads
```
* Downloading all the transactions available from 30-days ago into the user Downloads directory:
```
java -jar bank-statements-<VERSION>.jar --json=<path to the json file> --month --output=~/Downloads
```
* Downloading all the transactions available from the 28th of January 1980 into the user Downloads directory:
```
java -jar bank-statements-<VERSION>.jar --json=<path to the json file> --date=1980-01-28 --output=~/Downloads
```

## JSON File

A sample of the JSON file defining the configuration per bank can be found in src/main/resources.
Each entry describe a bank and a bank can have multiple accounts.

At the bank level the parameters are:

| Name | Description |
| ---- | ----------- |
| name | The name and the type of the bank ultimately. Can only be *metro*, *revolut*, *amex* or *credit mutuel* |
| connectionUrl | The url where the login page is for each bank |
| username | The username for the login page |
| password | The password the the login page |
| securityPin | Only used in the case of metro where an 8 digits security pin is required |
| waitTime | The maximum number of seconds before the scraping fails in a time-out. I would recommend 5 here |
| statementsDirectory | Only used for Revolut where we cannot scrape any website. The directory where Revolut statements will have been downloaded. Note that it will delete those from the directory |
| accounts | The list of accounts. See below for the structure of an account. |

Each bank can have 0 to multiple accounts. 
Referencing the accounts is not mandatory. The account number and account type (credit card or debit/savings) are automatically scraped from the webpages.
Now this is only to automatically add a suffix to the account number in the ofx so that the importation is made easier in Banktivity.

The structure of an account is the following:

| Name | Description |
| ---- | ----------- |
| accountId | The account ID which would need to be suffixed by a banktivity suffix on 4 letters |
| banktivitySuffix | When importing an OFX into banktivity, banktivity displays only the 4 last characters of the account number. To make it easier, I added a suffix to the account id in the OFX so that I can see in a single glance which account is which. Do not fill if you don't want this feature. |

```json
[
  {
    "name": "metro",
    "connectionUrl": "https://personal.metrobankonline.co.uk/MetroBankRetail",
    "username": "fake.user",
    "password": "fake.password",
    "securityPin": "535452435",
    "waitTime": 5,
    "accounts": [
      {
        "accountId": "32432432",
        "banktivitySuffix": "metr"
      },
      {
        "accountId": "23453532",
        "banktivitySuffix": "metr"
      }
    ]
  },
  {
    "name": "amex",
    "connectionUrl": "https://global.americanexpress.com/login?inav=iNavLnkLog",
    "username": "fake.user",
    "password": "fake.password",
    "waitTime": 5,
    "accounts": [
      {
        "accountId": "XXX-16241",
        "banktivitySuffix": "amex"
      }
    ]
  },
  {
    "name": "credit mutuel",
    "connectionUrl": "https://www.creditmutuel.fr/fr/authentification.html",
    "username": "3543532421",
    "password": "3423rfewdfaf",
    "waitTime": 5,
    "accounts": [
      {
        "accountId": "4352432423",
        "banktivitySuffix": "cmeu"
      },
      {
        "accountId": "3534643234",
        "banktivitySuffix": "cmch"
      },
      {
        "accountId": "325346573",
        "banktivitySuffix": "ccel"
      }
    ]
  },
  {
    "name": "revolut",
    "statementsDirectory": "/Users/username/OneDrive/revolut-statements",
    "waitTime": 5,
    "accounts": [
      {
        "accountId": "revolut-gbp",
        "banktivitySuffix": "rgbp"
      },
      {
        "accountId": "revolut-eur",
        "banktivitySuffix": "reur"
      },
      {
        "accountId": "revolut-chf",
        "banktivitySuffix": "rchf"
      }
    ]
  }
]
```

## Account naming & type

When creating the OFX file, the utility automatically fetches the account number and information from the website.
Note that distinction between credit card and debit / saving is important as it is required for producing an accurate OFX (Credit cards info are in different layouts).

The way to do so is following this method:

| Bank | Account Name | Account type |
| ---- | ------------ | ------------ |
| Metrobank | Account name is fetched from the webpage, just after logging in. Each account is inside a block containing its sort code and its account number | Account type are deducted from the icon of the account. An icon of a card (C) represent a credit card. Anything else is considered as a debit / saving |
| AMEX | Account name is fetched from the download statement CSV page by browsing into the different accounts that are available there | AMEX is always Credit Card |
| Revolut | Account name is fetched from the name of the CSV as being downloaded from the mobile app. It should be something like "Revolut-GBP-Statement*.csv" for a GBP account. Account name will be revolut-gbp (lower case) | Revolut is only DEBIT |
| Credit Mutuel | Account name is fetched directly from the name of the CSV which is named with the account number | Credit Mutuel is french there's no credit card there so DEBIT |

