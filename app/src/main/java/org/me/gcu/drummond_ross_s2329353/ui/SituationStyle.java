package org.me.gcu.drummond_ross_s2329353.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import org.me.gcu.drummond_ross_s2329353.R;
import org.me.gcu.drummond_ross_s2329353.model.DelaySeverity;
import org.me.gcu.drummond_ross_s2329353.model.DurationClass;
import org.me.gcu.drummond_ross_s2329353.model.SituationCategory;

/** Maps model values to icons and colours, shared by the list, detail and map views. */
public final class SituationStyle {
    private SituationStyle() { }

    @DrawableRes
    public static int icon(SituationCategory c) {
        switch (c) {
            case ROADWORKS: return R.drawable.ic_sit_roadworks;
            case ROAD_CLOSED: return R.drawable.ic_sit_road_closed;
            case LANE_RESTRICTION: return R.drawable.ic_sit_lane;
            case QUEUING_TRAFFIC: return R.drawable.ic_sit_queue;
            case ACCIDENT: return R.drawable.ic_sit_accident;
            case OBSTRUCTION: return R.drawable.ic_sit_obstruction;
            case CONDITIONS: return R.drawable.ic_sit_conditions;
            case EVENT: return R.drawable.ic_sit_event;
            default: return R.drawable.ic_sit_other;
        }
    }

    @ColorInt
    public static int color(Context ctx, SituationCategory c) {
        int res;
        switch (c) {
            case ROADWORKS: res = R.color.cat_roadworks; break;
            case ROAD_CLOSED: res = R.color.cat_road_closed; break;
            case LANE_RESTRICTION: res = R.color.cat_lane; break;
            case QUEUING_TRAFFIC: res = R.color.cat_queue; break;
            case ACCIDENT: res = R.color.cat_accident; break;
            case OBSTRUCTION: res = R.color.cat_obstruction; break;
            case CONDITIONS: res = R.color.cat_conditions; break;
            case EVENT: res = R.color.cat_event; break;
            default: res = R.color.cat_other;
        }
        return ContextCompat.getColor(ctx, res);
    }

    @ColorInt
    public static int delayColor(Context ctx, DelaySeverity d) {
        switch (d) {
            case VERY_LONG: return ContextCompat.getColor(ctx, R.color.delay_very_long);
            case LONG: return ContextCompat.getColor(ctx, R.color.delay_long);
            case DELAYS: return ContextCompat.getColor(ctx, R.color.delay_delays);
            default: return ContextCompat.getColor(ctx, R.color.delay_none);
        }
    }

    @ColorInt
    public static int durationColor(Context ctx, DurationClass d) {
        switch (d) {
            case SHORT: return ContextCompat.getColor(ctx, R.color.duration_short);
            case MEDIUM: return ContextCompat.getColor(ctx, R.color.duration_medium);
            case LONG: return ContextCompat.getColor(ctx, R.color.duration_long);
            default: return ContextCompat.getColor(ctx, R.color.duration_unknown);
        }
    }

    /** Round coloured marker bitmap with the white category icon, for the map. */
    public static Bitmap markerBitmap(Context ctx, SituationCategory c, @ColorInt int ringColor) {
        float d = ctx.getResources().getDisplayMetrics().density;
        int size = Math.round(34 * d);
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(ringColor);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
        paint.setColor(color(ctx, c));
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 3 * d, paint);
        Drawable icon = ContextCompat.getDrawable(ctx, icon(c));
        if (icon != null) {
            icon = icon.mutate();
            icon.setTint(0xFFFFFFFF);
            int pad = Math.round(8 * d);
            icon.setBounds(pad, pad, size - pad, size - pad);
            icon.draw(canvas);
        }
        return bmp;
    }
}
