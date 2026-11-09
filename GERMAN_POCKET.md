# Deutsches Pocket TTS: Test-Fork

Branch: `codex/native-pocket-german`, Basis `a4d496c4` aus diesem Fork.
Keine Änderungen, Issues, Kommentare oder PRs wurden an Upstream gesendet.

## Installation und erster Test

Die Test-APK wird unter den [Releases dieses Forks](https://github.com/madebycli/marmalade-tts-android-german/releases) bereitgestellt.
Sie installiert sich als **Marmalade TTS Deutsch / German**, Paket
`app.marmalade.tts.de.debug`, neben der Original-App.

1. APK installieren und in der bestehenden Einrichtung **Pocket TTS German** wählen;
   alternativ später unter **Engines** installieren.
2. Das deutsche Modell mit **allen 27 offiziellen Stimmen** dort herunterladen.
   Der separate Download ist ca.246MB, installiert ca.368MB. Für Download und
   Entpacken ungefähr1GB freien Speicher vorsehen. Die deutschen Modelle und
   Stimmendateien sind **nicht in der APK enthalten**. Der kleine ursprüngliche
   Kitten-Startmotor bleibt wie bei Upstream Bestandteil der App.
3. Unter den Stimmen beispielsweise **Anna**, Azelma, Eponine, Fantine, Jane,
   Mary oder Vera wählen; eine Persona erstellen und den Text im Speak-Bildschirm
   vorlesen lassen. Anna ist die Vorgabe. Das Original und Thorsten bleiben erhalten.
4. Für andere Apps in Androids TTS-Einstellungen **Marmalade TTS Deutsch** auswählen.
   Zur Original-App kann dort jederzeit zurückgewechselt werden.

Keine neue Android-Berechtigung wurde hinzugefügt. Zum Download braucht die App
Netzwerkzugriff; unter GrapheneOS muss dessen Netzwerkberechtigung dafür erlaubt
sein. Nach dem Download funktioniert die deutsche Sprachausgabe offline. Es ist
keine Aufnahme, kein Mikrofonzugriff, kein Konto und kein externer TTS-Dienst nötig.
Benachrichtigungen betreffen die vorhandene Wiedergabeanzeige. Die APK verwendet
eine Testsignatur; Aktualisierbarkeit mit späteren CI-Builds ist nicht garantiert.

## Was eingebaut ist

- Die produktive native `PocketEngine` wird wiederverwendet; die deutsche Unterklasse
  setzt nur Kennung, Standardstimme, Sprachaufbereitung und Fähigkeiten.
- Dieselben fünf ORT-Graphen, CPU-Thread-Einstellungen und Streaming-/Abbruchpfade
  wie bei Pocket. Kein neuer Laufzeitdienst, kein neues Android-Laufzeitpaket,
  kein GPU-/NNAPI-/NPU-Pfad. INT8-MatMul für FlowLM, übrige Graphen FP32.
- Aktueller offizieller deutscher Sechs-Layer-Checkpoint plus passender Tokenizer.
  Die 27 vorberechneten deutschen Stimmzustände werden in die vorhandene
  StateFrame-Prompt-Cache-Schicht geladen; nur die aktive Stimme bleibt dort.
  Englische Stimmzustände werden nicht wiederverwendet. Die App lädt keine
  Stimmen aus dem Netz während des Vorlesens nach.
- Eigener Katalogeintrag, Registry, Stimmenauswahl, Android-TTS-Verfügbarkeit,
  Credits und Benchmark-Auswahl. Englische Download-URLs/Prüfsummen und
  die bisherigen Funktionen bleiben erhalten.
- Deutsche Zahlen, Uhrzeiten, Datumsangaben, Umlaute, Abkürzungen, Prozent,
  Grad Celsius und Interpunktion. Neue Oberflächentexte stehen in Android-Ressourcen
  in allen acht bisherigen UI-Sprachvarianten sowie Deutsch.
- Keine erfundenen Tempo-/Klangbewertungen: die Bewertungsanzeigen für diesen
  Testmotor sind ausgeblendet, bis Messungen und Hörtests auf dem Telefon vorliegen.

Alle27: Alba, Anna, Azelma, Bill Boerst, Caro Davy, Charles, Cosette, Daan,
Eponine, Estelle, Eve, Fantine, George, Giovanni, Jane, Javert, Jean, Jürgen,
Lola, Marius, Mary, Michael, Paul, Peter Yearsley, Rafael, Stuart Bell, Vera.
Die Referenzen stammen aus verschiedenen Sprachen; das deutsche Modell erzeugt
mit ihnen Deutsch. Ein muttersprachlicher deutscher Akzent jeder Stimme wird
nicht versprochen. Die neue deutsche Mary ist die offizielle VCTK-Stimme,
nicht die gleichnamige englische Marmalade-Spenderstimme.

## Quellen, Lizenzen und bewusste Abgrenzung

[Ausführlicher Modell-/Stimmennachweis](LICENSES/pocket-tts-de.md),
[Stimmenquellen](scripts/pocket-bundle/german-voices.json) und
[versionierte Eingaben mit SHA-256](scripts/pocket-bundle/german-sources.json).
Quellcode MIT, APK als espeak-ng-Kombination GPL-3.0-or-later; Modell CC-BY-4.0.
Cosette/Expresso und Jean/EARS: **CC-BY-NC-4.0**, nur nichtkommerziell.
Diese Einschränkungen stehen auch in der App und im Download.

Der öffentlich verfügbare offizielle Checkpoint ohne Stimmenklonen genügt für
alle27offiziellen Stimmen. Die deutsche Engine lehnt den ohnehin nicht als UI
angebotenen Klon-Aufruf ab; die englische Implementierung bleibt bestehen.
Kein HF-Token oder eigenes Sprachsample erforderlich. Eine neue Aufnahme wurde
nicht angefordert, nachdem der Nutzer ausschließlich offizielle Stimmen wünschte.

Ein älterer öffentlich verfügbarer ONNX-Export wurde bei der Prüfung wegen
abweichender Gewichte und Token-IDs verworfen. Mit einem erhaltenen MIT-Exporter
wurde der aktuelle Checkpoint neu exportiert, gegen PyTorch geprüft und im
passenden Präzisionsmix quantisiert. Nichtsemantische ONNX-Debug-Metadaten werden
entfernt. Die 24-Layer-Variante hat dieselbe Grundstruktur mit72statt18Flow-Zuständen,
wurde hier jedoch weder exportiert noch auf einem Gerät verglichen. Angeboten wird
bewusst der kleinere Sechs-Layer-Checkpoint mit sämtlichen offiziellen Stimmen.

## Tatsächlich ausgeführte Prüfungen

- Exporter: Text-Conditioner, Mimi und FlowLM samt Zuständen gegen PyTorch innerhalb
  der Exporter-Toleranz geprüft; anschließend FlowLM dynamisch INT8 quantisiert.
- Export über das eingecheckte Wiederholungsskript erneut ausgeführt. Alle fünf
  Laufzeitgraphen nach Entfernen nichtsemantischer Traces bytegleich.
- **25/25** normalisierte Beispiele: produktiver Kotlin-Tokenizer gegen offiziellen
  deutschen JSON-Tokenizer. Zusätzlich Konvertierungsprüfungen JSON → SentencePiece.
- Produktiver Kotlin-Leser: **27/27** vollständige vorbereitete Stimmdateien geprüft;
  passende18FlowLM- und56Mimi-Zustände.
- Python/ORT-CPU auf dem Build-Rechner: **27/27** Stimmen mit endlichen,
  nichtstummen Audiodaten und plausibler Mindestdauer. Das ist kein Hörtest,
  keine Spracherkennungsbewertung und kein Android-/Pixel-Messergebnis.
- Android-Build und vollständige JVM-Suiten beider Distributionen: aktueller
  Status und Prüfzahlen werden nach dem abschließenden CI-Lauf ergänzt.

Rohdaten: [reports/pocket-german](reports/pocket-german). Es gibt kein angeschlossenes
Testgerät und keinen ausgeführten Emulator-Test. Oberfläche, System-TTS, Stop,
Streaming unter Last, Speicherspitze/PSS und hörbare Latenz auf Pixel/GrapheneOS
sind noch vom Nutzer zu prüfen. Zahlen des Build-Rechners sind keine Pixel-Prognose.

Auf dem Telefon bitte testen:

- „Guten Morgen. Heute scheint die Sonne.“ mit Anna und mehreren weiteren Stimmen.
- „Am 14. März um 16:30 Uhr sind es −4°C. Der Akku hat 25%. Dr. Müller kommt später.“
- Mehrere Absätze; während des Sprechens stoppen, erneut starten und Stimme wechseln.
- Modell installiert lassen, Netzwerkzugriff ausschalten und dieselben Texte testen.
- Original-App separat öffnen und vorlesen lassen. Dann TTS aus einer anderen App
  mit dem deutschen Testmotor prüfen.
- Für Leistung unter den vorhandenen Entwickler-/Benchmark-Werkzeugen Englisch
  Pocket und Pocket TTS German getrennt auswählen; jeweils kurzen und langen Text
  in der passenden Sprache einsetzen. Kalten Modellstart und warme Wiederholung
  unterscheiden. RTF = Rechenzeit / Audiodauer; die bestehende Benchmark-Anzeige
  verwendet teilweise den Kehrwert (Realtime ratio). Noch keine Pixel-Werte vorhanden.

## Android-Build wiederholen

JDK17, Android SDK36, NDK26.3.11579264 und CMake3.22.1; einmal das espeak-Submodul laden:

```sh
git submodule update --init third_party/espeak-ng
mkdir -p ~/.cache
flock ~/.cache/marmalade-gradle.lock ./gradlew :app:testFdroidDebugUnitTest :app:testPlayDebugUnitTest :app:assembleFdroidDebug -PgermanArm64 --no-daemon -Pkotlin.compiler.execution.strategy=in-process --max-workers=2
```

APK: `app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk`.
Der Workflow `.github/workflows/german-test.yml` prüft Testberichte, Paketkennung,
ARM64, espeak-Bibliothek, Signatur und das Fehlen deutscher Gewichte in der APK.

## Download reproduzieren

Nur Export-Werkzeuge benötigen Python/PyTorch; auf Android wird nichts davon installiert:

```sh
python3 -m venv .venv
.venv/bin/pip install torch==2.5.1+cpu --index-url https://download.pytorch.org/whl/cpu
.venv/bin/pip install -r scripts/pocket-bundle/requirements-export.txt
git clone https://github.com/lomotron/pocket-tts-onnx-export ../pocket-export
cd ../pocket-export
git checkout 8f52199533098963fe1cbd90bed35bb20d871b5e
cd -
.venv/bin/python scripts/pocket-bundle/export_german.py --exporter-dir ../pocket-export --cache /tmp/german-inputs --output /tmp/german-export
.venv/bin/python scripts/pocket-bundle/prepare_german_bundle.py --cache /tmp/german-inputs --export-dir /tmp/german-export --output /tmp/german-package
```

Der Exporter erhält nur die mitgelieferte kleine `--config`-Korrektur; Zustandsschema
und Inferenz folgen seiner bestehenden Fünf-Graph-Pipeline. Das Paket-Skript
verifiziert alle Eingangsdaten, erzeugt die27PPS1-Dateien und ein deterministisches
`pocket-tts-de.tar.gz` mit Wrapper-Verzeichnis, Metadaten und Lizenztexten.
Checkpoint, Graphen, Stimmen, APK und Tokens werden nicht neu in Git eingecheckt.
