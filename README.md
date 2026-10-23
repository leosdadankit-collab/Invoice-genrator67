# Bharat Bill — Indian Invoice Maker & GST Billing Android App

A production-quality, fully offline native Android application designed specifically for Indian merchants, shopkeepers, wholesalers, retailers, freelancers, and small businesses.

---

## Key Features

1. **Native Indian GST Calculation Engine**
   - Intra-state (CGST + SGST) vs Inter-state (IGST) automatic place-of-supply determination.
   - Comprehensive tax rates: 0%, 5%, 12%, 18%, 28%, and custom rates.
   - Tax-exclusive and tax-inclusive pricing options.
   - Item-level and invoice-wide discounts (percentage or fixed amount).
   - Compensation Cess support.
   - Round-off to the nearest rupee (+/-).
   - Real-time conversion of invoice amounts to Indian Words ("Rupees Twelve Thousand Five Hundred Only").

2. **Native Android Printing & PDF Export**
   - Native Android `PrintManager` integration for printing directly to connected WiFi/Bluetooth/network printers or Android Print Services.
   - Pixel-perfect, high-resolution vector A4 invoice rendering via `android.graphics.pdf.PdfDocument`.
   - 3-Inch (80mm) Thermal POS Receipt format for retail shop counters.
   - Android FileProvider integration for sharing PDF bills directly via WhatsApp, Gmail, or Telegram.

3. **Complete Offline Persistence**
   - Powered by Room SQLite database.
   - Full persistence across app restarts, reboots, and airplane mode.
   - Transactional safety for invoices, items, and inventory stock decrements.
   - Automatic pre-population of starter merchant data for instant evaluation.

4. **Business Profile & Indian Tax Compliance**
   - GSTIN format validation and automatic state code extraction (e.g. `27` for Maharashtra, `24` for Gujarat, `07` for Delhi).
   - Support for Regular GST Registered, Composition Scheme, and Unregistered businesses.
   - Bank details (Account Number, IFSC, Branch) and UPI ID for payment receipts.
   - Interactive on-screen touch digital signature pad for merchant signing.

5. **Customer & Inventory Management**
   - Customers directory with state code, GSTIN, and tracking of pending dues.
   - Products and services with HSN/SAC codes, standard units (PCS, KG, BOX, SET, etc.), stock levels, and pricing.
   - One-click CSV export for Sales Registers, Customer lists, and Product catalogs.

6. **GSTR-1 & Sales Reports**
   - Tax summary breaking down taxable turnover, CGST, SGST, and IGST totals.
   - B2B (with GSTIN) vs B2C retail breakdown.
   - Customer-wise outstanding balances ledger.

---

## Project Structure & Architecture

```
com.example
├── MainActivity.kt                // M3 Edge-to-Edge Navigation shell
├── model
│   ├── IndianStates.kt            // 36 Indian States & UT codes, GSTIN validators, Enums
│   ├── InvoiceEntities.kt         // Room entities (Invoice, Item, Customer, Product, Payment)
│   └── GstCalculationEngine.kt    // Pure, tested calculation math & Indian words converter
├── data
│   ├── InvoiceDao.kt              // Reactive Room DAO with transaction support
│   ├── AppDatabase.kt             // Room database singleton & pre-population
│   └── BillingRepository.kt       // Repository abstraction layer
├── pdf
│   ├── InvoicePdfGenerator.kt     // Native A4 & Thermal POS PDF document rendering
│   ├── InvoicePrinter.kt          // Android PrintManager adapter & share handler
│   └── CsvExporter.kt             // CSV exports for GST filing & bookkeeping
└── ui
    ├── components
    │   ├── CommonComponents.kt   // Dropdowns, Status Badges, Stats cards
    │   └── SignaturePad.kt       // Touch signature canvas component
    ├── screens
    │   ├── DashboardScreen.kt    // Financial overview & quick actions
    │   ├── InvoiceListScreen.kt  // Search, filter tabs, payment recording
    │   ├── CreateEditInvoiceScreen.kt // Comprehensive GST invoice builder
    │   ├── InvoiceDetailScreen.kt// Full invoice preview & print actions
    │   ├── ProductListScreen.kt  // Items catalog & stock management
    │   ├── CustomerListScreen.kt // Customers directory & addresses
    │   ├── ReportsScreen.kt      // GSTR-1 analysis & outstanding dues
    │   └── BusinessProfileScreen.kt // GST, bank, UPI, & signature setup
    ├── theme
    │   ├── Color.kt              // Indian FinTech Navy & Teal theme
    │   ├── Theme.kt              // Material 3 Dynamic / Branded Theme
    │   └── Type.kt               // Typography definitions
    └── viewmodel
        └── BillingViewModel.kt   // MVVM State management
```

---

## How to Open and Build in Android Studio

1. Open Android Studio (Ladybug / Iguana or later).
2. Choose **File > Open** and select this project directory.
3. Allow Gradle to sync.
4. Select `app` in the run configurations dropdown.
5. Click **Run (Shift + F10)** to launch on an Android phone, tablet, or emulator running Android 7.0 (API 24) or higher.
