#!/usr/bin/env python3
"""Generuje pliki tłumaczeń pl_pl.json i en_us.json oraz sprawdza pokrycie kluczy używanych w kodzie."""
import json, os, re, sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
LANG = os.path.join(ROOT, "src", "main", "resources", "assets", "skijump", "lang")

# klucz -> (pl, en)
T = {}


def add(key, pl, en):
    T[key] = (pl, en)


add("itemGroup.skijump.main", "Ski Jump", "Ski Jump")
add("key.categories.skijump", "Ski Jump", "Ski Jump")
add("key.skijump.vstyle", "Narty w układzie V (przełącznik)", "Skis in V-style (toggle)")
add("key.skijump.telemark", "Telemark (lądowanie)", "Telemark landing")
add("key.skijump.abort", "Przerwij skok", "Abort jump")

# bloki
for k, pl, en in [
    ("start_gate", "Belka startowa", "Start Gate"),
    ("inrun_track", "Tor najazdowy", "Inrun Track"),
    ("takeoff_table", "Próg", "Takeoff Table"),
    ("k_marker", "Znacznik punktu K", "K-Point Marker"),
    ("hs_marker", "Znacznik rozmiaru skoczni HS", "HS Marker"),
    ("anemometer", "Wiatromierz", "Anemometer"),
    ("scoreboard", "Tablica wyników", "Scoreboard"),
    ("judge_tower", "Wieża sędziowska", "Judges' Tower"),
    ("competition_station", "Stacja zawodów", "Competition Station"),
    ("trophy_gold", "Złoty puchar", "Gold Trophy"),
    ("trophy_silver", "Srebrny puchar", "Silver Trophy"),
    ("trophy_bronze", "Brązowy puchar", "Bronze Trophy"),
]:
    add("block.skijump." + k, pl, en)

# przedmioty
for k, pl, en in [
    ("ski_wood", "Narty drewniane", "Wooden Skis"),
    ("ski_iron", "Narty żelazne", "Iron Skis"),
    ("ski_composite", "Narty kompozytowe", "Composite Skis"),
    ("suit_standard", "Kombinezon skoczka", "Jumping Suit"),
    ("suit_pro", "Kombinezon wyczynowy", "Pro Jumping Suit"),
    ("helmet", "Kask skoczka", "Jumper's Helmet"),
    ("hill_wand", "Różdżka skoczni", "Hill Wand"),
    ("hill_blueprint", "Plan skoczni", "Hill Blueprint"),
    ("medal_gold", "Złoty medal", "Gold Medal"),
    ("medal_silver", "Srebrny medal", "Silver Medal"),
    ("medal_bronze", "Brązowy medal", "Bronze Medal"),
    ("skoczek_spawn_egg", "Jajko przyzywające: Skoczek", "Skoczek Spawn Egg"),
]:
    add("item.skijump." + k, pl, en)
add("entity.skijump.skoczek", "Skoczek", "Skoczek")
add("entity.skijump.jumper_seat", "Gondola skoczka", "Jumper Seat")

for k, pl, en in [("wind", "Wiatr", "Wind"), ("crowd", "Publiczność", "Crowd"), ("land", "Lądowanie", "Landing"),
                  ("fall", "Upadek", "Crash"), ("start", "Sygnał startu", "Start signal")]:
    add("skijump.subtitle." + k, pl, en)

# skocznie
for k, pl, en in [
    ("training_k40", "Skocznia treningowa K40", "Training Hill K40"),
    ("training_k65", "Skocznia treningowa K65", "Training Hill K65"),
    ("normal_k90", "Skocznia normalna K90", "Normal Hill K90"),
    ("wisla", "Wisła (Malinka)", "Wisla (Malinka)"),
    ("oslo", "Oslo (Holmenkollen)", "Oslo (Holmenkollen)"),
    ("innsbruck", "Innsbruck (Bergisel)", "Innsbruck (Bergisel)"),
    ("oberstdorf", "Oberstdorf (Schattenbergschanze)", "Oberstdorf (Schattenbergschanze)"),
    ("zakopane", "Zakopane (Wielka Krokiew)", "Zakopane (Wielka Krokiew)"),
    ("garmisch", "Garmisch-Partenkirchen", "Garmisch-Partenkirchen"),
    ("bischofshofen", "Bischofshofen", "Bischofshofen"),
    ("willingen", "Willingen (Mühlenkopfschanze)", "Willingen (Muhlenkopfschanze)"),
    ("planica", "Planica (mamut)", "Planica (flying hill)"),
    ("vikersund", "Vikersund (mamut)", "Vikersund (flying hill)"),
]:
    add("skijump.hill." + k, pl, en)

for k, pl, en in [("amateur", "Amator", "Amateur"), ("junior", "Junior", "Junior"), ("contender", "Zawodnik", "Contender"),
                  ("national", "Kadra narodowa", "National Team"), ("master", "Mistrz", "Master"), ("legend", "Legenda", "Legend")]:
    add("skijump.rank." + k, pl, en)

for k, pl, en in [("n", "północy", "north"), ("ne", "północnego wschodu", "north-east"), ("e", "wschodu", "east"),
                  ("se", "południowego wschodu", "south-east"), ("s", "południa", "south"), ("sw", "południowego zachodu", "south-west"),
                  ("w", "zachodu", "west"), ("nw", "północnego zachodu", "north-west")]:
    add("skijump.compass." + k, pl, en)

# HUD
add("skijump.hud.speed", "Prędkość: %1$s km/h (%2$s m/s)", "Speed: %1$s km/h (%2$s m/s)")
add("skijump.hud.phase.0", "Belka startowa", "Start gate")
add("skijump.hud.phase.1", "Rozbieg", "Inrun")
add("skijump.hud.phase.2", "Lot", "Flight")
add("skijump.hud.phase.3", "Lądowanie", "Landing")
add("skijump.hud.wind", "Wiatr", "Wind")
add("skijump.hud.headwind", "w twarz", "headwind")
add("skijump.hud.tailwind", "w plecy", "tailwind")
add("skijump.hud.hill", "K%1$s / HS%2$s  belka %3$s", "K%1$s / HS%2$s  gate %3$s")
add("skijump.hud.hint_seated", 'Spacja - ruszasz z belki (pozycja najazdowa jest automatyczna) | X - przerwij', 'Space - leave the gate (tuck is automatic) | X - abort')
add("skijump.hud.hint_inrun", '', '')
add("skijump.hud.hint_takeoff", 'WYBICIE: naciśnij SPACJĘ gdy biały znacznik trafi w zielone okno!', 'TAKEOFF: press SPACE when the white marker hits the green window!')
add("skijump.hud.crouch_on", "Pozycja najazdowa: TAK", "Tuck position: ON")
add("skijump.hud.crouch_off", "Pozycja najazdowa: NIE (przytrzymaj Shift)", "Tuck position: OFF (hold Shift)")
add("skijump.hud.timing", "Timing: %1$s ms", "Timing: %1$s ms")
add("skijump.hud.alpha", "Kąt %1$s°", "AoA %1$s°")
add("skijump.hud.v_on", "Styl V: ON", "V-style: ON")
add("skijump.hud.v_off", "Styl V: OFF (klawisz V)", "V-style: OFF (key V)")
add("skijump.hud.distance", "Odległość: %1$s m", "Distance: %1$s m")
add("skijump.hud.clearance", "Wysokość nad zboczem: %1$s m", "Height above slope: %1$s m")
add("skijump.hud.hint_telemark", "R - telemark tuż przed lądowaniem | W/S/mysz - pochylenie | A/D - przechył", "R - telemark just before landing | W/S/mouse - pitch | A/D - roll")
add("skijump.hud.telemark_now", "TELEMARK TERAZ! (R)", "TELEMARK NOW! (R)")
add("skijump.hud.telemark_ready", "Telemark gotowy", "Telemark ready")

# komunikaty
M = [
    ("skijump.msg.wind", "Wiatr: %1$s m/s z kierunku %2$s (porywy do %3$s m/s)", "Wind: %1$s m/s from the %2$s (gusts up to %3$s m/s)"),
    ("skijump.msg.no_hill_near", "Brak zarejestrowanej skoczni w pobliżu.", "No registered hill nearby."),
    ("skijump.msg.already", "Jesteś już w trakcie skoku.", "You are already jumping."),
    ("skijump.msg.gate_unregistered", "Ta belka nie należy do zarejestrowanej skoczni. Użyj różdżki skoczni lub planu skoczni.", "This gate does not belong to a registered hill. Use the Hill Wand or a Blueprint."),
    ("skijump.msg.hill_building", "Skocznia jest jeszcze w budowie.", "The hill is still being built."),
    ("skijump.msg.locked", "Skocznia zablokowana - wymagana ranga: %1$s.", "Hill locked - required rank: %1$s."),
    ("skijump.msg.seated", "Siedzisz na belce %1$s. Spacja - start.", "You sit on gate %1$s. Space to start."),
    ("skijump.msg.aborted", "Skok przerwany.", "Jump aborted."),
    ("skijump.msg.timeout_seat", "Za długo na belce - skok anulowany.", "Too long on the gate - jump cancelled."),
    ("skijump.msg.comp_registration", "Trwają zapisy do zawodów - poczekaj na start serii.", "Competition registration is open - wait for the round."),
    ("skijump.msg.comp_wait", "Na tej skoczni trwają zawody. Czekaj na swoją kolej.", "A competition is running here. Wait for your turn."),
    ("skijump.msg.result", "Skok: %1$s m (K%2$s) - razem %3$s pkt", "Jump: %1$s m (K%2$s) - total %3$s pts"),
    ("skijump.msg.result_detail", "Odległość %1$s | styl %2$s | wiatr %3$s | belka %4$s | noty: %5$s", "Distance %1$s | style %2$s | wind %3$s | gate %4$s | marks: %5$s"),
    ("skijump.msg.result_tech", "Prędkość na progu %1$s km/h | błąd wybicia %2$s ms | uderzenie %3$s m/s", "Takeoff speed %1$s km/h | takeoff error %2$s ms | impact %3$s m/s"),
    ("skijump.msg.xp", "+%1$s XP", "+%1$s XP"),
    ("skijump.msg.rank_up", "Awans! Nowa ranga: %1$s", "Promotion! New rank: %1$s"),
    ("skijump.msg.rank", "Ranga: %1$s | XP: %2$s (następna ranga: %3$s XP)", "Rank: %1$s | XP: %2$s (next rank: %3$s XP)"),
    ("skijump.msg.stats", "Statystyki - siła wybicia: %1$s | balans: %2$s | lądowanie: %3$s", "Stats - takeoff power: %1$s | balance: %2$s | landing: %3$s"),
    ("skijump.msg.career", "Skoki: %1$s | rekord życiowy: %2$s m | medale: %3$s złote, %4$s srebrne, %5$s brązowe", "Jumps: %1$s | personal best: %2$s m | medals: %3$s gold, %4$s silver, %5$s bronze"),
    ("skijump.msg.gate_set", "Belka %1$s z %2$s", "Gate %1$s of %2$s"),
    ("skijump.msg.hill_started", "Rozpoczęto budowę skoczni: %1$s (id %2$s). Poczekaj, aż budowa się zakończy.", "Started building: %1$s (id %2$s). Please wait until it is finished."),
    ("skijump.msg.hill_ready", "Skocznia %1$s gotowa (K%2$s, HS%3$s)! Belka startowa jest na szczycie rozbiegu.", "Hill %1$s is ready (K%2$s, HS%3$s)! The start gate is at the top of the inrun."),
    ("skijump.msg.hill_removed", "Usunięto skocznię z rejestru: %1$s (klocki zostają w świecie).", "Hill removed from the registry: %1$s (blocks stay in the world)."),
    ("skijump.msg.unknown_preset", "Nieznany preset: %1$s. Użyj /skijump list.", "Unknown preset: %1$s. Use /skijump list."),
    ("skijump.msg.list_header", "Dostępne skocznie (preset - nazwa - K/HS - ranga):", "Available hills (preset - name - K/HS - rank):"),
    ("skijump.msg.list_row", "%1$s - %2$s - K%3$s/HS%4$s - %5$s", "%1$s - %2$s - K%3$s/HS%4$s - %5$s"),
    ("skijump.msg.no_hills", "Nie ma jeszcze żadnych skoczni. Użyj /skijump place <preset> lub planu skoczni.", "There are no hills yet. Use /skijump place <preset> or a Hill Blueprint."),
    ("skijump.msg.hills_row", "[%1$s] %2$s - K%3$s/HS%4$s - próg: %5$s - belka %6$s", "[%1$s] %2$s - K%3$s/HS%4$s - edge: %5$s - gate %6$s"),
    ("skijump.msg.reloaded", "Konfiguracja przeładowana.", "Config reloaded."),
    ("skijump.msg.need_creative", "Ten przedmiot wymaga trybu creative lub uprawnień operatora.", "This item requires creative mode or operator permissions."),
    ("skijump.msg.blueprint_selected", "Plan skoczni: %1$s (K%2$s / HS%3$s)", "Hill blueprint: %1$s (K%2$s / HS%3$s)"),
    ("skijump.msg.wand_1", "Różdżka: zaznaczono SZCZYT rozbiegu. Teraz kliknij KRAWĘDŹ progu.", "Wand: inrun TOP marked. Now click the takeoff EDGE."),
    ("skijump.msg.wand_2", "Różdżka: zaznaczono krawędź progu. Teraz kliknij punkt K na zeskoku.", "Wand: takeoff edge marked. Now click the K-point on the landing slope."),
    ("skijump.msg.wand_reset", "Różdżka wyzerowana.", "Wand reset."),
    ("skijump.msg.wand_bad_inrun", "Rozbieg musi mieć min. 12 bloków długości i 4 bloki wysokości (szczyt wyżej niż próg).", "The inrun must be at least 12 blocks long and 4 blocks high (top above the edge)."),
    ("skijump.msg.wand_bad_k", "Punkt K musi leżeć min. 15 bloków za progiem w kierunku skoku.", "The K-point must be at least 15 blocks beyond the edge in the jump direction."),
    ("skijump.msg.wand_created", "Zarejestrowano skocznię %1$s (K%2$s, HS%3$s, id %4$s). Prawy klik różdżką w powietrzu pokazuje tor rozbiegu.", "Registered hill %1$s (K%2$s, HS%3$s, id %4$s). Right-click the wand in the air to preview the inrun path."),
    ("skijump.msg.wand_guide", "Podgląd toru rozbiegu: %1$s", "Inrun path preview: %1$s"),
    # komentarze
    ("skijump.comment.record", "REKORD SKOCZNI! %1$s m!", "HILL RECORD! %1$s m!"),
    ("skijump.comment.beyond_hs", "Skok poza rozmiar skoczni! Niesamowite!", "A jump beyond the hill size! Incredible!"),
    ("skijump.comment.perfect_takeoff", "Idealne wybicie i daleki skok - tak się to robi!", "Perfect takeoff and a long jump - textbook!"),
    ("skijump.comment.great", "Bardzo daleki skok, wyraźnie za punktem K!", "A great jump, well beyond the K-point!"),
    ("skijump.comment.good", "Dobry skok za punkt konstrukcyjny.", "A good jump past the K-point."),
    ("skijump.comment.borderline", "Lądowanie na granicy upadku, ale utrzymał(a)!", "Landing on the edge of a crash - but you kept it!"),
    ("skijump.comment.early", "Za wczesne wybicie - skok niski i płaski.", "Takeoff too early - a low, flat jump."),
    ("skijump.comment.late", "Za późne wybicie - wysoki, ale krótki skok.", "Takeoff too late - a high but short jump."),
    ("skijump.comment.asym", "Znoszenie w bok i asymetria w locie.", "Drifting sideways and asymmetric in flight."),
    ("skijump.comment.short", "Krótki skok przed punktem K.", "A short jump before the K-point."),
    ("skijump.comment.very_short", "Bardzo krótki skok. Popracuj nad wybiciem i kątem natarcia.", "A very short jump. Work on your takeoff and angle of attack."),
    ("skijump.comment.fall.off_course", "Upadek - lądowanie poza torem zeskoku!", "Crash - landed off the course!"),
    ("skijump.comment.fall.hard_impact", "Upadek - zbyt twarde lądowanie!", "Crash - landing too hard!"),
    ("skijump.comment.fall.unbalanced", "Upadek - utrata równowagi przy lądowaniu!", "Crash - lost balance on landing!"),
    ("skijump.comment.fall.no_telemark", "Upadek - brak telemarku i niepewne lądowanie!", "Crash - no telemark and an unsteady landing!"),
    # tablice
    ("skijump.board.title", "=== %1$s (K%2$s / HS%3$s) ===", "=== %1$s (K%2$s / HS%3$s) ==="),
    ("skijump.board.no_record", "Rekord skoczni: brak", "Hill record: none yet"),
    ("skijump.board.record", "Rekord skoczni: %1$s m (%2$s)", "Hill record: %1$s m (%2$s)"),
    ("skijump.board.row", "%1$s. %2$s - %3$s m - %4$s pkt", "%1$s. %2$s - %3$s m - %4$s pts"),
    ("skijump.judges.info", "Sędziowie: 5 not 0-20, odrzucana najwyższa i najniższa. 60 pkt za K, %1$s pkt za metr (K%2$s). Wiatr i belka są rekompensowane.", "Judges: 5 marks 0-20, highest and lowest dropped. 60 pts for K, %1$s pts per metre (K%2$s). Wind and gate are compensated."),
    ("skijump.judges.you", "Twój rekord życiowy: %1$s m", "Your personal best: %1$s m"),
    # zawody
    ("skijump.comp.opened", "Otwarto zapisy do zawodów na skoczni %1$s! Zapisy trwają %2$s s (stacja zawodów lub /skijump comp join).", "Registration opened for the competition at %1$s! %2$s s left (Competition Station or /skijump comp join)."),
    ("skijump.comp.reg_countdown", "Do końca zapisów: %1$s s", "Registration closes in %1$s s"),
    ("skijump.comp.already", "Na tej skoczni są już zawody.", "A competition is already running at this hill."),
    ("skijump.comp.none", "Na tej skoczni nie ma zawodów.", "There is no competition at this hill."),
    ("skijump.comp.joined", "%1$s zapisał(a) się do zawodów (zawodników: %2$s).", "%1$s joined the competition (entrants: %2$s)."),
    ("skijump.comp.left", "%1$s wypisał(a) się z zawodów.", "%1$s left the competition."),
    ("skijump.comp.cannot_join", "Nie możesz teraz dołączyć do zawodów.", "You cannot join the competition now."),
    ("skijump.comp.cannot_leave", "Nie możesz teraz wypisać się z zawodów.", "You cannot leave the competition now."),
    ("skijump.comp.cancelled_nobody", "Zawody odwołane - nikt się nie zapisał.", "Competition cancelled - nobody signed up."),
    ("skijump.comp.cancelled", "Zawody zostały odwołane.", "The competition was cancelled."),
    ("skijump.comp.round_start", "Seria %1$s - zawodników: %2$s", "Round %1$s - competitors: %2$s"),
    ("skijump.comp.your_turn", "Na belce: %1$s (seria %2$s).", "On the gate: %1$s (round %2$s)."),
    ("skijump.comp.your_turn_hint", "Twoja kolej! Usiądź na belce startowej (prawy klik) - współrzędne: %1$s %2$s %3$s", "Your turn! Sit on the start gate (right-click) - coordinates: %1$s %2$s %3$s"),
    ("skijump.comp.npc_turn", "Skacze %1$s (seria %2$s).", "%1$s is jumping (round %2$s)."),
    ("skijump.comp.turn_waiting", "Czekamy na %1$s...", "Waiting for %1$s..."),
    ("skijump.comp.dns", "%1$s nie wystartował(a) - 0 punktów.", "%1$s did not start - 0 points."),
    ("skijump.comp.fall", "upadek", "crash"),
    ("skijump.comp.jump_result", "%1$s: %2$s m, %3$s pkt - pozycja %4$s%5$s", "%1$s: %2$s m, %3$s pts - position %4$s%5$s"),
    ("skijump.comp.row", "%1$s. %2$s - %3$s pkt (%4$s)", "%1$s. %2$s - %3$s pts (%4$s)"),
    ("skijump.comp.standings", "--- Klasyfikacja ---", "--- Standings ---"),
    ("skijump.comp.after_round1", "--- Po 1. serii ---", "--- After round 1 ---"),
    ("skijump.comp.final", "=== KLASYFIKACJA KOŃCOWA ===", "=== FINAL STANDINGS ==="),
    ("skijump.comp.your_place", "Twoje miejsce: %1$s (+%2$s XP)", "Your place: %1$s (+%2$s XP)"),
    ("skijump.comp.status", "Zawody na skoczni %1$s: stan %2$s, zawodników: %3$s", "Competition at %1$s: state %2$s, entrants: %3$s"),
    ("skijump.comp.station_idle", "Stacja zawodów (%1$s). Kucnij + prawy klik, aby otworzyć zapisy.", "Competition station (%1$s). Sneak + right-click to open registration."),
]
for k, pl, en in M:
    add(k, pl, en)

add("skijump.npc.jump", "%1$s skoczył(a) %2$s m (%3$s pkt)", "%1$s jumped %2$s m (%3$s pts)")
add("skijump.tooltip.glide", "Ślizg: +%1$s%%", "Glide: +%1$s%%")
add("skijump.tooltip.ski_stab", "Stabilność nart: +%1$s%%", "Ski stability: +%1$s%%")
add("skijump.tooltip.ski_lift", "Nośność nart: +%1$s%%", "Ski lift: +%1$s%%")
add("skijump.tooltip.suit_lift", "Nośność kombinezonu: +%1$s%%", "Suit lift: +%1$s%%")
add("skijump.tooltip.suit_drag", "Mniejszy opór: +%1$s%%", "Less drag: +%1$s%%")
add("skijump.tooltip.suit_stab", "Stabilność w locie: +%1$s%%", "Flight stability: +%1$s%%")
add("skijump.tooltip.helmet", "Ochrona przy upadku: %1$s%%", "Crash protection: %1$s%%")
add("skijump.tutorial.title", "=== SKI JUMP - krótki przewodnik (powtórka: /skijump tutorial) ===", "=== SKI JUMP - quick guide (repeat: /skijump tutorial) ===")
add("skijump.tutorial.1", "1) Sprzęt: w creative zakładka 'Ski Jump'; w survivalu wyrabiasz narty, kombinezon i kask (opis działania w podpisie przedmiotu). Sprzęt zakładasz jak zbroję.", "1) Gear: in creative use the 'Ski Jump' tab; in survival craft skis, suit and helmet (stats are in the item tooltip). Wear them like armor.")
add("skijump.tutorial.2", '2) Skocznia: Plan skoczni craftujesz (papier + niebieski barwnik + blok śniegu). Kliknij nim klocek - w survivalu zbudujesz małe i średnie skocznie (K do 90) za śnieg, kamienne cegły i lód; Shift + prawy klik zmienia skocznię. Własną skocznię zarejestrujesz Różdżką; w creative: /skijump place zakopane.', '2) Hill: craft a Hill Blueprint (paper + blue dye + snow block). Click a block with it - in survival you can build small and medium hills (K up to 90) for snow, stone bricks and ice; Shift + right-click changes the hill. Register your own hill with the Wand; in creative: /skijump place zakopane.')
add("skijump.tutorial.3", "3) Start: wejdź na szczyt rozbiegu i kliknij prawym przyciskiem BELKĘ STARTOWĄ. Potem SPACJA = ruszasz. Pozycja najazdowa jest automatyczna.", "3) Start: climb to the top of the inrun and right-click the START GATE. Then SPACE = go. The tuck position is automatic.")
add("skijump.tutorial.4", "4) WYBICIE (najważniejsze!): przed progiem pojawia się pasek timingu. Naciśnij SPACJĘ drugi raz, gdy biały znacznik wejdzie w zielone okno. Bez tego skok kończy się po ok. 50 m!", "4) TAKEOFF (most important!): a timing bar shows before the edge. Press SPACE a second time when the white marker enters the green window. Without it the jump ends after ~50 m!")
add("skijump.tutorial.5", "5) LOT: W/S lub mysz = pochylenie (trzymaj wskaźnik kąta po lewej w zielonej strefie), A/D = przechył, V = narty w układzie V (więcej nośności i punktów).", "5) FLIGHT: W/S or mouse = pitch (keep the angle gauge on the left in the green zone), A/D = roll, V = V-style skis (more lift and style points).")
add("skijump.tutorial.6", "6) LĄDOWANIE: naciśnij R (telemark) ok. 0,3 s przed zetknięciem ze zboczem - HUD pokazuje wysokość i podpowiada moment. Bez telemarku tracisz noty i łatwo o upadek.", "6) LANDING: press R (telemark) about 0.3 s before touching the slope - the HUD shows height and prompts the moment. Without telemark you lose marks and may crash.")
add("skijump.tutorial.7", "7) Zawody: kucnij + prawy klik na STACJI ZAWODÓW otwiera zapisy, potem dołączają Skoczkowie (NPC). Jajko 'Skoczek' przy skoczni sam trenuje skoki.", "7) Competition: sneak + right-click the COMPETITION STATION to open registration; Skoczek NPCs join. A 'Skoczek' spawn egg near a hill trains by itself.")
add("skijump.tutorial.8", '8) Przydatne: /skijump guide on|off (skok z przewodnikiem), /skijump rank, /skijump top, /skijump rivals, /skijump season, /skijump abort lub X. Klawisze R i V można zmienić w Sterowaniu.', '8) Useful: /skijump guide on|off (guided jump), /skijump rank, /skijump top, /skijump rivals, /skijump season, /skijump abort or X. Keys R and V can be changed in Controls.')

add("skijump.msg.too_hungry", "Jesteś zbyt głodny/a, żeby skakać - zjedz coś (potrzeba min. 4 punktów głodu).", "You are too hungry to jump - eat something (needs at least 4 hunger points).")
add("skijump.msg.rank_reward", "Nagroda za awans: %1$s", "Promotion reward: %1$s")
add("skijump.msg.locate_hill", "Najbliższa skocznia: %1$s - próg na %2$s %3$s %4$s (ok. %5$s bloków)", "Nearest hill: %1$s - edge at %2$s %3$s %4$s (about %5$s blocks)")
add("skijump.msg.locate_structure", "Najbliższa naturalna skocznia (jeszcze niezbudowana): x=%1$s z=%2$s (ok. %3$s bloków). Dojdź tam - skocznia zbuduje się sama.", "Nearest natural hill (not built yet): x=%1$s z=%2$s (about %3$s blocks). Go there - the hill builds itself.")
add("skijump.msg.locate_none", "Nie znaleziono naturalnej skoczni w zasięgu 100 chunków (w starych, już wygenerowanych obszarach struktura się nie pojawi).", "No natural hill found within 100 chunks (it does not appear in already generated areas).")
add("skijump.tutorial.9", "9) Survival: naturalne skocznie znajdziesz komendą /locate structure skijump_hill (albo /skijump locate). Przy belce stoi beczka trenera ze sprzętem startowym. Skoki męczą (jedzenie!), a awans rangi daje nagrody.", "9) Survival: find natural hills with /locate structure skijump_hill (or /skijump locate). A coach's barrel with starter gear stands next to the gate. Jumps are tiring (food!) and each rank-up gives a reward.")

add("skijump.msg.bp_cost", "Koszt budowy: %1$s bloków śniegu, %2$s kamiennych cegieł, %3$s lodu zbitego.", "Build cost: %1$s snow blocks, %2$s stone bricks, %3$s packed ice.")
add("skijump.msg.bp_missing", "Za mało surowców (masz/potrzeba) - śnieg: %1$s, kamienne cegły: %2$s, lód zbity: %3$s.", "Not enough materials (have/need) - snow: %1$s, stone bricks: %2$s, packed ice: %3$s.")
add("skijump.msg.bp_too_big", "W survivalu możesz zbudować skocznię do K%1$s. Większe są dostępne w creative lub dla operatora.", "In survival you can build hills up to K%1$s. Bigger ones are for creative or operators.")

add("skijump.guide.started", "Skok z przewodnikiem: w kluczowych momentach czas się zatrzyma i wyjaśnię, co robić (wznowienie: SPACJA). Wyłączysz to: /skijump guide off", "Guided jump: at key moments time will stop and I will explain what to do (resume: SPACE). Turn off with: /skijump guide off")
add("skijump.guide.mode.0", "Przewodnik: automatycznie (tylko pierwsze skoki).", "Guide: automatic (first jumps only).")
add("skijump.guide.mode.1", "Przewodnik: włączony przy każdym treningu.", "Guide: on for every training jump.")
add("skijump.guide.mode.2", "Przewodnik: wyłączony.", "Guide: off.")
add("skijump.guide.continue", "SPACJA - kontynuuj", "SPACE - continue")
add("skijump.guide.1.title", "1/4 Rozbieg", "1/4 Inrun")
add("skijump.guide.1.text", "Jedziesz w pozycji najazdowej - jest automatyczna. Im dłuższy rozbieg (wyższa belka) i lepszy sprzęt, tym większa prędkość na progu. Zaraz zbliżymy się do progu, gdzie trzeba będzie się wybić.", "You ride in the tuck position - it is automatic. A longer inrun (higher gate) and better gear mean more speed at the edge. Soon we will reach the edge, where you must take off.")
add("skijump.guide.2.title", "2/4 WYBICIE - najważniejszy moment", "2/4 TAKEOFF - the key moment")
add("skijump.guide.2.text", "Czas stoi. Na dole widzisz pasek timingu: biały znacznik zbliża się do zielonego okna. Gdy wznowisz, naciśnij SPACJĘ dokładnie wtedy, gdy znacznik wejdzie w zielone okno (lub kliknij lewy przycisk myszy). Za wcześnie = niski, płaski skok; za późno = wysoki i krótki.", "Time is stopped. At the bottom you see the timing bar: the white marker approaches the green window. After resuming, press SPACE exactly when the marker enters the green window (or click the left mouse button). Too early = low, flat jump; too late = high and short.")
add("skijump.guide.3.title", "3/4 LOT - balans", "3/4 FLIGHT - balance")
add("skijump.guide.3.text", "Wskaźnik kąta po lewej: trzymaj kreskę w ZIELONEJ strefie (W/S lub mysz). Zbyt nisko = mała nośność, zbyt wysoko (czerwone) = przeciągnięcie. A/D = przechył (kreska przy środku paska na dole), V = narty w układzie V (więcej nośności i punktów).", "Angle gauge on the left: keep the line in the GREEN zone (W/S or mouse). Too low = little lift, too high (red) = stall. A/D = roll (keep the marker near the centre of the bottom bar), V = V-style skis (more lift and style points).")
add("skijump.guide.4.title", "4/4 LĄDOWANIE - telemark", "4/4 LANDING - telemark")
add("skijump.guide.4.text", "Zbliżasz się do zbocza. Naciśnij R (telemark) mniej więcej 0,3 s przed zetknięciem - HUD pokaże napis TELEMARK TERAZ. Zbyt wcześnie lub zbyt późno obniża noty, a brak telemarku grozi upadkiem.", "You are approaching the slope. Press R (telemark) about 0.3 s before touchdown - the HUD will show TELEMARK NOW. Too early or too late lowers marks, and no telemark risks a crash.")
add("skijump.hud.to.none", "Brak wybicia! Naciśnij SPACJĘ, gdy znacznik wejdzie w zielone okno", "No takeoff! Press SPACE when the marker enters the green window")
add("skijump.hud.to.perfect", "Wybicie: %1$s ms - IDEALNE!", "Takeoff: %1$s ms - PERFECT!")
add("skijump.hud.to.good", "Wybicie: %1$s ms - dobre", "Takeoff: %1$s ms - good")
add("skijump.hud.to.early", "Wybicie: %1$s ms - za wcześnie", "Takeoff: %1$s ms - too early")
add("skijump.hud.to.late", "Wybicie: %1$s ms - za późno", "Takeoff: %1$s ms - too late")
add("skijump.coach.prefix", "[Trener] ", "[Coach] ")
add("skijump.coach.no_takeoff", "Nie wybiłeś się na progu - naciśnij SPACJĘ, gdy znacznik wejdzie w zielone okno.", "You did not take off at the edge - press SPACE when the marker enters the green window.")
add("skijump.coach.early", "Wybicie za wcześnie o %1$s ms - poczekaj, aż znacznik dojdzie do środka.", "Takeoff %1$s ms too early - wait until the marker reaches the centre.")
add("skijump.coach.late", "Wybicie za późno o %1$s ms - naciśnij Spację odrobinę wcześniej.", "Takeoff %1$s ms too late - press Space a little earlier.")
add("skijump.coach.alpha_low", "Lecisz zbyt płasko (kąt natarcia za niski) - odchyl ciało do tyłu (S lub mysz do góry), aż kreska wejdzie w zieloną strefę.", "You fly too flat (angle of attack too low) - lean back (S or mouse up) until the line is in the green zone.")
add("skijump.coach.alpha_high", "Kąt natarcia za wysoki, grozi przeciągnięcie - pochyl się do przodu (W lub mysz w dół).", "Angle of attack too high, risk of stall - lean forward (W or mouse down).")
add("skijump.coach.roll", "Za duży przechył na boki - koryguj A/D, żeby znacznik wracał do środka.", "Too much sideways roll - correct with A/D so the marker returns to the centre.")
add("skijump.coach.v", "Użyj stylu V (klawisz V) zaraz po wybiciu - daje więcej nośności i punktów za styl.", "Use V-style (key V) right after takeoff - it gives more lift and style points.")
add("skijump.coach.telemark_none", "Zapomniałeś o telemarku - naciśnij R ok. 0,3 s przed zetknięciem ze zboczem.", "You forgot the telemark - press R about 0.3 s before touching the slope.")
add("skijump.coach.telemark_early", "Telemark za wcześnie - poczekaj na napis TELEMARK TERAZ.", "Telemark too early - wait for the TELEMARK NOW prompt.")
add("skijump.coach.telemark_late", "Telemark za późno - naciśnij R odrobinę wcześniej.", "Telemark too late - press R a bit earlier.")
add("skijump.coach.tailwind", "Wiał wiatr w plecy - utrudnia lot; mocniej utrzymuj kąt natarcia i użyj V.", "A tailwind made the flight harder - hold your angle of attack firmly and use V.")
add("skijump.coach.headwind", "Wiatr w twarz pomaga - wykorzystaj go, utrzymując kąt natarcia w zielonej strefie.", "A headwind helps - use it by keeping your angle of attack in the green zone.")
add("skijump.coach.great", "Świetny skok! Tak trzymaj.", "Great jump! Keep it up.")
add("skijump.rival.header", "--- Rywale ---", "--- Rivals ---")
add("skijump.rival.row", "%1$s (%2$s) - poziom %3$s | wygrane: %4$s, podia: %5$s, spotkania: %6$s", "%1$s (%2$s) - rating %3$s | wins: %4$s, podiums: %5$s, meetings: %6$s")
add("skijump.rival.type.0", "technik", "technician")
add("skijump.rival.type.1", "siłacz", "powerhouse")
add("skijump.rival.type.2", "mistrz telemarku", "telemark master")
add("skijump.rival.type.3", "ryzykant", "daredevil")
add("skijump.rival.type.4", "weteran", "veteran")
add("skijump.rival.type.5", "debiutant", "rookie")
add("skijump.rival.beat_you", "Rywal %1$s (miejsce %2$s) był lepszy od Ciebie - spróbuj z nim jeszcze raz!", "Rival %1$s (place %2$s) beat you - try again!")
add("skijump.season.title", "=== Sezon %1$s - zawody %2$s z %3$s ===", "=== Season %1$s - events %2$s of %3$s ===")
add("skijump.season.row", "%1$s. %2$s - %3$s pkt", "%1$s. %2$s - %3$s pts")
add("skijump.season.empty", "Brak punktów - sezon zacznie się po pierwszych zawodach.", "No points yet - the season starts after the first competition.")
add("skijump.season.event_done", "Punkty sezonu %1$s zaktualizowane (zawody %2$s z %3$s). Podgląd: /skijump season", "Season %1$s points updated (event %2$s of %3$s). View: /skijump season")
add("skijump.season.final_title", "=== KONIEC SEZONU %1$s - klasyfikacja generalna ===", "=== END OF SEASON %1$s - overall standings ===")
add("skijump.season.winner", "Zwycięzca sezonu: %1$s!", "Season winner: %1$s!")
add("skijump.season.reward", "Nagroda za zwycięstwo w sezonie: złoty puchar, złoty medal, diamenty i 150 XP!", "Season winner reward: gold trophy, gold medal, diamonds and 150 XP!")


def build():
    os.makedirs(LANG, exist_ok=True)
    for idx, name in ((0, "pl_pl"), (1, "en_us")):
        data = {k: v[idx] for k, v in sorted(T.items())}
        with open(os.path.join(LANG, name + ".json"), "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)


def check():
    used = set()
    for dp, _, fs in os.walk(os.path.join(ROOT, "src", "main", "java")):
        for fn in fs:
            if fn.endswith(".java"):
                src = open(os.path.join(dp, fn), encoding="utf-8").read()
                used |= set(re.findall(r'"(skijump\.[a-zA-Z0-9_.]*[a-zA-Z0-9_])"', src))
    prefixes = ("skijump.rank.", "skijump.hill.", "skijump.hud.phase.", "skijump.comment.fall.", "skijump.compass.")
    missing = [k for k in sorted(used) if k not in T and k != "skijump.json"]
    # klucze dynamiczne kończą się kropką i są pokryte listami powyżej
    print("kluczy zdefiniowanych:", len(T), "| użytych literalnie:", len(used))
    if missing:
        print("BRAKUJĄCE KLUCZE:", missing)
        return 1
    print("pokrycie OK")
    return 0


if __name__ == "__main__":
    build()
    sys.exit(check())
