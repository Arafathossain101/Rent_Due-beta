# Rent Record (Rent Due)

🏠 **Rent Record** is an elder-friendly, high-contrast Android application designed to simplify property management for landlords and property managers. Easily organize properties, track tenants, and manage monthly rental payments—all in one place.

## Features

- **Property & Stories Management**: Organize properties by house name, stories count (1–100), and custom photos. Easily add and navigate between floors and rooms.
- **Room & Tenant Tracking**: Manage room identifiers (e.g., 101, 102), tenant names, and ID document verification photos with built-in preview dialog.
- **Automated Monthly Ledger**: Automatically generates monthly bills (Rent, Gas, Electricity) with recurring support for custom utility bills (Water, Internet, etc.).
- **Smart Payment Distribution**: Easily apply full or specific payments; payments automatically settle previous due balances first, then current monthly bills, with overpayments credited to rent.
- **Activity Log & Audit Trail**: Maintain an activity history log documenting all billing additions, payments, and modifications.
- **Elder-Friendly UI**: High-contrast Material Design 3 layout featuring deep teal branding (`#00695C`), large readable typography, and generous touch targets (56dp+).

## Tech Stack & Architecture

- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose (Material 3)
- **Local Persistence**: Room Database 2.6.1 with Coroutines & StateFlow
- **Image Loading**: Coil 2.7.0
- **Navigation**: Navigation Compose 2.8.4
- **Architecture**: Clean Architecture / MVVM (Model-View-ViewModel)
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`)
