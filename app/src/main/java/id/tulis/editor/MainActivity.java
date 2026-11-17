package id.tulis.editor;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.UnderlineSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    static final int BLUE = 0xFF2B579A, GRAY = 0xFFF3F3F3;
    static final int INC = Spanned.SPAN_EXCLUSIVE_INCLUSIVE, PAR = Spanned.SPAN_PARAGRAPH;
    static final int[] PT = {8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24, 26, 28, 36, 48, 72};
    static final int[] TXT_COLORS = {0xFF000000, 0xFFC00000, 0xFFFF0000, 0xFFFFC000, 0xFFFFFF00, 0xFF92D050,
        0xFF00B050, 0xFF00B0F0, 0xFF0070C0, 0xFF002060, 0xFF7030A0, 0xFF7F7F7F};
    static final int[] HL_COLORS = {0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFFFF00FF, 0xFF0000FF, 0xFFFF0000, 0xFFC0C0C0};
    static final Pattern NUM = Pattern.compile("^\\d+\\.\\s");

    class Ed extends EditText {
        Ed(Context c) { super(c); }
        @Override protected void onSelectionChanged(int a, int b) { super.onSelectionChanged(a, b); updateState(); }
    }

    Ed ed;
    TextView title, status, fontBtn, sizeBtn, bB, bI, bU, bS, bSub, bSup;
    LinearLayout ribbon;
    HorizontalScrollView hs;
    ScrollView page;
    TextView[] tabs = new TextView[3];
    int curTab = 1;
    File cur, lastDir;
    String suggest;
    boolean dirty, loading, dark, restoring, pending;
    float d;
    final ArrayList<SpannableStringBuilder> hist = new ArrayList<SpannableStringBuilder>();
    int hp = -1;
    final Handler handler = new Handler();
    final Runnable snapRun = new Runnable() { public void run() { snapshot(); } };

    int dp(int v) { return Math.round(v * d); }
    void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
    File dir() { File f = new File(Environment.getExternalStorageDirectory(), "Dokumen"); f.mkdirs(); return f; }
    File draft() { return new File(getFilesDir(), "draft.docx"); }
    int lo() { return Math.min(ed.getSelectionStart(), ed.getSelectionEnd()); }
    int hi() { return Math.max(ed.getSelectionStart(), ed.getSelectionEnd()); }

    // ================= UI =================
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 23
            && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
        d = getResources().getDisplayMetrics().density;
        Docx.unit = d * 16f / 22f;
        Fonts.am = getAssets();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        LinearLayout top = new LinearLayout(this);
        top.setBackgroundColor(BLUE);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(ibtn(IconView.SAVE, 0xFFFFFFFF, 40, 34, v -> save()));
        top.addView(ibtn(IconView.UNDO, 0xFFFFFFFF, 40, 34, v -> undo()));
        top.addView(ibtn(IconView.REDO, 0xFFFFFFFF, 40, 34, v -> redo()));
        title = new TextView(this);
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(14);
        title.setSingleLine();
        title.setGravity(Gravity.CENTER);
        top.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        top.addView(new View(this), new LinearLayout.LayoutParams(dp(120), 1));
        root.addView(top, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout tr = new LinearLayout(this);
        tr.setBackgroundColor(BLUE);
        String[] names = {"Berkas", "Beranda", "Tampilan"};
        for (int i = 0; i < 3; i++) {
            final int k = i;
            TextView t = new TextView(this);
            t.setText(names[i]);
            t.setTextSize(14);
            t.setPadding(dp(18), dp(7), dp(18), dp(7));
            t.setOnClickListener(v -> {
                if (curTab == k) hs.setVisibility(hs.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                else { hs.setVisibility(View.VISIBLE); showTab(k); }
            });
            tabs[i] = t;
            tr.addView(t);
        }
        root.addView(tr, new LinearLayout.LayoutParams(-1, -2));

        hs = new HorizontalScrollView(this);
        hs.setBackgroundColor(GRAY);
        hs.setHorizontalScrollBarEnabled(false);
        ribbon = new LinearLayout(this);
        hs.addView(ribbon, new FrameLayout.LayoutParams(-2, -1));
        root.addView(hs, new LinearLayout.LayoutParams(-1, dp(92)));

        View dv = new View(this);
        dv.setBackgroundColor(0xFFC8C8C8);
        root.addView(dv, new LinearLayout.LayoutParams(-1, 1));

        page = new ScrollView(this);
        page.setFillViewport(true);
        ed = new Ed(this);
        ed.setHint("Mulai mengetik di sini");
        ed.setGravity(Gravity.TOP | Gravity.LEFT);
        ed.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        ed.setMinHeight(Math.max(dm.widthPixels, dm.heightPixels));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(10), dp(10), dp(10), dp(10));
        page.addView(ed, lp);
        root.addView(page, new LinearLayout.LayoutParams(-1, 0, 1f));
        applyTheme();
        applyDefaults();

        status = new TextView(this);
        status.setTextColor(0xFFFFFFFF);
        status.setTextSize(12);
        status.setBackgroundColor(BLUE);
        status.setPadding(dp(12), dp(4), dp(12), dp(4));
        root.addView(status, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);

        ed.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            public void onTextChanged(CharSequence s, int a, int b, int c) { }
            public void afterTextChanged(Editable s) {
                if (restoring) return;
                if (!loading) { dirty = true; schedule(); }
                refresh();
            }
        });

        Intent in = getIntent();
        boolean viewing = in != null && Intent.ACTION_VIEW.equals(in.getAction()) && in.getData() != null;
        if (!viewing && draft().exists() && load(draft())) {
            cur = null;
            dirty = true;
            toast("Draf terakhir dipulihkan");
        } else {
            resetHistory();
        }
        showTab(1);
        refresh();
        if (viewing) openUri(in.getData());
    }

    @Override
    protected void onNewIntent(Intent in) {
        super.onNewIntent(in);
        setIntent(in);
        if (in != null && Intent.ACTION_VIEW.equals(in.getAction()) && in.getData() != null) {
            final Uri u = in.getData();
            confirmDiscard(() -> openUri(u));
        }
    }

    void applyDefaults() {
        ed.setTypeface(Fonts.get(Docx.defName, false, false));
        ed.setTextSize(TypedValue.COMPLEX_UNIT_PX, Docx.defHp * Docx.unit);
    }

    void applyTheme() {
        page.setBackgroundColor(dark ? 0xFF121212 : 0xFFE6E6E6);
        ed.setBackgroundColor(dark ? 0xFF1E1E1E : 0xFFFFFFFF);
        ed.setTextColor(dark ? 0xFFE8E8E8 : 0xFF000000);
        ed.setHintTextColor(0xFF888888);
        ed.setPadding(dp(22), dp(20), dp(22), dp(20));
    }

    void refresh() {
        title.setText((cur == null ? "Tanpa judul" : cur.getName()) + (dirty ? " \u2022" : "") + "  -  Wahid Word");
        String t = ed.getText().toString().trim();
        int words = t.isEmpty() ? 0 : t.split("\\s+").length;
        status.setText("Kata: " + words + "     Karakter: " + ed.getText().length());
    }

    void mark() { dirty = true; schedule(); refresh(); updateState(); }

    String fmtHp(int h) { return h % 2 == 0 ? String.valueOf(h / 2) : String.valueOf(h / 2f); }

    FontSpan fontAt(Editable e, int i) {
        FontSpan r = null;
        for (FontSpan f : e.getSpans(i, i + 1, FontSpan.class)) r = f;
        return r;
    }

    void updateState() {
        if (fontBtn == null || ed == null) return;
        Editable e = ed.getText();
        int a = lo();
        if (a < 0) return;
        int pos = (a == hi() && a > 0) ? a - 1 : a;
        if (pos >= e.length()) pos = e.length() - 1;
        if (pos < 0) {
            fontBtn.setText(Docx.defName);
            sizeBtn.setText(fmtHp(Docx.defHp));
            bB.setSelected(false); bI.setSelected(false); bU.setSelected(false); bS.setSelected(false);
            bSub.setSelected(false); bSup.setSelected(false);
            return;
        }
        FontSpan f = fontAt(e, pos);
        int h = Docx.defHp;
        for (SizeSpan z : e.getSpans(pos, pos + 1, SizeSpan.class)) h = z.hp;
        fontBtn.setText(f == null ? Docx.defName : f.name);
        sizeBtn.setText(fmtHp(h));
        bB.setSelected(f != null && f.bold);
        bI.setSelected(f != null && f.italic);
        bU.setSelected(e.getSpans(pos, pos + 1, UnderlineSpan.class).length > 0);
        bS.setSelected(e.getSpans(pos, pos + 1, StrikethroughSpan.class).length > 0);
        bSub.setSelected(e.getSpans(pos, pos + 1, SubscriptSpan.class).length > 0);
        bSup.setSelected(e.getSpans(pos, pos + 1, SuperscriptSpan.class).length > 0);
    }

    // ---------- komponen ribbon ----------
    Drawable sel() {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(0xFFCFE0F5));
        s.addState(new int[]{android.R.attr.state_selected}, new ColorDrawable(0xFFBBD0EC));
        s.addState(new int[]{}, new ColorDrawable(0));
        return s;
    }

    TextView tbtn(CharSequence t, int w, int h, View.OnClickListener l) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(13);
        v.setTextColor(0xFF222222);
        v.setGravity(Gravity.CENTER);
        v.setFocusable(false);
        v.setBackground(sel());
        v.setOnClickListener(l);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(w), dp(h)));
        return v;
    }

    IconView ibtn(int type, int color, int w, int h, View.OnClickListener l) {
        IconView v = new IconView(this, type, color);
        v.setBackground(sel());
        v.setOnClickListener(l);
        v.setFocusable(false);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(w), dp(h)));
        return v;
    }

    LinearLayout row(View... vs) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        for (View v : vs) r.addView(v);
        return r;
    }

    LinearLayout col(View... vs) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        for (View v : vs) r.addView(v);
        return r;
    }

    void addGroup(String label, View content) {
        LinearLayout g = new LinearLayout(this);
        g.setOrientation(LinearLayout.VERTICAL);
        g.setPadding(dp(6), dp(4), dp(6), 0);
        g.addView(content);
        TextView l = new TextView(this);
        l.setText(label);
        l.setTextSize(11);
        l.setTextColor(0xFF666666);
        l.setGravity(Gravity.CENTER);
        g.addView(l, new LinearLayout.LayoutParams(-1, -2));
        ribbon.addView(g, new LinearLayout.LayoutParams(-2, -1));
        View v = new View(this);
        v.setBackgroundColor(0xFFD0D0D0);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(1, -1);
        p.setMargins(0, dp(6), 0, dp(6));
        ribbon.addView(v, p);
    }

    void showTab(int k) {
        curTab = k;
        for (int i = 0; i < 3; i++) {
            tabs[i].setTextColor(i == k ? BLUE : 0xFFFFFFFF);
            tabs[i].setBackgroundColor(i == k ? GRAY : BLUE);
        }
        ribbon.removeAllViews();
        if (k == 0) {
            addGroup("Berkas", row(
                tbtn("Baru", 60, 64, v -> newDoc()),
                tbtn("Buka", 60, 64, v -> openDlg()),
                tbtn("Simpan", 64, 64, v -> save()),
                tbtn("Simpan\nSebagai", 72, 64, v -> saveAs())));
        } else if (k == 1) {
            buildHome();
        } else {
            addGroup("Tema", row(tbtn(dark ? "Mode Terang" : "Mode Gelap", 100, 64,
                v -> { dark = !dark; applyTheme(); showTab(2); })));
        }
        updateState();
    }

    void buildHome() {
        TextView paste = tbtn("Paste", 52, 64, v -> ed.onTextContextMenuItem(android.R.id.paste));
        paste.setTypeface(null, Typeface.BOLD);
        addGroup("Clipboard", row(paste, col(
            tbtn("Cut", 46, 32, v -> ed.onTextContextMenuItem(android.R.id.cut)),
            tbtn("Copy", 46, 32, v -> ed.onTextContextMenuItem(android.R.id.copy)))));

        fontBtn = tbtn(Docx.defName, 108, 32, v -> fontDlg());
        sizeBtn = tbtn(fmtHp(Docx.defHp), 40, 32, v -> sizeDlg());
        bB = tbtn("B", 30, 32, v -> toggleBold(true)); bB.setTypeface(null, Typeface.BOLD);
        bI = tbtn("I", 30, 32, v -> toggleBold(false)); bI.setTypeface(null, Typeface.ITALIC);
        bU = tbtn("U", 30, 32, v -> toggleMark(UnderlineSpan.class));
        bU.setPaintFlags(bU.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        bS = tbtn("ab", 30, 32, v -> toggleMark(StrikethroughSpan.class));
        bS.setPaintFlags(bS.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        SpannableString sb = new SpannableString("x2");
        sb.setSpan(new SubscriptSpan(), 1, 2, 0);
        sb.setSpan(new RelativeSizeSpan(0.7f), 1, 2, 0);
        bSub = tbtn(sb, 30, 32, v -> script(false));
        SpannableString sp = new SpannableString("x2");
        sp.setSpan(new SuperscriptSpan(), 1, 2, 0);
        sp.setSpan(new RelativeSizeSpan(0.7f), 1, 2, 0);
        bSup = tbtn(sp, 30, 32, v -> script(true));
        SpannableString ca = new SpannableString("A");
        ca.setSpan(new ForegroundColorSpan(0xFFD00000), 0, 1, 0);
        ca.setSpan(new UnderlineSpan(), 0, 1, 0);
        SpannableString hl = new SpannableString(" ab ");
        hl.setSpan(new BackgroundColorSpan(0xFFFFFF00), 0, 4, 0);
        addGroup("Font", col(
            row(fontBtn, sizeBtn, tbtn("A+", 30, 32, v -> grow(1)), tbtn("A-", 30, 32, v -> grow(-1))),
            row(bB, bI, bU, bS, bSub, bSup, tbtn(ca, 30, 32, v -> colorDlg(true)), tbtn(hl, 36, 32, v -> colorDlg(false)))));

        addGroup("Paragraph", col(
            row(ibtn(IconView.BULLET, 0xFF444444, 34, 32, v -> bullets()),
                ibtn(IconView.NUMBER, 0xFF444444, 34, 32, v -> numbers()),
                ibtn(IconView.OUTDENT, 0xFF444444, 34, 32, v -> indent(-1)),
                ibtn(IconView.INDENT, 0xFF444444, 34, 32, v -> indent(1)),
                ibtn(IconView.SPACING, 0xFF444444, 34, 32, v -> spacingDlg())),
            row(ibtn(IconView.LEFT, 0xFF444444, 34, 32, v -> align(0)),
                ibtn(IconView.CENTER, 0xFF444444, 34, 32, v -> align(1)),
                ibtn(IconView.RIGHT, 0xFF444444, 34, 32, v -> align(2)),
                ibtn(IconView.JUSTIFY, 0xFF444444, 34, 32, v -> align(3)))));

        addGroup("Editing", row(col(
            tbtn("Find", 56, 32, v -> findDlg()),
            tbtn("Replace", 56, 32, v -> findDlg())),
            tbtn("Select\nAll", 52, 64, v -> ed.selectAll())));
    }

    // ================= Format huruf =================
    boolean needSel() {
        int a = lo(), b = hi();
        if (a < 0 || a == b) { toast("Blok teks dulu"); return true; }
        return false;
    }

    /** bm/im: 0 = biarkan, 1 = nyalakan, 2 = matikan. name: null = biarkan. */
    void editFont(int a, int b, String name, int bm, int im) {
        Editable e = ed.getText();
        FontSpan[] old = e.getSpans(a, b, FontSpan.class);
        TreeSet<Integer> cuts = new TreeSet<Integer>();
        cuts.add(a);
        cuts.add(b);
        for (FontSpan f : old) {
            int x = e.getSpanStart(f), y = e.getSpanEnd(f);
            if (x > a && x < b) cuts.add(x);
            if (y > a && y < b) cuts.add(y);
        }
        Integer[] cp = cuts.toArray(new Integer[0]);
        int n = cp.length - 1;
        String[] nn = new String[n];
        boolean[] bb = new boolean[n], ii = new boolean[n];
        for (int k = 0; k < n; k++) {
            FontSpan cov = null;
            for (FontSpan f : old) if (e.getSpanStart(f) <= cp[k] && e.getSpanEnd(f) >= cp[k + 1]) cov = f;
            String nm = cov == null ? Docx.defName : cov.name;
            boolean bo = cov != null && cov.bold, it = cov != null && cov.italic;
            nn[k] = name != null ? name : nm;
            bb[k] = bm == 0 ? bo : bm == 1;
            ii[k] = im == 0 ? it : im == 1;
        }
        for (FontSpan f : old) {
            int x = e.getSpanStart(f), y = e.getSpanEnd(f);
            e.removeSpan(f);
            if (x < a) e.setSpan(new FontSpan(f.name, f.bold, f.italic), x, a, INC);
            if (y > b) e.setSpan(new FontSpan(f.name, f.bold, f.italic), b, y, INC);
        }
        for (int k = 0; k < n; k++) {
            if (cp[k] >= cp[k + 1]) continue;
            if (nn[k].equals(Docx.defName) && !bb[k] && !ii[k]) continue;
            e.setSpan(new FontSpan(nn[k], bb[k], ii[k]), cp[k], cp[k + 1], INC);
        }
    }

    void toggleBold(boolean bold) {
        if (needSel()) return;
        int a = lo(), b = hi();
        Editable e = ed.getText();
        boolean all = true;
        for (int i = a; i < b && all; i++) {
            FontSpan f = fontAt(e, i);
            if (f == null || !(bold ? f.bold : f.italic)) all = false;
        }
        editFont(a, b, null, bold ? (all ? 2 : 1) : 0, bold ? 0 : (all ? 2 : 1));
        mark();
    }

    void setFontName(String n) {
        if (needSel()) return;
        editFont(lo(), hi(), n, 0, 0);
        mark();
    }

    void setSize(int h) {
        if (needSel()) return;
        int a = lo(), b = hi();
        Editable e = ed.getText();
        for (SizeSpan sp : e.getSpans(a, b, SizeSpan.class)) {
            int x = e.getSpanStart(sp), y = e.getSpanEnd(sp);
            e.removeSpan(sp);
            if (x < a) e.setSpan(new SizeSpan(sp.hp), x, a, INC);
            if (y > b) e.setSpan(new SizeSpan(sp.hp), b, y, INC);
        }
        if (h != Docx.defHp) e.setSpan(new SizeSpan(h), a, b, INC);
        mark();
    }

    void grow(int dir) {
        if (needSel()) return;
        int a = lo();
        int h = Docx.defHp;
        for (SizeSpan z : ed.getText().getSpans(a, a + 1, SizeSpan.class)) h = z.hp;
        float pt = h / 2f;
        float target = pt;
        if (dir > 0) { for (int i = 0; i < PT.length; i++) if (PT[i] > pt) { target = PT[i]; break; } }
        else { for (int i = PT.length - 1; i >= 0; i--) if (PT[i] < pt) { target = PT[i]; break; } }
        setSize(Math.round(target * 2));
    }

    Object newMark(Class<?> c) {
        if (c == UnderlineSpan.class) return new UnderlineSpan();
        if (c == StrikethroughSpan.class) return new StrikethroughSpan();
        if (c == SuperscriptSpan.class) return new SuperscriptSpan();
        if (c == SubscriptSpan.class) return new SubscriptSpan();
        return new RelativeSizeSpan(0.65f);
    }

    boolean hasAll(Editable e, int a, int b, Class<?> c) {
        for (int i = a; i < b; i++) if (e.getSpans(i, i + 1, c).length == 0) return false;
        return true;
    }

    void clearMark(Editable e, int a, int b, Class<?> c) {
        for (Object sp : e.getSpans(a, b, c)) {
            int x = e.getSpanStart(sp), y = e.getSpanEnd(sp);
            e.removeSpan(sp);
            if (x < a) e.setSpan(newMark(c), x, a, INC);
            if (y > b) e.setSpan(newMark(c), b, y, INC);
        }
    }

    void toggleMark(Class<?> c) {
        if (needSel()) return;
        int a = lo(), b = hi();
        Editable e = ed.getText();
        boolean all = hasAll(e, a, b, c);
        clearMark(e, a, b, c);
        if (!all) e.setSpan(newMark(c), a, b, INC);
        mark();
    }

    void script(boolean sup) {
        if (needSel()) return;
        int a = lo(), b = hi();
        Editable e = ed.getText();
        Class<?> c = sup ? SuperscriptSpan.class : SubscriptSpan.class;
        boolean all = hasAll(e, a, b, c);
        clearMark(e, a, b, SuperscriptSpan.class);
        clearMark(e, a, b, SubscriptSpan.class);
        clearMark(e, a, b, RelativeSizeSpan.class);
        if (!all) {
            e.setSpan(newMark(c), a, b, INC);
            e.setSpan(newMark(RelativeSizeSpan.class), a, b, INC);
        }
        mark();
    }

    Object mkc(boolean text, int c) { return text ? new ForegroundColorSpan(c) : new BackgroundColorSpan(c); }

    void setColor(boolean text, int col) {
        if (needSel()) return;
        int a = lo(), b = hi();
        Editable e = ed.getText();
        Class<?> c = text ? ForegroundColorSpan.class : BackgroundColorSpan.class;
        for (Object sp : e.getSpans(a, b, c)) {
            int x = e.getSpanStart(sp), y = e.getSpanEnd(sp);
            int old = text ? ((ForegroundColorSpan) sp).getForegroundColor() : ((BackgroundColorSpan) sp).getBackgroundColor();
            e.removeSpan(sp);
            if (x < a) e.setSpan(mkc(text, old), x, a, INC);
            if (y > b) e.setSpan(mkc(text, old), b, y, INC);
        }
        if (col != 0) e.setSpan(mkc(text, col), a, b, INC);
        mark();
    }

    // ================= Format paragraf =================
    int[] par() {
        String txt = ed.getText().toString();
        int s = Math.max(lo(), 0), t = Math.max(hi(), 0);
        int ps = s == 0 ? 0 : txt.lastIndexOf('\n', s - 1) + 1;
        int q = t > s ? t - 1 : t;
        int nl = txt.indexOf('\n', q);
        int pe = nl < 0 ? txt.length() : nl + 1;
        return new int[]{ps, pe};
    }

    Object copyPar(Object o) {
        if (o instanceof AlignmentSpan) return new AlignmentSpan.Standard(((AlignmentSpan) o).getAlignment());
        if (o instanceof JustifySpan) return new JustifySpan();
        if (o instanceof LineSpacingSpan) return new LineSpacingSpan(((LineSpacingSpan) o).line);
        return new IndentSpan(((IndentSpan) o).twips);
    }

    void clearPar(Editable e, int ps, int pe, Class<?> c) {
        for (Object sp : e.getSpans(ps, pe, c)) {
            int x = e.getSpanStart(sp), y = e.getSpanEnd(sp);
            Object c1 = copyPar(sp), c2 = copyPar(sp);
            e.removeSpan(sp);
            if (x < ps) e.setSpan(c1, x, ps, PAR);
            if (y > pe) e.setSpan(c2, pe, y, PAR);
        }
    }

    /** 0 kiri, 1 tengah, 2 kanan, 3 rata kiri-kanan. */
    void align(int k) {
        int[] r = par();
        Editable e = ed.getText();
        clearPar(e, r[0], r[1], AlignmentSpan.class);
        clearPar(e, r[0], r[1], JustifySpan.class);
        if (k == 1) e.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), r[0], r[1], PAR);
        else if (k == 2) e.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE), r[0], r[1], PAR);
        else if (k == 3) {
            e.setSpan(new JustifySpan(), r[0], r[1], PAR);
            toast("Rata kiri-kanan akan terlihat saat dibuka di Word");
        }
        mark();
    }

    void indent(int dir) {
        int[] r = par();
        Editable e = ed.getText();
        int c = 0;
        for (IndentSpan s : e.getSpans(r[0], r[1], IndentSpan.class)) c = s.twips;
        int nw = Math.max(0, c + dir * 720);
        clearPar(e, r[0], r[1], IndentSpan.class);
        if (nw > 0) e.setSpan(new IndentSpan(nw), r[0], r[1], PAR);
        mark();
    }

    void spacingDlg() {
        final int[] v = {240, 276, 360, 480};
        String[] it = {"1,0", "1,15", "1,5", "2,0"};
        new AlertDialog.Builder(this).setTitle("Jarak baris").setItems(it, (dl, w) -> {
            int[] r = par();
            Editable e = ed.getText();
            clearPar(e, r[0], r[1], LineSpacingSpan.class);
            if (v[w] != 240) e.setSpan(new LineSpacingSpan(v[w]), r[0], r[1], PAR);
            mark();
        }).show();
    }

    ArrayList<Integer> starts(String txt, int ps, int q) {
        ArrayList<Integer> st = new ArrayList<Integer>();
        int i = ps;
        while (true) {
            st.add(i);
            int nl = txt.indexOf('\n', i);
            if (nl < 0 || nl >= q) break;
            i = nl + 1;
        }
        return st;
    }

    String lineAt(String txt, int p) {
        int nl = txt.indexOf('\n', p);
        return txt.substring(p, nl < 0 ? txt.length() : nl);
    }

    void bullets() {
        Editable e = ed.getText();
        String txt = e.toString();
        int[] r = par();
        int s = Math.max(lo(), 0), t = Math.max(hi(), 0);
        ArrayList<Integer> st = starts(txt, r[0], t > s ? t - 1 : t);
        boolean all = true;
        for (int p : st) if (!txt.startsWith("\u2022 ", p)) all = false;
        for (int k = st.size() - 1; k >= 0; k--) {
            int p = st.get(k);
            if (all) e.delete(p, p + 2); else e.insert(p, "\u2022 ");
        }
        mark();
    }

    void numbers() {
        Editable e = ed.getText();
        String txt = e.toString();
        int[] r = par();
        int s = Math.max(lo(), 0), t = Math.max(hi(), 0);
        ArrayList<Integer> st = starts(txt, r[0], t > s ? t - 1 : t);
        boolean all = true;
        for (int p : st) if (!NUM.matcher(lineAt(txt, p)).find()) all = false;
        for (int k = st.size() - 1; k >= 0; k--) {
            int p = st.get(k);
            if (all) {
                Matcher m = NUM.matcher(lineAt(txt, p));
                if (m.find()) e.delete(p, p + m.end());
            } else e.insert(p, (k + 1) + ". ");
        }
        mark();
    }

    // ================= Dialog =================
    void fontDlg() {
        final String[] fam = Fonts.allFamilies();
        new AlertDialog.Builder(this).setTitle("Font").setItems(fam, (dl, w) -> setFontName(fam[w])).show();
    }

    void sizeDlg() {
        String[] it = new String[PT.length];
        for (int i = 0; i < PT.length; i++) it[i] = String.valueOf(PT[i]);
        new AlertDialog.Builder(this).setTitle("Ukuran huruf").setItems(it, (dl, w) -> setSize(PT[w] * 2)).show();
    }

    void colorDlg(final boolean text) {
        int[] cs = text ? TXT_COLORS : HL_COLORS;
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(8), dp(12), 0);
        final AlertDialog[] dl = new AlertDialog[1];
        LinearLayout r = null;
        for (int i = 0; i < cs.length; i++) {
            if (i % 6 == 0) { r = new LinearLayout(this); box.addView(r); }
            View v = new View(this);
            v.setBackgroundColor(cs[i]);
            final int col = cs[i];
            v.setOnClickListener(x -> { setColor(text, col); dl[0].dismiss(); });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(40), dp(40));
            p.setMargins(dp(4), dp(4), dp(4), dp(4));
            r.addView(v, p);
        }
        dl[0] = new AlertDialog.Builder(this).setTitle(text ? "Warna huruf" : "Sorot teks").setView(box)
            .setNeutralButton(text ? "Otomatis" : "Tanpa sorot", (x, y) -> setColor(text, 0))
            .setNegativeButton("Batal", null).create();
        dl[0].show();
    }

    void findDlg() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16), dp(8), dp(16), 0);
        final EditText a = new EditText(this);
        a.setHint("Cari");
        a.setSingleLine();
        final EditText b = new EditText(this);
        b.setHint("Ganti dengan");
        b.setSingleLine();
        l.addView(a);
        l.addView(b);
        new AlertDialog.Builder(this).setTitle("Cari & Ganti").setView(l)
            .setNeutralButton("Cari", (x, y) -> {
                String f = a.getText().toString();
                if (f.isEmpty()) return;
                String txt = ed.getText().toString();
                int i = txt.indexOf(f, Math.max(hi(), 0));
                if (i < 0) i = txt.indexOf(f);
                if (i < 0) { toast("Tidak ditemukan"); return; }
                ed.requestFocus();
                ed.setSelection(i, i + f.length());
            })
            .setPositiveButton("Ganti semua", (x, y) -> {
                String f = a.getText().toString();
                if (f.isEmpty()) return;
                Editable e = ed.getText();
                String txt = e.toString();
                int c = 0;
                int i = txt.lastIndexOf(f);
                while (i >= 0) {
                    e.replace(i, i + f.length(), b.getText());
                    c++;
                    i = i - f.length() >= 0 ? txt.lastIndexOf(f, i - f.length()) : -1;
                }
                toast(c + " penggantian");
            })
            .setNegativeButton("Tutup", null).show();
    }

    // ================= Undo / Redo =================
    void snapshot() {
        pending = false;
        if (restoring) return;
        SpannableStringBuilder s = new SpannableStringBuilder(ed.getText());
        while (hist.size() > hp + 1) hist.remove(hist.size() - 1);
        hist.add(s);
        if (hist.size() > 40) hist.remove(0);
        hp = hist.size() - 1;
    }

    void schedule() {
        pending = true;
        handler.removeCallbacks(snapRun);
        handler.postDelayed(snapRun, 600);
    }

    void resetHistory() {
        handler.removeCallbacks(snapRun);
        hist.clear();
        hp = -1;
        snapshot();
    }

    void restore(SpannableStringBuilder s) {
        restoring = true;
        int sel = ed.getSelectionStart();
        ed.setText(new SpannableStringBuilder(s));
        ed.setSelection(Math.max(0, Math.min(sel, ed.getText().length())));
        restoring = false;
        dirty = true;
        refresh();
        updateState();
    }

    void undo() {
        handler.removeCallbacks(snapRun);
        if (pending) snapshot();
        if (hp > 0) { hp--; restore(hist.get(hp)); } else toast("Tidak ada yang bisa di-undo");
    }

    void redo() {
        handler.removeCallbacks(snapRun);
        if (pending) snapshot();
        if (hp < hist.size() - 1) { hp++; restore(hist.get(hp)); } else toast("Tidak ada yang bisa di-redo");
    }

    // ================= Berkas =================
    boolean load(File f) {
        try {
            SpannableStringBuilder sb = Docx.read(f);
            applyDefaults();
            loading = true;
            ed.setText(sb);
            loading = false;
            dirty = false;
            resetHistory();
            updateState();
            return true;
        } catch (Exception x) {
            loading = false;
            toast("Gagal membuka: " + x.getMessage());
            return false;
        }
    }

    void confirmDiscard(final Runnable r) {
        if (!dirty) { r.run(); return; }
        new AlertDialog.Builder(this)
            .setMessage("Ada perubahan yang belum disimpan. Lanjutkan tanpa menyimpan?")
            .setPositiveButton("Lanjutkan", (a, b) -> r.run())
            .setNegativeButton("Batal", null)
            .show();
    }

    void newDoc() {
        confirmDiscard(() -> {
            Docx.defName = "Calibri";
            Docx.defHp = 22;
            applyDefaults();
            loading = true;
            ed.setText("");
            loading = false;
            cur = null;
            suggest = null;
            Docx.lost.clear();
            dirty = false;
            draft().delete();
            resetHistory();
            refresh();
            updateState();
        });
    }

    void openDlg() {
        confirmDiscard(() -> browse(lastDir == null ? dir() : lastDir));
    }

    void browse(File start) {
        final File base = (start != null && start.isDirectory()) ? start : dir();
        File[] all = base.listFiles();
        final ArrayList<File> list = new ArrayList<File>();
        ArrayList<String> names = new ArrayList<String>();
        if (base.getParentFile() != null) { list.add(base.getParentFile()); names.add(".. (naik satu folder)"); }
        if (all != null) {
            Arrays.sort(all);
            for (File f : all) {
                if (f.isDirectory() && !f.getName().startsWith(".")) { list.add(f); names.add("[ " + f.getName() + " ]"); }
            }
            for (File f : all) {
                String n = f.getName().toLowerCase();
                if (f.isFile() && (n.endsWith(".docx") || n.endsWith(".txt"))) { list.add(f); names.add(f.getName()); }
            }
        }
        new AlertDialog.Builder(this).setTitle(base.getPath())
            .setItems(names.toArray(new String[0]), (dl, w) -> {
                File f = list.get(w);
                if (f.isDirectory()) browse(f);
                else { lastDir = f.getParentFile(); openFile(f, f.getName(), true); }
            })
            .setNegativeButton("Batal", null).show();
    }

    String baseName(String n) {
        int i = n.lastIndexOf('.');
        return i > 0 ? n.substring(0, i) : n;
    }

    String readText(File f) throws Exception {
        InputStream is = new FileInputStream(f);
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) bo.write(buf, 0, n);
            return new String(bo.toByteArray(), "UTF-8");
        } finally {
            is.close();
        }
    }

    void openFile(File f, String name, boolean overwriteOk) {
        String ln = name.toLowerCase();
        if (ln.endsWith(".txt")) {
            try {
                String txt = readText(f);
                Docx.defName = "Calibri";
                Docx.defHp = 22;
                Docx.lost.clear();
                applyDefaults();
                loading = true;
                ed.setText(txt);
                loading = false;
                cur = null;
                suggest = baseName(name);
                dirty = false;
                draft().delete();
                resetHistory();
                refresh();
                updateState();
            } catch (Exception x) {
                loading = false;
                toast("Gagal membuka: " + x.getMessage());
            }
            return;
        }
        if (!load(f)) return;
        cur = null;
        suggest = null;
        if (!Docx.lost.isEmpty()) {
            suggest = baseName(name) + "-edit";
            StringBuilder sb = new StringBuilder();
            for (String x : Docx.lost) { if (sb.length() > 0) sb.append(", "); sb.append(x); }
            new AlertDialog.Builder(this).setTitle("Sebagian isi belum didukung")
                .setMessage("Dokumen ini berisi: " + sb + ".\n\nBagian itu belum bisa ditampilkan dan akan hilang jika disimpan. "
                    + "Teksnya tetap bisa diedit. Supaya aman, file asli tidak akan ditimpa: hasil edit disimpan sebagai salinan baru.")
                .setPositiveButton("Mengerti", null).show();
        } else if (overwriteOk) {
            cur = f;
        } else {
            suggest = baseName(name) + "-edit";
        }
        draft().delete();
        refresh();
    }

    void openUri(Uri u) {
        try {
            if ("file".equals(u.getScheme())) {
                File f = new File(u.getPath());
                openFile(f, f.getName(), true);
                return;
            }
            String name = "dokumen.docx";
            Cursor c = getContentResolver().query(u, null, null, null, null);
            if (c != null) {
                try {
                    int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (i >= 0 && c.moveToFirst() && c.getString(i) != null) name = c.getString(i);
                } finally {
                    c.close();
                }
            }
            File f = new File(getCacheDir(), name.toLowerCase().endsWith(".txt") ? "incoming.txt" : "incoming.docx");
            InputStream is = getContentResolver().openInputStream(u);
            OutputStream os = new FileOutputStream(f);
            try {
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) > 0) os.write(buf, 0, n);
            } finally {
                is.close();
                os.close();
            }
            openFile(f, name, false);
        } catch (Exception x) {
            toast("Gagal membuka: " + x.getMessage());
        }
    }

    void save() { if (cur == null) saveAs(); else doSave(cur); }

    void saveAs() {
        final EditText n = new EditText(this);
        n.setText(cur != null ? cur.getName().replace(".docx", "") : (suggest != null ? suggest : "Dokumen1"));
        new AlertDialog.Builder(this).setTitle("Simpan sebagai (.docx)").setView(n)
            .setPositiveButton("Simpan", (a, b) -> {
                String nm = n.getText().toString().trim().replaceAll("[\\\\/:*?\"<>|]", "_");
                if (nm.isEmpty()) nm = "Dokumen1";
                doSave(new File(dir(), nm + ".docx"));
            })
            .setNegativeButton("Batal", null).show();
    }

    void doSave(File f) {
        try {
            Docx.write(ed.getText(), f);
            cur = f;
            suggest = null;
            dirty = false;
            draft().delete();
            refresh();
            toast("Tersimpan: Dokumen/" + f.getName());
        } catch (Exception x) {
            toast("Gagal menyimpan: " + x.getMessage());
        }
    }

    // ================= Keyboard & siklus hidup =================
    @Override
    public boolean dispatchKeyEvent(KeyEvent ev) {
        if (ev.getAction() == KeyEvent.ACTION_DOWN && ev.isCtrlPressed()) {
            switch (ev.getKeyCode()) {
                case KeyEvent.KEYCODE_S: if (ev.isShiftPressed()) saveAs(); else save(); return true;
                case KeyEvent.KEYCODE_B: toggleBold(true); return true;
                case KeyEvent.KEYCODE_I: toggleBold(false); return true;
                case KeyEvent.KEYCODE_U: toggleMark(UnderlineSpan.class); return true;
                case KeyEvent.KEYCODE_Z: undo(); return true;
                case KeyEvent.KEYCODE_Y: redo(); return true;
                case KeyEvent.KEYCODE_O: openDlg(); return true;
                case KeyEvent.KEYCODE_N: newDoc(); return true;
                case KeyEvent.KEYCODE_F: case KeyEvent.KEYCODE_H: findDlg(); return true;
                case KeyEvent.KEYCODE_L: align(0); return true;
                case KeyEvent.KEYCODE_E: align(1); return true;
                case KeyEvent.KEYCODE_R: align(2); return true;
                case KeyEvent.KEYCODE_J: align(3); return true;
                default: break;
            }
        }
        return super.dispatchKeyEvent(ev);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (dirty) {
            try {
                if (cur != null) { Docx.write(ed.getText(), cur); dirty = false; refresh(); }
                else Docx.write(ed.getText(), draft());
            } catch (Exception ignore) { }
        }
    }
}
