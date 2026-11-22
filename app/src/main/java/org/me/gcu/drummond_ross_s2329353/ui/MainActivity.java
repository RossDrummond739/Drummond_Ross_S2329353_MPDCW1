package org.me.gcu.drummond_ross_s2329353.ui;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import org.me.gcu.drummond_ross_s2329353.R;
import org.me.gcu.drummond_ross_s2329353.data.DataSource;
import org.me.gcu.drummond_ross_s2329353.data.SettingsRepository;
import org.me.gcu.drummond_ross_s2329353.data.TrafficState;
import org.me.gcu.drummond_ross_s2329353.util.DateUtils;
import org.me.gcu.drummond_ross_s2329353.viewmodel.TrafficViewModel;
import org.me.gcu.drummond_ross_s2329353.viewmodel.TrafficViewModel.Tab;

/**
 * The single Activity. It hosts the fragments and decides where they go:
 * <ul>
 *   <li>Portrait: one pane; the detail view replaces the current tab (Back returns).</li>
 *   <li>Landscape: tab content on the left, details permanently on the right.</li>
 * </ul>
 * All state lives in {@link TrafficViewModel}, so the layout can be rebuilt after rotation
 * without losing the list, the search or the selected situation.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG_TAB = "tab_";
    private static final String TAG_DETAIL = "detail";
    private static final String TAG_DETAIL_PANE = "detail_pane";

    private TrafficViewModel viewModel;
    private boolean twoPane;
    private NavigationBarView nav;
    private View banner;
    private TextView bannerText;
    private CircularProgressIndicator bannerProgress;
    private OnBackPressedCallback backCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        viewModel = new ViewModelProvider(this).get(TrafficViewModel.class);
        twoPane = findViewById(R.id.detail_container).getVisibility() == View.VISIBLE;

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        applyWindowInsets(toolbar);
        toolbar.inflateMenu(R.menu.main_menu);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_refresh) {
                viewModel.refresh();
            } else if (id == R.id.action_settings) {
                showIntervalDialog();
            } else if (id == R.id.action_about) {
                new MaterialAlertDialogBuilder(this).setTitle(R.string.about_title)
                        .setMessage(R.string.about_text).setPositiveButton(R.string.ok, null).show();
            }
            return true;
        });

        banner = findViewById(R.id.status_banner);
        bannerText = findViewById(R.id.status_text);
        bannerProgress = findViewById(R.id.status_progress);

        nav = findViewById(R.id.nav);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            viewModel.setTab(id == R.id.nav_search ? Tab.SEARCH : id == R.id.nav_map ? Tab.MAP : Tab.LIST);
            return true;
        });
        nav.setOnItemReselectedListener(item -> viewModel.closeDetail());

        backCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                if (!twoPane && Boolean.TRUE.equals(viewModel.getDetailOpen().getValue())) {
                    viewModel.closeDetail();
                } else if (viewModel.getTab().getValue() != Tab.LIST) {
                    viewModel.setTab(Tab.LIST);
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backCallback);

        viewModel.getTab().observe(this, t -> {
            int id = t == Tab.SEARCH ? R.id.nav_search : t == Tab.MAP ? R.id.nav_map : R.id.nav_list;
            if (nav.getSelectedItemId() != id) nav.setSelectedItemId(id);
            renderPanes();
        });
        viewModel.getDetailOpen().observe(this, open -> renderPanes());
        viewModel.getState().observe(this, this::renderStatus);
        viewModel.isOnline().observe(this, o -> renderStatus(viewModel.getState().getValue()));
        viewModel.getNextAutoRefresh().observe(this, t -> renderStatus(viewModel.getState().getValue()));
    }

    /**
     * Apps targeting Android 15 are drawn edge-to-edge (behind the status and navigation bars).
     * Pad the toolbar below the status bar / camera cutout, keep content clear of the side cutouts
     * and navigation bar, and make room for the keyboard.
     */
    private void applyWindowInsets(MaterialToolbar toolbar) {
        View root = findViewById(R.id.root);
        View navView = findViewById(R.id.nav);
        final int toolbarHeight = toolbar.getLayoutParams().height;
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            int ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;

            ViewGroup.LayoutParams lp = toolbar.getLayoutParams();
            lp.height = toolbarHeight + bars.top;
            toolbar.setLayoutParams(lp);
            toolbar.setPadding(0, bars.top, 0, 0);

            if (twoPane) {
                // Landscape: nothing is docked at the bottom, so the whole layout sits above the nav bar
                v.setPadding(bars.left, 0, bars.right, Math.max(ime, bars.bottom));
            } else {
                // Portrait: the bottom navigation bar extends behind the gesture bar
                v.setPadding(bars.left, 0, bars.right, ime);
                navView.setPadding(navView.getPaddingLeft(), navView.getPaddingTop(),
                        navView.getPaddingRight(), ime > 0 ? 0 : bars.bottom);
            }
            return WindowInsetsCompat.CONSUMED;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Downloads data at start-up (if stale) and then at the chosen interval.
        viewModel.startAutoRefresh();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isChangingConfigurations()) viewModel.stopAutoRefresh();
    }

    /** Puts the right fragments in the containers for the current orientation and state. */
    private void renderPanes() {
        FragmentManager fm = getSupportFragmentManager();
        Tab tab = viewModel.getTab().getValue();
        if (tab == null) tab = Tab.LIST;
        boolean detailOpen = Boolean.TRUE.equals(viewModel.getDetailOpen().getValue());
        backCallback.setEnabled((!twoPane && detailOpen) || tab != Tab.LIST);

        if (twoPane) {
            showIn(fm, R.id.main_container, TAG_TAB + tab.name(), tabFragment(tab));
            Fragment pane = fm.findFragmentById(R.id.detail_container);
            if (pane == null || !TAG_DETAIL_PANE.equals(pane.getTag())) {
                fm.beginTransaction().replace(R.id.detail_container, new SituationDetailFragment(), TAG_DETAIL_PANE)
                        .commitNow();
            }
        } else {
            Fragment hidden = fm.findFragmentById(R.id.detail_container);
            if (hidden != null) fm.beginTransaction().remove(hidden).commitNow();
            if (detailOpen) {
                showIn(fm, R.id.main_container, TAG_DETAIL, new SituationDetailFragment());
            } else {
                showIn(fm, R.id.main_container, TAG_TAB + tab.name(), tabFragment(tab));
            }
        }
    }

    private static void showIn(FragmentManager fm, int container, String tag, Fragment fresh) {
        Fragment current = fm.findFragmentById(container);
        if (current != null && tag.equals(current.getTag())) return;
        fm.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(container, fresh, tag)
                .commitNow();
    }

    private static Fragment tabFragment(Tab tab) {
        switch (tab) {
            case SEARCH: return new SearchFragment();
            case MAP: return new TrafficMapFragment();
            default: return new SituationListFragment();
        }
    }

    /** Status banner: live / offline (cached) / sample data, with last and next update times. */
    private void renderStatus(TrafficState s) {
        if (s == null) return;
        boolean online = !Boolean.FALSE.equals(viewModel.isOnline().getValue());
        int color;
        String text;
        if (s.isLoading()) {
            color = R.color.status_loading;
            text = getString(R.string.status_loading);
        } else if (s.allLive()) {
            color = R.color.status_live;
            Long next = viewModel.getNextAutoRefresh().getValue();
            String last = DateUtils.formatTime(s.oldestDataTimestamp());
            text = next != null && next > 0
                    ? getString(R.string.status_live_next, last, DateUtils.formatTime(next))
                    : getString(R.string.status_live, last);
        } else if (s.anySource(DataSource.CACHE)) {
            color = R.color.status_offline;
            String when = DateUtils.formatDateTime(s.oldestDataTimestamp());
            text = online ? getString(R.string.status_cache, when) : getString(R.string.status_offline, when);
        } else if (s.anySource(DataSource.SAMPLE)) {
            color = R.color.status_sample;
            text = getString(R.string.status_sample);
        } else {
            color = R.color.status_offline;
            text = getString(R.string.status_none);
        }
        banner.setBackgroundTintList(null);
        banner.setBackgroundColor(ContextCompat.getColor(this, color));
        bannerText.setText(text);
        bannerProgress.setVisibility(s.isLoading() ? View.VISIBLE : View.GONE);
        bannerProgress.setIndicatorColor(0xFFFFFFFF);
        bannerProgress.setTrackColor(ColorStateList.valueOf(0x33FFFFFF).getDefaultColor());
    }

    private void showIntervalDialog() {
        int[] opts = SettingsRepository.REFRESH_OPTIONS_MIN;
        String[] labels = new String[opts.length];
        int checked = 0;
        for (int i = 0; i < opts.length; i++) {
            labels[i] = opts[i] < 60 ? "Every " + opts[i] + (opts[i] == 1 ? " minute (demo)" : " minutes")
                    : "Every " + (opts[i] / 60) + (opts[i] == 60 ? " hour" : " hours");
            if (opts[i] == viewModel.getRefreshMinutes()) checked = i;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_title)
                .setSingleChoiceItems(labels, checked, (d, which) -> {
                    viewModel.setRefreshMinutes(opts[which]);
                    d.dismiss();
                })
                .show();
    }
}
