# Tanzania Forex Pro

**Tanzania Forex Pro** is a comprehensive, real-time currency exchange application designed specifically for Tanzanian users. Track live forex rates, convert currencies, and monitor market trends with an elegant, user-friendly interface.

## Features

### Live Exchange Rates
- Real-time forex rates for 14+ currencies
- Automatic updates (configurable intervals: 1-30 minutes)
- Support for major world currencies (USD, EUR, GBP, JPY, CNY, INR, AED, ZAR)
- East African regional currencies (KES, UGX, RWF)
- Precious metals pricing (Gold XAU, Silver XAG)

### Smart Currency Converter
- Bi-directional conversion between any supported currencies
- Instant calculations with live market rates
- Quick swap functionality
- Clean, intuitive interface
- Support for decimal precision

### Market Insights
- 7-day USD/TZS trend visualization
- Change indicators (percentage & absolute values)
- Bank rate comparisons for major Tanzanian banks
- Detailed currency information on tap

### Customization
- Configurable auto-refresh intervals
- Toggle auto-refresh on/off
- Dark mode optimized design
- Offline mode with cached rates

### Beautiful UI/UX
- Modern, premium dark theme
- Gradient backgrounds and smooth animations
- Color-coded rate changes (green/red indicators)
- Responsive and fast performance
- Flag emojis for easy currency identification

## Technology Stack

- **Language**: Java
- **Platform**: Android (API 21+)
- **Architecture**: Single Activity with dynamic UI generation
- **Threading**: ExecutorService for background tasks
- **Data Persistence**: SharedPreferences
- **Network**: HttpURLConnection
- **Data Format**: JSON parsing

## API Integration

### ExchangeRate-API (v6)
- **Endpoint**: `API Key: 56bff02e7e890d6fae47bb57
Example Request: https://v6.exchangerate-api.com/v6/56bff02e7e890d6fae47bb57`
- **Purpose**: Real-time currency exchange rates
- **Update Frequency**: Configurable (1-30 minutes)

### MetalPriceAPI
- **Endpoint**: `https://api.metalpriceapi.com/v1/latest
  ?api_key=28b227b94a7053b0c52456cd3f453c09
  &base=USD
  &currencies=EUR,XAU,XAG`
- **Purpose**: Gold and Silver prices
- **Currencies**: XAU (Gold), XAG (Silver)

## Installation

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK API 21+
- Java Development Kit (JDK) 8+

### Building from Source

1. **Clone the repository**
   ```bash
   git clone https://github.com/Willykez/TanzRate-Pro.git
   cd tanzania-forex-pro
   ```

2. **Open in Android Studio**
   - File → Open → Select project directory

3. **Add API Keys** (if required)
   - Create `local.properties` file
   - Add your API keys:
     ```properties
     EXCHANGE_API_KEY=56bff02e7e890d6fae47bb57
     METAL_API_KEY=28b227b94a7053b0c52456cd3f453c0ourusername/4. **Build the project**
   - Build → Make Project (Ctrl+F9)

5. **Run on device/emulator**
   - Run → Run 'app' (Shift+F10)

### Install APK
Download the latest APK from [Releases](https://github.com/Willykez/TanzRate-Pro/releases)

## Supported Currencies

| Currency | Code | Name |
|----------|------|------|
| 🇹🇿 | TZS | Tanzanian Shilling |
| 🇺🇸 | USD | US Dollar |
| 🇪🇺 | EUR | Euro |
| 🇬🇧 | GBP | British Pound |
| 🇯🇵 | JPY | Japanese Yen |
| 🇨🇳 | CNY | Chinese Yuan |
| 🇮🇳 | INR | Indian Rupee |
| 🇦🇪 | AED | UAE Dirham |
| 🇿🇦 | ZAR | South African Rand |
| 🇰🇪 | KES | Kenyan Shilling |
| 🇺🇬 | UGX | Ugandan Shilling |
| 🇷🇼 | RWF | Rwandan Franc |
| 💰 | XAU | Gold (per oz) |
| 💎 | XAG | Silver (per oz) |

## Development

### Project Structure
```
TanzaniaForexApp/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/tz/forexapp/
│   │   │   │   └── TanzaniaForexApp.java
│   │   │   ├── res/
│   │   │   │   ├── values/
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   └── strings.xml
│   │   │   └── AndroidManifest.xml
├── README.md
└── LICENSE
```

### Key Classes

**TanzaniaForexApp.java**
- Main Activity class
- UI generation and management
- API integration
- Data persistence
- Background threading

### Code Highlights

```java
// Live data fetching
private void fetchExchangeRates() {
    executor.execute(() -> {
        // API call implementation
        // Rate calculations
        // UI updates on main thread
    });
}

// Currency conversion logic
private void performConversion() {
    // Handles TZS and non-TZS conversions
    // Supports bidirectional conversion
}

// Data persistence
private void saveRates() {
    // Saves to SharedPreferences
    // Enables offline functionality
}
```

## Configuration

### Refresh Intervals
```java
// Available options
1 minute  = 60000 ms
5 minutes = 300000 ms (default)
10 minutes = 600000 ms
30 minutes = 1800000 ms
```

### Color Scheme
```java
PRIMARY_BG = "#0A0E27"     // Dark blue background
CARD_BG = "#1A1F3A"        // Card background
ACCENT_GOLD = "#FFD700"    // Gold highlights
ACCENT_GREEN = "#4CAF50"   // Positive changes
ACCENT_RED = "#F44336"     // Negative changes
ACCENT_BLUE = "#2196F3"    // Interactive elements
```

## Permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## Known Issues

- Bank rates are simulated estimates based on market rates with typical spreads
- Historical trend data is generated based on current rates (not actual historical data)
- Metal prices may occasionally fail to fetch; fallback values are used

## Roadmap

- [ ] Add more currencies (BTC, ETH, etc.)
- [ ] Historical rate charts (30 days, 90 days, 1 year)
- [ ] Rate alerts and notifications
- [ ] Widget support for home screen
- [ ] Multi-language support (Swahili, English)
- [ ] Calculator mode for complex conversions
- [ ] Export rate history to CSV
- [ ] Share rates via social media

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Contribution Guidelines
- Follow Java coding conventions
- Add comments for complex logic
- Test on multiple Android versions
- Update README if adding features
- Ensure no API keys are committed

## License

This project is licensed under the MIT License - see the LICENSE file for details.

```
MIT License

Copyright (c) 2025 Tanzania Forex Pro

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Author

**Your Name**
- GitHub: [@willykez](https://github.com/willykez)
- Email: willykez01@gmail.com

## Acknowledgments

- [ExchangeRate-API](https://www.exchangerate-api.com/) for currency data
- [MetalPriceAPI](https://metalpriceapi.com/) for precious metals pricing
- Bank of Tanzania for financial regulations reference
- The Android development community

## Disclaimer

**Tanzania Forex Pro** provides exchange rate information for reference purposes only. The rates displayed are obtained from third-party APIs and may not reflect the exact rates offered by banks, forex bureaus, or other financial institutions.

**Important Notes:**
- Always verify exchange rates with your bank or authorized dealer before making transactions
- Past performance and historical data do not guarantee future rates
- The app is not liable for any financial losses incurred based on the information provided
- Not affiliated with any bank or financial institution
- Use at your own risk

For official exchange rates, please consult:
- Bank of Tanzania: https://www.bot.go.tz
- Your commercial bank
- Licensed forex bureaus

## Support

For support, bug reports, or feature requests:
- **Email**: willykez@gmail.com
- **Issues**: [GitHub Issues](https://github.com/Willykez/TanzRate-Pro/issues)

---

**Made with ❤️ in Tanzania 🇹🇿**

*If you find this app useful, please consider giving it a ⭐ on GitHub!*
