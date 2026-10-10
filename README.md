# Crop Price Alert System (MandiWatch)

A Spring Boot web application that helps farmers decide **where and when to sell their crops**.
It reads daily crop prices published by the Government of India, works out which market gives
the farmer the most money after transport and market charges, and sends SMS alerts in
Telugu, Hindi or English when a price the farmer is waiting for is reached.

The app is called **MandiWatch** inside the code and on its pages.

---

## Contents

1. [The problem](#1-the-problem)
2. [Features](#2-features)
3. [Tech stack](#3-tech-stack)
4. [Quick start](#4-quick-start)
5. [Step-by-step setup](#5-step-by-step-setup)
6. [Using the application](#6-using-the-application)
7. [Demo walkthrough (5 minutes)](#7-demo-walkthrough-5-minutes)
8. [Configuration](#8-configuration)
9. [Using real government data](#9-using-real-government-data)
10. [JSON API](#10-json-api)
11. [How the take-home amount is calculated](#11-how-the-take-home-amount-is-calculated)
12. [Architecture](#12-architecture)
13. [Design decisions](#13-design-decisions)
14. [Tests](#14-tests)
15. [Troubleshooting](#15-troubleshooting)
16. [Glossary](#16-glossary)
17. [Possible extensions](#17-possible-extensions)

---

## 1. The problem

In India, farmers sell their produce at regulated wholesale markets called **mandis**. Prices for
the same crop can differ a lot between mandis on the same day, but a farmer usually sells at the
nearest one because they don't know what the others are paying.

The government already publishes daily mandi prices (the *Agmarknet* dataset on
[data.gov.in](https://data.gov.in)), but only as large raw tables. This project turns that data into
answers to three practical questions:

| Question | What the app does |
|---|---|
| **What are prices today?** | Shows the latest daily bulletin, filterable by crop and district. |
| **Where should I sell?** | Ranks nearby mandis by what the farmer actually **takes home** after transport, market fee and loading charges, not by the headline price. A mandi further away is only worth it if the extra price covers the extra cost. |
| **Tell me when the price is right.** | A farmer sets an alert such as *"Tomato above ₹2,600 within 100 km"* and receives an SMS in their language when a new bulletin matches. |

## 2. Features

- **Daily price bulletin** with crop and district filters, and a "moved most this week" summary.
- **Best-market calculator** that compares net earnings per mandi and shows how much extra the farmer earns compared with the nearest mandi.
- **Price trends** with a chart, 7-day moving average, week-on-week change and a simple 7-day projection.
- **Farmer registration and price alerts**, with SMS in English, Telugu (తెలుగు) or Hindi (हिन्दी).
- **Three ways to load prices**: live from the data.gov.in API, CSV upload, or generated sample data.
- **Data cleaning**: bad rows (wrong dates, non-numeric prices, impossible values) are skipped, and the reasons are recorded in the import history.
- **Safe re-imports**: importing the same day again updates prices instead of duplicating them, and never texts a farmer twice for the same bulletin.
- **Admin area** behind a login for importing data, viewing sent SMS and editing mandi details.
- **JSON API** for prices and best-market results, with standard error responses.
- **Health endpoint** that also reports when prices were last imported.
- **Works offline**: the first start loads sample data, so every screen works without internet or an API key.

## 3. Tech stack

The project deliberately sticks to **core Java and the Spring ecosystem**. There is no separate frontend framework, message broker or external database.

| Area | Used |
|---|---|
| Language | Java 21 (records, switch expressions, pattern matching, virtual threads) |
| Framework | Spring Boot 4.1 (Spring Framework 7) |
| Web | Spring MVC, Thymeleaf server-rendered pages |
| Data | Spring Data JPA (Hibernate), H2 embedded database stored in a local file |
| Validation | Jakarta Bean Validation, including a custom cross-field constraint |
| Security | Spring Security: form login, role-based access, CSRF protection, BCrypt |
| HTTP client | Spring `RestClient` with timeouts and Spring's built-in `@Retryable` |
| Scheduling and events | `@Scheduled`, `@TransactionalEventListener` |
| Internationalisation | Spring `MessageSource` (SMS text in 3 languages) |
| Monitoring | Spring Boot Actuator with a custom health indicator |
| Testing | JUnit 5, AssertJ, Mockito, `@SpringBootTest`, `@WebMvcTest`, `@DataJpaTest`, Spring Security Test |
| Build | Maven (bundled through the Maven Wrapper, no install needed) |
| Other | Apache Commons CSV for uploads, Chart.js (from a CDN) for the trend chart |

## 4. Quick start

For readers who already have Java 21:

```bash
git clone https://github.com/<your-username>/crop-price-alert-system.git
cd crop-price-alert-system
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

Open **http://localhost:8080**. The admin login is `admin` / `admin123`.

## 5. Step-by-step setup

### 5.1 Requirements

| Requirement | Notes |
|---|---|
| **JDK 21 or newer** | The only thing you must install. Any distribution works (Oracle JDK, Eclipse Temurin, Microsoft Build of OpenJDK, Amazon Corretto). |
| Internet on the **first** run | The Maven Wrapper downloads Maven and the project's libraries once. After that the app runs offline. |
| Maven | **Not** needed. The project includes the Maven Wrapper (`mvnw` / `mvnw.cmd`). |
| Database | **Not** needed. H2 runs inside the application and stores data in a `data/` folder. |
| An IDE | Optional. IntelliJ IDEA, Eclipse or VS Code with Java extensions can open it as a Maven project. |

### 5.2 Check your Java version

```bash
java -version
```

The output must show version **21** or higher, for example `java version "21.0.4"`.
If it shows an older version or "command not found", install JDK 21 (for example from
https://adoptium.net) and open a **new** terminal.

If several JDKs are installed, point `JAVA_HOME` at JDK 21:

```powershell
# Windows PowerShell (current window only)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
```
```bash
# macOS / Linux
export JAVA_HOME=$(/usr/libexec/java_home -v 21)   # macOS
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk      # Linux (path may differ)
```

### 5.3 Get the code

Either clone with Git:

```bash
git clone https://github.com/<your-username>/crop-price-alert-system.git
cd crop-price-alert-system
```

or, on the GitHub page, click **Code → Download ZIP**, extract it, and open a terminal inside the
extracted folder (the one that contains `pom.xml`).

### 5.4 Run the application

| OS | Command |
|---|---|
| Windows (PowerShell or Command Prompt) | `mvnw.cmd spring-boot:run` |
| macOS / Linux | `./mvnw spring-boot:run` |

On macOS/Linux, if you get "permission denied", run `chmod +x mvnw` once.

The first run takes a few minutes while dependencies download. The app is ready when the log shows:

```
Started MandiWatchApplication in ... seconds
```

followed by the sample data loading:

```
Empty database, loading sample Telangana data (60 days)
Import from Sample data (generated): read=..., saved=..., skipped=0
```

Open **http://localhost:8080** in your browser. To stop the app, press `Ctrl + C` in the terminal.

### 5.5 Run from an IDE (optional)

1. Open the project folder as a **Maven project**.
2. Make sure the project SDK is **Java 21**.
3. Run the `main` method in `src/main/java/com/mandiwatch/MandiWatchApplication.java`.

### 5.6 Build a runnable JAR (optional)

```bash
./mvnw clean package            # Windows: mvnw.cmd clean package
java -jar target/mandiwatch-1.0.0.jar
```

`package` also runs all tests. Add `-DskipTests` to skip them.

### 5.7 Start with a fresh database

All data is stored in the `data/` folder next to `pom.xml`. To reset everything, stop the app and delete that folder:

```powershell
Remove-Item -Recurse -Force data      # Windows PowerShell
```
```bash
rm -rf data                           # macOS / Linux
```

Sample data loads again on the next start.

## 6. Using the application

### 6.1 Pages

| Page | URL | Who can open it | What it shows |
|---|---|---|---|
| Today's prices | `/` | Everyone | Latest bulletin with crop/district filters and the weekly movers |
| Where to sell | `/best-mandi` | Everyone | Net take-home per mandi for a crop, village and quantity |
| Trends | `/trends` | Everyone | Price chart, moving average, weekly change, projection |
| Farmers & alerts | `/farmers`, `/farmers/{id}` | Everyone | Register farmers, add, pause or delete price alerts |
| Mandis | `/mandis` | Everyone can view, **admin** can save | Market list; set coordinates and market fee |
| Import data | `/import` | **Admin** | CSV upload, API fetch, import history |
| SMS outbox | `/notifications` | **Admin** | Every message sent, newest first, 25 per page |
| Login / logout | `/login`, `/logout` | Everyone | Spring Security's built-in login and logout pages |

### 6.2 Admin login

| | Default | Change with environment variable |
|---|---|---|
| Username | `admin` | `MANDI_ADMIN_USER` |
| Password | `admin123` | `MANDI_ADMIN_PASSWORD` |

The default password is for local use only. Set your own before running the app anywhere others can reach it:

```powershell
$env:MANDI_ADMIN_PASSWORD = "choose-a-strong-one"; .\mvnw.cmd spring-boot:run
```

### 6.3 Viewing the database (H2 console)

1. Log in as admin.
2. Open **http://localhost:8080/h2-console**.
3. Enter:
   - **JDBC URL:** `jdbc:h2:file:./data/mandiwatch;AUTO_SERVER=TRUE` (exactly as in `application.yml`)
   - **User Name:** `sa`
   - **Password:** *(leave empty)*
4. Click **Connect**. Main tables: `DAILY_PRICE`, `MANDI`, `COMMODITY`, `FARMER`, `PRICE_ALERT`, `NOTIFICATION`, `IMPORT_RUN`.

### 6.4 About the sample data and SMS

- The sample data covers **Telangana**: 14 mandis, 12 villages, 10 crops and 60 days of prices. The prices are **generated** (see `SampleDataService`) so the screens behave like they would on live data. They are not real prices.
- SMS messages are **not sent to real phones**. Real SMS in India needs a paid gateway and DLT template registration, so the app prints each message to the console (`SMS -> +91 ...`) and saves it in the SMS outbox. Plugging in a real gateway means writing one class that implements the `SmsSender` interface.

## 7. Demo walkthrough (5 minutes)

1. **Today's prices** (`/`): filter by crop or district. Point out the "moved most this week" box.
2. **Where to sell** (`/best-mandi`): pick *Tomato*, village *Chevella*, *20* quintals. The top result is often **not** the mandi with the highest price. The page also shows how much more the farmer earns than at the nearest mandi.
3. **Trends** (`/trends`): Tomato, 60 days. Shows the 7-day average, week-on-week change and a rough projection.
4. **Farmers & alerts** (`/farmers`): register a farmer (any 10-digit number starting with 6–9, language *Telugu*). Open the farmer and add an alert: *Tomato, goes above, ₹2,600, 100 km*.
5. **Import data** (`/import`, log in as admin): click **"ready-made next-day bulletin"** to download a demo CSV, then upload it with **Upload & import**. The message shows how many rows were imported and how many SMS were sent.
6. **SMS outbox** (`/notifications`): the Telugu message is listed. It is also printed in the terminal.
7. Upload **the same file again**: the rows are updated, not duplicated, and the farmer is **not** texted a second time.
8. Optional: open `/api/best-mandi?commodity=Tomato&villageId=1` to see the same ranking as JSON.

## 8. Configuration

All settings are in `src/main/resources/application.yml`. Any of them can be overridden without
editing the file, using an environment variable (e.g. `SERVER_PORT=9090`) or a command-line
argument (e.g. `--server.port=9090`).

| Setting | Default | Meaning |
|---|---|---|
| `server.port` | `8080` | Web port |
| `spring.datasource.url` | `jdbc:h2:file:./data/mandiwatch` | Where the H2 database file is stored |
| `mandi.admin.username` / `password` | `admin` / `admin123` | Admin login (env: `MANDI_ADMIN_USER`, `MANDI_ADMIN_PASSWORD`) |
| `mandi.api.key` | *(empty)* | data.gov.in API key (env: `DATA_GOV_IN_KEY`). Leave empty to use sample data and CSV only |
| `mandi.api.state` | `Telangana` | State to fetch from the API |
| `mandi.api.scheduled` | `false` | `true` = fetch prices automatically every day |
| `mandi.api.cron` | `0 30 19 * * *` | When the daily fetch runs (19:30 India time) |
| `mandi.api.connect-timeout` / `read-timeout` | `5s` / `30s` | HTTP timeouts for the government API |
| `mandi.transport.rate-per-km-per-quintal` | `1.0` | Default transport cost in ₹ per km per quintal |
| `mandi.transport.road-factor` | `1.3` | Straight-line distance × this ≈ road distance |
| `mandi.transport.hamali-per-quintal` | `30` | Loading/unloading labour charge in ₹ per quintal |
| `mandi.transport.freshness-days` | `3` | Ignore a mandi's price if it is older than this many days |
| `mandi.sample.enabled` / `days` | `true` / `60` | Load generated sample data into an empty database |

The `mandi.*` settings are validated at startup (for example `page-size` must be positive), so a
wrong value stops the app with a clear message instead of failing later.

## 9. Using real government data

1. Register at [data.gov.in](https://data.gov.in) and generate a free API key (**My Account → Generate API Key**).
2. Start the app with the key in an environment variable:

   ```powershell
   # Windows PowerShell
   $env:DATA_GOV_IN_KEY = "your-key"; .\mvnw.cmd spring-boot:run
   ```
   ```bash
   # macOS / Linux
   DATA_GOV_IN_KEY=your-key ./mvnw spring-boot:run
   ```
3. Log in as admin, go to **Import data** and click **Fetch now**.

Instead of the API you can download the CSV from the dataset page on data.gov.in and upload it on
the same screen. The expected columns are `State, District, Market, Commodity, Variety,
Arrival_Date, Min_Price, Max_Price, Modal_Price`. Small spelling variations in headers are accepted.

Mandis that arrive from the real feed have no location. Add their latitude and longitude on the
**Mandis** page so they are included in "Where to sell" and in alerts.

To fetch automatically every evening, set `mandi.api.scheduled: true`.

> Never commit a real API key. Always pass it through the `DATA_GOV_IN_KEY` environment variable.

## 10. JSON API

All endpoints are public, read-only `GET` requests.

| Endpoint | Example | Returns |
|---|---|---|
| `/api/prices` | `/api/prices?commodity=Tomato&district=Hyderabad` | Prices for the latest day, or for a given day with `&date=2026-10-03` |
| `/api/commodities` | `/api/commodities` | List of crop names |
| `/api/best-mandi` | `/api/best-mandi?commodity=Tomato&villageId=1&quantity=20&rate=1.0&radius=150` | Mandis ranked by net take-home, with the full cost breakdown |

Parameters are validated (for example `quantity` must be greater than 0, `radius` between 1 and 500).
Errors use the standard **Problem Details** format (RFC 9457) with content type `application/problem+json`:

```json
{
  "title": "Not Found",
  "status": 404,
  "detail": "Commodity 'Xyz' not found",
  "instance": "/api/prices"
}
```

Health check: `GET /actuator/health`. Anyone sees the overall status; the admin also sees details,
including when prices were last imported (`lastImport`).

## 11. How the take-home amount is calculated

```
sale value  = modal price × quantity
transport   = road km × rate (₹/km/quintal) × quantity
market fee  = sale value × mandi fee %
hamali      = ₹30 × quantity
take-home   = sale value − transport − market fee − hamali
```

**Example:** 20 quintals of tomato at ₹2,000/quintal, 50 km away, transport ₹1/km/quintal, 1% fee:

| | Amount |
|---|---|
| Sale value | 2,000 × 20 = ₹40,000 |
| Transport | 50 × 1 × 20 = ₹1,000 |
| Market fee | 1% of 40,000 = ₹400 |
| Hamali | 30 × 20 = ₹600 |
| **Take-home** | **₹38,000** |

Road distance is estimated as the straight-line (haversine) distance × 1.3. For each mandi the
newest price within the last 3 days of the latest bulletin is used, because not every mandi reports
every crop every day. All these defaults can be changed (see [Configuration](#8-configuration)).

## 12. Architecture

The code follows a classic layered structure:

```
Browser / API client
        │
        ▼
web/          Controllers, form objects, JSON API, API error handling
        │
        ▼
service/      Business logic and transactions
        │
        ▼
repository/   Spring Data JPA interfaces and query result records
        │
        ▼
domain/       JPA entities  ──►  H2 database (data/ folder)
```

**How a price import flows:**

```
data.gov.in API ─┐
CSV upload ──────┼──► RawPriceRow ──► PriceImportService ──► database
Sample data ─────┘                     (validate, clean,          │
                                        upsert)                    │ after commit:
                                                                   ▼ PricesImportedEvent
                                                            AlertService
                                                     (match alerts, send SMS,
                                                      save to outbox)
```

**Package layout:**

```
src/main/java/com/mandiwatch
├── MandiWatchApplication.java         entry point
├── config/
│   ├── MandiProperties.java           typed, validated settings from application.yml
│   ├── SecurityConfig.java            access rules and the admin user
│   ├── JpaConfig.java                 automatic created-date timestamps
│   └── AppConfig.java                 Clock bean, enables @Retryable
├── domain/                            JPA entities: Mandi, Commodity, DailyPrice, Village,
│                                      Farmer, PriceAlert, Notification, ImportRun
├── repository/                        Spring Data interfaces + query result records
├── service/
│   ├── RawPriceRow.java               one incoming price row, from any source
│   ├── CsvPriceParser.java            CSV upload → rows
│   ├── DataGovClient.java             data.gov.in API → rows (paged, with retries)
│   ├── CommodityNormalizer.java       "Paddy(Dhan)(Common)" → "Paddy"
│   ├── PriceImportService.java        validate, clean and save; publishes PricesImportedEvent
│   ├── AlertService.java              checks alerts after commit, sends SMS
│   ├── FarmerService / MandiService   use cases behind the controllers
│   ├── BestMandiService.java          take-home ranking
│   ├── TrendService.java              moving average, weekly change, projection
│   ├── MarketSummaryService.java      "moved most this week"
│   ├── SampleDataService.java         sample data and the demo bulletin
│   ├── LastImportHealthIndicator.java "lastImport" in /actuator/health
│   └── SmsSender / ConsoleSmsSender   SMS abstraction; console implementation
└── web/                               MVC controllers, forms, validators, JSON API

src/main/resources
├── application.yml                    configuration
├── messages*.properties               SMS text in English, Telugu, Hindi
├── templates/                         Thymeleaf pages
└── static/css/style.css

src/test/java/...                      tests (see section 14)
src/test/resources/application-test.yml settings used by the tests
```

## 13. Design decisions

- **One import path.** API, CSV and sample data are all converted to `RawPriceRow` and go through `PriceImportService`, so the cleaning rules live in one place.
- **Clean the data, don't trust the feed.** Rows with a bad date, a non-numeric price, a modal price outside the min–max range, or an impossible value are skipped. The reason is stored in the import history.
- **Upsert on (mandi, crop, date).** A unique constraint and an update-or-insert make re-importing the same day safe. Existing rows for the file's date range are loaded with one query up front instead of one query per row. New rows use sequence-generated ids so Hibernate can send inserts in batches.
- **Alerts only after the data is saved.** `AlertService` listens with `@TransactionalEventListener`, both after an import and after a farmer adds or resumes an alert. An SMS is therefore never sent for data that was rolled back. `lastTriggeredFor` prevents repeat messages for the same bulletin.
- **No duplicate SMS when two imports overlap.** Each alert that fires is handled in its own transaction: mark it as sent, flush (a versioned update protected by `@Version`), and only then send the SMS. If two imports race, the second update fails and rolls back before anyone is texted.
- **No database access from templates.** `spring.jpa.open-in-view` is off. Services load everything a page needs up front (`join fetch`, `@EntityGraph`), so rendering a page never triggers hidden queries.
- **Thin controllers.** Controllers handle HTTP and validation. Transactions and business rules live in services.
- **SMS behind an interface.** `SmsSender` hides the delivery channel. A real gateway or WhatsApp sender would be one additional class.
- **Testable time.** Code reads "today" from an injected `Clock` (India time) instead of the server clock, so tests can fix the date.

## 14. Tests

Run all tests:

```bash
./mvnw test                     # Windows: mvnw.cmd test
```

Expected result: `Tests run: 51, Failures: 0, Errors: 0` and `BUILD SUCCESS`.

| Kind | Test class | What it checks |
|---|---|---|
| Plain unit tests | `BestMandiServiceTest`, `TrendServiceTest`, `GeoUtilsTest`, `CommodityNormalizerTest`, `CsvPriceParserTest`, `RawPriceRowTest` | Business calculations and parsing, no Spring needed |
| Mockito | `AlertServiceTest` | Alert matching rules; the alert is marked before the SMS is sent; a concurrent update means no SMS |
| Bean Validation | `MandiFormValidationTest` | Form rules and the custom `@BothCoordinatesOrNone` constraint (parameterized test) |
| `@DataJpaTest` | `DailyPriceRepositoryTest` | Custom JPQL queries against a real embedded database |
| `@WebMvcTest` | `FarmerControllerTest` | Form binding, validation errors, redirects and 404, with services mocked by `@MockitoBean` |
| `@SpringBootTest` + MockMvc | `SecurityAndApiTest` | Access rules, CSRF, login, API error format, health endpoint |
| `@SpringBootTest` | `MandiWatchApplicationTests` | End to end: sample data loads, a farmer sets an alert, a bulletin is uploaded, exactly one Telugu SMS is sent, a re-upload sends none |

Tests that start Spring use the `test` profile (`src/test/resources/application-test.yml`): an
in-memory database and 20 days of sample data. They never touch your `data/` folder.

## 15. Troubleshooting

| Problem | Fix |
|---|---|
| `java: command not found`, or an error mentioning `release 21` | Install JDK 21 and open a new terminal; check with `java -version` (see 5.2). |
| `Port 8080 was already in use` | Stop the other program, or start on another port: `mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=9090"` (macOS/Linux: `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=9090`). |
| `./mvnw: Permission denied` (macOS/Linux) | Run `chmod +x mvnw` once. |
| `./mvnw` fails with `/bin/sh^M: bad interpreter` | The file got Windows line endings. Run `sed -i 's/\r$//' mvnw` or re-clone the repository. |
| Dependency download fails on first run | Check your internet connection or proxy, then run the command again. |
| Uploading prices fails with `Unique index or primary key violation` on `DAILY_PRICE` | The `data/` folder was created by an older version of the app. Stop the app and delete `data/` (see 5.7). |
| `/import` keeps sending you to the login page | Those pages are admin-only. Log in with the admin user (see 6.2). |
| "Where to sell" shows no result for some mandis | Mandis without coordinates are skipped. Add latitude/longitude on the Mandis page. |
| "Fetch now" says no API key is set | Set `DATA_GOV_IN_KEY` before starting the app (see section 9), or use CSV upload. |
| The trend chart doesn't appear | Chart.js loads from a CDN, so that page needs internet. All other pages work offline. |
| Telugu/Hindi text shows as `????` in the Windows terminal | Only the console display is affected; the pages and the SMS outbox show it correctly. Use Windows Terminal or run `chcp 65001` first. |

## 16. Glossary

| Term | Meaning |
|---|---|
| **Mandi** | A government-regulated wholesale market where farmers sell produce (APMC market). |
| **Quintal (qtl)** | 100 kg. Indian mandi prices are quoted in ₹ per quintal. |
| **Min / Max / Modal price** | The lowest, highest and most common price a crop sold for that day. The app uses the modal price. |
| **Hamali** | Charge for loading and unloading labour at the market. |
| **Market fee** | Percentage of the sale value charged by the mandi (fee plus commission). |
| **Bulletin** | The daily list of prices published for all mandis. |
| **Agmarknet** | The government's agricultural marketing information network that publishes these prices. |
| **DLT** | India's registration system for commercial SMS senders and message templates. |

## 17. Possible extensions

- Real SMS or WhatsApp delivery through a gateway
- Login for farmers using a one-time password (OTP)
- Database migrations with Flyway instead of `ddl-auto: update`
- Caching the latest-prices lookup with `@Cacheable`, cleared when an import finishes
- PostgreSQL instead of H2 for a hosted deployment
- Real road distances from a routing service instead of the 1.3 factor
