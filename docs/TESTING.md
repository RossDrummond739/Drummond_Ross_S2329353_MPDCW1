# Documented testing — Scot Traffic (Drummond_Ross_S2329353)

How to use this file: run each test on the emulator/phone, take a screenshot (or data dump) as
evidence, fill in **Actual outcome** and **Result**. Rows marked *(automated)* are covered by the
JVM unit tests in `app/src/test` (`./gradlew testDebugUnitTest`); their actual outcome is the test
result.

`MainActivityUiTest` (Robolectric) also runs the real activity on the JVM with the bundled sample
feeds and checks navigation, search, the landscape two-pane layout and rotation. It saves the
screenshots in `docs/screenshots/` (regenerate them in `app/build/screenshots/`). Those rows are marked
*(automated UI)*. Still repeat them on the emulator with live data for the report and video.

Test data: live feeds where possible. For repeatable checks, the two example feeds supplied
with the coursework are bundled (`app/src/main/assets`) and are used when no API keys are set.

## 1. Data in the feed is consistent with the data displayed

| ID | Test | Steps | Expected outcome | Actual outcome | Result |
|----|------|-------|------------------|----------------|--------|
| D1 | Number of unplanned events *(automated)* | Parse `unplannedContentExample.xml` | 4 situations | 4 situations | Pass |
| D2 | Number of roadworks *(automated)* | Parse `roadworksContentExample.xml` | 405 situations | 405 situations | Pass |
| D3 | Field values of first incident *(automated)* | Parse incident `I_20260810_248` | Comment "The A74M(S) junction 19 (Ecclefechan) off slip is closed…", type road closed, 0 operational lanes, southbound, to = J19 (55.065432, -3.273135) "A74(M) J19 Eaglesfield", from = J18 "A74(M) J18 Ecclefechan" | As expected | Pass |
| D4 | Delay types *(automated)* | Count `delaysType` in roadworks sample | 130 delays, 26 long delays, 1 very long delay (same as `grep` on the XML) | 130 / 26 / 1 | Pass |
| D5 | Unknown XML fields *(automated)* | Parse a record with an extra element `<brandNewField>` | No crash; value listed under "Other data in the feed" | As expected | Pass |
| D6 | Counts on screen | Open app; compare chip counts "Incidents (n)" and "Roadworks (n)" with the number of `<situation>` elements in the downloaded XML (open the URL in a browser with the same credentials, or use the sample files) | Counts match | | |
| D7 | List item matches XML | Pick one card; find its `generalPublicComment` in the XML | Type and comment on the card are the same as in the XML | | |
| D8 | Detail view matches XML | Open the same situation | Start/end times, delays, lanes, direction, from/to names and coordinates match the XML | | |
| D9 | Map position matches XML | "Show on map" on that situation | Marker between the from/to coordinates, line drawn from → to | | |

## 2. Navigation around the app

| ID | Test | Steps | Expected outcome | Actual outcome | Result |
|----|------|-------|------------------|----------------|--------|
| N1 | Start-up | Launch the app | Status banner "Updating…", then list of situations; banner shows "Live data · updated HH:MM · next update HH:MM" | | |
| N2 | Bottom navigation (portrait) | Tap Situations / Search / Map | Each screen shown; selected item highlighted | | |
| N3 | Open details (portrait) *(automated UI)* | Tap a card | Detail screen replaces the list | Detail screen shown, comment matches the selected record (`02_portrait_detail.png`) | Pass |
| N4 | Back from details *(automated UI)* | Press Back | Returns to the list | List shown again | Pass |
| N5 | Navigation rail (landscape) | Rotate to landscape; tap each rail item | Rail on the left; screen content in the middle; details pane on the right | | |
| N6 | Two-pane details (landscape) *(automated UI)* | Tap a card in landscape | Details appear on the right, card highlighted on the left | As expected (`06_landscape_two_pane.png`) | Pass |
| N7 | Rotation keeps state *(automated UI)* | Select a filter, open a situation, rotate both ways | Same filter, same selected situation; portrait shows the detail screen, landscape shows it in the right pane | Incidents filter and selection kept both ways (`07_rotated_landscape_keeps_state.png`) | Pass |
| N8 | Toolbar menu | Tap refresh; open "Auto-update interval"; open "About & data source" | Refresh runs; interval dialog lists 1 min…2 h; about dialog with source and colour key | | |
| N9 | Map → details | Tap a marker, then its info window | Details of that situation open | | |

## 3. Functionality in the specification

| ID | Test | Steps | Expected outcome | Actual outcome | Result |
|----|------|-------|------------------|----------------|--------|
| F1 | List shows type + description | Open Situations | Every card shows a type (e.g. "Roadworks", "Road closed", "Queuing traffic") and the generalPublicComment | | |
| F2 | Filter incidents / roadworks *(automated UI, sample data)* | Tap "Incidents", then "Roadworks", then "All" | Only that feed is listed; counts match | 4 / 405 / 409 rows (`01`, `03`) | Pass |
| F3 | Text filter | Type "closure" | Only situations whose description contains "closure" | | |
| F4 | Colour-coded delays | Look at roadworks with delays / long / very long delays | Strip and badge yellow / orange / red; no delay = green | | |
| F5 | Duration colour | Compare a short and a long roadworks | Duration badge green (≤3 days), orange (≤4 weeks), red (>4 weeks) | | |
| F6 | Icons | Look at incidents and roadworks | Different icon and colour per type (roadworks cone, closed = no entry sign, queue = cars…) | | |
| F7 | Road search *(automated UI, sample data)* | Search › By road › type "M8" › Search | Only situations on the M8 (not M80/M876); count shown | 23 results, all on the M8 (`04_portrait_search_road.png`) | Pass |
| F8 | Road search autocomplete / chips | Type "A9"; tap a popular road chip | Suggestions list roads present in data; chip runs the search | | |
| F9 | Road search with no results | Search "B9999" | "No results", Show on map disabled | | |
| F10 | Nearby search (fixed place) *(automated UI, sample data)* | Search › Nearby › choose "GCU, Glasgow", radius 10 km | Situations within 10 km, nearest first, distance shown on each card | All results ≤ 10 km, in ascending order (`05_portrait_search_nearby.png`) | Pass |
| F11 | Nearby radius | Move slider to 50 km | More results; all ≤ 50 km | | |
| F12 | Nearby with phone location | Tap "Use my location", allow permission (set emulator location to Glasgow 55.86, -4.25) | Origin "My location", results near that point | | |
| F13 | Location permission denied | Deny permission | Message shown; fixed location kept | | |
| F14 | Map: all | Map tab › All | Markers for all situations in the current filter; lines for from→to | | |
| F15 | Map: road search results | Run road search "M8" › Show on map | Only the M8 situations on the map; title "Road search: M8" | | |
| F16 | Map: nearby | Nearby search › Show on map | Circle of the chosen radius around origin + results | Automated: map tab opens with the same 28 results (`08`); tiles need a Maps key, so check on the emulator | |
| F17 | Map: single | Details › Show on map | Map zooms to that situation | | |
| F18 | Automatic update | Set interval to "Every 1 minute", wait | Banner time changes every minute without user action | | |
| F19 | Update at start-up | Close and reopen the app after the interval | Data refreshes automatically on start | | |

## 4. No internet connection / connection lost

| ID | Test | Steps | Expected outcome | Actual outcome | Result |
|----|------|-------|------------------|----------------|--------|
| C1 | Connection lost while running | With data loaded, turn on aeroplane mode, tap refresh | No crash; banner red "Offline · showing data saved <time>"; previous data still listed | | |
| C2 | Start offline with saved data | Aeroplane mode on, restart app | Last downloaded data shown with red offline banner | | |
| C3 | Connection comes back | Turn aeroplane mode off | Data refreshes by itself; banner green "Live data" | | |
| C4 | First start, offline, nothing saved | Clear app data, aeroplane mode, start | "No data available · check your connection" (or sample data if no keys) – no crash | | |
| C5 | Bad credentials | Wrong key in local.properties | Download fails gracefully; cached/sample data shown | | |
