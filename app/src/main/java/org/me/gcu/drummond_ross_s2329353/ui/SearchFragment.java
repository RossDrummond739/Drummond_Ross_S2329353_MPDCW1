package org.me.gcu.drummond_ross_s2329353.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;

import org.me.gcu.drummond_ross_s2329353.R;
import org.me.gcu.drummond_ross_s2329353.data.SettingsRepository;
import org.me.gcu.drummond_ross_s2329353.model.NearbyOrigin;
import org.me.gcu.drummond_ross_s2329353.model.Situation;
import org.me.gcu.drummond_ross_s2329353.viewmodel.MapContent;
import org.me.gcu.drummond_ross_s2329353.viewmodel.TrafficViewModel;
import org.me.gcu.drummond_ross_s2329353.viewmodel.TrafficViewModel.SearchMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Search by road name, or for situations within a distance of the current/fixed location. */
public class SearchFragment extends Fragment {
    private TrafficViewModel vm;
    private SituationAdapter adapter;
    private TextView resultCount, resultsEmpty;
    private View showMap;
    private AutoCompleteTextView roadInput, placeInput;
    private ActivityResultLauncher<String> permissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) locate();
            else Toast.makeText(requireContext(), R.string.location_denied, Toast.LENGTH_LONG).show();
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        View roadPanel = view.findViewById(R.id.road_panel);
        View nearbyPanel = view.findViewById(R.id.nearby_panel);
        MaterialButtonToggleGroup toggle = view.findViewById(R.id.mode_toggle);
        roadInput = view.findViewById(R.id.road_input);
        placeInput = view.findViewById(R.id.place_input);
        ChipGroup popular = view.findViewById(R.id.popular_roads);
        Slider slider = view.findViewById(R.id.radius_slider);
        TextView radiusLabel = view.findViewById(R.id.radius_label);
        resultCount = view.findViewById(R.id.result_count);
        resultsEmpty = view.findViewById(R.id.results_empty);
        showMap = view.findViewById(R.id.show_map);

        RecyclerView results = view.findViewById(R.id.results);
        results.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SituationAdapter(vm::select);
        results.setAdapter(adapter);

        // ---- mode toggle ----
        toggle.check(vm.getSearchMode().getValue() == SearchMode.NEARBY ? R.id.mode_nearby : R.id.mode_road);
        toggle.addOnButtonCheckedListener((g, id, checked) -> {
            if (checked) vm.setSearchMode(id == R.id.mode_nearby ? SearchMode.NEARBY : SearchMode.ROAD);
        });
        vm.getSearchMode().observe(getViewLifecycleOwner(), mode -> {
            int want = mode == SearchMode.NEARBY ? R.id.mode_nearby : R.id.mode_road;
            if (toggle.getCheckedButtonId() != want) toggle.check(want);
            roadPanel.setVisibility(mode == SearchMode.ROAD ? View.VISIBLE : View.GONE);
            nearbyPanel.setVisibility(mode == SearchMode.NEARBY ? View.VISIBLE : View.GONE);
            renderResults();
        });

        // ---- road search ----
        roadInput.setText(vm.getRoadQuery().getValue());
        roadInput.setOnItemClickListener((p, v, pos, id) -> runRoadSearch());
        roadInput.setOnEditorActionListener((v, actionId, e) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { runRoadSearch(); return true; }
            return false;
        });
        view.findViewById(R.id.road_go).setOnClickListener(v -> runRoadSearch());
        vm.getKnownRoads().observe(getViewLifecycleOwner(), roads -> {
            roadInput.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, roads));
            buildPopularChips(popular);
        });
        vm.getRoadResults().observe(getViewLifecycleOwner(), r -> renderResults());

        // ---- nearby search ----
        List<String> places = new ArrayList<>();
        places.add(getString(R.string.my_location));
        for (String p : SettingsRepository.PLACE_NAMES) places.add(p);
        placeInput.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, places));
        placeInput.setOnItemClickListener((p, v, pos, id) -> {
            if (pos == 0) requestLocation();
            else vm.setOrigin(new NearbyOrigin(SettingsRepository.PLACE_NAMES[pos - 1],
                    SettingsRepository.PLACE_POINTS[pos - 1], false), pos - 1);
        });
        view.findViewById(R.id.use_location).setOnClickListener(v -> requestLocation());
        vm.getOrigin().observe(getViewLifecycleOwner(), o -> {
            placeInput.setText(o.getName(), false);
            adapter.setDistanceOrigin(vm.getSearchMode().getValue() == SearchMode.NEARBY ? o.getPoint() : null);
        });

        Integer r = vm.getRadiusKm().getValue();
        slider.setValue(Math.max(1, Math.min(100, r == null ? 20 : r)));
        radiusLabel.setText(getString(R.string.search_radius, (int) slider.getValue()));
        slider.addOnChangeListener((s, value, fromUser) -> radiusLabel.setText(getString(R.string.search_radius, (int) value)));
        slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override public void onStartTrackingTouch(@NonNull Slider s) { }
            @Override public void onStopTrackingTouch(@NonNull Slider s) { vm.setRadiusKm((int) s.getValue()); }
        });
        vm.getNearbyResults().observe(getViewLifecycleOwner(), list -> renderResults());

        showMap.setOnClickListener(v -> vm.showOnMap(vm.getSearchMode().getValue() == SearchMode.NEARBY
                ? MapContent.Mode.NEARBY : MapContent.Mode.ROAD_SEARCH));
        vm.getSelected().observe(getViewLifecycleOwner(), s -> adapter.setSelectedId(s == null ? null : s.getRecordId()));
    }

    private void runRoadSearch() {
        vm.searchRoad(roadInput.getText().toString());
        roadInput.dismissDropDown();
        InputMethodManager imm = ContextCompat.getSystemService(requireContext(), InputMethodManager.class);
        if (imm != null) imm.hideSoftInputFromWindow(roadInput.getWindowToken(), 0);
    }

    /** Chips for the roads with most situations, for one-tap searching. */
    private void buildPopularChips(ChipGroup group) {
        Map<String, Integer> counts = new HashMap<>();
        List<Situation> all = vm.getState().getValue() == null ? new ArrayList<>() : vm.getState().getValue().getSituations();
        for (Situation s : all) {
            String road = s.getPrimaryRoad();
            if (road != null) counts.merge(road, 1, Integer::sum);
        }
        List<Map.Entry<String, Integer>> top = new ArrayList<>(counts.entrySet());
        top.sort((a, b) -> b.getValue() - a.getValue());
        group.removeAllViews();
        for (int i = 0; i < Math.min(10, top.size()); i++) {
            String road = top.get(i).getKey();
            Chip chip = new Chip(requireContext());
            chip.setText(road + " (" + top.get(i).getValue() + ")");
            chip.setOnClickListener(v -> {
                roadInput.setText(road);
                runRoadSearch();
            });
            group.addView(chip);
        }
    }

    private void renderResults() {
        if (resultCount == null) return;
        boolean nearby = vm.getSearchMode().getValue() == SearchMode.NEARBY;
        List<Situation> list = nearby ? vm.getNearbyResults().getValue() : vm.getRoadResults().getValue();
        NearbyOrigin o = vm.getOrigin().getValue();
        adapter.setDistanceOrigin(nearby && o != null ? o.getPoint() : null);
        adapter.submitList(list);
        int n = list == null ? 0 : list.size();
        String q = vm.getRoadQuery().getValue();
        boolean prompt = !nearby && (q == null || q.isEmpty());
        resultCount.setText(prompt ? "" : n == 0 ? getString(R.string.search_results_none) : getString(R.string.search_results, n));
        showMap.setEnabled(n > 0);
        resultsEmpty.setVisibility(n == 0 ? View.VISIBLE : View.GONE);
        resultsEmpty.setText(prompt ? getString(R.string.search_prompt) : getString(R.string.search_results_none));
    }

    private void requestLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locate();
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
    }

    private void locate() {
        Toast.makeText(requireContext(), R.string.location_finding, Toast.LENGTH_SHORT).show();
        LocationHelper.getCoarseLocation(requireContext().getApplicationContext(), point -> {
            if (!isAdded()) return;
            if (point == null) {
                Toast.makeText(requireContext(), R.string.location_unavailable, Toast.LENGTH_LONG).show();
                NearbyOrigin current = vm.getOrigin().getValue();
                if (current != null) placeInput.setText(current.getName(), false);
            } else {
                vm.setOrigin(new NearbyOrigin(getString(R.string.my_location), point, true), -1);
            }
        });
    }
}
