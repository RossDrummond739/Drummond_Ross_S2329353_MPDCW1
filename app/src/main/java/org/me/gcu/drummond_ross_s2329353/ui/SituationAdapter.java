package org.me.gcu.drummond_ross_s2329353.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import org.me.gcu.drummond_ross_s2329353.R;
import org.me.gcu.drummond_ross_s2329353.model.DelaySeverity;
import org.me.gcu.drummond_ross_s2329353.model.DurationClass;
import org.me.gcu.drummond_ross_s2329353.model.FeedType;
import org.me.gcu.drummond_ross_s2329353.model.GeoPoint;
import org.me.gcu.drummond_ross_s2329353.model.Situation;
import org.me.gcu.drummond_ross_s2329353.util.DateUtils;

import java.util.Locale;
import java.util.Objects;

/** RecyclerView adapter showing a situation's type, description, road, delay and duration. */
public class SituationAdapter extends ListAdapter<Situation, SituationAdapter.Holder> {

    public interface OnSituationClick { void onClick(Situation s); }

    private final OnSituationClick listener;
    private String selectedId;
    private GeoPoint distanceOrigin;

    public SituationAdapter(OnSituationClick listener) {
        super(DIFF);
        this.listener = listener;
    }

    /** Highlights the selected row (useful in the landscape two-pane layout). */
    public void setSelectedId(String id) {
        if (Objects.equals(id, selectedId)) return;
        selectedId = id;
        notifyDataSetChanged();
    }

    /** When set, each row also shows its distance from this point. */
    public void setDistanceOrigin(GeoPoint origin) {
        distanceOrigin = origin;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_situation, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Situation s = getItem(position);
        Context ctx = h.itemView.getContext();

        h.type.setText(s.getTypeLabel());
        h.comment.setText(s.getComment() == null ? "(no description)" : s.getComment());
        String road = s.getPrimaryRoad();
        h.road.setVisibility(road == null ? View.GONE : View.VISIBLE);
        h.road.setText(road);

        h.icon.setImageResource(SituationStyle.icon(s.getCategory()));
        h.iconBg.setBackgroundTintList(ColorStateList.valueOf(SituationStyle.color(ctx, s.getCategory())));

        DelaySeverity delay = s.getDelaySeverity();
        int delayColor = SituationStyle.delayColor(ctx, delay);
        h.strip.setBackgroundColor(delayColor);
        boolean showDelay = s.getFeedType() == FeedType.ROADWORKS || delay != DelaySeverity.NONE;
        h.delay.setVisibility(showDelay ? View.VISIBLE : View.GONE);
        h.delay.setText(delay.getLabel());
        h.delay.setBackgroundTintList(ColorStateList.valueOf(delayColor));

        DurationClass dc = s.getValidity().getDurationClass();
        if (dc == DurationClass.UNKNOWN) {
            if (s.getValidity().getStart() != null) {
                h.duration.setText("Since " + DateUtils.formatDate(s.getValidity().getStart()));
                h.duration.setVisibility(View.VISIBLE);
            } else {
                h.duration.setVisibility(View.GONE);
            }
        } else {
            h.duration.setVisibility(View.VISIBLE);
            h.duration.setText(DateUtils.humanDuration(s.getValidity().getDuration())
                    + " · until " + DateUtils.formatDate(s.getValidity().getEnd()));
        }
        h.duration.setBackgroundTintList(ColorStateList.valueOf(SituationStyle.durationColor(ctx, dc)));

        if (distanceOrigin != null && s.hasCoordinates()) {
            h.distance.setVisibility(View.VISIBLE);
            h.distance.setText(String.format(Locale.UK, "%.1f km", s.distanceKmFrom(distanceOrigin)));
        } else {
            h.distance.setVisibility(View.GONE);
        }

        h.card.setChecked(s.getRecordId() != null && s.getRecordId().equals(selectedId));
        h.card.setOnClickListener(v -> listener.onClick(s));
    }

    static class Holder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final View strip, iconBg;
        final ImageView icon;
        final TextView type, road, comment, delay, duration, distance;

        Holder(View v) {
            super(v);
            card = v.findViewById(R.id.card);
            strip = v.findViewById(R.id.delay_strip);
            iconBg = v.findViewById(R.id.icon_bg);
            icon = v.findViewById(R.id.icon);
            type = v.findViewById(R.id.type);
            road = v.findViewById(R.id.road);
            comment = v.findViewById(R.id.comment);
            delay = v.findViewById(R.id.delay_badge);
            duration = v.findViewById(R.id.duration_badge);
            distance = v.findViewById(R.id.distance);
        }
    }

    private static final DiffUtil.ItemCallback<Situation> DIFF = new DiffUtil.ItemCallback<Situation>() {
        @Override
        public boolean areItemsTheSame(@NonNull Situation a, @NonNull Situation b) {
            return Objects.equals(a.getRecordId(), b.getRecordId()) && a.getFeedType() == b.getFeedType();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Situation a, @NonNull Situation b) {
            return Objects.equals(a.getVersion(), b.getVersion())
                    && Objects.equals(a.getComment(), b.getComment())
                    && a.getDelaySeverity() == b.getDelaySeverity();
        }
    };
}
