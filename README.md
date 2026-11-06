# Kodi Auto — prototipo 0.1.0

Progetto Android nativo in Kotlin + Jetpack Compose. Questa prima consegna contiene la base grafica navigabile e la pipeline cloud per generare un APK debug.

## Compilazione senza PC
1. Crea un repository GitHub vuoto dal browser del telefono.
2. Carica tutti i file di questa cartella mantenendo le cartelle.
3. Apri la scheda **Actions** del repository.
4. Avvia **Build Kodi Auto APK** con **Run workflow** (oppure attendi il build dopo un push).
5. Quando il job termina, aprilo e scarica l'artifact `KodiAuto-debug-apk`.
6. Estrai lo ZIP dell'artifact e installa `app-debug.apk` sul telefono.

## Stato reale del prototipo
- Navigazione grafica: presente.
- Lettore audio/video: non ancora collegato.
- MediaLibraryService / integrazione Android Auto: non ancora implementati.
- Sorgenti USB, SMB/NAS e plugin: da implementare.
- APK debug: generato dal workflow cloud.

Non installare o condividere APK provenienti da workflow non riconosciuti. Il primo APK è una build di sviluppo, non una versione pronta per l'uso in auto.
