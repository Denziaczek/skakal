# Ski Jump (skijump) - skoki narciarskie dla Minecrafta

Mod do **Minecraft Java Edition 1.20.1** na **Fabric** (Fabric Loader + Fabric API), Java 17, Gradle + Fabric Loom, mappings Yarn.
Pełny cykl skoku (rozbieg, wybicie z paskiem timingu, lot z fizyką aerodynamiczną i wiatrem, telemark), punktacja jak w prawdziwych
skokach, skocznie wzorowane na realnych, zawody w turach z komputerowymi zawodnikami, kariera i multiplayer z autorytatywnym serwerem.

## Budowanie

### GitHub Actions (zalecane)
1. Wrzuć zawartość tego katalogu do repozytorium na GitHubie.
2. Workflow `.github/workflows/build.yml` uruchamia się po każdym pushu (albo ręcznie: *Actions -> build -> Run workflow*).
3. Gotowy `skijump-1.4.0.jar` pobierzesz z *Artifacts -> skijump-jar*.

### Lokalnie
Wymagane: JDK 17 oraz Gradle 8.5.
```bash
gradle build            # jar: build/libs/skijump-1.4.0.jar
gradle runClient        # uruchomienie klienta testowego
```
(Do repozytorium nie dołączono pliku `gradle-wrapper.jar`; jeśli wolisz wrapper, wykonaj raz `gradle wrapper --gradle-version 8.5`.)

Wersje zależności są w `gradle.properties` (Minecraft 1.20.1, Yarn `1.20.1+build.10`, Loader `0.15.11`, Fabric API `0.92.2+1.20.1`).

## Instalacja
Wrzuć `skijump-*.jar` do `mods/` razem z **Fabric API**. Mod działa w singleplayerze i na serwerze (klient i serwer muszą mieć mod).

## Szybki start
1. W creative weź **Plan skoczni** z zakładki *Ski Jump* (każdy preset to osobny przedmiot), stań na wzniesieniu i kliknij klocek - skocznia zbuduje się
   stopniowo (progu = klikniętym klockiem, rozbieg za Tobą). Albo komendą: `/skijump place zakopane`.
2. Wejdź na szczyt rozbiegu i kliknij prawym **Belkę startową**. Siadasz na belce. **Spacja** = start.
3. Skacz! Wyniki, noty sędziów i komentarz pojawią się na czacie.

## Mechanika skoku (4 fazy)
| Faza | Co robisz | Co się liczy |
|---|---|---|
| **1. Rozbieg** | pozycja najazdowa jest **automatyczna** (mniejszy opór, większa prędkość) | długość rozbiegu (numer belki), sprzęt (ślizg nart, kombinezon), stan śniegu (pogoda), wiatr |
| **2. Wybicie** | **Spacja** w zielonym oknie paska timingu (idealnie +-12 ms) | błąd w ms wpływa na kąt wybicia, prędkość pionową i obrót: za wcześnie = skok niski i płaski, za późno = wysoki i krótki, idealnie = bonus |
| **3. Lot** | **W/S + mysz** = pochylenie (kąt natarcia), **A/D** = przechył, **V** = narty w układzie V (przełącznik) | siła nośna zależna od kąta natarcia (optimum 24-30 stopni, przeciągnięcie > 35), opór, wiatr (w twarz/plecy/boczny), porywy, turbulencje przy zboczu |
| **4. Lądowanie** | **R** = telemark w oknie ok. 0,1-0,45 s przed zetknięciem (HUD podpowiada) | prędkość pionowa, równowaga, timing telemarku, statystyka "lądowanie"; możliwy upadek (małe obrażenia, nigdy śmiertelne, kask zmniejsza) |

Dodatkowo **X** przerywa skok. Po pierwszym dołączeniu do świata mod wyświetla samouczek (powtórka: `/skijump tutorial`). Wszystkie klawisze można zmienić w ustawieniach sterowania (kategoria *Ski Jump*).

Fizyka jest **deterministyczna**: każdy skok jest inny, bo różni się wiatr (losowy, zmienny w czasie, zależny od pogody), Twoje błędy timingu i balansu oraz sprzęt.
Źle utrzymany balans daje kołysanie, utratę nośności lub znoszenie w bok. Serwer liczy całą fizykę; klient wysyła tylko sterowanie i wizualizuje stan.

### Punktacja
* **K** i **HS** na każdej skoczni; 60 pkt za K, punkty za metr zależne od wielkości skoczni (np. 1.8 pkt/m dla K100-169, 1.2 dla mamutów).
* **5 sędziów**, noty 0-20 (lot, telemark, balans, styl V), odrzucana najwyższa i najniższa.
* Rekompensata za wiatr i belkę (uproszczona).
* **Zawody**: 2 serie, do drugiej awansuje top 30 (lub wszyscy, gdy startuje mniej).

## Skocznie
`/skijump list` pokazuje presety. Wymiary są orientacyjne; geometria profilu jest **kalibrowana symulacją fizyki** (idealny skok ląduje między K a HS).
| Preset | K / HS | Ranga |
|---|---|---|
| training_k40, training_k65 | 40/45, 65/72 | Amator |
| normal_k90 | 90/100 | Junior |
| wisla, oslo, innsbruck | 120/134, 120/134, 120/128 | Zawodnik |
| oberstdorf, zakopane, garmisch, bischofshofen | 120/137, 125/140, 125/142, 125/142 | Kadra narodowa |
| willingen | 130/147 | Mistrz |
| planica, vikersund (mamuty) | 200/240 | Legenda |

* Skala w configu: `hillScale` (domyślnie 1.0 = wymiary rzeczywiste; mamut ma wtedy ponad 450 m długości - zmniejsz, jeśli chcesz mniej klocków). Fizyka zawsze liczy się w metrach rzeczywistych, skala dotyczy tylko świata.
* Małe i średnie skocznie pojawiają się **naturalnie i rzadko** w górskich, śnieżnych i tajgowych biomach (struktura `skijump_hill`).
  Najbliższą znajdziesz komendą `/locate structure skijump_hill` albo `/skijump locate`. Struktura pojawia się tylko w **nowo generowanych** częściach świata.
  Przy belce stoi **beczka trenera** ze sprzętem startowym (drewniane narty, kombinezon, kask, chleb).
* **Własne skocznie w survivalu**: zbuduj rozbieg, próg i zeskok z craftowanych bloków, a potem zarejestruj je **Różdżką skoczni**:
  kliknij (1) szczyt rozbiegu, (2) krawędź progu, (3) punkt K na zeskoku. Prawy klik w powietrze pokazuje obliczony tor rozbiegu (cząsteczki).
  Prawy klik różdżką na belce zmienia numer belki (Shift = niżej).
* Bloki: belka startowa, tor najazdowy, próg, znaczniki K i HS (warstwowe, gładki profil), wiatromierz (pokazuje siłę wiatru i działa jako informacja po kliknięciu),
  tablica wyników, wieża sędziowska, stacja zawodów, puchary (złoty/srebrny/brązowy).

## Sprzęt
* **Narty** (drewniane / żelazne / kompozytowe) - ślizg i stabilność; zakładane na nogi, widoczne jako długie narty (w locie w układzie V). Statystyki widać w opisie przedmiotu.
* **Kombinezon** (zwykły / wyczynowy) - nośność, opór, stabilność (noszony jako napierśnik).
* **Kask** - zmniejsza obrażenia przy upadku.
* Craftowane w survivalu, wszystko dostępne w creative (zakładka *Ski Jump*).

## Wiatr i pogoda
Wiatr jest losowy, zmienia się powoli, a deszcz i burza go wzmacniają (burza = silne porywy). Widać go na wiatromierzu i w HUD (w twarz / w plecy / boczny).
Wiatr "pod narty" (w twarz) tworzy dodatkową składową wznoszącą od zbocza.

## Kariera i zawodnicy
* Statystyki gracza (0-100): **siła wybicia, balans, technika lądowania** - rosną z doświadczeniem (`/skijump rank`).
* **Rangi** odblokowują skocznie (operator i creative omijają blokadę; wyłączysz w configu: `enforceRanks`).
* Nagrody za zawody: medale (przedmioty), puchary (bloki dekoracyjne), diamenty/szmaragdy, XP. Rekordy każdej skoczni są zapisywane w danych świata.
* Jajko **Skoczek** postawione przy skoczni samo trenuje: co jakiś czas wchodzi na belkę i skacze (wynik na czacie).
* Encja **Skoczek** (NPC) z fikcyjnymi imionami i różnymi statystykami startuje w zawodach razem z graczem (ta sama fizyka, odtwarzana na żywo).

## Nauka, trener, rywale, sezon
* **Skok z przewodnikiem:** pierwsze 2 skoki (`guidedFirstJumps`) przebiegają etapowo - w 4 kluczowych momentach (rozbieg, wybicie, lot, lądowanie) czas się zatrzymuje, a panel
  wyjaśnia, co robić (wznowienie Spacją). Sterowanie: `/skijump guide on|off|auto`. Po wybiciu HUD pokazuje błąd w ms ("za wcześnie / za późno / idealne") albo "Brak wybicia".
  Wybicie działa też lewym przyciskiem myszy.
* **Trener:** po każdym skoku na czacie pojawiają się 1-3 porady (wybicie, kąt natarcia, przechył, styl V, telemark, wiatr). Wyłączysz w configu (`coachTips`).
* **Rywale:** `/skijump rivals` - stali zawodnicy (technik, siłacz, mistrz telemarku, ryzykant, weteran, debiutant). Mają różne mocne strony, startują w zawodach i rozwijają się z czasem.
* **Sezon (Puchar Świata):** punkty za miejsca w zawodach na dowolnych skoczniach (100, 80, 60...); po `seasonLength` zawodach lider dostaje nagrodę (`/skijump season`).

## Survival
**Plan skoczni w survivalu:** craft = papier + niebieski barwnik + blok śniegu. Prawy klik na klocku stawia skocznię z presetu, Shift + prawy klik zmienia preset
(tylko małe i średnie, do `survivalBlueprintMaxK` = 90; wymagana ranga presetu). Koszt: 3xK bloków śniegu, 2xK kamiennych cegieł i K/2 lodu zbitego
(np. K65: 195 / 130 / 32). W creative i dla operatora plan działa za darmo dla wszystkich skoczni.

Skoki męczą: żeby skoczyć, potrzebujesz min. 4 punktów głodu, a każdy skok zużywa jedzenie (`survivalHunger` w configu). Awans rangi daje nagrodę (kask, żelazne narty,
kombinezon wyczynowy, kompozytowe narty, diamenty). Sprzęt craftujesz sam, a własną skocznię budujesz z craftowanych bloków i rejestrujesz Różdżką.

## Tryby
* **Singleplayer**: trening, rekordy, zawody z NPC.
* **Multiplayer**: zawody w turach, kolejka startowa, wspólna tabela wyników, serwer autorytatywny w obliczeniach fizyki.
* **Zawody**: kucnij + prawy klik na **Stacji zawodów** (otwiera zapisy), potem prawy klik = zapis/wypis, albo `/skijump comp start|join|leave|status|cancel`.
  Po zapisach dochodzą zawodnicy NPC (`npcCount`), a gdy przychodzi Twoja kolej - siadasz na belce i skaczesz.

## Komendy
| Komenda | Opis |
|---|---|
| `/skijump place <preset> [skala]` | (op) stawia skocznię z presetu na pozycji gracza |
| `/skijump list`, `/skijump hills` | presety / zarejestrowane skocznie |
| `/skijump top` | rekordy najbliższej skoczni |
| `/skijump rank` | ranga, XP, statystyki, medale |
| `/skijump gate <n>` | (op) belka startowa 1-24 najbliższej skoczni |
| `/skijump comp start/join/leave/status/cancel` | zawody |
| `/skijump abort` | przerwij skok |
| `/skijump remove`, `/skijump reload` | (op) usuń skocznię z rejestru / przeładuj config |

## Konfiguracja (`config/skijump.json`)
`hillScale`, `npcCount`, `registrationSeconds`, `turnTimeoutSeconds`, `enforceRanks`, `windStrength`, `buildBlocksPerTick`, `naturalHillChance` (>0 = naturalne skocznie włączone), `survivalHunger`, `survivalBlueprintMaxK`.

## Struktura projektu
```
src/main/java/dev/skijump/
  physics/    czysta Java: symulacja skoku (JumpSimulation), wiatr, geometria skoczni, punktacja, autopilot NPC
  hill/       presety, rejestr skoczni, generator (HillBuilder), losowe skocznie
  session/    sesja skoku po stronie serwera, teren ze świata, wiatr
  comp/       zawody (zapisy, serie, kolejka, klasyfikacja, nagrody)
  career/     ranga, XP, statystyki
  block/ item/ entity/ registry/ net/ command/
  client/     HUD, sterowanie, poza V (mixiny), renderery
src/main/resources/   assets (tekstury, modele, lang pl/en), data (receptury, loot tables)
tools/        gen_assets.py, gen_lang.py (regeneracja zasobów), PhysicsHarness.java (testy fizyki bez Minecrafta)
```
Fizykę (pakiet `physics`) można uruchomić i skalibrować bez Minecrafta:
```bash
javac -d /tmp/out src/main/java/dev/skijump/physics/*.java tools/*.java
java -cp /tmp/out PhysicsHarness      # odległości dla kolejnych belek i skoczni
java -cp /tmp/out Scen                # wpływ timingu, wiatru, sprzętu, umiejętności
```
Zasoby (tekstury, modele, receptury, tłumaczenia) są generowane skryptami: `python3 tools/gen_assets.py && python3 tools/gen_lang.py` (wymaga Pillow).

## Uwagi techniczne
* Skoczek siedzi w niewidzialnej "gondoli" (encja) przesuwanej przez serwer - ruch jest płynny, serwer autorytatywny, a inni gracze widzą skok.
  Klawisze Shift/W/S/A/D/Spacja są w czasie skoku przechwytywane przez mod (inaczej Shift wysadzałby z gondoli).
* Poza gracza (pochylenie, przechył, nogi w V, telemark) rysują dwa mixiny po stronie klienta; mają `require = 0`, więc przy innej wersji mappingów tylko
  wyłączą się animacje, a mod nadal zadziała.
* Dźwięki wiatru, publiczności i lądowania korzystają z dźwięków wbudowanych w grę (mapowane w `sounds.json`).
