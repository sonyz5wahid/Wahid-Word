package id.tulis.editor;

import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Environment;
import android.text.Layout;
import android.text.TextPaint;
import android.text.style.LeadingMarginSpan;
import android.text.style.LineHeightSpan;
import android.text.style.MetricAffectingSpan;
import android.text.style.ParagraphStyle;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/** Font bawaan aplikasi + font yang ada di perangkat (/system/fonts dan folder Fonts di penyimpanan). */
final class Fonts {
    static final String[] NAMES = {"Calibri", "Times New Roman", "Arial", "Courier New", "Cambria",
        "Roboto", "Open Sans", "Lato", "DejaVu Sans", "DejaVu Serif"};
    private static final String[] FILES = {"Carlito", "LiberationSerif", "LiberationSans", "LiberationMono", "Caladea",
        "Roboto", "OpenSans", "Lato", "DejaVuSans", "DejaVuSerif"};
    private static final HashMap<String, Typeface> CACHE = new HashMap<String, Typeface>();
    private static final HashMap<String, File[]> DEV = new HashMap<String, File[]>();
    private static final HashMap<String, String> DEVNAME = new HashMap<String, String>();
    private static boolean scanned;
    static AssetManager am;

    static int family(String name) {
        if (name == null) return 0;
        String n = name.toLowerCase();
        for (int k = 0; k < NAMES.length; k++) if (NAMES[k].toLowerCase().equals(n)) return k;
        if (n.contains("mono") || n.contains("courier") || n.contains("consol")) return 3;
        if (n.contains("sans") || n.contains("arial") || n.contains("helvet") || n.contains("verdana") || n.contains("tahoma")) return 2;
        if (n.contains("times") || n.contains("serif") || n.contains("georgia") || n.contains("garamond") || n.contains("book") || n.contains("palatino")) return 1;
        if (n.contains("cambria")) return 4;
        return 0;
    }

    private static int exact(String name) {
        if (name == null) return -1;
        for (int k = 0; k < NAMES.length; k++) if (NAMES[k].equalsIgnoreCase(name)) return k;
        return -1;
    }

    private static Typeface bundled(int k, boolean bold, boolean italic) {
        boolean dv = FILES[k].startsWith("DejaVu");
        String suf;
        if (dv) suf = bold ? (italic ? "-BoldOblique" : "-Bold") : (italic ? (k == 9 ? "-Italic" : "-Oblique") : "");
        else suf = bold ? (italic ? "-BoldItalic" : "-Bold") : (italic ? "-Italic" : "-Regular");
        if (dv && k == 9 && bold && italic) suf = "-BoldItalic";
        String key = FILES[k] + suf;
        Typeface t = CACHE.get(key);
        if (t == null) {
            try {
                t = Typeface.createFromAsset(am, "fonts/" + key + ".ttf");
            } catch (Exception e) {
                Typeface base = (k == 1 || k == 4 || k == 9) ? Typeface.SERIF : k == 3 ? Typeface.MONOSPACE : Typeface.SANS_SERIF;
                t = Typeface.create(base, (bold ? 1 : 0) | (italic ? 2 : 0));
            }
            CACHE.put(key, t);
        }
        return t;
    }

    static Typeface get(String name, boolean bold, boolean italic) {
        int k = exact(name);
        if (k >= 0) return bundled(k, bold, italic);
        scan();
        File[] fs = name == null ? null : DEV.get(name.toLowerCase());
        if (fs != null) {
            int idx = (bold ? 1 : 0) | (italic ? 2 : 0);
            File f = fs[idx] != null ? fs[idx] : fs[0] != null ? fs[0] : fs[1] != null ? fs[1] : fs[2] != null ? fs[2] : fs[3];
            if (f != null) {
                String key = "dev:" + f.getPath();
                Typeface t = CACHE.get(key);
                if (t == null) {
                    try { t = Typeface.createFromFile(f); } catch (Exception e) { t = null; }
                    if (t != null) CACHE.put(key, t);
                }
                if (t != null) return t;
            }
        }
        return bundled(family(name), bold, italic);
    }

    /** Daftar semua nama font: bawaan dulu, lalu font yang ditemukan di perangkat. */
    static String[] allFamilies() {
        scan();
        ArrayList<String> dev = new ArrayList<String>(DEVNAME.values());
        Collections.sort(dev, String.CASE_INSENSITIVE_ORDER);
        ArrayList<String> all = new ArrayList<String>();
        for (String n : NAMES) all.add(n);
        for (String n : dev) if (exact(n) < 0) all.add(n);
        return all.toArray(new String[0]);
    }

    private static synchronized void scan() {
        if (scanned) return;
        scanned = true;
        File[] dirs = {new File("/system/fonts"), new File(Environment.getExternalStorageDirectory(), "Fonts"),
            new File(Environment.getExternalStorageDirectory(), "fonts")};
        for (File d : dirs) {
            File[] fl = d.listFiles();
            if (fl == null) continue;
            for (File f : fl) {
                String n = f.getName().toLowerCase();
                if (!(n.endsWith(".ttf") || n.endsWith(".otf"))) continue;
                String[] fn = ttfNames(f);
                if (fn == null) continue;
                String key = fn[0].toLowerCase();
                String sub = fn[1].toLowerCase();
                boolean bold = sub.contains("bold");
                boolean italic = sub.contains("italic") || sub.contains("oblique");
                File[] arr = DEV.get(key);
                if (arr == null) { arr = new File[4]; DEV.put(key, arr); DEVNAME.put(key, fn[0]); }
                int idx = (bold ? 1 : 0) | (italic ? 2 : 0);
                if (arr[idx] == null) arr[idx] = f;
            }
        }
    }

    /** Membaca nama keluarga dan gaya langsung dari tabel 'name' file TTF/OTF. */
    static String[] ttfNames(File f) {
        RandomAccessFile r = null;
        try {
            r = new RandomAccessFile(f, "r");
            int tag = r.readInt();
            if (tag == 0x74746366) return null;
            int n = r.readUnsignedShort();
            r.skipBytes(6);
            long off = -1;
            for (int i = 0; i < n; i++) {
                int t = r.readInt();
                r.readInt();
                long o = r.readInt() & 0xFFFFFFFFL;
                r.readInt();
                if (t == 0x6E616D65) off = o;
            }
            if (off < 0) return null;
            r.seek(off);
            r.readUnsignedShort();
            int cnt = r.readUnsignedShort();
            int so = r.readUnsignedShort();
            String fam = null, sub = null;
            for (int i = 0; i < cnt; i++) {
                int pid = r.readUnsignedShort();
                r.readUnsignedShort();
                r.readUnsignedShort();
                int nid = r.readUnsignedShort();
                int ln = r.readUnsignedShort();
                int no = r.readUnsignedShort();
                if ((nid == 1 || nid == 2) && (pid == 3 || pid == 1) && ln > 0 && ln < 512) {
                    long save = r.getFilePointer();
                    byte[] b = new byte[ln];
                    r.seek(off + so + no);
                    r.readFully(b);
                    String s = new String(b, pid == 3 ? "UTF-16BE" : "ISO-8859-1");
                    if (nid == 1 && (fam == null || pid == 3)) fam = s;
                    if (nid == 2 && (sub == null || pid == 3)) sub = s;
                    r.seek(save);
                }
            }
            return fam == null ? null : new String[]{fam, sub == null ? "Regular" : sub};
        } catch (Exception e) {
            return null;
        } finally {
            if (r != null) { try { r.close(); } catch (Exception ignore) { } }
        }
    }
}

/** Jenis huruf + tebal + miring dalam satu span, supaya memakai file font yang sebenarnya. */
class FontSpan extends MetricAffectingSpan {
    final String name;
    final boolean bold, italic;
    FontSpan(String name, boolean bold, boolean italic) { this.name = name; this.bold = bold; this.italic = italic; }
    private void apply(TextPaint p) {
        p.setTypeface(Fonts.get(name, bold, italic));
        p.setFakeBoldText(false);
        p.setTextSkewX(0);
    }
    @Override public void updateDrawState(TextPaint p) { apply(p); }
    @Override public void updateMeasureState(TextPaint p) { apply(p); }
}

/** Ukuran huruf dalam setengah poin (22 = 11 pt), persis seperti di file docx. */
class SizeSpan extends MetricAffectingSpan {
    final int hp;
    SizeSpan(int hp) { this.hp = hp; }
    @Override public void updateDrawState(TextPaint p) { p.setTextSize(hp * Docx.unit); }
    @Override public void updateMeasureState(TextPaint p) { p.setTextSize(hp * Docx.unit); }
}

/** Penanda rata kiri-kanan (Android 4.2 belum bisa menampilkannya, tapi tersimpan di docx). */
class JustifySpan implements ParagraphStyle { }

/** Jarak baris: 240 = 1.0, 276 = 1.15, 360 = 1.5, 480 = 2.0. */
class LineSpacingSpan implements LineHeightSpan {
    final int line;
    LineSpacingSpan(int line) { this.line = line; }
    @Override
    public void chooseHeight(CharSequence text, int start, int end, int spanstartv, int v, Paint.FontMetricsInt fm) {
        int h = fm.descent - fm.ascent;
        int extra = Math.round(h * (line / 240f - 1f));
        fm.descent += extra;
        fm.bottom += extra;
    }
}

/** Indentasi kiri dalam twips (720 = 1,27 cm). */
class IndentSpan implements LeadingMarginSpan {
    final int twips;
    IndentSpan(int twips) { this.twips = twips; }
    @Override public int getLeadingMargin(boolean first) { return Math.round(twips * Docx.unit / 10f); }
    @Override
    public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, int top, int baseline, int bottom,
                                  CharSequence text, int start, int end, boolean first, Layout layout) { }
}
