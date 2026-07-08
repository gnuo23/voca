# Voca Android

Native Android app for Voca. It talks to the Spring Boot backend API directly; it does not wrap the web frontend in a WebView.

## Current Native Flows

- Login and register
- Dashboard metrics
- Normal decks and difficult decks
- Deck detail with vocabulary list
- Import vocabulary into a deck
- Learn session with MCQ and written answers
- Review cards with Again, Hard, Good, Easy
- Difficult word list and difficult deck creation
- Class list, create class, join class
- Profile and sign out

## Build APK

```bash
cd android
gradle :app:assembleDebug
```

The debug APK is created at:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

## API URL

The default backend URL is configured in [gradle.properties](gradle.properties):

```properties
vocaApiUrl=http://52.220.241.27:8080
```

For production with a domain, change it to HTTPS:

```properties
vocaApiUrl=https://api.dungne.io.vn
```

If you keep using HTTP, also update `app/src/main/res/xml/network_security_config.xml`.
