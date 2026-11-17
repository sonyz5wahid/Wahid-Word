package id.tulis.editor;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.UnderlineSpan;
import android.util.Xml;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.xmlpull.v1.XmlPullParser;

/** Baca/tulis .docx: teks, font, ukuran, tebal, miring, garis bawah, coret, warna, sorot, perataan, jarak baris, indentasi. */
final class Docx {
    static String defName = "Calibri";   // font bawaan dokumen
    static int defHp = 22;               // ukuran bawaan dokumen (setengah poin)
    static final LinkedHashSet<String> lost = new LinkedHashSet<String>(); // bagian dokumen yang belum didukung
    static float unit = 1f;              // piksel per setengah poin (diatur oleh MainActivity)

    private static final int INC = Spanned.SPAN_EXCLUSIVE_INCLUSIVE;
    private static final int PAR = Spanned.SPAN_PARAGRAPH;
    private static final String W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final String DECL = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";

    // ---------- TULIS ----------
    static void write(Spanned t, File f) throws IOException {
        File tmp = new File(f.getPath() + ".tmp");
        ZipOutputStream z = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(tmp)));
        try {
            put(z, "[Content_Types].xml", DECL
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
                + "<Override PartName=\"/word/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml\"/>"
                + "</Types>");
            put(z, "_rels/.rels", DECL
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>"
                + "</Relationships>");
            put(z, "word/_rels/document.xml.rels", DECL
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
                + "</Relationships>");
            String fn = esc(defName);
            put(z, "word/styles.xml", DECL + "<w:styles xmlns:w=\"" + W + "\"><w:docDefaults><w:rPrDefault><w:rPr>"
                + "<w:rFonts w:ascii=\"" + fn + "\" w:hAnsi=\"" + fn + "\" w:eastAsia=\"" + fn + "\" w:cs=\"" + fn + "\"/>"
                + "<w:sz w:val=\"" + defHp + "\"/><w:szCs w:val=\"" + defHp + "\"/><w:lang w:val=\"id-ID\"/>"
                + "</w:rPr></w:rPrDefault><w:pPrDefault><w:pPr><w:spacing w:after=\"0\" w:line=\"240\" w:lineRule=\"auto\"/></w:pPr></w:pPrDefault></w:docDefaults>"
                + "<w:style w:type=\"paragraph\" w:default=\"1\" w:styleId=\"Normal\"><w:name w:val=\"Normal\"/><w:qFormat/></w:style></w:styles>");
            put(z, "word/document.xml", body(t));
        } finally {
            z.close();
        }
        if (!tmp.renameTo(f)) {
            f.delete();
            if (!tmp.renameTo(f)) throw new IOException("Tidak bisa menulis file");
        }
    }

    private static void put(ZipOutputStream z, String name, String s) throws IOException {
        z.putNextEntry(new ZipEntry(name));
        z.write(s.getBytes("UTF-8"));
        z.closeEntry();
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static Object at(Spanned t, Object[] arr, int ps, int pe) {
        for (Object o : arr) {
            int s = t.getSpanStart(o), e = t.getSpanEnd(o);
            if (s <= ps && (e > ps || (e == ps && pe == ps))) return o;
        }
        return null;
    }

    private static String body(Spanned t) {
        StringBuilder x = new StringBuilder(DECL);
        x.append("<w:document xmlns:w=\"").append(W).append("\"><w:body>");
        String s = t.toString();
        int len = t.length();
        Object[] al = t.getSpans(0, len, AlignmentSpan.class);
        Object[] js = t.getSpans(0, len, JustifySpan.class);
        Object[] ls = t.getSpans(0, len, LineSpacingSpan.class);
        Object[] ids = t.getSpans(0, len, IndentSpan.class);
        int ps = 0;
        while (true) {
            int pe = s.indexOf('\n', ps);
            boolean last = pe < 0;
            if (last) pe = s.length();
            x.append("<w:p>");
            String jc = null;
            AlignmentSpan a = (AlignmentSpan) at(t, al, ps, pe);
            if (a != null) {
                Layout.Alignment k = a.getAlignment();
                if (k == Layout.Alignment.ALIGN_CENTER) jc = "center";
                else if (k == Layout.Alignment.ALIGN_OPPOSITE) jc = "right";
            }
            if (jc == null && at(t, js, ps, pe) != null) jc = "both";
            LineSpacingSpan l = (LineSpacingSpan) at(t, ls, ps, pe);
            IndentSpan ind = (IndentSpan) at(t, ids, ps, pe);
            if (jc != null || l != null || ind != null) {
                x.append("<w:pPr>");
                if (l != null) x.append("<w:spacing w:line=\"").append(l.line).append("\" w:lineRule=\"auto\"/>");
                if (ind != null) x.append("<w:ind w:left=\"").append(ind.twips).append("\"/>");
                if (jc != null) x.append("<w:jc w:val=\"").append(jc).append("\"/>");
                x.append("</w:pPr>");
            }
            int i = ps;
            while (i < pe) {
                int n = t.nextSpanTransition(i, pe, CharacterStyle.class);
                if (n <= i) n = pe;
                run(x, t, i, n);
                i = n;
            }
            x.append("</w:p>");
            if (last) break;
            ps = pe + 1;
        }
        x.append("<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/>"
            + "<w:pgMar w:top=\"1440\" w:right=\"1440\" w:bottom=\"1440\" w:left=\"1440\" w:header=\"720\" w:footer=\"720\" w:gutter=\"0\"/>"
            + "</w:sectPr></w:body></w:document>");
        return x.toString();
    }

    private static String hex(int c) { return String.format("%06X", c & 0xFFFFFF); }

    private static void run(StringBuilder x, Spanned t, int a, int b) {
        FontSpan fs = null;
        SizeSpan sz = null;
        ForegroundColorSpan fg = null;
        BackgroundColorSpan bg = null;
        boolean un = false, st = false, sup = false, sub = false;
        for (Object o : t.getSpans(a, b, Object.class)) {
            if (o instanceof FontSpan) fs = (FontSpan) o;
            else if (o instanceof SizeSpan) sz = (SizeSpan) o;
            else if (o instanceof ForegroundColorSpan) fg = (ForegroundColorSpan) o;
            else if (o instanceof BackgroundColorSpan) bg = (BackgroundColorSpan) o;
            else if (o instanceof UnderlineSpan) un = true;
            else if (o instanceof StrikethroughSpan) st = true;
            else if (o instanceof SuperscriptSpan) sup = true;
            else if (o instanceof SubscriptSpan) sub = true;
        }
        x.append("<w:r>");
        boolean hasSz = sz != null && sz.hp != defHp;
        if (fs != null || hasSz || fg != null || bg != null || un || st || sup || sub) {
            x.append("<w:rPr>");
            if (fs != null) {
                String n = esc(fs.name);
                x.append("<w:rFonts w:ascii=\"").append(n).append("\" w:hAnsi=\"").append(n).append("\" w:cs=\"").append(n).append("\"/>");
                if (fs.bold) x.append("<w:b/>");
                if (fs.italic) x.append("<w:i/>");
            }
            if (st) x.append("<w:strike/>");
            if (fg != null) x.append("<w:color w:val=\"").append(hex(fg.getForegroundColor())).append("\"/>");
            if (hasSz) x.append("<w:sz w:val=\"").append(sz.hp).append("\"/>");
            if (un) x.append("<w:u w:val=\"single\"/>");
            if (bg != null) x.append("<w:shd w:val=\"clear\" w:color=\"auto\" w:fill=\"").append(hex(bg.getBackgroundColor())).append("\"/>");
            if (sup) x.append("<w:vertAlign w:val=\"superscript\"/>");
            else if (sub) x.append("<w:vertAlign w:val=\"subscript\"/>");
            x.append("</w:rPr>");
        }
        String s = t.subSequence(a, b).toString();
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\t') {
                flush(x, buf);
                x.append("<w:tab/>");
            } else if (c == '&') buf.append("&amp;");
            else if (c == '<') buf.append("&lt;");
            else if (c == '>') buf.append("&gt;");
            else if (c >= 0x20) buf.append(c);
        }
        flush(x, buf);
        x.append("</w:r>");
    }

    private static void flush(StringBuilder x, StringBuilder buf) {
        if (buf.length() > 0) {
            x.append("<w:t xml:space=\"preserve\">").append(buf).append("</w:t>");
            buf.setLength(0);
        }
    }

    // ---------- BACA ----------
    static SpannableStringBuilder read(File f) throws Exception {
        ZipFile zf = new ZipFile(f);
        try {
            ZipEntry de = zf.getEntry("word/document.xml");
            if (de == null) throw new IOException("Bukan file .docx yang valid");
            defName = "Calibri";
            defHp = 22;
            lost.clear();
            ZipEntry se = zf.getEntry("word/styles.xml");
            if (se != null) {
                InputStream si = zf.getInputStream(se);
                try { parseStyles(si); } catch (Exception ignore) { } finally { si.close(); }
            }
            InputStream in = zf.getInputStream(de);
            try { return parse(in); } finally { in.close(); }
        } finally {
            zf.close();
        }
    }

    private static void parseStyles(InputStream in) throws Exception {
        XmlPullParser p = Xml.newPullParser();
        p.setInput(in, "UTF-8");
        boolean inDef = false, inNormal = false;
        String font = null;
        int hp = 0;
        for (int ev = p.getEventType(); ev != XmlPullParser.END_DOCUMENT; ev = p.next()) {
            if (ev == XmlPullParser.START_TAG) {
                String n = p.getName();
                if (n.equals("w:rPrDefault")) inDef = true;
                else if (n.equals("w:style")) inNormal = "Normal".equals(p.getAttributeValue(null, "w:styleId"));
                else if ((inDef || inNormal) && n.equals("w:rFonts")) {
                    String v = p.getAttributeValue(null, "w:ascii");
                    if (v != null) font = v;
                } else if ((inDef || inNormal) && n.equals("w:sz")) {
                    try { hp = Integer.parseInt(p.getAttributeValue(null, "w:val")); } catch (Exception ignore) { }
                }
            } else if (ev == XmlPullParser.END_TAG) {
                String n = p.getName();
                if (n.equals("w:rPrDefault")) inDef = false;
                else if (n.equals("w:style")) inNormal = false;
            }
        }
        if (font != null) defName = font;
        if (hp > 0) defHp = hp;
    }

    private static boolean on(XmlPullParser p) {
        String v = p.getAttributeValue(null, "w:val");
        return v == null || !(v.equals("0") || v.equals("false") || v.equals("none"));
    }

    private static int hl(String v) {
        if (v == null) return 0;
        if (v.equals("yellow")) return 0xFFFFFF00;
        if (v.equals("green")) return 0xFF00FF00;
        if (v.equals("cyan")) return 0xFF00FFFF;
        if (v.equals("magenta")) return 0xFFFF00FF;
        if (v.equals("blue")) return 0xFF0000FF;
        if (v.equals("red")) return 0xFFFF0000;
        if (v.equals("lightGray")) return 0xFFC0C0C0;
        if (v.equals("darkGray")) return 0xFF808080;
        return 0;
    }

    private static int rgb(String v) {
        if (v == null || v.length() != 6) return 0;
        try { return 0xFF000000 | Integer.parseInt(v, 16); } catch (Exception e) { return 0; }
    }

    private static SpannableStringBuilder parse(InputStream in) throws Exception {
        XmlPullParser p = Xml.newPullParser();
        p.setInput(in, "UTF-8");
        SpannableStringBuilder sb = new SpannableStringBuilder();
        int ps = 0, line = 0, ind = 0;
        String jc = null;
        boolean inPPr = false, inRPr = false, sawP = false;
        String fn = null;
        boolean bo = false, it = false, un = false, st = false;
        int hp = 0, col = 0, shd = 0, va = 0;
        for (int ev = p.getEventType(); ev != XmlPullParser.END_DOCUMENT; ev = p.next()) {
            if (ev == XmlPullParser.START_TAG) {
                String n = p.getName();
                if (n.equals("w:tbl")) lost.add("tabel");
                else if (n.equals("w:drawing") || n.equals("w:pict") || n.equals("w:object")) lost.add("gambar/objek");
                else if (n.equals("w:footnoteReference") || n.equals("w:endnoteReference")) lost.add("catatan kaki");
                else if (n.equals("w:headerReference") || n.equals("w:footerReference")) lost.add("header/footer");
                else if (n.equals("w:numPr")) lost.add("daftar bernomor otomatis");
                if (n.equals("w:p")) { ps = sb.length(); jc = null; line = 0; ind = 0; sawP = true; }
                else if (n.equals("w:pPr")) inPPr = true;
                else if (n.equals("w:r")) { fn = null; bo = false; it = false; un = false; st = false; hp = 0; col = 0; shd = 0; va = 0; }
                else if (n.equals("w:rPr")) inRPr = true;
                else if (inPPr && !inRPr) {
                    if (n.equals("w:jc")) jc = p.getAttributeValue(null, "w:val");
                    else if (n.equals("w:spacing")) {
                        String l = p.getAttributeValue(null, "w:line");
                        String r = p.getAttributeValue(null, "w:lineRule");
                        if (l != null && (r == null || r.equals("auto"))) {
                            try { line = Integer.parseInt(l); } catch (Exception ignore) { }
                        }
                    } else if (n.equals("w:ind")) {
                        String v = p.getAttributeValue(null, "w:left");
                        if (v == null) v = p.getAttributeValue(null, "w:start");
                        try { ind = Integer.parseInt(v); } catch (Exception ignore) { }
                    }
                }
                else if (inRPr && !inPPr) {
                    if (n.equals("w:rFonts")) {
                        String v = p.getAttributeValue(null, "w:ascii");
                        if (v == null) v = p.getAttributeValue(null, "w:hAnsi");
                        if (v != null) fn = v;
                    }
                    else if (n.equals("w:b")) bo = on(p);
                    else if (n.equals("w:i")) it = on(p);
                    else if (n.equals("w:u")) un = on(p);
                    else if (n.equals("w:strike")) st = on(p);
                    else if (n.equals("w:color")) col = rgb(p.getAttributeValue(null, "w:val"));
                    else if (n.equals("w:highlight")) { int c = hl(p.getAttributeValue(null, "w:val")); if (c != 0) shd = c; }
                    else if (n.equals("w:shd")) { int c = rgb(p.getAttributeValue(null, "w:fill")); if (c != 0) shd = c; }
                    else if (n.equals("w:vertAlign")) {
                        String v = p.getAttributeValue(null, "w:val");
                        va = "superscript".equals(v) ? 1 : "subscript".equals(v) ? 2 : 0;
                    }
                    else if (n.equals("w:sz")) {
                        try { hp = Integer.parseInt(p.getAttributeValue(null, "w:val")); } catch (Exception ignore) { }
                    }
                }
                else if (n.equals("w:tab")) sb.append('\t');
                else if (n.equals("w:br")) sb.append('\n');
                else if (n.equals("w:t")) add(sb, p.nextText(), fn, bo, it, un, st, hp, col, shd, va);
            } else if (ev == XmlPullParser.END_TAG) {
                String n = p.getName();
                if (n.equals("w:pPr")) inPPr = false;
                else if (n.equals("w:rPr")) inRPr = false;
                else if (n.equals("w:p")) {
                    sb.append('\n');
                    int e = sb.length();
                    try {
                        if ("center".equals(jc)) sb.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), ps, e, PAR);
                        else if ("right".equals(jc) || "end".equals(jc)) sb.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE), ps, e, PAR);
                        else if ("both".equals(jc) || "distribute".equals(jc)) sb.setSpan(new JustifySpan(), ps, e, PAR);
                        if (line > 0 && line != 240) sb.setSpan(new LineSpacingSpan(line), ps, e, PAR);
                        if (ind > 0) sb.setSpan(new IndentSpan(ind), ps, e, PAR);
                    } catch (RuntimeException ignore) { }
                }
            }
        }
        if (sawP && sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') sb.delete(sb.length() - 1, sb.length());
        return sb;
    }

    private static void add(SpannableStringBuilder sb, String t, String fn, boolean bo, boolean it, boolean un,
                            boolean st, int hp, int col, int shd, int va) {
        int s = sb.length();
        sb.append(t);
        int e = sb.length();
        if (e == s) return;
        if (fn != null || bo || it) sb.setSpan(new FontSpan(fn == null ? defName : fn, bo, it), s, e, INC);
        if (hp > 0 && hp != defHp) sb.setSpan(new SizeSpan(hp), s, e, INC);
        if (col != 0) sb.setSpan(new ForegroundColorSpan(col), s, e, INC);
        if (shd != 0) sb.setSpan(new BackgroundColorSpan(shd), s, e, INC);
        if (un) sb.setSpan(new UnderlineSpan(), s, e, INC);
        if (st) sb.setSpan(new StrikethroughSpan(), s, e, INC);
        if (va == 1) { sb.setSpan(new SuperscriptSpan(), s, e, INC); sb.setSpan(new RelativeSizeSpan(0.65f), s, e, INC); }
        else if (va == 2) { sb.setSpan(new SubscriptSpan(), s, e, INC); sb.setSpan(new RelativeSizeSpan(0.65f), s, e, INC); }
    }
}
