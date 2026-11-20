package org.me.gcu.drummond_ross_s2329353.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.me.gcu.drummond_ross_s2329353.R;
import org.me.gcu.drummond_ross_s2329353.model.DelaySeverity;
import org.me.gcu.drummond_ross_s2329353.model.DurationClass;
import org.me.gcu.drummond_ross_s2329353.model.LocationPoint;
import org.me.gcu.drummond_ross_s2329353.model.Situation;
import org.me.gcu.drummond_ross_s2329353.util.DateUtils;
import org.me.gcu.drummond_ross_s2329353.viewmodel.MapContent;
import org.me.gcu.drummond_ross_s2329353.viewmodel.TrafficViewModel;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;

/** Full details of the selected situation: impact, validity, location(s) and record data. */
public class SituationDetailFragment extends Fragment {
    private TrafficViewModel vm;
    private View content, placeholder;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        // Wrap so we can swap between the details and a "select something" placeholder.
        android.widget.FrameLayout root = new android.widget.FrameLayout(requireContext());
        content = inflater.inflate(R.layout.fragment_detail, root, false);
        placeholder = inflater.inflate(R.layout.fragment_placeholder, root, false);
        root.addView(content);
        root.addView(placeholder);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);
        view.findViewById(R.id.show_on_map).setOnClickListener(v -> vm.showOnMap(MapContent.Mode.SINGLE));
        vm.getSelected().observe(getViewLifecycleOwner(), this::bind);
    }

    private void bind(Situation s) {
        content.setVisibility(s == null ? View.GONE : View.VISIBLE);
        placeholder.setVisibility(s == null ? View.VISIBLE : View.GONE);
        if (s == null) return;
        Context ctx = requireContext();
        View v = content;

        // Header
        ((TextView) v.findViewById(R.id.type)).setText(s.getTypeLabel());
        String feed = s.getFeedType().getLabel()
                + (s.getRoads().isEmpty() ? "" : " · " + String.join(", ", s.getRoads()));
        ((TextView) v.findViewById(R.id.feed)).setText(feed);
        ((TextView) v.findViewById(R.id.comment)).setText(s.getComment() == null ? "(no description)" : s.getComment());
        ((ImageView) v.findViewById(R.id.icon)).setImageResource(SituationStyle.icon(s.getCategory()));
        v.findViewById(R.id.icon_bg).setBackgroundTintList(
                ColorStateList.valueOf(SituationStyle.color(ctx, s.getCategory())));
        v.findViewById(R.id.show_on_map).setVisibility(s.hasCoordinates() ? View.VISIBLE : View.GONE);

        // Impact
        DelaySeverity delay = s.getDelaySeverity();
        TextView delayBadge = v.findViewById(R.id.delay_badge);
        delayBadge.setText(delay.getLabel());
        delayBadge.setBackgroundTintList(ColorStateList.valueOf(SituationStyle.delayColor(ctx, delay)));
        LinearLayout impact = v.findViewById(R.id.impact_rows);
        while (impact.getChildCount() > 1) impact.removeViewAt(1);
        Situation.Impact im = s.getImpact();
        if (im.getLanesRestricted() != null) addRow(impact, getString(R.string.detail_lanes), im.getLanesRestricted() + " restricted");
        if (im.getOperationalLanes() != null) {
            addRow(impact, getString(R.string.detail_lanes), im.getOperationalLanes() == 0
                    ? "No lanes open (closed)" : im.getOperationalLanes() + " open");
        }
        if (im.getDelayTimeSeconds() != null) addRow(impact, "Delay time", (im.getDelayTimeSeconds() / 60) + " min");
        if (s.getSubType() != null) addRow(impact, getString(R.string.detail_type), DateUtils.humanise(s.getSubType()));

        // Validity
        Situation.Validity val = s.getValidity();
        LinearLayout validity = v.findViewById(R.id.validity_rows);
        validity.removeAllViews();
        addRow(validity, getString(R.string.detail_start), DateUtils.format(val.getStart()));
        addRow(validity, getString(R.string.detail_end), val.getEnd() == null ? "Until further notice" : DateUtils.format(val.getEnd()));
        DurationClass dc = val.getDurationClass();
        TextView durBadge = v.findViewById(R.id.duration_badge);
        Duration d = val.getDuration();
        durBadge.setText(d == null ? dc.getLabel() : DateUtils.humanDuration(d) + " · " + dc.getLabel());
        int durColor = SituationStyle.durationColor(ctx, dc);
        durBadge.setBackgroundTintList(ColorStateList.valueOf(durColor));
        LinearProgressIndicator progress = v.findViewById(R.id.progress);
        TextView progressLabel = v.findViewById(R.id.progress_label);
        if (d != null && !d.isZero() && val.getStart() != null) {
            long total = d.toMinutes();
            long done = Duration.between(val.getStart(), OffsetDateTime.now()).toMinutes();
            int pct = (int) Math.max(0, Math.min(100, done * 100 / Math.max(1, total)));
            progress.setVisibility(View.VISIBLE);
            progressLabel.setVisibility(View.VISIBLE);
            progress.setProgressCompat(pct, false);
            progress.setIndicatorColor(durColor);
            progressLabel.setText(done < 0 ? "Not started yet" : pct >= 100 ? "Scheduled end has passed" : pct + "% of planned time elapsed");
        } else {
            progress.setVisibility(View.GONE);
            progressLabel.setVisibility(View.GONE);
        }

        // Location
        LinearLayout loc = v.findViewById(R.id.location_rows);
        loc.removeAllViews();
        if (!s.getRoads().isEmpty()) addRow(loc, getString(R.string.detail_roads), String.join(", ", s.getRoads()));
        if (s.getDirection() != null) addRow(loc, getString(R.string.detail_direction), DateUtils.humanise(s.getDirection()).replace(" bound", "bound"));
        addPoint(loc, getString(R.string.detail_from), s.getFrom());
        addPoint(loc, getString(R.string.detail_to), s.getTo());
        addPoint(loc, "Point", s.getPoint());
        if (s.getFrom() != null && s.getTo() != null && s.getFrom().hasCoordinates() && s.getTo().hasCoordinates()) {
            double km = s.getFrom().toGeoPoint().distanceKmTo(s.getTo().toGeoPoint());
            addRow(loc, "Length", String.format(java.util.Locale.UK, "approx. %.1f km", km));
        }
        if (loc.getChildCount() == 0) addRow(loc, "-", "No location given");

        // Record
        LinearLayout rec = v.findViewById(R.id.record_rows);
        rec.removeAllViews();
        addRow(rec, getString(R.string.detail_id), s.getRecordId() + (s.getVersion() == null ? "" : " (v" + s.getVersion() + ")"));
        addRow(rec, "Record type", DateUtils.humanise(s.getRecordType()));
        if (s.getSourceName() != null) addRow(rec, getString(R.string.detail_source), s.getSourceName()
                + (s.getSourceType() == null ? "" : " (" + DateUtils.humanise(s.getSourceType()) + ")"));
        addRow(rec, getString(R.string.detail_created), DateUtils.format(s.getCreationTime()));
        addRow(rec, getString(R.string.detail_updated), DateUtils.format(s.getVersionTime()));
        if (s.getProbability() != null) addRow(rec, getString(R.string.detail_probability), DateUtils.humanise(s.getProbability()));
        if (s.getComplianceOption() != null) addRow(rec, getString(R.string.detail_compliance), DateUtils.humanise(s.getComplianceOption()));
        if (s.getUrgentRoadworks() != null) addRow(rec, getString(R.string.detail_urgent), s.getUrgentRoadworks() ? "Yes" : "No");

        // Other (unrecognised) data
        LinearLayout other = v.findViewById(R.id.other_rows);
        other.removeAllViews();
        for (Map.Entry<String, String> e : s.getOtherData().entrySet()) addRow(other, e.getKey(), e.getValue());
        int otherVis = s.getOtherData().isEmpty() ? View.GONE : View.VISIBLE;
        v.findViewById(R.id.other_title).setVisibility(otherVis);
        v.findViewById(R.id.other_card).setVisibility(otherVis);
    }

    private void addPoint(LinearLayout parent, String label, LocationPoint p) {
        if (p == null) return;
        StringBuilder sb = new StringBuilder(p.describe());
        if (p.getJunctionName() != null && p.getPlaceName() != null) sb.append("\nJunction ").append(p.getJunctionName());
        if (p.getAreaName() != null && p.getPlaceName() != null) sb.append("\n").append(p.getAreaName());
        if (p.hasCoordinates()) sb.append("\n").append(p.toGeoPoint());
        addRow(parent, label, sb.toString());
    }

    private void addRow(LinearLayout parent, String label, String value) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_detail_row, parent, false);
        ((TextView) row.findViewById(R.id.label)).setText(label);
        ((TextView) row.findViewById(R.id.value)).setText(value == null ? "-" : value);
        parent.addView(row);
    }
}
