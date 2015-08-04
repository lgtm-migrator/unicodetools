package org.unicode.tools;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.ChainedMap.M3;
import org.unicode.cldr.util.ChainedMap.M4;
import org.unicode.cldr.util.Counter;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.text.utility.Settings;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMap.EntryRange;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Output;


public class Ids {

    private static final boolean DEBUG = false;

    private static final Splitter DOT_SPLITTER = Splitter.on('.');
    private static final Splitter VBAR_SPLITTER = Splitter.on('|');
    private static final Splitter TAB_SPLITTER = Splitter.on('\t');
    private static final Splitter SEMI_SPLITTER = Splitter.on(';').trimResults();
    private static final Joiner SPACE_JOINER = Joiner.on(' ');
    private static final Joiner CRLF_JOINER = Joiner.on('\n');

    private static final IndexUnicodeProperties iup = IndexUnicodeProperties.make(Settings.latestVersion);
    private static final UnicodeMap<List<String>> radicalStroke = iup.loadList(UcdProperty.kRSUnicode);
    private static final UnicodeMap<Set<String>> adobeRadicalStroke = iup.loadSet(UcdProperty.kRSAdobe_Japan1_6);
    private static final UnicodeMap<Integer> numericRadicalStroke;
    private static final M3<Integer,Boolean,UnicodeSet> USTROKE = ChainedMap.of(new TreeMap(), new TreeMap(), UnicodeSet.class);
    static {
        numericRadicalStroke = new UnicodeMap<>();
        for (Entry<String, List<String>> entry : radicalStroke.entrySet()) {
            List<String> items = entry.getValue();
            List<String> parts = DOT_SPLITTER.splitToList(items.get(0));
            String rad = parts.get(0);
            int radInt;
            boolean alt = rad.endsWith("'");
            radInt = Integer.parseInt(alt ? rad.substring(0,rad.length()-1) : rad);
            final int remStrokes = Integer.parseInt(parts.get(1));
            numericRadicalStroke.put(entry.getKey(), radInt*10000 + (alt ? 1000 : 0) + remStrokes);
            if (remStrokes == 0) {
                UnicodeSet uset = USTROKE.get(radInt, alt);
                if (uset == null) {
                    USTROKE.put(radInt, alt, uset = new UnicodeSet());
                }
                uset.add(entry.getKey());
            }
        }
        numericRadicalStroke.freeze();

        for (Entry<Integer, Map<Boolean, UnicodeSet>> entry : USTROKE) {
            for (Entry<Boolean, UnicodeSet> entry3 : entry.getValue().entrySet()) {
                entry3.getValue().freeze();
            }
        }
    }

    private static final Comparator<String> UNIHAN = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            int diff = compare2(o1, o2);
            if (diff == 0 && !o1.equals(o2)) {
                compare2(o1, o2);
                throw new IllegalAccessError();
            }
            return diff;
        }
        public int compare2(String o1, String o2) {
            int cp1;
            int cp2;
            int i1 = 0;
            int i2 = 0;
            while (i1 < o1.length() && i2 < o2.length()) {
                cp1 = o1.codePointAt(i1++);
                cp2 = o2.codePointAt(i2++);
                if (cp1 != cp2) {
                    Integer rs1 = numericRadicalStroke.get(cp1);
                    Integer rs2 = numericRadicalStroke.get(cp2);
                    if (rs1 == null) {
                        if (rs2 != null) {
                            return -1;
                        }
                    } else { // ≠ null
                        if (rs2 == null) {
                            return 1;
                        } else { // ≠ null
                            int diff = rs1 - rs2;
                            if (diff != 0) {
                                return diff;
                            }
                        }
                    }
                    return cp1 - cp2;
                }
                if (cp1 > 0xFFFF) ++i1;
                if (cp2 > 0xFFFF) ++i2;
            }
            return o1.length() - o2.length();
        }

    };
    //private static final UnicodeMap<String> totalStrokes = iup.load(UcdProperty.kTotalStrokes);

    static final Pattern ADOBE = Pattern.compile("[CV]\\+[0-9]{1,5}\\+([1-9][0-9]{0,2})\\.([1-9][0-9]?)\\.([0-9]{1,2})");
    /**
     *  radical, rstrokes, remaining => Unicode => remaining

     */
    private static final M4<Integer,Integer,Integer,UnicodeSet> ASTROKE = ChainedMap.of(new TreeMap(), new TreeMap(), new TreeMap(), UnicodeSet.class);
    static {
        Matcher m = ADOBE.matcher("");
        for (Entry<String, Set<String>> entry : adobeRadicalStroke.entrySet()) {
            String source = entry.getKey();
            for (String s : entry.getValue()) {
                if (!m.reset(s).matches()) {
                    throw new IllegalArgumentException();
                }
                int radical = Integer.parseInt(m.group(1));
                int rstrokes = Integer.parseInt(m.group(2));
                int remaining = Integer.parseInt(m.group(3));
                UnicodeSet map = ASTROKE.get(radical, rstrokes, remaining);
                if (map == null) {
                    ASTROKE.put(radical, rstrokes, remaining, map = new UnicodeSet());
                }
                map.add(source);
            }
        }
        for (Entry<Integer, Map<Integer, Map<Integer, UnicodeSet>>> entry : ASTROKE) {
            for (Entry<Integer, Map<Integer, UnicodeSet>> entry2 : entry.getValue().entrySet()) {
                for (Entry<Integer, UnicodeSet> entry3 : entry2.getValue().entrySet()) {
                    entry3.getValue().freeze();
                }
            }
        }
    }

    private static final UnicodeMap<String> unicodeToRadical;
    private static final Relation<String,String> rawRadToUnicode;
    private static final Relation<String,String> radToUnicode;
    private static final Relation<Integer,String> radToCjkRad;
    static {
        UnicodeMap<List<String>> unicodeToRadicalRaw = iup.loadList(UcdProperty.CJK_Radical);

        rawRadToUnicode = Relation.of(new TreeMap(), TreeSet.class);
        for (Entry<String, List<String>> entry : unicodeToRadicalRaw.entrySet()) {
            rawRadToUnicode.putAll(entry.getValue(), entry.getKey());
        }
        rawRadToUnicode.freeze();

        unicodeToRadical = new UnicodeMap<>();
        // Add extra Adobe radicals first
        for (Entry<Integer, Map<Integer, Map<Integer, UnicodeSet>>> entry : ASTROKE) {
            final int radical = entry.getKey();
            final String radString = String.valueOf(radical);
            for (Entry<Integer, Map<Integer, UnicodeSet>> entry2 : entry.getValue().entrySet()) {
                final int strokesInRadical = entry2.getKey();
                if (radical == 212 && strokesInRadical == 10) {
                    int debug = 0;
                }
                UnicodeSet itemsForZero = entry2.getValue().get(0);
                if (itemsForZero != null) {
                    for (String s : itemsForZero) {
                        unicodeToRadical.put(s, radString);
                    }
                }
            }
        }

        fillRadical(unicodeToRadicalRaw);

        radToUnicode = Relation.of(new TreeMap(), TreeSet.class);
        for (Entry<String, String> entry : unicodeToRadical.entrySet()) {
            radToUnicode.put(entry.getValue(), entry.getKey());
        }

        radToCjkRad = Relation.of(new TreeMap(), TreeSet.class);
        for (String line : FileUtilities.in(Ids.class, "idsCjkRadicals.txt")) {
            int hashPos = line.indexOf('#');
            if (hashPos >= 0) {
                line= line.substring(0, hashPos).trim();
            }
            if (line.isEmpty()) {
                continue;
            }
            List<String> parts = SEMI_SPLITTER.splitToList(line);
            int cjkRad = Integer.parseInt(parts.get(0), 16);
            final String radString = parts.get(1);
            int radNumber = Integer.parseInt(radString);
            radToCjkRad.put(radNumber, UTF16.valueOf(cjkRad));
            radToUnicode.put(radString, UTF16.valueOf(cjkRad));
        }
        
        radToUnicode.freeze();
    }

    private static void fillRadical(UnicodeMap<List<String>> cjkRadicalRaw) {
        Relation<String,Integer> unicodeToRsRadicals;
        for (EntryRange<List<String>> entry : radicalStroke.entryRanges()) {
            for (int cp = entry.codepoint; cp <= entry.codepointEnd; ++cp) {
                for (String rs : entry.value) {
                    if (rs.endsWith(".0")) {
                        List<String> rad = cjkRadicalRaw.get(cp);
                        if (rad == null) {
                            rad = new ArrayList<String>();
                            for (String vb : VBAR_SPLITTER.split(rs)) {
                                List<String> ds = DOT_SPLITTER.splitToList(vb);
                                if ("0".equals(ds.get(1))) {
                                    rad.add(ds.get(0));
                                }
                            }
                            unicodeToRadical.put(cp, rad.iterator().next());
                        }
                    }
                }
            }
        }
        for (Entry<String, List<String>> entry : cjkRadicalRaw.entrySet()) {
            if (!unicodeToRadical.containsKey(entry.getKey())) {
                unicodeToRadical.put(entry.getKey(), entry.getValue().iterator().next());
                //System.out.println("Missing:\t" + entry.getKey() + "\t" + entry.getValue() + "\t" + UCharacter.getName(entry.getKey(),"+"));
            }
        }
        unicodeToRadical.freeze();
    }

    private static final UnicodeSet IDS = new UnicodeSet("[[:IDS_Binary_Operator:][:IDS_Trinary_Operator:]]").freeze();
    private static final UnicodeSet STROKES = new UnicodeSet("[㇀-㇣]").freeze();
    private static final UnicodeSet EXT = new UnicodeSet(0xE000,0xE07F).freeze();
    private static final UnicodeSet EXT_E = new UnicodeSet(0x2B820,0x2CEA1).freeze();
    private static final UnicodeSet IDEOGRAPHIC = new UnicodeSet("[[:Ideographic:]-[:Block=CJK_Symbols_And_Punctuation:]]").freeze();
    private static final UnicodeSet RADICAL = new UnicodeSet("[:Radical:]").freeze();

    private static final class Positioning {
        private static final Positioning BASE = new Positioning(0,0,1,1);
        final double x1;
        final double y1;
        final double x2;
        final double y2;

        public Positioning(double x, double y, double x2, double y2) {
            this.x1 = x;
            this.y1 = y;
            this.x2 = x2;
            this.y2 = y2;
        }

        public Positioning times(Positioning other) {
            final double x = x1 + other.x1 - x1*other.x1;
            final double y = y1 + other.y1 - y1*other.y1;
            final double width = (x2-x1) * (other.x2-other.x1);
            final double height = (y2-y1) * (other.y2-other.y1);
            return new Positioning(
                    x,
                    y,
                    x + width,
                    y + height);
        }

        @Override
        public String toString() {
            return "{" + (int)(100*x1) 
                    + ", " + (int)(100*y1) 
                    + "; " + (int)(100*x2) 
                    + ", " + (int)(100*y2)
                    + "}";
        }
        @Override
        public boolean equals(Object obj) {
            Positioning other = (Positioning)obj;
            return x1 == other.x1 && x2 == other.x2 && y1 == other.y1 && y2 == other.y2;
        }
        @Override
        public int hashCode() {
            return Objects.hash(x1, x2, y1, y2);
        }
    }

    private static final class IdsData {
        private static final IdsData IDEO = new IdsData("IDEO", "", null);

        final String sample;
        final String sampleDecomp;
        final List<Positioning> part;

        public IdsData(String sample, String sampleDecomp, List<Positioning> part) {
            this.sample = sample;
            this.sampleDecomp = sampleDecomp;
            this.part = part;
        }

        @Override
        public String toString() {
            return part.toString();
        }
    }

    private static void add(int codepoint, String sample, String sampleDecomp, Positioning... part) {
        IDS_INFO.put(codepoint, new IdsData(sample, sampleDecomp, Arrays.asList(part)));
    }

    private static final UnicodeMap<IdsData> IDS_INFO = new UnicodeMap<>();
    static {

        IDS_INFO.putAll(IDEOGRAPHIC, IdsData.IDEO);
        IDS_INFO.putAll(RADICAL, IdsData.IDEO);
        IDS_INFO.putAll(STROKES, IdsData.IDEO);
        IDS_INFO.putAll(EXT, IdsData.IDEO);
        IDS_INFO.putAll(EXT_E, IdsData.IDEO);

        //  ⿰  U+2FF0 IDEOGRAPHIC DESCRIPTION CHARACTER LEFT TO RIGHT
        add(0x2ff0, "㐖", "⿰吉乚",
                new Positioning(0,0,0.5,1), 
                new Positioning(0.5,0,1,1));
        //  ⿱  U+2FF1 IDEOGRAPHIC DESCRIPTION CHARACTER ABOVE TO BELOW
        add(0x2ff1, "㐀", "⿱卝一",
                new Positioning(0,0,1,0.5), 
                new Positioning(0,0.5,1,1));
        //  ⿲  U+2FF2 IDEOGRAPHIC DESCRIPTION CHARACTER LEFT TO MIDDLE AND RIGHT
        add(0x2ff2, "㣠", "⿲彳丨冬",
                new Positioning(0,0,0.3,1), 
                new Positioning(0.3,0,0.7,1), 
                new Positioning(0.7,0,1,1));
        //  ⿳  U+2FF3 IDEOGRAPHIC DESCRIPTION CHARACTER ABOVE TO MIDDLE AND BELOW
        add(0x2ff3, "㞿", "⿳山土乂", 
                new Positioning(0,0,1,0.3), 
                new Positioning(0,0.3,1,0.7), 
                new Positioning(0,0.7,1,1));
        //  ⿴  U+2FF4 IDEOGRAPHIC DESCRIPTION CHARACTER FULL SURROUND
        add(0x2ff4, "囝", "⿴囗子",
                new Positioning(0,0,1,1), 
                new Positioning(0.25,0.25,0.75,0.75));
        //  ⿵  U+2FF5 IDEOGRAPHIC DESCRIPTION CHARACTER SURROUND FROM ABOVE
        add(0x2ff5, "悶", "⿵門心",
                new Positioning(0,0,1,1), 
                new Positioning(0.3,0.3,0.7,1));
        //  ⿶  U+2FF6 IDEOGRAPHIC DESCRIPTION CHARACTER SURROUND FROM BELOW
        add(0x2ff6, "𠙶", "⿶凵了",
                new Positioning(0,0,1,1), 
                new Positioning(0.3,0,0.7,0.7));
        //  ⿷  U+2FF7 IDEOGRAPHIC DESCRIPTION CHARACTER SURROUND FROM LEFT
        add(0x2ff7, "𠤭", "⿷匚人",
                new Positioning(0,0,1,1), 
                new Positioning(0.3,0.3,1,0.7));
        //  ⿸  U+2FF8 IDEOGRAPHIC DESCRIPTION CHARACTER SURROUND FROM UPPER LEFT
        add(0x2ff8, "産", "⿸产生",
                new Positioning(0,0,0.9,0.9), 
                new Positioning(0.5,0.5,1,1));
        //  ⿹  U+2FF9 IDEOGRAPHIC DESCRIPTION CHARACTER SURROUND FROM UPPER RIGHT
        add(0x2ff9, "甸", "⿹勹田",
                new Positioning(0,0,1,1), 
                new Positioning(0,0.5,0.5,1));
        //  ⿺  U+2FFA IDEOGRAPHIC DESCRIPTION CHARACTER SURROUND FROM LOWER LEFT
        add(0x2ffa, "䆪", "⿺光空",
                new Positioning(0,0.2,0.8,1), 
                new Positioning(0.5,0,1,0.5));
        //  ⿻  U+2FFB IDEOGRAPHIC DESCRIPTION CHARACTER OVERLAID
        add(0x2ffb, "𠆥", "⿻人丿",
                new Positioning(0,0,0.9,0.9), 
                new Positioning(0.1,0.1,1,1));
    }

    //㿂 ⿸疒⿰⿱山王攵
    private static final String FLIPPED = "或止虎";
    private static final String MIRRORED = "𦣞正止臣";

    private static final class CpPart {
        final int codepoint;
        final Positioning part;
        final float color;

        @Override
        public boolean equals(Object obj) {
            CpPart other = (CpPart) obj;
            return codepoint == other.codepoint && part == other.part;
        }
        @Override
        public int hashCode() {
            return Objects.hash(codepoint, part);
        }

        public CpPart(int codepoint, Positioning part, float color) {
            this.codepoint = codepoint;
            this.part = part;
            this.color = color;
        }

        public String getColor() {
            float c = color * 0x3;
            final int dr, dg, db;
            if (c <= 1) {
                dr = Math.round(c*0xFF);
                dg = Math.round((1-c)*0xFF);
                db = 0;
            } else if (c <= 2) {
                dr = 0;
                dg = Math.round((c-1)*0xFF);
                db = Math.round((2-c)*0xFF);
            } else {
                dg = 0;
                db = Math.round((c-2)*0xFF);
                dr = Math.round((3-c)*0xFF);
            }
            long v = 0x1000000L | (dr<<16) | (dg <<8) | db;
            String rgb = Long.toHexString(v);
            return "#" + rgb.substring(rgb.length()-6);
        }

        public static List<CpPart> parse(String sourceChar, String source, Output<Boolean> questionable) {
            if (DEBUG) System.out.println(source);
            ArrayList<CpPart> result = new ArrayList<CpPart>();
            final int[] codePoints = CharSequences.codePoints(source);
            int reached;
            try {
                reached = parse(sourceChar, 1, Positioning.BASE, 0, codePoints, result, questionable);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new IllegalArgumentException("Error: expected more characters");
            }
            if (reached != codePoints.length) {
                throw new IllegalArgumentException("Error: expected only " + reached + " character" + (reached == 1 ? "" : "s"));
            }
            return result;
        }

        private static int parse(String sourceChar, int depth, Positioning position, int pos, int[] codePoints, ArrayList<CpPart> result, Output<Boolean> questionable) {
            questionable.value = false;
            final int lead = codePoints[pos++];
            final IdsData ids = IDS_INFO.get(lead);
            if (ids == null || ids == IdsData.IDEO) {
                String radical = unicodeToRadical.get(lead);
                if (radical != null) {
                    result.add(new CpPart(lead, position, pos/(float)codePoints.length));
                    return pos;
                }
                throw new IllegalArgumentException("Error: no IDS/Radical/Stroke at " + (pos-1));
            }
            if (DEBUG) System.out.println(Utility.repeat("\t",depth) + UTF16.valueOf(lead) + " => " + ids.part);
            for (final Positioning subpart : ids.part) {
                final Positioning combo = position.times(subpart);
                int codePoint = codePoints[pos++];
                if (DEBUG) System.out.println(Utility.repeat("\t",depth) + UTF16.valueOf(codePoint) + " & " + combo);

                IdsData partData = IDS_INFO.get(codePoint);
                while (partData == null && codePoint == '？') {
                    questionable.value = true;
                    codePoint = codePoints[pos++];
                    partData = IDS_INFO.get(codePoint);
                }
                if (partData == null) {
                    if (codePoint == '{') {
                        int first = codePoints[pos++] - 0x30;
                        if (first < 0 || first > 9) {
                            throw new IllegalArgumentException("Error: unexpected character " + Utility.hex(codePoint) + " at " + (pos-1));
                        }
                        int second = codePoints[pos++] - 0x30;
                        if (second < 0 || second > 9) {
                            throw new IllegalArgumentException("Error: unexpected character " + Utility.hex(codePoint) + " at " + (pos-1));
                        }
                        int third = codePoints[pos++];
                        if (third != '}') {
                            throw new IllegalArgumentException("Error: unexpected character " + Utility.hex(codePoint) + " at " + (pos-1));
                        }
                        codePoint = 0xE000 + first * 10 + second;
                        Special.addToSpecial(codePoint, sourceChar);
                        partData = IDS_INFO.get(codePoint);
                        //                    } else if (codePoint == '？') {
                        //                        codePoint = 0xE07F;
                        //                        partData = IDS_INFO.get(codePoint);
                    } else if (codePoint == '↔') {
                        codePoint = codePoints[pos++];
                        int mirrored = MIRRORED.indexOf(codePoint);
                        if (mirrored >= 0) {
                            partData = IDS_INFO.get(0xE040 + mirrored);
                            Special.addSpecial(0xE040 + mirrored, sourceChar, "mirrored " + UTF16.valueOf(codePoint));
                        }
                    } else if (codePoint == '↷') {
                        codePoint = codePoints[pos++];
                        int mirrored = FLIPPED.indexOf(codePoint);
                        if (mirrored >= 0) {
                            partData = IDS_INFO.get(0xE050 + mirrored);
                            Special.addSpecial(0xE050 + mirrored, sourceChar, "rotated " + UTF16.valueOf(codePoint));
                        }
                    }
                }
                if (partData == null) {
                    throw new IllegalArgumentException("Error: unexpected character " + Utility.hex(codePoint) + " at " + (pos-1));
                } else if (partData == IdsData.IDEO) {
                    result.add(new CpPart(codePoint, combo, pos/(float)codePoints.length));
                } else {
                    pos = parse(sourceChar, depth + 1, combo, pos-1, codePoints, result, questionable);
                }
            }
            return pos;
        }

        @Override
        public String toString() {
            return UTF16.valueOf(codepoint) + part;
        }

        public String svgRect(String color, boolean showRect) {
            return "<g>"
                    + (showRect ?
                            "<rect x='" + (LARGE*part.x1)
                            + "' y='" + (LARGE*part.y1)
                            + "' width='" + (LARGE*(part.x2-part.x1))
                            + "' height='" + (LARGE*(part.y2-part.y1))
                            + "' style='fill:none; stroke:black;stroke-width:1'/>\n" : "")
                            + "<text x='" + (LARGE*part.x1)/(part.x2-part.x1)
                            + "' y='" + (LARGE*part.y2)/(part.y2-part.y1)
                            + "' fill='" + color + "'"
                            + " font-size='" + LARGE + "'"
                            + " dominant-baseline='ideographic'"
                            + " transform='scale(" + (part.x2-part.x1) + " " + (part.y2-part.y1) + ")'" 
                            + ">" + UTF16.valueOf(codepoint) + "</text></g>\n";
        }
    }

    private static final int LARGE = 72;

    private static final UnicodeMap<CharacterIds> IDS_RECURSIVE = new UnicodeMap<>();
    
    private static class Special {
        static final Map<Integer, Special> specials = new TreeMap<>();

        final String description;
        UnicodeSet samples = new UnicodeSet();
        
        public Special(String description) {
            this.description = description;
        }
        
        public static void addSpecial(int codepoint, String sourceChar, String description) {
            codepoint -= 0xE000;
            Special special = specials.get(codepoint);
            if (specials.get(codepoint) == null) {
                special = new Special(description);
                specials.put(codepoint, special);
            }
            special.samples.add(sourceChar);
        }

        public static void addSpecial(String line) {
            int codepoint = (line.charAt(1)-0x30)*10+(line.charAt(2)-0x30);
            if (specials.get(codepoint) != null) {
                throw new IllegalArgumentException("special collision");
            }
            String description = line.substring(4).trim();
            Special special = new Special(description);
            specials.put(codepoint, special);
        }

        public static void addToSpecial(int codePoint, String sourceChar) {
            Special special = specials.get(codePoint-0xE000);
            special.samples.add(sourceChar);
        }
        
        static void listSpecials() throws IOException {
            try (PrintWriter out = BagFormatter.openUTF8Writer(Settings.GEN_DIR + "ids/",
                    "specials.html");
                    ) {
                showHeader(out);
                out.println("<tr><td>" + "key"
                        + "</td><td nowrap>" + "description"
                        + "</td><td>" + "samples"
                        + "</td></tr>");
                for (Entry<Integer, Special> entry : specials.entrySet()) {
                    out.println("<tr><td>" + entry.getKey()
                            + "</td><td nowrap>" + entry.getValue().description
                            + "</td><td>" + entry.getValue().samples.toPattern(false)
                            + "</td></tr>");

                }
                showFooter(out);
                //System.out.println("items:\t" + count);
            }
        }
        @Override
        public String toString() {
            return description + "\t" + samples.toPattern(false);
        }
    }

    public static void main(String[] args) throws IOException {
        load();
        showRadicalCompare();
        showMainList();
        showParseErrors(failures, "parseFailures.html");
        showParseErrors(missing, "missing.html");
        showRadicalMissing();
        Special.listSpecials();


//        for (int i : CharSequences.codePoints(FLIPPED)) {
//            System.out.println("FLIPPED:\t" + UTF16.valueOf(i) + "\t" + IDS_DATA.get(i) + "\t" + radicalStroke.get(i));
//        }
//        for (int i : CharSequences.codePoints(MIRRORED)) {
//            System.out.println("MIRRORED:\t" + UTF16.valueOf(i) + "\t" + IDS_DATA.get(i) + "\t" + radicalStroke.get(i));
//        }
//        for (Entry<String, CharacterIds> entry : IDS_DATA.entrySet()) {
//            String source = entry.getKey();
//            final String idsSource = entry.getValue().idsSource;
//            if (idsSource.contains("↔") || idsSource.contains("↷")) {
//                System.out.println(source + "\t" + idsSource + "\t" + radicalStroke.get(source));
//            }
//        }
    }

    private static void showRadicalMissing() throws IOException {
        // Relation<List<CpPart>, String> invert = Relation.of(new HashMap(), HashSet.class);
        try (PrintWriter out = BagFormatter.openUTF8Writer(Settings.GEN_DIR + "ids/", "radicalMissing.html");
                ) {
            showHeader(out);     
            int count = 0;
            out.println("<tr><th>Count</th><th>Source</th><th>IDS</th><th>Source Radical + RS(.0)</th><th>Source RS</th></tr>");
            main:
                for (Entry<String, CharacterIds> entry : IDS_RECURSIVE.entrySet()) {
                    //            invert.put(entry.getValue().parts, entry.getKey());
                    final String source = entry.getKey();
                    String rs = radicalStroke.get(source).get(0);
                    String idsSource = entry.getValue().idsSource;
                    Set<String> rads = radToUnicode.get(DOT_SPLITTER.splitToList(rs).get(0));
                    for (String rad : rads) {
                        if (idsSource.contains(rad)) {
                            continue main;
                        }
                        CharacterIds decomp = IDS_RECURSIVE.get(rad);
                        if (decomp != null) {
                            String rad2 = decomp.idsSource;
                            if (idsSource.contains(rad2)) {
                                continue main;
                            }
                        }
                    }
                    //System.out.println("Rec. Decomp doesn't contain radical: " + entry.getKey() + "\t" + rs + "\t" + rads + "\t" + idsSource);
                    out.println("<tr>"
                            + "<th style='text-align:right'>" + ++count + "</th>"
                            + "<td style='font-size: " + LARGE/2 + "px'>" + source + "</td>"
                            + "<td >" + rs + "</td>"
                            + "<td style='font-size: " + LARGE/2 + "px'>" + SPACE_JOINER.join(rads) + "</td>"
                            + "<td style='font-size: " + LARGE/2 + "px'>" + idsSource + "</td>"
                            + "</tr>");

                }
            //        for (Entry<List<CpPart>, Set<String>> entry : invert.keyValuesSet()) {
            //            if (entry.getValue().size() > 1) {
            //                System.out.println(entry);
            //            }
            //        }
            showFooter(out);
            System.out.println("items:\t" + count);
        }
    }

    private static String getDecomp(String idsSource) {
        StringBuffer b = new StringBuffer();
        for (int cp : CharSequences.codePoints(idsSource)) {
            CharacterIds data = IDS_RECURSIVE.get(cp);
            if (data == null) {
                data = IDS_DATA.get(cp);
            }
            if (data != null) {
                b.append(data.idsSource);
                continue;
            }

            b.appendCodePoint(cp);
        }
        return b.toString();
    }

    static void showParseErrors(M3<String, String, String> problems, String fileName) throws IOException {
        try (PrintWriter out = BagFormatter.openUTF8Writer(Settings.GEN_DIR + "ids/", fileName);
                ) {
            showHeader(out);
            out.println("<tr><th>Count</th><th>Reason for failure</th><th>Source</th><th>IDS</th><th>Source Radical + RS(.0)</th><th>Source RS</th></tr>");
            Output<Set<String>> radChar = new Output<>();
            //System.out.println("Failed to parse: ");
            int count = 0;
            for (Entry<String, Map<String, String>> entry : problems) {
                for (Entry<String, String> entry2 : entry.getValue().entrySet()) {
                    //System.out.println(entry.getKey() + "\t" + entry2.getKey() + "\t" + entry2.getValue());
                    final String key = entry2.getKey();
                    List<String> rad = getRS(key, radChar);
                    out.println("<tr>"
                            + "<th style='text-align:right'>" + ++count + "</th>"
                            + "<td>" + entry.getKey() + "</td>"
                            + "<td style='font-size: " + LARGE/2 + "px'>" + key + "</td>"
                            + "<td style='font-size: " + LARGE/2 + "px'>" + entry2.getValue() + "</td>"
                            + "<td style='font-size: " + LARGE/2 + "px'>" + SPACE_JOINER.join(radChar.value) + "</td>"
                            + "<td>" + rad + "</td>"
                            + "</tr>");
                }
            }
            showFooter(out);
            System.out.println("items:\t" + count);
        }
    }

    private static List<String> getRS(final String key, Output<Set<String>> radicalChar) {
        final List<String> rs = radicalStroke.get(key);
        if (rs == null) {
            radicalChar.value = Collections.EMPTY_SET;
            return Collections.EMPTY_LIST;
        }
        String rs1 = rs.iterator().next();
        List<String> parts = DOT_SPLITTER.splitToList(rs1);
        Set<String> rad = radToUnicode.get(parts.get(0));
        radicalChar.value = rad;
        return rs;
    }

    private static void showRadicalCompare() throws IOException {

        try (PrintWriter out = BagFormatter.openUTF8Writer(Settings.GEN_DIR + "ids/", "radicalCompare.html");
                ) {
            showHeader(out);
            Map<Double, R2<Set<String>,Set<String>>> sorted = new TreeMap<>();
            for (Entry<String, Set<String>> entry : radToUnicode.keyValuesSet()) {
                final String key = entry.getKey();
                final double clean = cleanRadical(key);
                sorted.put(clean, Row.of(entry.getValue(), rawRadToUnicode.get(key)));
            }
            out.println("<tr><th>Radical#</th>"
                    + "<th>CJKRadicals.txt:F1</th>"
                    + "<th>RSUnicode(.0)</th>"
                    + "<th>CJK_Rad Block?</th>"
                    + "<th>RSAdobe(.0)</th>"
                    + "<th>RSAdobe Details</th>"
                    + "<th>Name</th></tr>");
            int count = 0;
            Set<String> adobeItems = new TreeSet<>();

            for (Entry<Double, R2<Set<String>, Set<String>>> entry : sorted.entrySet()) {
                ++count;
                final Double key = entry.getKey();
                final R2<Set<String>, Set<String>> rad2 = entry.getValue();
                Set<String> rad = rad2.get0();
                final Set<String> raw = rad2.get1();
                double doubleRadical = key.doubleValue();
                int intRadical = (int)doubleRadical;
                final boolean alt = intRadical != doubleRadical;
                String samples = alt ? "" : getSamples((int)key.doubleValue());
                String key2 = intRadical + (alt ? "'" : "");
                final Set<String> cjkRad = alt ? Collections.EMPTY_SET : radToCjkRad.get(intRadical);
                UnicodeSet RSUnicode = USTROKE.get(intRadical, alt);
                M3<Integer, Integer, UnicodeSet> adobe = ASTROKE.get(intRadical);
                adobeItems.clear();
                if (!alt) {
                    for (Entry<Integer, Map<Integer, UnicodeSet>> entry2 : adobe) {
                        Map<Integer, UnicodeSet> remStrokesToSet = entry2.getValue();
                        UnicodeSet us = remStrokesToSet.get(0);
                        if (us != null) {
                            UnicodeSet temp = us;
                            if (RSUnicode != null) {
                                temp = new UnicodeSet(us).removeAll(RSUnicode);
                            }
                            temp.addAllTo(adobeItems);
                        }
                    }
                }
                out.println("<tr>"
                        + "<th style='text-align:right'>" + key2 + "</th>"
                        + "<td style='font-size: 24px'>" + raw.iterator().next() + "</td>"
                        + "<td style='font-size: 24px'>" + (RSUnicode == null ? "" : SPACE_JOINER.join(RSUnicode)) + "</td>"
                        + "<td style='font-size: 24px'>" + (cjkRad == null ? "" : SPACE_JOINER.join(cjkRad)) + "</td>"
                        + "<td style='font-size: 24px'>" + SPACE_JOINER.join(adobeItems) + "</td>"
                        + "<td style='font-size: 24px'>" + samples + "</td>"
                        + "<td>" + UCharacter.getName(raw.iterator().next(), ",") + "</td>"
                        + "</tr>");
            }
            showFooter(out);
            System.out.println("items:\t" + count);
        }
    }

    private static String getSamples(int radical) {
        M3<Integer, Integer, UnicodeSet> data = ASTROKE.get(radical);
        int count = 0;
        StringBuilder samples = new StringBuilder();
        for (final Entry<Integer, Map<Integer, UnicodeSet>> entry2 : data) {
            if (++count != 1) {
                samples.append("<br>");
            }
            final int rstrokes = entry2.getKey();
            int count2 = 0;
            for (final Entry<Integer, UnicodeSet> entry3 : entry2.getValue().entrySet()) {
                if (++count2 > 4) {
                    samples.append(",…");
                    break;
                } else if (count2 != 1) {
                    samples.append(", ");
                }
                final int remaining = entry3.getKey();
                samples.append(rstrokes).append(':').append(remaining).append("=[");
                int count3 = 4;
                for (final String s : entry3.getValue()) {
                    if (remaining != 0 && --count3 == 0) {
                        samples.append('…');
                        break;
                    }
                    samples.append(s);
                }
                samples.append("]");
            }
        }
        return samples.toString();
    }

    private static void showMainList() throws IOException {
        try (PrintWriter out = BagFormatter.openUTF8Writer(Settings.GEN_DIR + "ids/",
                "ids.html");
                ) {
            showHeader(out);
            TreeSet<String> sorted = new TreeSet<>(UNIHAN);
            //System.out.println("IDS_DATA.keySet(): " + IDS_DATA.keySet().size());
            IDS_DATA.keySet().addAllTo(sorted);
            int count = 0;
            out.println("<tr><th>Count</th><th>Source</th><th>IDS App. Pos.</th><th>IDS</th><th>App. Pos.</th></tr>");
            for (String s : sorted) {
                CharacterIds entry = IDS_DATA.get(s);
                show(++count, out, s.codePointAt(0), entry.idsSource, entry.parts);
            }
            showFooter(out);
            System.out.println("items:\t" + count);
        }
    }

    private static void showFooter(PrintWriter out) {
        out.println("</table></body></html>");
    }

    private static void showHeader(PrintWriter out) {
        out.println("<!DOCTYPE html><html><head><style>\n"
                + ".move { "
                + "font-size: " + LARGE
                + "px;"
                + "width: " + LARGE
                + "px; "
                + "height: " + LARGE
                + "px; "
                + "border: 1px solid black; "
                + "position: absolute;"
                + "}\n"
                + "</style></head><body><table border='1'>");
    }

    private static final class CharacterIds {
        final String idsSource;
        final List<CpPart> parts;
        final boolean questionable;
        public CharacterIds(String sourceChar, String idsSource) {
            String x = clean(idsSource);
            this.idsSource = x;
            Output<Boolean> questionable = new Output<>();
            this.parts = CpPart.parse(sourceChar, this.idsSource, questionable);
            this.questionable = questionable.value;
        }
        @Override
        public String toString() {
            return idsSource + "\t" + parts;
        }
    }

    public static class MyTreeMap<K,V> extends TreeMap<K,V> {
        public MyTreeMap() {
            super((Comparator<K>) UNIHAN);
        }
    }

    private static final UnicodeMap<CharacterIds> IDS_DATA = new UnicodeMap<>();
    //   private static final UnicodeMap<CharacterIds> IDS2_DATA = new UnicodeMap();
    private static final M3<String,String,String> failures = ChainedMap.of(new TreeMap<String,Object>(), new MyTreeMap<String,Object>(), String.class);
    private static final M3<String,String,String> missing = ChainedMap.of(new TreeMap<String,Object>(), new MyTreeMap<String,Object>(), String.class);

    private static void load() {
        String [] biggestCp = new String[50];
        CharacterIds [] biggest = new CharacterIds[50];
        Counter<Integer> counter = new Counter<>();

        for (String line : FileUtilities.in(Settings.OTHER_WORKSPACE_DIRECTORY + "DATA/ids/", "babelstoneIds.txt")) {
            // U+3FCD 㿍 ⿸疒解
            if (line.startsWith("#")) {
                line = line.substring(1).trim();
                if (line.startsWith("{")) {
                    Special.addSpecial(line);
                }
                continue;
            }
            String[] parts = line.split("\\s+");
            if (parts.length != 3) {
                continue;
            }
            final String ids = clean(parts[2]);
            final String source = parts[1];
            try {
                if (ids.equals(source)) {
                    if (unicodeToRadical.get(ids) == null) {
                        failures.put("Error: no IDS/Radical/Stroke at 0", source, source);
                    }
                    continue;
                }
                final CharacterIds chIds = new CharacterIds(source, ids);
                IDS_DATA.put(source, chIds);
                final int size = chIds.parts.size();
                counter.add(size, 1);
                if (biggestCp[size] == null) {
                    biggestCp[size] = source;
                    biggest[size] = chIds;
                    System.out.println(size + "\t" + source + "\t" + chIds);
                }
            } catch (Exception e) {
                failures.put(e.getMessage(), source, ids);
            }
        }
        IDS_DATA.freeze();
        for (String s : IDEOGRAPHIC) {
            if (!IDS_DATA.containsKey(s) && !unicodeToRadical.containsKey(s)) {
                missing.put("Missing", s, "∅");
            }
        }

        System.out.println(counter);
        for (Entry<String, CharacterIds> entry : IDS_DATA.entrySet()) {
            String decomp = getDecomp(entry.getValue().idsSource);
            //System.out.println(entry.getKey() + "\t" + decomp);
            try {
                IDS_RECURSIVE.put(entry.getKey(), new CharacterIds(entry.getKey(), decomp));
            } catch (Exception e) {
                //failures.put(e.getMessage(), entry.getKey(), decomp);
            }
        }
        IDS_RECURSIVE.freeze();


        //        UnicodeSet missing = new UnicodeSet(IDEOGRAPHIC)
        //        .removeAll(identicals)
        //        .removeAll(IDS_DATA.keySet())
        //        .removeAll(cjkRadical.keySet());
        //
        //        System.out.println("Identicals:\t" + identicals.size());
        //        System.out.println("Other missing:\t" + missing.size());
        //
        //        showMissing("identicals", identicals);
        //        showMissing("other-missing", missing);

        //        System.out.println("Failed to parse: ");
        //        for (Entry<String, Map<String, String>> entry : failures) {
        //            for (Entry<String, String> entry2 : entry.getValue().entrySet()) {
        //                System.out.println(entry.getKey() + "\t" + entry2.getKey() + "\t" + entry2.getValue());
        //            }
        //        }
        //        for (Entry<String, String> entry : cjkRadical.entrySet()) {
        //            String key = entry.getKey();
        //            String totalStrokeCount = totalStrokes.get(key);
        //            double value = cleanRadical(entry.getValue());
        //            System.out.println(entry.getKey() + "\t" + value + "\t" + UCharacter.getName(entry.getKey(), "+") + "\t" + totalStrokeCount);
        //        }
        //        for (String s : STROKES) {
        //            String totalStrokeCount = totalStrokes.get(s);
        //            System.out.println(s + "\t" + "?" + "\t" + UCharacter.getName(s, "+") + "\t" + totalStrokeCount);
        //        }
    }

    //    private static void showMissing(String title, UnicodeSet missing) {
    //        Set<String> missingSorted = missing.addAllTo(new TreeSet<>(UNIHAN));
    //        for (String s : missingSorted) {
    //            final Set<String> rs = radicalStroke.get(s);
    //            if (rs == null) {
    //                System.out.println(title + "\t" + s + "\t" + "null" + "\t" + "null" + "\t" + totalStrokes.get(s));
    //                continue;
    //            }
    //            //            if (rs.contains("|")) {
    //            //                System.out.println(s + "\t" + rs);
    //            //            }
    //            for (String rsItem : rs) {
    //                List<String> rsAlts = VBAR_SPLITTER.splitToList(rsItem);
    //                List<String> rsArray = DOT_SPLITTER.splitToList(rsAlts.get(0));
    //                System.out.println(title + "\t" + s + "\t" + cleanRadical(rsArray.get(0)) + "\t" + rsArray.get(1) + "\t" + totalStrokes.get(s));
    //            }
    //        }
    //    }

    private static double cleanRadical(String string) {
        double increment = 0;
        if (string.endsWith("'")) {
            increment = 0.5;
            string = string.substring(0,string.length()-1);
        }
        return Double.parseDouble(string) + increment;
    }

    private static String clean(String idsSource) {
        int start = 0; 
        int end = idsSource.length();
        if (idsSource.startsWith("^")) {
            start = 1;
        }
        if (idsSource.endsWith("$")) {
            end = end-1;
        }
        String x = idsSource.substring(start,end);
        return x;
    }

    //    private static void show(PrintWriter out, int codepoint, String source) {
    //        show(out, codepoint,  source, CpPart.parse(source));
    //    }

    private static void show(int count, PrintWriter out, int codepoint, String source, List<CpPart> breakdown) {
        if (DEBUG) System.out.println(UTF16.valueOf(codepoint) + "\t" + source);
        out.println("<tr>"
                + "<td style='text-align:right'>" + count + "</td>\n"
                + "<td style='font-size:"
                + LARGE
                + "px'>" + UTF16.valueOf(codepoint) + "</td>\n"
                + "<td height='"
                + LARGE
                + "px' width='"
                + LARGE
                + "px' style='position:relative'>\n"
                + show(breakdown) 
                + "</td>\n"
                + "<td style='font-size:24px'>" + source + "</td>\n"
                + "<td>" + CollectionUtilities.join(breakdown, "<br>") + "</td>"
                + "</tr>"
                );
    }

    private static String show(List<CpPart> data) {
        StringBuilder b = new StringBuilder("<svg width='"
                + LARGE
                + "' height='"
                + LARGE
                + "'>");
        int count = 0;
        for (CpPart part : data) {
            b.append(part.svgRect(part.getColor(), false));
            //      b.append("<div class='move' "
            //          + "style='" + part.html() + "'>" 
            //          + codePoint + ":" + ++count + "</div>\n");
            ++count;
        }
        return b.append("</svg>").toString();
    }
    // Confusable
    /*
    ⼖  U+2F16 KANGXI RADICAL HIDING ENCLOSURE
    匚  U+531A CJK UNIFIED IDEOGRAPH-531A
    匸  U+5338 CJK UNIFIED IDEOGRAPH-5338

     ⼢  U+2F22 KANGXI RADICAL GO SLOWLY
        夂     U+5902 CJK UNIFIED IDEOGRAPH-5902
        夊  U+590A CJK UNIFIED IDEOGRAPH-590A
         ⽇  U+2F47 KANGXI RADICAL SUN
         日  U+65E5 CJK UNIFIED IDEOGRAPH-65E5
         曰  U+66F0 CJK UNIFIED IDEOGRAPH-66F0

          ⽒     U+2F52 KANGXI RADICAL CLAN
 氏  U+6C0F CJK UNIFIED IDEOGRAPH-6C0F
 氐  U+6C10 CJK UNIFIED IDEOGRAPH-6C10

  辶     U+8FB6 CJK UNIFIED IDEOGRAPH-8FB6
 辶  U+FA66 CJK COMPATIBILITY IDEOGRAPH-FA66

  ⿈     U+2FC8 KANGXI RADICAL YELLOW
 黃  U+9EC3 CJK UNIFIED IDEOGRAPH-9EC3
 黄  U+9EC4 CJK UNIFIED IDEOGRAPH-9EC4
     */
}
