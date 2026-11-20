package org.me.gcu.drummond_ross_s2329353;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.me.gcu.drummond_ross_s2329353.data.TrafficState;
import org.me.gcu.drummond_ross_s2329353.model.FeedType;
import org.me.gcu.drummond_ross_s2329353.model.Situation;
import org.me.gcu.drummond_ross_s2329353.ui.MainActivity;
import org.me.gcu.drummond_ross_s2329353.viewmodel.TrafficViewModel;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Runs the real activity (Robolectric) with the bundled sample feeds: checks navigation,
 * the two-pane landscape layout, search and that state survives rotation.
 * Screenshots go to app/build/screenshots for the test report.
 */
@RunWith(AndroidJUnit4.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34, qualifiers = "w411dp-h891dp-port-xxhdpi")
public class MainActivityUiTest {

    private static void waitUntil(BooleanSupplier cond) throws InterruptedException {
        for (int i = 0; i < 2400; i++) {
            shadowOf(Looper.getMainLooper()).idle();
            if (cond.getAsBoolean()) return;
            Thread.sleep(25);
        }
        throw new AssertionError("timed out");
    }

    private static void shot(MainActivity a, String name) {
        View root = a.getWindow().getDecorView();
        shadowOf(Looper.getMainLooper()).idle();
        Bitmap bmp = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bmp));
        File dir = new File("build/screenshots");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @org.junit.Before
    public void logs() { org.robolectric.shadows.ShadowLog.stream = System.out; }

    private static TrafficViewModel vm(MainActivity a) {
        return new ViewModelProvider(a).get(TrafficViewModel.class);
    }

    private static boolean loaded(MainActivity a) {
        TrafficState s = vm(a).getState().getValue();
        return s != null && !s.isLoading() && !s.getSituations().isEmpty();
    }

    @Test
    public void portraitListDetailSearchAndBack() throws Exception {
        try (ActivityScenario<MainActivity> sc = ActivityScenario.launch(MainActivity.class)) {
            sc.onActivity(a -> {
                try {
                    waitUntil(() -> loaded(a));
                    TrafficState st = vm(a).getState().getValue();
                    // Live data if local.properties has keys, otherwise the bundled samples (4 + 405)
                    final int inc = st.count(FeedType.UNPLANNED), rw = st.count(FeedType.ROADWORKS);
                    assertTrue(inc > 0 && rw > 0);
                    if (st.anySource(org.me.gcu.drummond_ross_s2329353.data.DataSource.SAMPLE)) {
                        assertEquals(4, inc);
                        assertEquals(405, rw);
                    }
                    assertEquals(View.GONE, a.findViewById(R.id.detail_container).getVisibility());

                    RecyclerView rv = a.findViewById(R.id.recycler);
                    waitUntil(() -> rv.getAdapter() != null && rv.getAdapter().getItemCount() == inc + rw);
                    TextView status = a.findViewById(R.id.status_text);
                    String bannerText = status.getText().toString();
                    assertTrue(bannerText, bannerText.startsWith("Sample data") || bannerText.startsWith("Live data"));
                    shot(a, "01_portrait_list");

                    // Open the first incident
                    rv.findViewHolderForAdapterPosition(0).itemView.performClick();
                    waitUntil(() -> Boolean.TRUE.equals(vm(a).getDetailOpen().getValue()));
                    Fragment main = a.getSupportFragmentManager().findFragmentById(R.id.main_container);
                    assertEquals("detail", main.getTag());
                    TextView comment = main.requireView().findViewById(R.id.comment);
                    Situation sel = vm(a).getSelected().getValue();
                    assertEquals(sel.getComment(), comment.getText().toString());
                    shot(a, "02_portrait_detail");

                    // Back returns to the list
                    a.getOnBackPressedDispatcher().onBackPressed();
                    shadowOf(Looper.getMainLooper()).idle();
                    waitUntil(() -> "tab_LIST".equals(a.getSupportFragmentManager()
                            .findFragmentById(R.id.main_container).getTag()));

                    // Filter roadworks
                    vm(a).setFilter(TrafficViewModel.Filter.ROADWORKS);
                    RecyclerView rv2 = a.findViewById(R.id.recycler);   // list fragment was recreated
                    waitUntil(() -> rv2.getAdapter().getItemCount() == rw);
                    shot(a, "03_portrait_roadworks");

                    // Road search via the Search tab
                    a.<com.google.android.material.navigation.NavigationBarView>findViewById(R.id.nav)
                            .setSelectedItemId(R.id.nav_search);
                    waitUntil(() -> "tab_SEARCH".equals(a.getSupportFragmentManager()
                            .findFragmentById(R.id.main_container).getTag()));
                    View search = a.getSupportFragmentManager().findFragmentById(R.id.main_container).requireView();
                    AutoCompleteTextView input = search.findViewById(R.id.road_input);
                    input.setText("M8");
                    search.findViewById(R.id.road_go).performClick();
                    waitUntil(() -> vm(a).getRoadResults().getValue() != null
                            && !vm(a).getRoadResults().getValue().isEmpty());
                    for (Situation s : vm(a).getRoadResults().getValue()) assertTrue(s.isOnRoad("M8"));
                    shot(a, "04_portrait_search_road");

                    // Nearby search from GCU, 10 km
                    vm(a).setSearchMode(TrafficViewModel.SearchMode.NEARBY);
                    vm(a).setRadiusKm(10);
                    waitUntil(() -> vm(a).getNearbyResults().getValue() != null);
                    List<Situation> near = vm(a).getNearbyResults().getValue();
                    assertTrue(!near.isEmpty());
                    double prev = -1;
                    for (Situation s : near) {
                        double d = s.distanceKmFrom(vm(a).getOrigin().getValue().getPoint());
                        assertTrue(d <= 10 && d >= prev);
                        prev = d;
                    }
                    shot(a, "05_portrait_search_nearby");

                    // Show the nearby results on the map (map tiles need a key / Play services, so only the
                    // header and the content passed to the map are checked here)
                    a.getSupportFragmentManager().findFragmentById(R.id.main_container).requireView()
                            .findViewById(R.id.show_map).performClick();
                    waitUntil(() -> "tab_MAP".equals(a.getSupportFragmentManager()
                            .findFragmentById(R.id.main_container).getTag()));
                    assertEquals(near.size(), vm(a).getMapContent().getValue().getSituations().size());
                    shot(a, "08_portrait_map_nearby");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xxhdpi")
    public void landscapeShowsTwoPanes() throws Exception {
        try (ActivityScenario<MainActivity> sc = ActivityScenario.launch(MainActivity.class)) {
            sc.onActivity(a -> {
                try {
                    waitUntil(() -> loaded(a));
                    assertEquals(View.VISIBLE, a.findViewById(R.id.detail_container).getVisibility());
                    RecyclerView rv = a.findViewById(R.id.recycler);
                    waitUntil(() -> rv.getAdapter() != null && rv.getAdapter().getItemCount() > 0);
                    rv.findViewHolderForAdapterPosition(1).itemView.performClick();
                    waitUntil(() -> vm(a).getSelected().getValue() != null);
                    // List stays on the left, details on the right
                    assertEquals("tab_LIST", a.getSupportFragmentManager().findFragmentById(R.id.main_container).getTag());
                    Fragment pane = a.getSupportFragmentManager().findFragmentById(R.id.detail_container);
                    assertNotNull(pane);
                    assertEquals(View.VISIBLE, pane.requireView().findViewById(R.id.content).getVisibility());
                    shot(a, "06_landscape_two_pane");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Test
    public void rotationKeepsSelectionAndFilter() throws Exception {
        try (ActivityScenario<MainActivity> sc = ActivityScenario.launch(MainActivity.class)) {
            final String[] id = new String[1];
            sc.onActivity(a -> {
                try {
                    waitUntil(() -> loaded(a));
                    vm(a).setFilter(TrafficViewModel.Filter.INCIDENTS);
                    int inc = vm(a).getState().getValue().count(FeedType.UNPLANNED);
                    RecyclerView rv = a.findViewById(R.id.recycler);
                    waitUntil(() -> rv.getAdapter() != null && rv.getAdapter().getItemCount() == inc);
                    rv.findViewHolderForAdapterPosition(2).itemView.performClick();
                    waitUntil(() -> vm(a).getSelected().getValue() != null);
                    id[0] = vm(a).getSelected().getValue().getRecordId();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            RuntimeEnvironment.setQualifiers("w891dp-h411dp-land-xxhdpi");
            sc.recreate();
            sc.onActivity(a -> {
                try {
                    waitUntil(() -> a.getSupportFragmentManager().findFragmentById(R.id.detail_container) != null);
                    assertEquals(View.VISIBLE, a.findViewById(R.id.detail_container).getVisibility());
                    assertEquals(id[0], vm(a).getSelected().getValue().getRecordId());
                    assertEquals(TrafficViewModel.Filter.INCIDENTS, vm(a).getFilter().getValue());
                    shot(a, "07_rotated_landscape_keeps_state");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            RuntimeEnvironment.setQualifiers("w411dp-h891dp-port-xxhdpi");
            sc.recreate();
            sc.onActivity(a -> {
                try {
                    waitUntil(() -> "detail".equals(a.getSupportFragmentManager()
                            .findFragmentById(R.id.main_container).getTag()));
                    assertEquals(id[0], vm(a).getSelected().getValue().getRecordId());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
