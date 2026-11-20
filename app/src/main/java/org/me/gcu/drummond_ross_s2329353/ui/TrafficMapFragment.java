package org.me.gcu.drummond_ross_s2329353.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.chip.ChipGroup;

import org.me.gcu.drummond_ross_s2329353.R;
import org.me.gcu.drummond_ross_s2329353.model.GeoPoint;
import org.me.gcu.drummond_ross_s2329353.model.Situation;
import org.me.gcu.drummond_ross_s2329353.model.SituationCategory;
import org.me.gcu.drummond_ross_s2329353.viewmodel.MapContent;
import org.me.gcu.drummond_ross_s2329353.viewmodel.TrafficViewModel;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Google map of all situations, the road / nearby search results, or the selected one.
 * Linear locations are drawn as a line from "from" to "to", coloured by delay, with a marker.
 */
public class TrafficMapFragment extends Fragment {
    private static final LatLng SCOTLAND = new LatLng(56.8, -4.2);

    private TrafficViewModel vm;
    private GoogleMap map;
    private TextView title;
    private ChipGroup modeGroup;
    private boolean updatingChips;
    private final Map<SituationCategory, BitmapDescriptor> icons = new EnumMap<>(SituationCategory.class);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);
        title = view.findViewById(R.id.map_title);
        modeGroup = view.findViewById(R.id.map_mode);
        view.findViewById(R.id.no_key).setVisibility(vm.hasMapsKey() ? View.GONE : View.VISIBLE);

        modeGroup.setOnCheckedStateChangeListener((g, ids) -> {
            if (updatingChips || ids.isEmpty()) return;
            int id = ids.get(0);
            vm.setMapMode(id == R.id.map_road ? MapContent.Mode.ROAD_SEARCH
                    : id == R.id.map_nearby ? MapContent.Mode.NEARBY
                    : id == R.id.map_selected ? MapContent.Mode.SINGLE : MapContent.Mode.ALL);
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_holder);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction().replace(R.id.map_holder, mapFragment).commitNow();
        }
        mapFragment.getMapAsync(googleMap -> {
            map = googleMap;
            map.getUiSettings().setZoomControlsEnabled(true);
            map.getUiSettings().setMapToolbarEnabled(false);
            if (isNightMode()) applyDarkStyle();
            map.setPadding(0, dp(110), 0, 0);
            map.setOnInfoWindowClickListener(this::onMarkerInfoClick);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(SCOTLAND, 6f));
            vm.getMapContent().observe(getViewLifecycleOwner(), this::render);
        });
        vm.getMapContent().observe(getViewLifecycleOwner(), this::renderHeader);
    }

    private void renderHeader(MapContent c) {
        if (c == null) return;
        int n = 0;
        for (Situation s : c.getSituations()) if (s.hasCoordinates()) n++;
        title.setText(c.getTitle() + " · " + getString(R.string.map_count, n));
        updatingChips = true;
        modeGroup.check(c.getMode() == MapContent.Mode.ROAD_SEARCH ? R.id.map_road
                : c.getMode() == MapContent.Mode.NEARBY ? R.id.map_nearby
                : c.getMode() == MapContent.Mode.SINGLE ? R.id.map_selected : R.id.map_all);
        updatingChips = false;
    }

    private void render(MapContent c) {
        if (map == null || c == null) return;
        Context ctx = requireContext();
        map.clear();
        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        int points = 0;

        if (c.getOrigin() != null) {
            GeoPoint o = c.getOrigin().getPoint();
            LatLng centre = new LatLng(o.getLatitude(), o.getLongitude());
            map.addCircle(new CircleOptions().center(centre).radius(c.getRadiusKm() * 1000.0)
                    .strokeColor(0xAA0B4F8A).strokeWidth(3f).fillColor(0x220B4F8A));
            map.addMarker(new MarkerOptions().position(centre).title(c.getOrigin().getName())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            bounds.include(centre);
            points++;
        }

        List<Situation> list = c.getSituations();
        for (Situation s : list) {
            List<GeoPoint> pts = s.getGeoPoints();
            if (pts.isEmpty()) continue;
            int delayColor = SituationStyle.delayColor(ctx, s.getDelaySeverity());
            if (pts.size() >= 2) {
                PolylineOptions line = new PolylineOptions().width(dp(5)).color(delayColor).geodesic(true);
                for (GeoPoint p : pts) line.add(new LatLng(p.getLatitude(), p.getLongitude()));
                map.addPolyline(line);
            }
            GeoPoint centre = s.getCentre();
            LatLng pos = new LatLng(centre.getLatitude(), centre.getLongitude());
            Marker m = map.addMarker(new MarkerOptions().position(pos)
                    .title(s.getTypeLabel() + (s.getPrimaryRoad() == null ? "" : " · " + s.getPrimaryRoad()))
                    .snippet(s.getComment())
                    .anchor(0.5f, 0.5f)
                    .icon(iconFor(s.getCategory(), delayColor)));
            if (m != null) m.setTag(s.getRecordId());
            for (GeoPoint p : pts) bounds.include(new LatLng(p.getLatitude(), p.getLongitude()));
            points++;
        }

        if (points == 0) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(SCOTLAND, 6f));
        } else if (points == 1 && c.getOrigin() == null) {
            GeoPoint centre = list.size() == 1 && list.get(0).getCentre() != null ? list.get(0).getCentre() : null;
            if (centre != null) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(centre.getLatitude(), centre.getLongitude()), 13f));
            } else {
                safeFit(bounds.build());
            }
        } else {
            safeFit(bounds.build());
        }
    }

    private void safeFit(LatLngBounds b) {
        View v = getView();
        if (v == null) return;
        v.post(() -> {
            if (map == null) return;
            try {
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(b, dp(48)));
            } catch (IllegalStateException e) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(b.getCenter(), 9f));
            }
        });
    }

    private BitmapDescriptor iconFor(SituationCategory c, int ring) {
        // Category icons are cached; the ring colour (delay) varies, so key on both when it matters.
        if (c != SituationCategory.ROADWORKS) {
            BitmapDescriptor cached = icons.get(c);
            if (cached == null) {
                cached = BitmapDescriptorFactory.fromBitmap(SituationStyle.markerBitmap(requireContext(), c, Color.WHITE));
                icons.put(c, cached);
            }
            return cached;
        }
        return BitmapDescriptorFactory.fromBitmap(SituationStyle.markerBitmap(requireContext(), c, ring));
    }

    private void onMarkerInfoClick(Marker marker) {
        Object tag = marker.getTag();
        if (!(tag instanceof String)) return;
        MapContent c = vm.getMapContent().getValue();
        if (c == null) return;
        for (Situation s : c.getSituations()) {
            if (tag.equals(s.getRecordId())) { vm.select(s); return; }
        }
    }

    private boolean isNightMode() {
        int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    private void applyDarkStyle() {
        try {
            map.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_dark));
        } catch (Exception ignored) {
            // keep the default style
        }
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
