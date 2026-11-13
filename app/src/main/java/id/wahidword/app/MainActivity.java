package id.wahidword.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int BLUE = Color.rgb(43, 87, 154);
    private EditText editor;
    private TextView status;
    private LinearLayout ribbonTools;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        if (android.os.Build.VERSION.SDK_INT >= 21) {\n            getWindow().setStatusBarColor(Color.rgb(23, 54, 93));\n        }
        buildEditor();
    }

    private void buildEditor() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(235, 238, 242));

        TextView title = new TextView(this);
        title.setText("Wahid Word     •     Dokumen baru");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(dp(16), 0, dp(12), 0);
        title.setBackgroundColor(Color.rgb(23, 54, 93));
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(52)));

        HorizontalScrollView tabsScroll = new HorizontalScrollView(this);
        tabsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setBackgroundColor(Color.WHITE);
        addTab(tabs, "File");
        addTab(tabs, "Home");
        addTab(tabs, "Insert");
        addTab(tabs, "Layout");
        addTab(tabs, "Review");
        addTab(tabs, "View");
        tabsScroll.addView(tabs);
        root.addView(tabsScroll, new LinearLayout.LayoutParams(-1, dp(46)));

        HorizontalScrollView toolsScroll = new HorizontalScrollView(this);
        toolsScroll.setHorizontalScrollBarEnabled(false);
        ribbonTools = new LinearLayout(this);
        ribbonTools.setOrientation(LinearLayout.HORIZONTAL);
        ribbonTools.setGravity(Gravity.CENTER_VERTICAL);
        ribbonTools.setPadding(dp(6), dp(5), dp(6), dp(5));
        ribbonTools.setBackgroundColor(Color.rgb(248, 249, 251));
        addTool("B", new Runnable() { public void run() { toggleStyle(new StyleSpan(Typeface.BOLD), StyleSpan.class); }});
        addTool("I", new Runnable() { public void run() { toggleStyle(new StyleSpan(Typeface.ITALIC), StyleSpan.class); }});
        addTool("U", new Runnable() { public void run() { toggleStyle(new UnderlineSpan(), UnderlineSpan.class); }});
        addTool("A+", new Runnable() { public void run() { changeSize(1.15f); }});
        addTool("A−", new Runnable() { public void run() { changeSize(0.87f); }});
        addTool("Hitam", new Runnable() { public void run() { setTextColor(Color.BLACK); }});
        addTool("Biru", new Runnable() { public void run() { setTextColor(BLUE); }});
        addTool("Kiri", new Runnable() { public void run() { editor.setGravity(Gravity.TOP | Gravity.LEFT); }});
        addTool("Tengah", new Runnable() { public void run() { editor.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL); }});
        addTool("Kanan", new Runnable() { public void run() { editor.setGravity(Gravity.TOP | Gravity.RIGHT); }});
        toolsScroll.addView(ribbonTools);
        root.addView(toolsScroll, new LinearLayout.LayoutParams(-1, dp(52)));

        ScrollView pageScroll = new ScrollView(this);
        pageScroll.setFillViewport(true);
        LinearLayout pageHolder = new LinearLayout(this);
        pageHolder.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        pageHolder.setPadding(dp(12), dp(14), dp(12), dp(14));

        editor = new EditText(this);
        editor.setTextColor(Color.BLACK);
        editor.setTextSize(16);
        editor.setGravity(Gravity.TOP | Gravity.LEFT);
        editor.setPadding(dp(26), dp(28), dp(26), dp(28));
        editor.setHint("Mulai mengetik dokumen Anda di sini...");
        editor.setHintTextColor(Color.GRAY);
        editor.setBackgroundColor(Color.WHITE);
        editor.setMinHeight(dp(540));
        editor.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editor.setText("Wahid Word\n\nMulai menulis di sini.");
        pageHolder.addView(editor, new LinearLayout.LayoutParams(-1, -2));
        pageScroll.addView(pageHolder, new ScrollView.LayoutParams(-1, -2));
        root.addView(pageScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        status = new TextView(this);
        status.setText("Halaman 1     |     Kata: 0     |     Prototipe editor");
        status.setTextColor(Color.DKGRAY);
        status.setTextSize(12);
        status.setPadding(dp(12), 0, dp(12), 0);
        status.setGravity(Gravity.CENTER_VERTICAL);
        status.setBackgroundColor(Color.WHITE);
        root.addView(status, new LinearLayout.LayoutParams(-1, dp(34)));

        editor.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                status.setText("Halaman 1     |     Kata: " + countWords(s.toString()) + "     |     Belum disimpan");
            }
            public void afterTextChanged(Editable e) {}
        });
        setContentView(root);
    }

    private void addTab(LinearLayout tabs, final String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(13);
        b.setTextColor(label.equals("Home") ? BLUE : Color.DKGRAY);
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,
                        label + ": panel lanjutan akan diaktifkan bertahap.", Toast.LENGTH_SHORT).show();
            }
        });
        tabs.addView(b, new LinearLayout.LayoutParams(-2, dp(46)));
    }

    private void addTool(String label, final Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(12);
        b.setMinWidth(dp(46));
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { action.run(); }
        });
        ribbonTools.addView(b, new LinearLayout.LayoutParams(-2, dp(42)));
    }

    private void toggleStyle(Object span, Class kind) {
        int start = editor.getSelectionStart();
        int end = editor.getSelectionEnd();
        if (start == end) {
            Toast.makeText(this, "Pilih teks terlebih dahulu.", Toast.LENGTH_SHORT).show();
            return;
        }
        Editable text = editor.getText();
        Object[] existing = text.getSpans(start, end, kind);
        if (existing.length > 0) {
            for (Object item : existing) text.removeSpan(item);
        } else {
            text.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void changeSize(float factor) {
        int start = editor.getSelectionStart();
        int end = editor.getSelectionEnd();
        if (start == end) {
            Toast.makeText(this, "Pilih teks terlebih dahulu.", Toast.LENGTH_SHORT).show();
            return;
        }
        editor.getText().setSpan(new RelativeSizeSpan(factor), start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void setTextColor(int color) {
        int start = editor.getSelectionStart();
        int end = editor.getSelectionEnd();
        if (start == end) {
            Toast.makeText(this, "Pilih teks terlebih dahulu.", Toast.LENGTH_SHORT).show();
            return;
        }
        editor.getText().setSpan(new ForegroundColorSpan(color), start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private int countWords(String value) {
        String trimmed = value.trim();
        if (trimmed.length() == 0) return 0;
        return trimmed.split("\\s+").length;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
