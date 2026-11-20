# Scot Traffic — Drummond_Ross_S2329353

Mobile Platform Development (MHI326841) coursework 1, session 2026/27.

An Android app (Java + XML layouts, Android 8.0 / API 26 and later) that downloads the
Traffic Scotland **DATEX II** feeds for *unplanned events* and *current roadworks*, parses them
with `XmlPullParser`, and lets the user browse, search and map the current "situations".

## Opening the project

1. Clone this repository and open the root folder in Android Studio (it is a standard Gradle project).
2. Create/edit `local.properties` in the project root (it is git-ignored) and add your keys:

   ```properties
   TS_CLIENT_ID=your-traffic-scotland-client-id
   TS_CLIENT_KEY=your-traffic-scotland-client-key
   MAPS_API_KEY=your-google-maps-android-api-key
   ```

   The keys are injected into the manifest by the `secrets-gradle-plugin` (as described in
   *Protecting your API keys*), so they never appear in the source code or on GitHub.
   `local.defaults.properties` holds placeholders so the project still builds without them.
3. Run the `app` configuration on an emulator or phone.

Without Traffic Scotland keys the app still runs using the bundled example feeds and says so
in the status banner ("Sample data"). Without a Maps key the map shows no tiles and a notice.

## Features (mapped to the specification)

| Specification item | Where |
|---|---|
| List of all situations showing type + `generalPublicComment` | *Situations* tab (`SituationListFragment`), filter chips for All / Incidents / Roadworks with counts, text filter, pull-to-refresh |
| Detailed information on a selected situation | `SituationDetailFragment`: impact (delays, lanes), validity (start, end, duration, progress), location (roads, direction, from/to names, junctions, coordinates, length), record data (id, version, source, created/updated, probability, compliance, urgent) and any extra fields found in the feed |
| Colour-coded roadworks delays (short/long) | Colour strip + badge on every card and in the details: green none, yellow *delays*, orange *long delays*, red *very long delays*. Planned duration is also colour coded (green ≤ 3 days, orange ≤ 4 weeks, red > 4 weeks) |
| Search by road name | *Search › By road*: autocomplete of every road in the data, one-tap chips for the busiest roads; "M8" matches M8 but not M80, "A74" matches A74 and A74(M); also matches place names in descriptions |
| Search by distance | *Search › Nearby*: phone's coarse location (runtime permission) or a fixed place (GCU Glasgow, Edinburgh, Aberdeen…), radius slider 1–100 km, results sorted by distance and showing it |
| Icons for situation types | 9 original icons: roadworks, road closed, lane restriction, queuing traffic, accident, obstruction/breakdown, road conditions/weather, event, other |
| Map of all / selected situations | *Map* tab: all (current filter), road-search results, nearby results (with radius circle) or the single selected situation. From/to lines coloured by delay, category markers, tap a marker's info window to open its details |
| Portrait and landscape | `layout/activity_main.xml` (toolbar, banner, one pane, bottom navigation) and `layout-land/activity_main.xml` (navigation rail on the left, list/search/map in the middle and details always visible on the right). State is kept in the ViewModel, so rotating keeps the list position, search, selection and map content |

## Architecture — single Activity + Fragments + MVVM

```
ui/            View layer: MainActivity (single activity), SituationListFragment, SearchFragment,
               TrafficMapFragment, SituationDetailFragment, SituationAdapter, SituationStyle, LocationHelper
viewmodel/     TrafficViewModel (activity-scoped, shared by all fragments) + MapContent
data/          TrafficRepository (single source of truth, threading, auto-update, offline cache),
               FeedDownloader, FeedCache, ApiCredentials, SettingsRepository, TrafficState, FeedStatus
parser/        TrafficXmlParser (XmlPullParser)
model/         Situation (+ Impact, Validity), LocationPoint, GeoPoint, FeedType, SituationCategory,
               DelaySeverity, DurationClass, NearbyOrigin, ParseResult
util/          RoadNames (road reference extraction / matching), DateUtils
```

* Fragments only observe `LiveData` from the ViewModel and forward user actions to it.
* The ViewModel derives the filtered list, road search results, nearby results and map content
  with `MediatorLiveData`, so every view updates automatically after each refresh.
* The repository owns all I/O.

## XML processing

`TrafficXmlParser` uses `XmlPullParser` on the downloaded **String**. It keeps a stack of open
elements so generic tags (`<value>`, `<name>`, `<descriptor>`) are interpreted by context, reads
`xsi:type` attributes (record type, location type, point type) and breaks every
`<situationRecord>` down into a `Situation` object:

* identifiers and versions, record type and sub-type (`roadMaintenanceType`, `abnormalTrafficType`,
  `roadOrCarriagewayOrLaneManagementType`, …), creation/version times, probability, source;
* `validity` → start/end as `OffsetDateTime`, duration and duration class;
* `impact` → operational lanes, restricted lanes, `delaysType` → `DelaySeverity`;
* `groupOfLocations` → direction plus `from`/`to` `LocationPoint`s with coordinates, junction name,
  road (linkName / ILC name 1), area (ILC name 2) and place name;
* free text is further split with string methods/regex into road references (`RoadNames`);
* unrecognised elements are kept in an "other data" map, so new feed fields never crash the app.

## Threading and automatic updates

* Downloads and parsing run on an `ExecutorService` (both feeds in parallel), coordinated by a
  single-thread executor so only one refresh runs at a time. Results reach the UI with
  `LiveData.postValue` — no `runOnUiThread` and no `AsyncTask`.
* A `ScheduledExecutorService` refreshes the data at start-up (if older than the interval) and then
  every hour by default. The interval can be changed (down to every minute for demos) from the
  toolbar menu. The banner shows the last and next update time.
* `ConnectivityManager` network callbacks detect when the connection drops/returns; when it
  returns, a refresh runs straight away.

## Offline behaviour

Every successful download is saved to internal storage. If a download fails (no connection,
server error), the last saved data is shown and the banner turns red: *"Offline · showing data
saved …"*. With no saved data and no keys the bundled example feeds are used.

## Tests

* `./gradlew testDebugUnitTest` — JVM unit tests for the parser against the two example feeds
  supplied with the coursework (counts, field values, categories, delays, locations), unknown
  elements, empty feeds, road-name extraction/matching, distance and duration classes.
* `MainActivityUiTest` — Robolectric UI tests running the real activity in portrait and landscape
  (navigation, details, back, filters, road and nearby search, map hand-off, rotation keeping state).
  Saves screenshots in `app/build/screenshots/`. Copies are in `docs/screenshots/`.
* `docs/TESTING.md` — the documented test plan (expected vs actual outcome) for the report.

## Icons

All icons in `res/drawable/ic_sit_*.xml`, `ic_nav_*.xml` and the launcher icon are original vector
artwork drawn for this project, so there are no third-party licence restrictions.

## Data source

Traffic Scotland DATEX II (Transport Scotland). The app downloads only the two permitted
publications; the "About" screen credits the source.
