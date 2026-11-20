package org.me.gcu.drummond_ross_s2329353.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import org.me.gcu.drummond_ross_s2329353.R;
import org.me.gcu.drummond_ross_s2329353.data.TrafficState;
import org.me.gcu.drummond_ross_s2329353.model.FeedType;
import org.me.gcu.drummond_ross_s2329353.viewmodel.TrafficViewModel;
import org.me.gcu.drummond_ross_s2329353.viewmodel.TrafficViewModel.Filter;

/** List of all current situations, filterable by incidents / roadworks and by description text. */
public class SituationListFragment extends Fragment {
    private TrafficViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        RecyclerView recycler = view.findViewById(R.id.recycler);
        TextView empty = view.findViewById(R.id.empty);
        SwipeRefreshLayout swipe = view.findViewById(R.id.swipe_refresh);
        ChipGroup filters = view.findViewById(R.id.filter_group);
        Chip all = view.findViewById(R.id.chip_all);
        Chip incidents = view.findViewById(R.id.chip_incidents);
        Chip roadworks = view.findViewById(R.id.chip_roadworks);
        TextInputEditText filterText = view.findViewById(R.id.filter_text);

        SituationAdapter adapter = new SituationAdapter(vm::select);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        swipe.setOnRefreshListener(vm::refresh);

        Filter f = vm.getFilter().getValue();
        filters.check(f == Filter.INCIDENTS ? R.id.chip_incidents : f == Filter.ROADWORKS ? R.id.chip_roadworks : R.id.chip_all);
        filters.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            vm.setFilter(id == R.id.chip_incidents ? Filter.INCIDENTS
                    : id == R.id.chip_roadworks ? Filter.ROADWORKS : Filter.ALL);
            recycler.scrollToPosition(0);
        });

        filterText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { vm.setListText(s.toString()); }
        });

        vm.getListItems().observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list);
            TrafficState st = vm.getState().getValue();
            boolean loading = st == null || (st.isLoading() && st.getSituations().isEmpty());
            empty.setText(loading ? R.string.list_loading : R.string.list_empty);
            empty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
        });
        vm.getState().observe(getViewLifecycleOwner(), st -> {
            swipe.setRefreshing(st.isLoading() && !st.getSituations().isEmpty());
            int inc = st.count(FeedType.UNPLANNED), rw = st.count(FeedType.ROADWORKS);
            all.setText(getString(R.string.filter_all, inc + rw));
            incidents.setText(getString(R.string.filter_incidents, inc));
            roadworks.setText(getString(R.string.filter_roadworks, rw));
        });
        vm.getSelected().observe(getViewLifecycleOwner(),
                s -> adapter.setSelectedId(s == null ? null : s.getRecordId()));
    }
}
