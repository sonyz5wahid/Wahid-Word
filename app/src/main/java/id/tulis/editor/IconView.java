package id.tulis.editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Ikon sederhana yang digambar lewat kode (tanpa file gambar). */
class IconView extends View {
    static final int LEFT = 0, CENTER = 1, RIGHT = 2, JUSTIFY = 3, BULLET = 4, NUMBER = 5,
        OUTDENT = 6, INDENT = 7, SPACING = 8, UNDO = 9, REDO = 10, SAVE = 11;
    private final int type, color;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    IconView(Context c, int type, int color) {
        super(c);
        this.type = type;
        this.color = color;
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float s = Math.min(w, h) * 0.58f, x0 = (w - s) / 2f, y0 = (h - s) / 2f;
        p.setColor(color);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1.5f, s / 11f));
        switch (type) {
            case LEFT: case CENTER: case RIGHT: case JUSTIFY: {
                float[] ln = {1f, .62f, 1f, .62f};
                for (int i = 0; i < 4; i++) {
                    float y = y0 + s * (0.1f + 0.27f * i);
                    float len = type == JUSTIFY ? s : s * ln[i];
                    float xs = (type == LEFT || type == JUSTIFY) ? x0 : type == CENTER ? x0 + (s - len) / 2f : x0 + s - len;
                    c.drawLine(xs, y, xs + len, y, p);
                }
                break;
            }
            case BULLET: case NUMBER: {
                for (int i = 0; i < 3; i++) {
                    float y = y0 + s * (0.17f + 0.33f * i);
                    p.setStyle(Paint.Style.FILL);
                    if (type == BULLET) c.drawCircle(x0 + s * 0.08f, y, s * 0.08f, p);
                    else { p.setTextSize(s * 0.32f); c.drawText(String.valueOf(i + 1), x0, y + s * 0.11f, p); }
                    p.setStyle(Paint.Style.STROKE);
                    c.drawLine(x0 + s * 0.32f, y, x0 + s, y, p);
                }
                break;
            }
            case OUTDENT: case INDENT: {
                for (int i = 0; i < 4; i++) {
                    float y = y0 + s * (0.1f + 0.27f * i);
                    float xs = (i == 0 || i == 3) ? x0 : x0 + s * 0.5f;
                    c.drawLine(xs, y, x0 + s, y, p);
                }
                float ym = y0 + s * 0.5f, a = s * 0.16f;
                Path t = new Path();
                if (type == INDENT) { t.moveTo(x0, ym - a); t.lineTo(x0 + s * 0.3f, ym); t.lineTo(x0, ym + a); }
                else { t.moveTo(x0 + s * 0.3f, ym - a); t.lineTo(x0, ym); t.lineTo(x0 + s * 0.3f, ym + a); }
                t.close();
                p.setStyle(Paint.Style.FILL);
                c.drawPath(t, p);
                break;
            }
            case SPACING: {
                float xv = x0 + s * 0.12f;
                c.drawLine(xv, y0 + s * 0.15f, xv, y0 + s * 0.85f, p);
                p.setStyle(Paint.Style.FILL);
                Path u = new Path();
                u.moveTo(xv - s * 0.1f, y0 + s * 0.28f); u.lineTo(xv, y0); u.lineTo(xv + s * 0.1f, y0 + s * 0.28f); u.close();
                c.drawPath(u, p);
                Path d = new Path();
                d.moveTo(xv - s * 0.1f, y0 + s * 0.72f); d.lineTo(xv, y0 + s); d.lineTo(xv + s * 0.1f, y0 + s * 0.72f); d.close();
                c.drawPath(d, p);
                p.setStyle(Paint.Style.STROKE);
                for (int i = 0; i < 3; i++) {
                    float y = y0 + s * (0.1f + 0.4f * i);
                    c.drawLine(x0 + s * 0.4f, y, x0 + s, y, p);
                }
                break;
            }
            case UNDO: case REDO: {
                c.save();
                if (type == REDO) c.scale(-1f, 1f, w / 2f, h / 2f);
                RectF r = new RectF(x0 + s * 0.1f, y0 + s * 0.25f, x0 + s * 0.95f, y0 + s * 0.95f);
                c.drawArc(r, 200f, 220f, false, p);
                p.setStyle(Paint.Style.FILL);
                Path a = new Path();
                a.moveTo(x0 - s * 0.02f, y0 + s * 0.42f);
                a.lineTo(x0 + s * 0.34f, y0 + s * 0.36f);
                a.lineTo(x0 + s * 0.12f, y0 + s * 0.72f);
                a.close();
                c.drawPath(a, p);
                c.restore();
                break;
            }
            case SAVE: {
                c.drawRect(x0, y0, x0 + s, y0 + s, p);
                p.setStyle(Paint.Style.FILL);
                c.drawRect(x0 + s * 0.25f, y0, x0 + s * 0.75f, y0 + s * 0.32f, p);
                p.setStyle(Paint.Style.STROKE);
                c.drawRect(x0 + s * 0.2f, y0 + s * 0.55f, x0 + s * 0.8f, y0 + s, p);
                break;
            }
            default: break;
        }
    }
}
