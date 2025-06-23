# ⛽ TankYou

**Fuel tracking made effortless.**

*Mobile Application Programming 2024/2025 Project @UniBO Cesena*

<p align="center">
  <img src="https://img.shields.io/badge/Android-API%2026+-brightgreen" alt="Android API">
  <img src="https://img.shields.io/badge/Kotlin-1.9.20-blue" alt="Kotlin">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Latest-orange" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/License-IODL%202.0-yellow" alt="License">
</p>

---

## 📱 Overview

TankYou is a modern Android application designed to simplify fuel tracking and gas station discovery across Italy.
Built with Jetpack Compose and Material 3, it is powered by real-time government data, providing users with comprehensive fuel price information, advanced search capabilities and personalized tracking features.

---

## ✨ Key Features

### 🗺️ Interactive Map
- **Real-time Gas Station Locations**: View all active gas stations in Italy on an interactive OpenStreetMap
- **Smart Search**: Search for stations by name, location, or apply advanced filters
- **Location Services**: Find nearby stations with GPS integration
- **Station Details**: View comprehensive information including fuel prices, services, and operating hours

### 🔍 Advanced Filtering
- **Fuel Type Filters**: Filter by gasoline, diesel, LPG, methane, and more
- **Service Filters**: Find stations with specific services (24h, car wash, etc.)
- **Brand Filters**: Search by specific gas station brands
- **Price Sorting**: Sort results by fuel prices to find the best deals

### 👤 User Management
- **Secure Authentication**: Register and login with email verification via Supabase Auth
- **Guest Mode**: Use the app without registration for basic features
- **Profile Management**: Update personal information and profile pictures
- **Saved Stations**: Bookmark favorite gas stations for quick access

### ⚙️ Customization
- **Multi-language Support**: Available in multiple languages
- **Theme Selection**: Choose from different visual themes
- **Settings Management**: Customize map behavior and location preferences

---

## 🏗️ Architecture

TankYou follows modern Android development practices with a clean, layered architecture:

```
app/
├── data/
│   ├── database/
│   │   ├── entities/          # Data models (User, GasStation, Fuel, etc.)
│   │   ├── auth/              # Authentication ViewModels and states
│   │   └── model/             # Business logic ViewModels
│   └── repositories/          # Data access layer (Auth, User, App repositories)
├── ui/
│   ├── components/            # Reusable UI components
│   ├── screens/               # Screen composables (Map, Profile, Settings, etc.)
│   ├── navigation/            # Navigation configuration
│   └── theme/                 # App theming and design system
└── utils/                     # Utility classes and constants
└── MainActivity.kt            # Application Entry Point
```

---

## 🛠️ Technologies Used

### Core Framework
- **Kotlin**: Primary programming language
- **Jetpack Compose**: Modern declarative UI toolkit
- **Android SDK**: Target API 35, Minimum API 26

### Backend & Database
- **Supabase**: Backend-as-a-Service platform
  - **PostgreSQL**: Relational database via Supabase
  - **Supabase Auth**: User authentication and authorization
  - **Supabase Storage**: Profile image storage
  - **Realtime**: Live data synchronization

### Maps & Location
- **OSMDroid**: OpenStreetMap integration for Android
- **OSMBonuspack**: Additional mapping utilities
- **Android Location Services**: GPS and location tracking

### Data Processing
- **OpenCSV (python)**: CSV parsing for government data
- **Kotlinx Serialization**: JSON serialization/deserialization
- **Kotlinx Datetime**: Date and time handling

### Architecture Components
- **Navigation Compose**: Type-safe navigation
- **Room**: Local database for caching
- **Lifecycle ViewModels**: State management
- **Coroutines**: Asynchronous programming and processing

### Networking
- **Ktor Client**: HTTP client for API calls
- **OkHttp**: HTTP networking

### UI & UX
- **Material Design 3**: Google's latest design system
- **Coil**: Image loading and caching
- **Material Icons Extended**: Material 3 icon set

---

## 📊 Data Sources

All fuel price and gas station data is sourced from official Italian government open datasets:

### Government Data Sources
- **Main Dataset**: [Carburanti - Prezzi praticati e anagrafica degli impianti](https://www.mimit.gov.it/index.php/it/open-data/elenco-dataset/carburanti-prezzi-praticati-e-anagrafica-degli-impianti)
- **Authority**: [MIMIT](https://www.mimit.gov.it/) - *Ministero delle Imprese e del Made in Italy*
- **License**: [IODL 2.0](https://www.dati.gov.it/iodl/2.0/) - *Italian Open Data License 2.0*

### Data Files
- **Price Data** (*updated daily at 8:00 AM Italian time*): [prezzo_alle_8.csv](https://www.mimit.gov.it/images/exportCSV/prezzo_alle_8.csv)
- **Gas Station Registry**: [anagrafica_impianti_attivi.csv](https://www.mimit.gov.it/images/exportCSV/anagrafica_impianti_attivi.csv)
- **Metadata Documentation**: [Metadati Prezzi Carburanti](https://www.mimit.gov.it/images/stories/documenti/Metadati_05sett22_prezzi_carburanti.pdf)

### Additional Resources
- **OpenStreetMap Integration**: [OSMDroid](https://github.com/osmdroid/osmdroid/) - Apache 2.0 License

> [!NOTE]
> The data is parsed by our python script and then loaded on our database. We do not use external APIs.

---

## 🔧 System Requirements

### Requirements
- **Android Version**: API 26 (Android 8.0 Oreo) or more
- **Storage**: at least 30MB for the app installation (and some more for the cache data)
- **Network**: Internet connection required for real-time data

---

## 🚀 Getting Started

### Prerequisites
1. Clone the repository
2. Create a `local.properties` file in the root directory
3. Add our Supabase credentials (currently not available to the public):
   ```properties
   database.url=your_supabase_url
   database.key=your_supabase_anon_key
   ```

### Build Configuration
The app uses Kotlin DSL for build configuration and requires the Supabase credentials to be properly configured in `local.properties`.

> [!CAUTION]
> Without the local.properties file, or without the right data, the application build process will fail!

---

## 📱 App Screens

- **Map Screen**: Interactive map with gas station locations and search functionality
- **Login / Register**: Secure user authentication with email verification
- **Profile Screen**: User profile management and saved stations
- **Settings Screen**: App customization and preferences

---

## 👨‍💻 Authors

### Marco Marrelli
- **GitHub**: [@marcomarrelli](https://github.com/marcomarrelli)
- **LinkedIn**: [Marco Marrelli](https://www.linkedin.com/in/marcomarrelli/)
- **Academic Email**: [marco.marrelli@studio.unibo.it](mailto:marco.marrelli@studio.unibo.it)

### Margherita Zanchini
- **GitHub**: [@margheritazanchini](https://github.com/margheritazanchini)
- **LinkedIn**: [Margherita Zanchini](https://www.linkedin.com/in/margherita-zanchini/)
- **Academic Email**: [margherita.zanchini@studio.unibo.it](mailto:margherita.zanchini@studio.unibo.it)

---

## 📄 License

This project uses data licensed under the [Italian Open Data License 2.0 (IODL 2.0)](https://www.dati.gov.it/iodl/2.0/) from the Italian Ministry of Business and Made in Italy.

The OpenStreetMap integration is provided through OSMDroid, licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).

---

## 🎓 Academic Context

This project was developed as part of the Mobile Application Programming course for the 2024/2025 academic year at the University of Bologna, Cesena Campus.

---

*Made with ❤️ for fuel-conscious drivers across Italy*
