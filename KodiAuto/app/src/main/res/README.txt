Kodi Auto - icona launcher Android

Contenuto:
- res/mipmap-* : icone PNG legacy per tutte le densita Android
- res/mipmap-anydpi-v26 : icone adaptive per Android 8.0 e successivi
- res/drawable : livello foreground e sfondo dell'icona adaptive

Installazione:
1. Copia le cartelle contenute in res/ nella cartella app/src/main/res/ del progetto Android.
2. Se richiesto, sostituisci i file ic_launcher esistenti.
3. Verifica che AndroidManifest.xml usi android:icon='@mipmap/ic_launcher'
   e android:roundIcon='@mipmap/ic_launcher_round'.
4. Ricompila e reinstalla l'APK per vedere la nuova icona.
