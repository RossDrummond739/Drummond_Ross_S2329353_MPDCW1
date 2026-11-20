package org.me.gcu.drummond_ross_s2329353.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import org.me.gcu.drummond_ross_s2329353.data.SettingsRepository;
import org.me.gcu.drummond_ross_s2329353.data.TrafficRepository;
import org.me.gcu.drummond_ross_s2329353.data.TrafficState;
import org.me.gcu.drummond_ross_s2329353.model.FeedType;
import org.me.gcu.drummond_ross_s2329353.model.GeoPoint;
import org.me.gcu.drummond_ross_s2329353.model.NearbyOrigin;
import org.me.gcu.drummond_ross_s2329353.model.Situation;
import org.me.gcu.drummond_ross_s2329353.util.RoadNames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Activity-scoped ViewModel shared by all fragments. Holds UI state (selected tab, filter,
 * selection, search inputs) and exposes derived LiveData (filtered list, search results,
 * map content). Survives rotation, so switching layouts keeps the displayed data.
 */
public class TrafficViewModel extends AndroidViewModel {

    public enum Filter { ALL, INCIDENTS, ROADWORKS }
    public enum Tab { LIST, SEARCH, MAP }
    public enum SearchMode { ROAD, NEARBY }

    private final TrafficRepository repository;
    private final SettingsRepository settings;

    private final MutableLiveData<Filter> filter = new MutableLiveData<>(Filter.ALL);
    private final MutableLiveData<String> listText = new MutableLiveData<>("");
    private final MediatorLiveData<List<Situation>> listItems = new MediatorLiveData<>();

    private final MutableLiveData<String> selectedId = new MutableLiveData<>(null);
    private final MediatorLiveData<Situation> selected = new MediatorLiveData<>();
    private final MutableLiveData<Boolean> detailOpen = new MutableLiveData<>(false);
    private final MutableLiveData<Tab> tab = new MutableLiveData<>(Tab.LIST);

    private final MutableLiveData<SearchMode> searchMode = new MutableLiveData<>(SearchMode.ROAD);
    private final MutableLiveData<String> roadQuery = new MutableLiveData<>("");
    private final MediatorLiveData<List<Situation>> roadResults = new MediatorLiveData<>();
    private final MediatorLiveData<List<String>> knownRoads = new MediatorLiveData<>();

    private final MutableLiveData<NearbyOrigin> origin = new MutableLiveData<>();
    private final MutableLiveData<Integer> radiusKm = new MutableLiveData<>();
    private final MediatorLiveData<List<Situation>> nearbyResults = new MediatorLiveData<>();

    private final MutableLiveData<MapContent.Mode> mapMode = new MutableLiveData<>(MapContent.Mode.ALL);
    private final MediatorLiveData<MapContent> mapContent = new MediatorLiveData<>();

    public TrafficViewModel(@NonNull Application app) {
        super(app);
        repository = TrafficRepository.getInstance(app);
        settings = new SettingsRepository(app);

        int place = Math.min(settings.getPlaceIndex(), SettingsRepository.PLACE_NAMES.length - 1);
        origin.setValue(new NearbyOrigin(SettingsRepository.PLACE_NAMES[place],
                SettingsRepository.PLACE_POINTS[place], false));
        radiusKm.setValue(settings.getRadiusKm());

        LiveData<TrafficState> state = repository.getState();

        listItems.addSource(state, s -> updateList());
        listItems.addSource(filter, f -> updateList());
        listItems.addSource(listText, t -> updateList());

        selected.addSource(state, s -> updateSelected());
        selected.addSource(selectedId, id -> updateSelected());

        knownRoads.addSource(state, s -> updateKnownRoads());
        roadResults.addSource(state, s -> updateRoadResults());
        roadResults.addSource(roadQuery, q -> updateRoadResults());

        nearbyResults.addSource(state, s -> updateNearby());
        nearbyResults.addSource(origin, o -> updateNearby());
        nearbyResults.addSource(radiusKm, r -> updateNearby());

        mapContent.addSource(mapMode, m -> updateMap());
        mapContent.addSource(listItems, l -> updateMap());
        mapContent.addSource(roadResults, l -> updateMap());
        mapContent.addSource(nearbyResults, l -> updateMap());
        mapContent.addSource(selected, s -> updateMap());
    }

    // ---------------- exposed state ----------------
    public LiveData<TrafficState> getState() { return repository.getState(); }
    public LiveData<Boolean> isOnline() { return repository.isOnline(); }
    public LiveData<Long> getNextAutoRefresh() { return repository.getNextAutoRefresh(); }
    public LiveData<Filter> getFilter() { return filter; }
    public LiveData<List<Situation>> getListItems() { return listItems; }
    public LiveData<Situation> getSelected() { return selected; }
    public LiveData<Boolean> getDetailOpen() { return detailOpen; }
    public LiveData<Tab> getTab() { return tab; }
    public LiveData<SearchMode> getSearchMode() { return searchMode; }
    public LiveData<String> getRoadQuery() { return roadQuery; }
    public LiveData<List<Situation>> getRoadResults() { return roadResults; }
    public LiveData<List<String>> getKnownRoads() { return knownRoads; }
    public LiveData<NearbyOrigin> getOrigin() { return origin; }
    public LiveData<Integer> getRadiusKm() { return radiusKm; }
    public LiveData<List<Situation>> getNearbyResults() { return nearbyResults; }
    public LiveData<MapContent> getMapContent() { return mapContent; }
    public boolean hasMapsKey() { return repository.getCredentials().hasMapsKey(); }
    public boolean hasTrafficKeys() { return repository.getCredentials().hasTrafficScotlandKeys(); }
    public int getRefreshMinutes() { return settings.getRefreshMinutes(); }
    public int getPlaceIndex() { return settings.getPlaceIndex(); }

    // ---------------- actions ----------------
    public void refresh() { repository.refresh(); }

    public void startAutoRefresh() { repository.startAutoRefresh(settings.getRefreshMinutes()); }

    public void stopAutoRefresh() { repository.stopAutoRefresh(); }

    public void setRefreshMinutes(int minutes) {
        settings.setRefreshMinutes(minutes);
        startAutoRefresh();
    }

    public void setFilter(Filter f) { filter.setValue(f); }

    public void setListText(String text) { listText.setValue(text == null ? "" : text); }

    public void select(Situation s) {
        selectedId.setValue(s == null ? null : s.getRecordId());
        detailOpen.setValue(s != null);
    }

    public void closeDetail() { detailOpen.setValue(false); }

    public void clearSelection() {
        selectedId.setValue(null);
        detailOpen.setValue(false);
    }

    public void setTab(Tab t) {
        if (t != tab.getValue()) tab.setValue(t);
        detailOpen.setValue(false);
    }

    public void setSearchMode(SearchMode m) { searchMode.setValue(m); }

    public void searchRoad(String query) { roadQuery.setValue(query == null ? "" : query.trim()); }

    public void setOrigin(NearbyOrigin o, int placeIndex) {
        if (placeIndex >= 0) settings.setPlaceIndex(placeIndex);
        origin.setValue(o);
    }

    public void setRadiusKm(int km) {
        settings.setRadiusKm(km);
        radiusKm.setValue(km);
    }

    /** Switch to the map tab showing the given set. */
    public void showOnMap(MapContent.Mode mode) {
        mapMode.setValue(mode);
        setTab(Tab.MAP);
    }

    public void setMapMode(MapContent.Mode mode) { mapMode.setValue(mode); }

    // ---------------- derivations ----------------
    private List<Situation> all() {
        TrafficState s = repository.getState().getValue();
        return s == null ? Collections.emptyList() : s.getSituations();
    }

    private void updateList() {
        Filter f = filter.getValue();
        String text = listText.getValue() == null ? "" : listText.getValue().toLowerCase(Locale.UK).trim();
        List<Situation> out = new ArrayList<>();
        for (Situation s : all()) {
            if (f == Filter.INCIDENTS && s.getFeedType() != FeedType.UNPLANNED) continue;
            if (f == Filter.ROADWORKS && s.getFeedType() != FeedType.ROADWORKS) continue;
            if (!text.isEmpty()) {
                String c = s.getComment() == null ? "" : s.getComment().toLowerCase(Locale.UK);
                if (!c.contains(text) && !s.getTypeLabel().toLowerCase(Locale.UK).contains(text)) continue;
            }
            out.add(s);
        }
        listItems.setValue(out);
    }

    private void updateSelected() {
        String id = selectedId.getValue();
        Situation found = null;
        if (id != null) {
            for (Situation s : all()) {
                if (id.equals(s.getRecordId())) { found = s; break; }
            }
        }
        selected.setValue(found);
    }

    private void updateKnownRoads() {
        Set<String> roads = new TreeSet<>(Comparator.comparing(RoadNames::sortKey));
        for (Situation s : all()) roads.addAll(s.getRoads());
        knownRoads.setValue(new ArrayList<>(roads));
    }

    private void updateRoadResults() {
        String q = roadQuery.getValue();
        List<Situation> out = new ArrayList<>();
        if (q != null && !q.isEmpty()) {
            String lower = q.toLowerCase(Locale.UK);
            boolean looksLikeRoad = !RoadNames.extract(q).isEmpty();
            for (Situation s : all()) {
                if (s.isOnRoad(q)) {
                    out.add(s);
                } else if (!looksLikeRoad && s.getComment() != null
                        && s.getComment().toLowerCase(Locale.UK).contains(lower)) {
                    out.add(s);   // place names such as "Kingston Bridge"
                }
            }
        }
        roadResults.setValue(out);
    }

    private void updateNearby() {
        NearbyOrigin o = origin.getValue();
        Integer r = radiusKm.getValue();
        List<Situation> out = new ArrayList<>();
        if (o != null && r != null) {
            GeoPoint p = o.getPoint();
            for (Situation s : all()) {
                if (s.distanceKmFrom(p) <= r) out.add(s);
            }
            out.sort(Comparator.comparingDouble(s -> s.distanceKmFrom(p)));
        }
        nearbyResults.setValue(out);
    }

    private void updateMap() {
        MapContent.Mode mode = mapMode.getValue() == null ? MapContent.Mode.ALL : mapMode.getValue();
        List<Situation> items;
        String title;
        switch (mode) {
            case ROAD_SEARCH:
                items = roadResults.getValue();
                title = "Road search: " + roadQuery.getValue();
                break;
            case NEARBY:
                items = nearbyResults.getValue();
                NearbyOrigin o = origin.getValue();
                title = "Within " + radiusKm.getValue() + " km of " + (o == null ? "?" : o.getName());
                break;
            case SINGLE:
                Situation s = selected.getValue();
                items = s == null ? Collections.emptyList() : Collections.singletonList(s);
                title = s == null ? "No situation selected" : s.getTypeLabel();
                break;
            default:
                items = listItems.getValue();
                Filter f = filter.getValue();
                title = f == Filter.INCIDENTS ? "All incidents" : f == Filter.ROADWORKS ? "All roadworks"
                        : "All incidents and roadworks";
        }
        mapContent.setValue(new MapContent(mode, title, items == null ? Collections.emptyList() : items,
                mode == MapContent.Mode.NEARBY ? origin.getValue() : null,
                radiusKm.getValue() == null ? 0 : radiusKm.getValue()));
    }
}
