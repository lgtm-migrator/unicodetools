package org.unicode.jsp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.IdnaLabelTester;
import org.unicode.cldr.util.Predicate;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.test.util.BNF;
import com.ibm.icu.dev.test.util.PrettyPrinter;
import com.ibm.icu.dev.test.util.Quoter;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.CanonicalIterator;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;

public class UnicodeUtilities {
  public static final Charset UTF8 = Charset.forName("utf-8");

  private static final List<String> REGEX_PROPS = Arrays.asList(new String[] {"xdigit", "alnum", "blank", "graph", "print", "word"});

  private static final List<String> UNICODE_PROPS = Arrays.asList(new String[] {
          "Numeric_Value", "Bidi_Mirroring_Glyph", "Case_Folding",
          "Decomposition_Mapping", "FC_NFKC_Closure",
          "Lowercase_Mapping", "Special_Case_Condition",
          "Simple_Case_Folding", "Simple_Lowercase_Mapping",
          "Simple_Titlecase_Mapping", "Simple_Uppercase_Mapping",
          "Titlecase_Mapping", "Uppercase_Mapping", "ISO_Comment",
          "Name", "Unicode_1_Name", "Unicode_Radical_Stroke", "Age",
          "Block", "Script", "Bidi_Class", "Canonical_Combining_Class",
          "Decomposition_Type", "East_Asian_Width", "General_Category",
          "Grapheme_Cluster_Break", "Hangul_Syllable_Type",
          "Joining_Group", "Joining_Type", "Line_Break",
          "NFC_Quick_Check", "NFD_Quick_Check", "NFKC_Quick_Check",
          "NFKD_Quick_Check", "Numeric_Type", "Sentence_Break",
          "Word_Break", "ASCII_Hex_Digit", "Alphabetic", "Bidi_Control",
          "Bidi_Mirrored", "Composition_Exclusion",
          "Full_Composition_Exclusion", "Dash", "Deprecated",
          "Default_Ignorable_Code_Point", "Diacritic", "Extender",
          "Grapheme_Base", "Grapheme_Extend", "Grapheme_Link",
          "Hex_Digit", "Hyphen", "ID_Continue", "Ideographic",
          "ID_Start", "IDS_Binary_Operator", "IDS_Trinary_Operator",
          "Join_Control", "Logical_Order_Exception", "Lowercase", "Math",
          "Noncharacter_Code_Point", "Other_Alphabetic",
          "Other_Default_Ignorable_Code_Point", "Other_Grapheme_Extend",
          "Other_ID_Continue", "Other_ID_Start", "Other_Lowercase",
          "Other_Math", "Other_Uppercase", "Pattern_Syntax",
          "Pattern_White_Space", "Quotation_Mark", "Radical",
          "Soft_Dotted", "STerm", "Terminal_Punctuation",
          "Unified_Ideograph", "Uppercase", "Variation_Selector",
          "White_Space", "XID_Continue", "XID_Start", "Expands_On_NFC",
          "Expands_On_NFD", "Expands_On_NFKC", "Expands_On_NFKD",
          "toNFC", "toNFD", "toNFKC", "toNFKD"});
  /*
   * Arrays.asList(new String[] {
        "Numeric_Value", "Bidi_Mirroring_Glyph", "Case_Folding",
        "Decomposition_Mapping", "FC_NFKC_Closure", "Lowercase_Mapping",
        "Special_Case_Condition", "Simple_Case_Folding",
        "Simple_Lowercase_Mapping", "Simple_Titlecase_Mapping",
        "Simple_Uppercase_Mapping", "Titlecase_Mapping", "Uppercase_Mapping",
        "ISO_Comment", "Name", "Unicode_1_Name", "Unicode_Radical_Stroke",
        "Age", "Block", "Script", "Bidi_Class", "Canonical_Combining_Class",
        "Decomposition_Type", "East_Asian_Width", "General_Category",
        "Grapheme_Cluster_Break", "Hangul_Syllable_Type", "Joining_Group",
        "Joining_Type", "Line_Break", "NFC_Quick_Check", "NFD_Quick_Check",
        "NFKC_Quick_Check", "NFKD_Quick_Check", "Numeric_Type",
        "Sentence_Break", "Word_Break", "ASCII_Hex_Digit", "Alphabetic",
        "Bidi_Control", "Bidi_Mirrored", "Composition_Exclusion",
        "Full_Composition_Exclusion", "Dash", "Deprecated",
        "Default_Ignorable_Code_Point", "Diacritic", "Extender",
        "Grapheme_Base", "Grapheme_Extend", "Grapheme_Link", "Hex_Digit",
        "Hyphen", "ID_Continue", "Ideographic", "ID_Start",
        "IDS_Binary_Operator", "IDS_Trinary_Operator", "Join_Control",
        "Logical_Order_Exception", "Lowercase", "Math",
        "Noncharacter_Code_Point", "Other_Alphabetic",
        "Other_Default_Ignorable_Code_Point", "Other_Grapheme_Extend",
        "Other_ID_Continue", "Other_ID_Start", "Other_Lowercase", "Other_Math",
        "Other_Uppercase", "Pattern_Syntax", "Pattern_White_Space",
        "Quotation_Mark", "Radical", "Soft_Dotted", "STerm",
        "Terminal_Punctuation", "Unified_Ideograph", "Uppercase",
        "Variation_Selector", "White_Space", "XID_Continue", "XID_Start",
        "Expands_On_NFC", "Expands_On_NFD", "Expands_On_NFKC",
        "Expands_On_NFKD", "toNFC", "toNFD", "toNFKC", "toNFKD", })
   */

  private static Subheader subheader = null;

  static Transliterator toHTML;
  static String HTML_RULES_CONTROLS;
  static {

    String BASE_RULES = "'<' > '&lt;' ;" + "'<' < '&'[lL][Tt]';' ;"
    + "'&' > '&amp;' ;" + "'&' < '&'[aA][mM][pP]';' ;"
    + "'>' < '&'[gG][tT]';' ;" + "'\"' < '&'[qQ][uU][oO][tT]';' ; "
    + "'' < '&'[aA][pP][oO][sS]';' ; ";

    String CONTENT_RULES = "'>' > '&gt;' ;";

    String HTML_RULES = BASE_RULES + CONTENT_RULES + "'\"' > '&quot;' ; ";

    HTML_RULES_CONTROLS = HTML_RULES
    + "[[:di:]-[:cc:]-[:cs:]-[\\u200E\\u200F]] > ; " // remove, should ignore in rendering (but may not be in browser)
    + "[[:nchar:][:cn:][:cs:][:co:][:cc:]-[:whitespace:]-[\\u200E\\u200F]] > \\uFFFD ; "; // should be missing glyph (but may not be in browser)
    //     + "([[:C:][:Z:][:whitespace:][:Default_Ignorable_Code_Point:]-[\\u0020]]) > &hex/xml($1) ; "; // [\\u0080-\\U0010FFFF]

    toHTML = Transliterator.createFromRules("any-xml", HTML_RULES_CONTROLS,
            Transliterator.FORWARD);
  }

  public static String toHTML(String input) {
    return toHTML.transliterate(input);
  }

  static Transliterator UNICODE = Transliterator.getInstance("hex-any");

  static UnicodeSet isCaseFolded = new UnicodeSet();

  static UnicodeSet isLowercase = new UnicodeSet();

  static UnicodeSet isUppercase = new UnicodeSet();

  static UnicodeSet isTitlecase = new UnicodeSet();

  static UnicodeSet isCased = new UnicodeSet();

  public enum IdnaType { valid, ignored, mapped, disallowed};

  static final IdnaType OUTPUT = IdnaType.valid, IGNORED = IdnaType.ignored, 
  REMAPPED = IdnaType.mapped, DISALLOWED = IdnaType.disallowed;
  
  static final int IDNA_TYPE_LIMIT = 4;

  static final Map<IdnaType,UnicodeSet> idnaTypeSet = new TreeMap<IdnaType,UnicodeSet>();
  static {
    for (IdnaType i : IdnaType.values()) {
      idnaTypeSet.put(i, new UnicodeSet());
    }
  }
  
  static UnicodeSet ignoreInDiff = UnicodeSetUtilities.parseUnicodeSet("[[:Cc:][:Cn:][:Co:][:Cs:]]").freeze();

  static UnicodeMap<String> getIdnaDifferences(UnicodeSet remapped, UnicodeSet overallAllowed) {
    UnicodeMap<String> result = new UnicodeMap<String>();
    UnicodeSet valid2008 = getIdna2008Valid();

    for (int i = 0x80; i <= 0x10FFFF; ++i) {
      if ((i & 0xFFF) == 0) System.out.println(Utility.hex(i));
      if (i == 0x20000) {
        System.out.println("debug");
      }
      if (ignoreInDiff.contains(i)) continue;
      boolean isNew = UCharacter.getAge(i).compareTo(VersionInfo.UNICODE_3_2) > 0;
      String age = isNew ? "v4.0-5.2" : "v3.2";
      IdnaType idna2003 = Idna2003.getIDNA2003Type2(UTF16.valueOf(i));
      IdnaType tr46 = Uts46.getUts46Type(i, overallAllowed);
      if (isNew) {// skip
      } else if (tr46 == REMAPPED || idna2003 == REMAPPED) {
        remapped.add(i);
      }
      //TestStatus testResult = valid2008.contains(i);
      IdnaType idna2008 = valid2008.contains(i) ? OUTPUT : DISALLOWED;
      String iClass = age
      + "\t" + getShortName(idna2003) 
      + "\t" + getShortName(tr46)
      + "\t" + getShortName(idna2008)
      ;
      result.put(i, iClass);
    }
    return result.freeze();
  }

  public static UnicodeSet getIdna2008Valid() {
    IdnaLabelTester tester = getIdna2008Tester();
    UnicodeSet valid2008 = UnicodeSetUtilities.parseUnicodeSet(tester.getVariable("$Valid"));
    return valid2008;
  }

  static String getShortName(IdnaType tr46) {
    // TODO Auto-generated method stub
    return UCharacter.toTitleCase(
            tr46==OUTPUT ? "Valid" 
                    : tr46==IGNORED || tr46==REMAPPED ? "Mapped/Ignored" 
            : tr46.toString()
                        , null);
  }
  static {
    for (int cp = 0; cp <= 0x10FFFF; ++cp) {

      int cat = UCharacter.getType(cp);
      if (cat == UCharacter.UNASSIGNED || cat == UCharacter.PRIVATE_USE  || cat == UCharacter.SURROGATE) {
        idnaTypeSet.get(DISALLOWED).add(cp); // faster
        isCaseFolded.add(cp);
        isLowercase.add(cp);
        isTitlecase.add(cp);
        isUppercase.add(cp);
        continue;
      }

      IdnaType idnaType = Idna2003.getIDNA2003Type(cp);
      idnaTypeSet.get(idnaType).add(cp);

      String s = UTF16.valueOf(cp);
      if (UCharacter.foldCase(s, true).equals(s)) {
        isCaseFolded.add(cp);
      }
      if (UCharacter.toLowerCase(ULocale.ROOT, s).equals(s)) {
        isLowercase.add(cp);
      }
      if (UCharacter.toUpperCase(ULocale.ROOT, s).equals(s)) {
        isUppercase.add(cp);
      }
      if (UCharacter.toTitleCase(ULocale.ROOT, s, null).equals(s)) {
        isTitlecase.add(cp);
      }
    }
    // isCased if isLowercase=false OR isUppercase=false OR isTitlecase=false
    // or := ! (isLowercase && isUppercase && isTitlecase)
    isCased = new UnicodeSet(isLowercase).retainAll(isUppercase).retainAll(
            isTitlecase).complement();
  }



  static final int 
  TO_NFC = UProperty.STRING_LIMIT,
  TO_NFD = UProperty.STRING_LIMIT + 1,
  TO_NFKC = UProperty.STRING_LIMIT + 2,
  TO_NFKD = UProperty.STRING_LIMIT + 3,
  TO_CASEFOLD  = UProperty.STRING_LIMIT + 4,
  TO_LOWERCASE  = UProperty.STRING_LIMIT + 5,
  TO_UPPERCASE  = UProperty.STRING_LIMIT + 6,
  TO_TITLECASE  = UProperty.STRING_LIMIT + 7,
  SUBHEAD = TO_TITLECASE + 1,
  ARCHAIC = SUBHEAD + 1,
  XSTRING_LIMIT = ARCHAIC + 1; 

  static List<String> XPROPERTY_NAMES = Arrays.asList(new String[]{"tonfc", "tonfd", "tonfkc", "tonfkd", "tocasefold", "tolowercase", "touppercase", "totitlecase", "subhead", "archaic"});
  static final UnicodeSet MARK = (UnicodeSet) UnicodeSetUtilities.parseUnicodeSet("[:M:]").freeze();

  static String getXStringPropertyValue(int propertyEnum, int codepoint, int nameChoice, Normalizer.Mode compat) {
    if (compat == null || Normalizer.isNormalized(codepoint, compat, 0)) {
      return getXStringPropertyValue(propertyEnum, codepoint, nameChoice);
    }
    String s = UnicodeSetUtilities.MyNormalize(codepoint, compat);
    int cp;
    String lastPart = null;
    for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
      cp = UTF16.charAt(s, i);
      String part = getXStringPropertyValue(propertyEnum, cp, nameChoice);
      if (lastPart == null) {
        lastPart = part;
      } else if (!lastPart.equals(part)) {
        if (propertyEnum == UProperty.SCRIPT && MARK.contains(cp)) {
          continue;
        }
        return "Mixed";
      }
    }
    return lastPart;
  }
  static String getXStringPropertyValue(int propertyEnum, int codepoint, int nameChoice) {

    switch (propertyEnum) {
    case TO_NFC: return UnicodeSetUtilities.MyNormalize(codepoint, Normalizer.NFC);
    case TO_NFD: return UnicodeSetUtilities.MyNormalize(codepoint, Normalizer.NFD);
    case TO_NFKC: return UnicodeSetUtilities.MyNormalize(codepoint, Normalizer.NFKC);
    case TO_NFKD: return UnicodeSetUtilities.MyNormalize(codepoint, Normalizer.NFKD);
    case TO_CASEFOLD: return UCharacter.foldCase(UTF16.valueOf(codepoint), true);
    case TO_LOWERCASE: return UCharacter.toLowerCase(ULocale.ROOT, UTF16.valueOf(codepoint));
    case TO_UPPERCASE: return UCharacter.toUpperCase(ULocale.ROOT, UTF16.valueOf(codepoint));
    case TO_TITLECASE: return UCharacter.toTitleCase(ULocale.ROOT, UTF16.valueOf(codepoint), null);
    case SUBHEAD: return getSubheader().getSubheader(codepoint);
    case ARCHAIC: return ScriptCategoriesCopy.ARCHAIC_31.contains(codepoint) ? "uax31" 
            : ScriptCategoriesCopy.ARCHAIC_39.contains(codepoint) ? "utr39" 
                    : ScriptCategoriesCopy.ARCHAIC_HEURISTIC.contains(codepoint) ? "heuristic" 
                            : ScriptCategoriesCopy.ARCHAIC_ADDITIONS.contains(codepoint) ? "addition" 
                                    : "no";
    }
    return UCharacter.getStringPropertyValue(propertyEnum, codepoint, nameChoice);
  }

  static int getXPropertyEnum(String propertyAlias) {
    int extra = XPROPERTY_NAMES.indexOf(propertyAlias
            .toLowerCase(Locale.ENGLISH));
    if (extra != -1) {
      return UProperty.STRING_LIMIT + extra;
    }
    return UCharacter.getPropertyEnum(propertyAlias);
  }

  protected static boolean getIdnaProperty(String propertyValue,
          UnicodeSet result) {
    IdnaType i = IdnaType.valueOf(propertyValue.toLowerCase(Locale.ENGLISH));
    result.clear().addAll(idnaTypeSet.get(i));
    return true;
  }

  static boolean getBinaryValue(String propertyValue) {
    boolean invert;
    if (propertyValue.length() == 0 || propertyValue.equalsIgnoreCase("true")
            || propertyValue.equalsIgnoreCase("t")
            || propertyValue.equalsIgnoreCase("yes")
            || propertyValue.equalsIgnoreCase("y")) {
      invert = false;
    } else if (propertyValue.equalsIgnoreCase("false")
            || propertyValue.equalsIgnoreCase("f")
            || propertyValue.equalsIgnoreCase("no")
            || propertyValue.equalsIgnoreCase("n")) {
      invert = true;
    } else {
      throw new IllegalArgumentException(
      "PropertyValue must be empty (= T) or one of: True, T, False, F");
    }
    return invert;
  }

  public static boolean equals(CharSequence inbuffer, CharSequence outbuffer) {
    if (inbuffer.length() != outbuffer.length()) {
      return false;
    }
    for (int i = inbuffer.length() - 1; i >= 0; --i) {
      if (inbuffer.charAt(i) != outbuffer.charAt(i)) {
        return false;
      }
    }
    return true;
  }

  static final int BLOCK_ENUM = UCharacter.getPropertyEnum("block");

  public static void showSet(UnicodeSet a, boolean abbreviate, boolean ucdFormat, Appendable out) throws IOException {
    if (a.size() < 20000 && !abbreviate) {
      String oldBlock = "";
      String oldSubhead = "";
      for (UnicodeSetIterator it = new UnicodeSetIterator(a); it.next();) {
        int s = it.codepoint;
        if (s == UnicodeSetIterator.IS_STRING) {
          String newBlock = "Strings";
          if (!newBlock.equals(oldBlock)) {
            out.append("<h3>" + newBlock + "</b></h3>\r\n");
            oldBlock = newBlock;
          }
          out.append(showCodePoint(it.string)).append("<br>\r\n");
        } else {
          String newBlock = UCharacter.getStringPropertyValue(BLOCK_ENUM, s, UProperty.NameChoice.LONG).replace('_', ' ');
          String newSubhead = getSubheader().getSubheader(s);
          if (newSubhead == null) {
            newSubhead = "<i>no subhead</i>";
          }
          if (!newBlock.equals(oldBlock) || !oldSubhead.equals(newSubhead)) {
            out.append("<h3>" + newBlock + " - <i>" + newSubhead + "</i></b></h3>\r\n");
            oldBlock = newBlock;
            oldSubhead = newSubhead;
          }
          showCodePoint(s, ucdFormat, out);
        }
      }
    } else if (a.getRangeCount() < 10000) {
      for (UnicodeSetIterator it = new UnicodeSetIterator(a); it.nextRange();) {
        int s = it.codepoint;
        if (s == UnicodeSetIterator.IS_STRING) {
          out.append(showCodePoint(it.string)).append("<br>\r\n");
        } else {        
          int end = it.codepointEnd;
          if (end == s) {
            showCodePoint(s, ucdFormat, out);
          } else if (end == s + 1) {
            showCodePoint(s, ucdFormat, out);
            showCodePoint(end, ucdFormat, out);
          } else {
            if (ucdFormat) {
              out.append(getHex(s, ucdFormat));
              out.append("..");
              showCodePoint(end, ucdFormat, out);
            } else {
              showCodePoint(s, ucdFormat, out);
              out.append("\u2026{" + (end-s-1) + "}\u2026");
              showCodePoint(end, ucdFormat, out);
            }
          }
        }
      }
    } else {
      out.append("<i>Too many to list individually</i>\r\n");
    }
  }

  static private UnicodeSet RTL= UnicodeSetUtilities.parseUnicodeSet("[[:bc=R:][:bc=AL:]]");

  private static String showCodePoint(int codepoint) {
    return showCodePoint(UTF16.valueOf(codepoint));
  }

  private static String showCodePoint(String s) {
    String literal = getLiteral(s);
    return "<a target='c' href='list-unicodeset.jsp?a=" + toHTML.transliterate(UtfParameters.fixQuery(s)) + "'>\u00a0" + literal + "\u00a0</a>";
  }

  private static String getLiteral(int codepoint) {
    return getLiteral(UTF16.valueOf(codepoint));
  }

  private static String getLiteral(String s) {
    String literal = toHTML.transliterate(s);
    if (RTL.containsSome(literal)) {
      literal = '\u200E' + literal + '\u200E';
    }
    return literal;
  }

  private static void showCodePoint(int codePoint, boolean ucdFormat, Appendable out) throws IOException {
    final String string = UTF16.valueOf(codePoint);
    String separator = ", ";
    showString(string, ucdFormat, separator, out);
  }

  private static void showString(final String string, boolean ucdFormat, String separator,
          Appendable out) throws IOException {
    String literal = toHTML.transliterate(string);
    if (RTL.containsSome(literal)) {
      literal = '\u200E' + literal + '\u200E';
    }
    String name = getName(string, separator, false);
    if (name == null || name.length() == 0) {
      name = "<i>no name</i>";
    } else {
      boolean special = name.indexOf('<') >= 0;
      name = toHTML.transliterate(name);
      if (special) {
        name = "<i>" + name + "</i>";
      }
    }
    out.append(getHex(string, separator, ucdFormat) + " " + (ucdFormat ? 	"\t;" : "(\u00A0" + literal + "\u00A0) ") + name + "<br>\r\n");
  }

  private static String getName(String string, String separator, boolean andCode) {
    StringBuilder result = new StringBuilder();
    int cp;
    for (int i = 0; i < string.length(); i += UTF16.getCharCount(cp)) {
      cp = UTF16.charAt(string, i);
      if (i != 0) {
        result.append(separator);
      }
      if (andCode) {
        result.append("U+").append(com.ibm.icu.impl.Utility.hex(cp, 4)).append(' ');
      }
      result.append(UCharacter.getExtendedName(cp));
    }
    return result.toString();
  }

  private static String getHex(int codePoint, boolean ucdFormat) {
    String hex = com.ibm.icu.impl.Utility.hex(codePoint, 4);
    final String string = "<code><a target='c' href='character.jsp?a=" + hex + "'>"
    + (ucdFormat ? "" : "U+")
    + hex + "</a></code>";
    return string;
  }

  private static String getHex(String string, String separator, boolean ucdFormat) {
    StringBuilder result = new StringBuilder();
    int cp;
    for (int i = 0; i < string.length(); i += UTF16.getCharCount(cp)) {
      if (i != 0) {
        result.append(separator);
      }
      result.append(getHex(cp = UTF16.charAt(string, i), ucdFormat));
    }
    return result.toString();
  }

  //  private static void showString(String s, String separator, boolean ucdFormat, Writer out) throws IOException {
  //    int cp;
  //    for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
  //      if (i != 0) {
  //        out.write(separator);
  //      }
  //      showCodePoint(cp = UTF16.charAt(s, i), ucdFormat, out);
  //    }
  //  }

  public static String getSimpleSet(String setA, UnicodeSet a, boolean abbreviate, boolean escape) {
    String a_out;
    a.clear();
    try {
      setA = UnicodeSetUtilities.MyNormalize(setA, Normalizer.NFC);
      a.addAll(UnicodeSetUtilities.parseUnicodeSet(setA));
      a_out = getPrettySet(a, abbreviate, escape);
    } catch (Exception e) {
      a_out = e.getMessage();
    }
    return a_out;
  }

  static final UnicodeSet MAPPING_SET = UnicodeSetUtilities.parseUnicodeSet("[:^c:]");

  private static final UnicodeSet ASCII = UnicodeSetUtilities.parseUnicodeSet("[:ASCII:]");

  //public static boolean haveCaseFold = false;

  static {
    Transliterator.registerInstance(getTransliteratorFromFile("en-IPA", "en-IPA.txt", Transliterator.FORWARD));
    Transliterator.registerInstance(getTransliteratorFromFile("IPA-en", "en-IPA.txt", Transliterator.REVERSE));

    Transliterator.registerInstance(getTransliteratorFromFile("deva-ipa", "Deva-IPA.txt", Transliterator.FORWARD));
    Transliterator.registerInstance(getTransliteratorFromFile("ipa-deva", "Deva-IPA.txt", Transliterator.REVERSE));
  }

  public static Transliterator getTransliteratorFromFile(String ID, String file, int direction) {
    try {
      BufferedReader br = openFile(UnicodeUtilities.class, file);
      StringBuffer input = new StringBuffer();
      while (true) {
        String line = br.readLine();
        if (line == null) {
          break;
        }
        if (line.startsWith("\uFEFF")) {
          line = line.substring(1); // remove BOM
        }
        input.append(line);
        input.append('\n');
      }
      return Transliterator.createFromRules(ID, input.toString(), direction);
    } catch (IOException e) {
      throw (IllegalArgumentException) new IllegalArgumentException("Can't open transliterator file " + file).initCause(e);
    }
  }

  public static final Transliterator UNESCAPER = Transliterator.getInstance("hex-any");


  public static String showTransform(String transform, String sample) {
//    if (!haveCaseFold) {
//      registerCaseFold();
//    }
    Transliterator trans;
    try {
      trans = Transliterator.createFromRules("foo", transform, Transliterator.FORWARD);
    } catch (Exception e) {
      try {
        trans = Transliterator.getInstance(transform);
      } catch (Exception e2) {
        return "Error: " + toHTML.transform(e.getMessage() + "; " + e2.getMessage());
      }
    }

    UnicodeSet set = null;
    // see if sample is a UnicodeSet
    if (UnicodeSet.resemblesPattern(sample, 0)) {
      try {
        set = UnicodeSetUtilities.parseUnicodeSet(sample);
      } catch (Exception e) {}
    }
    if (set == null) {
      sample = UNESCAPER.transform(sample);
      return getLiteral(trans.transform(sample)).replace("\n", "<br>");
    }

    PrettyPrinter pp = new PrettyPrinter().setOrdering(Collator.getInstance(ULocale.ROOT)).setSpaceComparator(Collator.getInstance(ULocale.ROOT).setStrength2(RuleBasedCollator.PRIMARY)).setSpaceComparator(new Comparator<String>() {
      public int compare(String o1, String o2) {
        return 1;
      }
    });

    Map<String, UnicodeSet> mapping = new TreeMap<String,UnicodeSet>(pp.getOrdering());

    for (UnicodeSetIterator it = new UnicodeSetIterator(set); it.next();) {
      String s = it.getString();
      String mapped = trans.transform(s);
      if (!mapped.equals(s)) {
        UnicodeSet x = mapping.get(mapped);
        if (x == null) {
          mapping.put(mapped, x = new UnicodeSet());
        }
        x.add(s);
      }
    }
    StringBuilder result = new StringBuilder();
    for (String mapped : mapping.keySet()) {
      UnicodeSet source = mapping.get(mapped);
      result.append(showCodePoint(mapped));
      result.append("\t←\t");
      if (source.size() == 1) {
        UnicodeSetIterator it = new UnicodeSetIterator(source);
        it.next();
        result.append(showCodePoint(it.getString()));
      } else {
        result.append(showCodePoint(pp.format(source)));
      }
      result.append("</br>\r\n");
    }
    return result.toString();
  }

  static class StringPair implements Comparable<StringPair> {
    String first;
    String second;
    public StringPair(String first, String second) {
      this.first = first;
      this.second = second;
    }
    public int compareTo(StringPair o) {
      int result = first.compareTo(o.first);
      if (result != 0) {
        return result;
      }
      return second.compareTo(o.second);
    }
  }

  static String TRANSFORMLIST = null;

  public static String listTransforms() {
    if (TRANSFORMLIST == null) {
      StringBuilder result = new StringBuilder();
      Set<StringPair> pairs = new TreeSet<StringPair>();
      Set<String> sources = append(new TreeSet<String>(col), (Enumeration<String>) Transliterator.getAvailableSources());
      for (String source : sources) {
        Set<String> targets = append(new TreeSet<String>(col), (Enumeration<String>) Transliterator.getAvailableTargets(source));
        for (String target : targets) {
          Set<String> variants = append(new TreeSet<String>(col), (Enumeration<String>) Transliterator.getAvailableVariants(source, target));
          for (String variant : variants) {
            final String id = toHTML.transform(source + "-" + target + (variant.length() == 0 ? "" : "/" + variant));
            pairs.add(new StringPair(target, id));
          }
        }
      }
      result.append("<hr><table><tr><th>Result</th><th>IDs</th></tr>\n");
      String last = "";
      boolean first = true;
      for (StringPair pair : pairs) {
        if (!last.equals(pair.first)) {
          if (first) {
            first = false;
          } else {
            result.append("</td></tr>\n");
          }
          result.append("<tr><th>" + pair.first + "</th><td>");
        }
        result.append("<a href='transform.jsp?a=" + pair.second + "'>" + pair.second + "</a>\n");
        last = pair.first;
      }
      result.append("\t\t</ul>\n\t</li>\n");
      result.append("</table>");
      TRANSFORMLIST = result.toString();
    }
    return TRANSFORMLIST;
  }

  private static <T, U extends Collection<T>> U append(U result, Enumeration<T> sources) {
    while (sources.hasMoreElements()) {
      result.add(sources.nextElement());
    }
    return result;
  }

//  private static void registerCaseFold() {
//    StringBuilder rules = new StringBuilder();
//    for (UnicodeSetIterator it = new UnicodeSetIterator(MAPPING_SET); it.nextRange();) {
//      for (int i = it.codepoint; i <= it.codepointEnd; ++i) {
//        String s = UTF16.valueOf(i);
//        String caseFold = UCharacter.foldCase(s, true);
//        String lower = UCharacter.toLowerCase(Locale.ENGLISH, s);
//        if (!caseFold.equals(lower) || i == 'Σ') {
//          rules.append(s + ">" + caseFold + " ;\r\n");
//        }
//      }
//    }
//    rules.append("::Lower;");
//    Transliterator.registerInstance(Transliterator.createFromRules("Any-CaseFold", rules.toString(), Transliterator.FORWARD));
//    haveCaseFold = true;
//  }
  
  static class FilteredStringTransform implements StringTransform {
    final UnicodeSet toExclude;
    final StringTransform trans;
    public FilteredStringTransform(UnicodeSet toExclude, StringTransform trans) {
      this.toExclude = toExclude;
      this.trans = trans;
    }
    public String transform(String source) {
      StringBuilder result = new StringBuilder();
      int start = 0;
      while (start < source.length()) {
        int end = toExclude.findIn(source, start, false);
        result.append(trans.transform(source.substring(start,end)));
        if (end == source.length()) break;
        start = toExclude.findIn(source, end, true);
        result.append(source.substring(end,start));
      }
      return result.toString();
    }
  }

  private static String getPrettySet(UnicodeSet a, boolean abbreviate, boolean escape) {
    String a_out;
    if (a.size() < 10000 && !abbreviate) {
      PrettyPrinter pp = new PrettyPrinter().setOrdering(Collator.getInstance(ULocale.ROOT)).setSpaceComparator(Collator.getInstance(ULocale.ROOT).setStrength2(RuleBasedCollator.PRIMARY));
      if (escape) {
        pp.setToQuote(UnicodeSetUtilities.parseUnicodeSet("[^\\u0021-\\u007E]"));
      }
      a_out = toHTML(pp.format(a));
    } else {
      a.complement().complement();
      a_out = toHTML(a.toPattern(escape));
    }
    // insert spaces occasionally
    int cp;
    int oldCp = 0;
    StringBuffer out = new StringBuffer();
    int charCount = 0;
    for (int i = 0; i < a_out.length(); i+= UTF16.getCharCount(cp)) {
      cp = UTF16.charAt(a_out, i);
      ++charCount;
      if (charCount > 20) {
        // add a space, but not in x-y, or \\uXXXX
        if (cp == '-' || oldCp == '-') {
          // do nothing
        } else if (oldCp == '\\' || cp < 0x80) {
          // do nothing
        } else {
          out.append(' ');
          charCount = 0;
        }
      }
      UTF16.append(out, cp);
      oldCp = cp;
    }
    return out.toString();
  }

  public static UnicodeSet  parseSimpleSet(String setA, String[] exceptionMessage) {
    try {
      exceptionMessage[0] = null;
      //setA = MyNormalize(setA, Normalizer.NFC);
      return UnicodeSetUtilities.parseUnicodeSet(setA);
    } catch (Exception e) {
      exceptionMessage[0] = e.getMessage();
    }
    return null;
  }

  public static void getDifferences(String setA, String setB,
          boolean abbreviate, String[] abResults, int[] abSizes, String[] abLinks) {
    boolean escape = false;

    String setAr = toHTML.transliterate(UtfParameters.fixQuery(setA));
    String setBr = toHTML.transliterate(UtfParameters.fixQuery(setB));
    abLinks[0] = "http://unicode.org/cldr/utility/list-unicodeset.jsp?a=[" + setAr + '-' + setBr + "]";
    abLinks[1] = "http://unicode.org/cldr/utility/list-unicodeset.jsp?a=[" + setBr + '-' + setAr + "]";
    abLinks[2] = "http://unicode.org/cldr/utility/list-unicodeset.jsp?a=[" + setAr + "%26" + setBr + "]";
    String[] aMessage = new String[1];
    String[] bMessage = new String[1];

    UnicodeSet a = UnicodeUtilities.parseSimpleSet(setA, aMessage);
    UnicodeSet b = UnicodeUtilities.parseSimpleSet(setB, bMessage);

    String a_b;
    String b_a;
    String ab;

    // try {
    // setA = MyNormalize(setA, Normalizer.NFC);
    // a = UnicodeUtilities.parseUnicodeSet(setA);
    // } catch (Exception e) {
    // a_b = e.getMessage();
    // }
    // UnicodeSet b = null;
    // try {
    // setB = MyNormalize(setB, Normalizer.NFC);
    // b = UnicodeUtilities.parseUnicodeSet(setB);
    // } catch (Exception e) {
    // b_a = e.getMessage();
    // }
    int a_bSize = 0, b_aSize = 0, abSize = 0;
    if (a == null || b == null) {
      a_b = a == null ? aMessage[0] : "error" ;
      b_a = b == null ? bMessage[0] : "error" ;
      ab = "error";
    } else  {
      UnicodeSet temp = new UnicodeSet(a).removeAll(b);
      a_bSize = temp.size();
      a_b = getPrettySet(temp, abbreviate, escape);

      temp = new UnicodeSet(b).removeAll(a);
      b_aSize = temp.size();
      b_a = getPrettySet(temp, abbreviate, escape);

      temp = new UnicodeSet(a).retainAll(b);
      abSize = temp.size();
      ab = getPrettySet(temp, abbreviate, escape);
    }
    abResults[0] = a_b;
    abSizes[0] = a_bSize;
    abResults[1] = b_a;
    abSizes[1] = b_aSize;
    abResults[2] = ab;
    abSizes[2] = abSize;
  }

  static int[][] ranges = { { UProperty.BINARY_START, UProperty.BINARY_LIMIT },
    { UProperty.INT_START, UProperty.INT_LIMIT },
    { UProperty.DOUBLE_START, UProperty.DOUBLE_LIMIT },
    { UProperty.STRING_START, UProperty.STRING_LIMIT }, };

  static Collator col = Collator.getInstance(ULocale.ROOT);
  static {
    ((RuleBasedCollator) col).setNumericCollation(true);
  }

  public static void showProperties(String text, Appendable out) throws IOException {
    text = UTF16.valueOf(text, 0);
    int cp = UTF16.charAt(text, 0);
    Set<String> showLink = new HashSet<String>();
    Map<String,String> alpha = new TreeMap<String,String>(col);

    for (int range = 0; range < ranges.length; ++range) {
      for (int propIndex = ranges[range][0]; propIndex < ranges[range][1]; ++propIndex) {
        String propName = UCharacter.getPropertyName(propIndex,
                UProperty.NameChoice.LONG);
        String propValue = null;
        int ival;
        switch (range) {
        default:
          propValue = "???";
          break;
        case 0:
          ival = UCharacter.getIntPropertyValue(cp, propIndex);
          if (ival != 0) {
            propValue = "True";
          }
          showLink.add(propName);
          break;
        case 2:
          double nval = UCharacter.getNumericValue(cp);
          if (nval != -1) {
            propValue = String.valueOf(nval);
            showLink.add(propName);
          }
          break;
        case 3:
          propValue = UCharacter.getStringPropertyValue(propIndex, cp,
                  UProperty.NameChoice.LONG);
          if (text.equals(propValue)) {
            propValue = null;
          }
          break;
        case 1:
          ival = UCharacter.getIntPropertyValue(cp, propIndex);
          if (ival != 0) {
            propValue = UCharacter.getPropertyValueName(propIndex, ival,
                    UProperty.NameChoice.LONG);
            if (propValue == null) {
              propValue = String.valueOf(ival);
            }
          }
          showLink.add(propName);
          break;
        }
        if (propValue != null) {
          alpha.put(propName, propValue);
        }
      }
    }
    showLink.add("Age");

    String x;
    String upper = x = UCharacter.toUpperCase(ULocale.ENGLISH, text);
    if (!text.equals(x)) {
      alpha.put("toUppercase", x);
    }
    String lower = x = UCharacter.toLowerCase(ULocale.ENGLISH, text);
    if (!text.equals(x)) {
      alpha.put("toLowercase", x);
    }
    String title = x = UCharacter.toTitleCase(ULocale.ENGLISH, text, null);
    if (!text.equals(x)) {
      alpha.put("toTitlecase", x);
    }

    String nfc = x = UnicodeSetUtilities.MyNormalize(text, Normalizer.NFC);
    if (!text.equals(x)) {
      alpha.put("toNFC", x);
    }
    String nfd = x = UnicodeSetUtilities.MyNormalize(text, Normalizer.NFD);
    if (!text.equals(x)) {
      alpha.put("toNFD", x);
    }
    x = UnicodeSetUtilities.MyNormalize(text, Normalizer.NFKD);
    if (!text.equals(x)) {
      alpha.put("toNFKD", x);
    }
    x = UnicodeSetUtilities.MyNormalize(text, Normalizer.NFKC);
    if (!text.equals(x)) {
      alpha.put("toNFKC", x);
    }

    CanonicalIterator ci = new CanonicalIterator(text);
    int count = 0;
    for (String item = ci.next(); item != null; item = ci.next()) {
      if (item.equals(text)) {
        continue;
      }
      if (item.equals(nfc)) {
        continue;
      }
      if (item.equals(nfd)) {
        continue;
      }
      alpha.put("toOther_Canonical_Equivalent#" + (++count), item);
    }

    /*
     * CaseIterator cai = new CaseIterator(); cai.reset(text); count = 0; for
     * (String item = cai.next(); item != null; item = cai.next()) { if
     * (item.equals(text)) continue; if (item.equals(upper)) continue; if
     * (item.equals(lower)) continue; if (item.equals(title)) continue;
     * alpha.put("toOther_Case_Equivalent#" + (++count), item); }
     */

    Set<String> unicodeProps = new TreeSet<String>(UNICODE_PROPS);

    Set<String> regexProps = new TreeSet<String>(REGEX_PROPS);
    Set<String> icuProps = new TreeSet<String>(alpha.keySet());
    icuProps.removeAll(unicodeProps);
    icuProps.removeAll(regexProps);

    out.append("<table>\r\n");
    String name = (String) alpha.get("Name");
    if (name != null) {
      name = toHTML.transliterate(name);
    }

    out.append("<tr><th>" + "Character" + "</th><td>"
            + toHTML.transliterate(text) + "</td></tr>\r\n");
    out.append("<tr><th>" + "Code_Point" + "</th><td>"
            + com.ibm.icu.impl.Utility.hex(cp, 4) + "</td></tr>\r\n");
    out.append("<tr><th>" + "Name" + "</th><td>" + name + "</td></tr>\r\n");
    alpha.remove("Name");
    showPropertyValue(alpha, showLink, "", unicodeProps, out); 
    showPropertyValue(alpha, showLink, "® ", regexProps, out);
    showPropertyValue(alpha, showLink, "© ", icuProps, out);
    out.append("</table>\r\n");
  }

  private static void showPropertyValue(Map<String,String> alpha, Set<String> showLink, String flag, 
          Set<String> unicodeProps, Appendable out) throws IOException {
    for (String string : alpha.keySet()) {
      String propName = (String) string;
      if (!unicodeProps.contains(propName)) {
        continue;
      }
      String propValue = (String) alpha.get(propName);

      String hValue = toHTML.transliterate(propValue);
      hValue = showLink.contains(propName) ? "<a target='u' href='list-unicodeset.jsp?a=[:"
              + propName + "=" + propValue + ":]'>" + hValue + "</a>"
              : hValue;

      out.append("<tr><th><a target='c' href='properties.jsp#" + propName + "'>"
              + flag + propName + "</a></th><td>" + hValue + "</td></tr>\r\n");
    }
  }

  public static Set<String> showPropsTable(Appendable out) throws IOException {
    int[][] ranges = {{UProperty.BINARY_START, UProperty.BINARY_LIMIT},
            {UProperty.INT_START, UProperty.INT_LIMIT},
            {UProperty.DOUBLE_START, UProperty.DOUBLE_LIMIT},
            {UProperty.STRING_START, UProperty.STRING_LIMIT},
    };
    Collator col = Collator.getInstance(ULocale.ROOT);
    ((RuleBasedCollator)col).setNumericCollation(true);
    Map<String, Map<String, String>> alpha = new TreeMap<String, Map<String, String>>(col);
    Map<String, String> longToShort = new HashMap<String, String>();

    Set<String> showLink = new HashSet<String>();

    for (int range = 0; range < ranges.length; ++range) {
      for (int propIndex = ranges[range][0]; propIndex < ranges[range][1]; ++propIndex) {
        String propName = UCharacter.getPropertyName(propIndex, UProperty.NameChoice.LONG);
        String shortPropName = UCharacter.getPropertyName(propIndex, UProperty.NameChoice.SHORT);
        longToShort.put(propName, shortPropName == null ? propName : shortPropName);
        //propName = getName(propIndex, propName, shortPropName);
        Map<String, String> valueOrder = new TreeMap<String, String>(col);
        alpha.put(propName, valueOrder);
        //out.println(propName + "<br>");
        switch (range) {
        default: valueOrder.put("[?]", ""); break;
        case 0: valueOrder.put("True", "T"); 
        valueOrder.put("False", "F"); 
        showLink.add(propName); 
        break;
        case 2: valueOrder.put("[double]", ""); 
        break;
        case 3: valueOrder.put("[string]", ""); 
        break;
        case 1:
          for (int valueIndex = 0; valueIndex < 256; ++valueIndex) {
            try {
              String valueName = UCharacter.getPropertyValueName(propIndex, valueIndex, UProperty.NameChoice.LONG);
              //out.println("----" + valueName + "<br>");
              String shortValueName = UCharacter.getPropertyValueName(propIndex, valueIndex, UProperty.NameChoice.SHORT);
              if (valueName == null) {
                valueName = shortValueName;
              }
              //valueName = getName(valueIndex, valueName, shortValueName);
              if (valueName != null) {
                valueOrder.put(valueName, shortValueName != null ? shortValueName : "");
              } else if (propIndex == UProperty.CANONICAL_COMBINING_CLASS) {
                String posVal = String.valueOf(valueIndex);
                if (UnicodeSetUtilities.parseUnicodeSet("[:ccc=" + posVal + ":]").size() != 0) {
                  valueOrder.put(posVal, posVal);
                }
              }
              showLink.add(propName);
            } catch (RuntimeException e) {
              // just skip
            }
          }
        }
      }
    }
    Set<String> unicodeProps = new TreeSet<String>(UNICODE_PROPS);

    Set<String> regexProps = new TreeSet<String>(REGEX_PROPS);

    out.append("<table>\r\n");
    for (String string : alpha.keySet()) {
      String propName = (String) string;
      String shortPropName = longToShort.get(propName);
      String sPropName = propName + (shortPropName == null ? "" : " (" + shortPropName + ")");
      Map<String, String> values = alpha.get(propName);
      if (unicodeProps.contains(propName)) {
        unicodeProps.remove(propName);
      } else if (regexProps.contains(propName)) {
        regexProps.remove(propName);
        sPropName = "<tt>\u00AE\u00A0" + sPropName + "</tt>";
      } else {
        sPropName = "<i>\u00A9\u00A0" + sPropName + "</i>";
      }

      out.append("<tr><th width='1%'><a name='" + propName + "'>" + sPropName + "</a></th>\r\n");
      out.append("<td>\r\n");
      boolean first = true;
      for (String string2 : values.keySet()) {
        String propValue = (String) string2;
        String alternates = values.get(propValue);
        if (first) {
          first = false;
        } else {
          out.append(", ");
        }


        if (showLink.contains(propName)) {
          propValue = getPropLink(propName, propValue, propValue) 
          + getPropLink(shortPropName, alternates.length() == 0 ? propValue : alternates, "♻");
        }

        out.append(propValue);
      }
      out.append("</td></tr>\r\n");
    }
    out.append("</table>\r\n");
    unicodeProps.addAll(regexProps);
    return unicodeProps;
  }

  private static String getPropLink(String propName, String propValue, String linkText) {
    final String propExp = 
      propValue == "T" ? propName
              : propValue == "F" ? "^" + propName
                      : propName + "=" + propValue;
    return "<a target='u' href='list-unicodeset.jsp?a=[:" + propExp + ":]'>" + linkText + "</a>";
  }

  static Subheader getSubheader() {
    if (subheader == null) {
      // /home/users/jakarta/apache-tomcat-6.0.14/bin
      // /home/users/jakarta/apache-tomcat-6.0.14/webapps/cldr/utility
      subheader = new Subheader(SubheaderSnapshot.data);
      //      try {
      //        final String unicodeDataDirectory = "../webapps/cldr/utility/";
      //        //System.out.println(canonicalPath);
      //        subheader = new Subheader(unicodeDataDirectory);
      //      } catch (IOException e) {
      //        try {
      //          final String unicodeDataDirectory = "./jsp/";
      //          subheader = new Subheader(unicodeDataDirectory);
      //        } catch (IOException e2) {
      //          final String[] list = new File("home").list();
      //          String currentDirectory = list == null ? null : new TreeSet<String>(Arrays.asList(list)).toString();
      //          throw (RuntimeException) new IllegalArgumentException("Can't find file starting from: <" + currentDirectory + ">").initCause(e);
      //        }
      //      }
    }
    return subheader;
  }

  public static String showRegexFind(String regex, String test) {
    try {
      Matcher matcher = Pattern.compile(regex, Pattern.COMMENTS).matcher(test);
      String result = toHTML.transform(matcher.replaceAll("⇑⇑$0⇓⇓"));
      result = result.replaceAll("⇑⇑", "<u>").replaceAll("⇓⇓", "</u>");
      return result;
    } catch (Exception e) {
      return "Error: " + e.getMessage();
    }
  }

  static IdnaLabelTester tester = null;
  static String removals = UnicodeSetUtilities.parseUnicodeSet("[\u1806[:di:]-[:cn:]]").complement().complement().toPattern(false);
  static Matcher rem = Pattern.compile(removals).matcher("");
  static String defaultIdnaInput = "\u0001.com"
    +"\nöbb.at ÖBB.at ÖBB.at"
    +"\nȡog.de ☕.de I♥NY.de"
    +"\nfass.de faß.de fäß.de Schäffer.de"
    +"\nＡＢＣ・日本.co.jp 日本｡co｡jp 日本｡co．jp 日本⒈co．jp"
    +"\nx\\u0327\\u0301.de x\\u0301\\u0327.de"
    +"\nσόλος.gr Σόλος.gr ΣΌΛΟΣ.gr"
    +"\nﻋﺮﺑﻲ.de عربي.de نامهای.de نامه\\u200Cای.de";

  public static String getDefaultIdnaInput() {
    return defaultIdnaInput;
  }

  public static String testIdnaLines(String lines, String filter) {
    Transliterator hex = Transliterator.getInstance("any-hex");
    try {

      lines = IdnaLabelTester.UNESCAPER.transform(lines);
      StringBuilder resultLines = new StringBuilder();
      getIdna2008Tester();

      Predicate<String> verifier2008 = new Predicate<String>() {
        public boolean is(String item) {
          return Normalizer.isNormalized(item, Normalizer.NFC, 0) && tester.test(item) == null;
        }
      };

      resultLines.append("<table>\n");
      resultLines.append("<th></th><th class='cn'>Input</th><th class='cn'>IDNA2003</th><th class='cn'>IDNA46</th><th class='cn'>IDNA2008</th>\n");

      boolean first = true;
      for (String line : lines.split("\\s+")) {
        if (first) {
          first = false;
        } else {
          addBlank(resultLines);
        }

        String rawPunycode = processLabels(line, DOTS, true, new Predicate() {
          public boolean is(Object item) {
            return true;
          }});
        R2<String, String> idna2003Pair = Idna2003.getIdna2033(line);
        String idna2003 = idna2003Pair.get0();
        String idna2003back = idna2003Pair.get1();


        String tr46back = Uts46.toUts46(line);
        String tr46 = processLabels(tr46back, DOTS, true, new Predicate<String>() {
          public boolean is(String item) {
            return Uts46.Uts46Chars.containsAll(item);
          }
        });
        String tr46display = Uts46.foldDisplay.transform(line);
        tr46display = processLabels(tr46display, DOTS, false, new Predicate<String>() {
          public boolean is(String item) {
            return Uts46.Uts46CharsDisplay.containsAll(item);
          }
        });

        String idna2008 = processLabels(line, DOT, true, verifier2008);
        String idna2008back = processLabels(line, DOT, false, verifier2008);

        // first lines
        resultLines.append("<tr>");
        resultLines.append("<th>Display</th>");
        addCell(resultLines, hex, line, "class='cn ltgreen'");
        addCell(resultLines, hex, idna2003back, "class='cn i2003'");
        addCell(resultLines, hex, tr46display, "class='cn i46'");
        addCell(resultLines, hex, idna2008back, "class='cn i2008'");
        resultLines.append("<tr></tr>");

        resultLines.append("<th class='mono'>Punycode</th>");
        addCell(resultLines, hex, rawPunycode, "class='cn ltgreen mono'");
        addCell(resultLines, hex, idna2003, "class='cn mono i2003'");
        addCell(resultLines, hex, tr46, "class='cn mono i46'");
        addCell(resultLines, hex, idna2008, "class='cn mono i2008'");

        //        if (result == null) {
        //          resultLines.append("<td class='c'>\u00A0</td><td class='c'>\u00A0</td>");
        //        } else {
        //          resultLines.append("<td class='c'>")
        //          .append(toHTML.transform(IdnaLabelTester.ESCAPER.transform(normalized.substring(0, result.position))) 
        //                  + "<span class='x'>\u2639</span>" + toHTML.transform(IdnaLabelTester.ESCAPER.transform(normalized.substring(result.position))) 
        //                  + "</td><td>" + result.title
        //                  //+ "</td><td class='c'>" + result.ruleLine
        //                  + "</td>");
        //        }
        resultLines.append("</tr>\n");
      }

      resultLines.append("</table>\n");
      return resultLines.toString();
    } catch (Exception e) {
      return toHTML.transform(e.getMessage());
    }
  }


  private static IdnaLabelTester getIdna2008Tester() {
    if (tester == null) {
      try {
        URL path = UnicodeUtilities.class.getResource("idnaContextRules.txt");
        String externalForm = path.toExternalForm();
        if (externalForm.startsWith("file:")) {
          externalForm = externalForm.substring(5);
        }
        tester = new IdnaLabelTester(externalForm);
      } catch (IOException e) {
        throw new IllegalArgumentException(e);
      }
    }
    return tester;
  }

  static Pattern DOTS = Pattern.compile("[.。｡]");
  static Pattern DOT = Pattern.compile("[.]");

  private static void addBlank(StringBuilder resultLines) {
    resultLines.append("<tr><td colSpan='5'>&nbsp;</td></tr>\n");
  }

  private static void addCell(StringBuilder resultLines, Transliterator hex, String tr46, String attributes) {
    if (tr46 == null) {
      resultLines.append("<td " +
              attributes +
      "><i>fails</i></td>\n");
    } else {
      resultLines.append("<td " +
              attributes +
              (" title='" + hex.transform(tr46) + "'") +
      ">").append(showEscaped(tr46)).append("</td>\n");
    }
  }

  static String processLabels(String inputLabels, Pattern dotPattern, boolean punycode, Predicate<String> verifier) {
    StringBuilder result = new StringBuilder();
    for (String label : dotPattern.split(inputLabels)) {
      if (result.length() != 0) {
        result.append('.');
      }
      try {
        if (!verifier.is(label)) {
          throw new IllegalArgumentException();
        }
        if (!punycode || ASCII.containsAll(label)) {
          result.append(label);
        } else {
          StringBuffer puny = Punycode.encode(new StringBuffer(label), null);
          if (puny.length() == 0) {
            throw new IllegalArgumentException();
          }
          result.append("xn--").append(puny);
        }
      } catch (Exception e) {
        result.append('\uFFFD');
      }
    }
    return result.toString();
  }


  static final Transliterator ESCAPER = Transliterator.createFromRules("escaper", 
          "(" + IdnaLabelTester.TO_QUOTE + ") > '<span class=\"q\">'&any-hex($1)'</span>';"
          + HTML_RULES_CONTROLS, Transliterator.FORWARD);

  private static String showEscaped(String line) {
    String toShow = toHTML.transform(line);
    String escaped = ESCAPER.transform(line);
    if (!escaped.equals(toShow)) {
      toShow += "<br><span class='esc'>" + escaped + "</span>";
    }
    return toShow;
  }

  /**
   * The regex doesn't have to have the UnicodeSets resolved.
   * @param regex
   * @param count
   * @param maxRepeat
   * @return
   */
  public static String getBnf(String regexSource, int count, int maxRepeat) {
    //String regex = new UnicodeRegex().compileBnf(rules);
    String regex = regexSource.replace("(?:", "(").replace("(?i)", "");

    BNF bnf = new BNF(new Random(), new Quoter.RuleQuoter());
    if (maxRepeat > 20) {
      maxRepeat = 20;
    }
    bnf.setMaxRepeat(maxRepeat)
    .addRules("$root=" + regex + ";")
    .complete();
    StringBuffer output = new StringBuffer();
    for (int i = 0; i < count; ++i) {
      String line = bnf.next();
      output.append("<p>").append(toHTML(line)).append("</p>");
    }
    return output.toString();
  }

  public static String showBidi(String str, int baseDirection, boolean asciiHack) {
    // warning, only BMP for now
    final StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);

    BidiCharMap bidiCharMap = new BidiCharMap(asciiHack);

    String[] parts = str.split("\\r\\n?|\\n");
    for (int i = 0; i < parts.length; ++i) {
      writer.println("<h3>Paragraph " + (i+1) + "</h3>");
      if (parts[i] == null || parts[i].length() == 0) {
        continue;
      }
      showBidiLine(parts[i], baseDirection, writer, bidiCharMap);
    }

    if (asciiHack) {
      writer.println("<h3>ASCII Hack</h3>");
      writer.println("<p>For testing the UBA with only ASCII characters, the following property values are used (<,> are RLM and LRM):</p>");
      writer.println("<table>");
      for (byte i = 0; i < BidiReference.typenames.length; ++i) {
        final UnicodeSet modifiedClass = bidiCharMap.getAsciiHack(i);
        writer.println("<tr><th>" + BidiReference.getHtmlTypename(i) + "</th><td>" + getList(modifiedClass) + "</td></tr>"); 
      }
      writer.println("</table>");
    }

    writer.flush();
    return stringWriter.toString();
  }

  private static String getList(final UnicodeSet uset) {
    StringBuffer codePointString = new StringBuffer();
    for (UnicodeSetIterator it = new UnicodeSetIterator(uset); it.next();) {
      if (codePointString.length() != 0) {
        codePointString.append(" ");
      }
      final String literal = it.codepoint <= 0x20 ? "\u00AB" + getLiteral(UCharacter.getExtendedName(it.codepoint)) + "\u00BB" : getLiteral(it.codepoint);
      codePointString.append(literal);
    }
    return codePointString.toString();
  }

  private static void showBidiLine(String str, int baseDirection, PrintWriter writer, BidiCharMap bidiCharMap) {
    byte[] codes = new byte[str.length()];
    for (int i = 0; i < str.length(); ++i) {
      codes[i] = bidiCharMap.getBidiClass(str.charAt(i));
    }
    int[] linebreaks = new int[1];
    linebreaks[0] = str.length();

    BidiReference bidi = new BidiReference(codes, (byte)baseDirection);
    int[] reorder = bidi.getReordering(new int[] { codes.length });
    byte[] levels = bidi.getLevels(linebreaks);

    writer.println("<table><tr><th>Base Level</th>");
    final byte baseLevel = bidi.getBaseLevel();
    writer.println("<td>" + baseLevel + " = " + (baseLevel == 0 ? "LTR" : "RTL") + "</td><td>" + (baseDirection >= 0 ? "explicit" : "heuristic") + "</td>");
    writer.println("</tr></table>");

    // output original text
    writer.println("<h3>Source</h3>");
    writer.println("<table><tr><th>Memory Position</th>");
    for (int i = 0; i < str.length(); ++i) {
      writer.println("<td class='bcell'>" + i + "</td>");
    }
    writer.println("</tr><tr><th>Character</th>");
    for (int i = 0; i < str.length(); ++i) {
      final String s = str.substring(i,i+1);
      String title = toHTML.transform(getName(s, "", true));
      writer.println("<td class='bccell' title='" + title + "'> " + getLiteral(getBidiChar(str, i, codes[i])) + " </td>");
    }
    writer.println("</tr><tr><th>Bidi Class</th>");
    for (int i = 0; i < str.length(); ++i) {
      writer.println("<td class='bcell'><tt>" + BidiReference.getHtmlTypename(codes[i]) + "</tt></td>");
    }
    writer.println("</tr><tr><th>Rules Applied</th>");
    for (int i = 0; i < str.length(); ++i) {
      writer.println("<td class='bcell'><tt>" + bidi.getChanges(i).replace("\n", "<br>") + "</tt></td>");
    }
    writer.println("</tr><tr><th>Resulting Level</th>");
    for (int i = 0; i < str.length(); ++i) {
      writer.println("<td class='bcell'><tt>" + showLevel(levels[i]) + "</tt></td>");
    }
    writer.println("</tr></table>");

    // output visually ordered text
    writer.println("<h3>Reordered</h3>");
    writer.println("<table><th>Display Position</th>");
    for (int k = 0; k < str.length(); ++k) {
      final int i = reorder[k];
      final String bidiChar = getBidiChar(str, i, codes[i]);
      String td = bidiChar.length() == 0 ? "<td class='bxcell'>" : "<td class='bcell'>";
      writer.println(td + k + "</td>");
    }
    writer.println("</tr><tr><th>Memory Position</th>");
    for (int k = 0; k < str.length(); ++k) {
      final int i = reorder[k];
      final String bidiChar = getBidiChar(str, i, codes[i]);
      String td = bidiChar.length() == 0 ? "<td class='bxcell'>" : "<td class='bcell'>";
      writer.println(td + i + "</td>");
    }
    writer.println("</tr><tr><th>Character</th>");
    for (int k = 0; k < str.length(); ++k) {
      final int i = reorder[k];
      final String bidiChar = getBidiChar(str, i, codes[i]);
      String title = bidiChar.length() == 0 ? "deleted" : toHTML.transform(getName(bidiChar, "", true));
      String td = bidiChar.length() == 0 ? "bxcell" : "bccell";
      writer.println("<td class='" + td + "' title='" + title + "'>" + " " + getLiteral(bidiChar) +"</td>");
    }
    writer.println("</tr></table>");

  }

  private static String getBidiChar(String str, int i, byte b) {
    if (b == BidiReference.PDF || b == BidiReference.RLE || b == BidiReference.LRE || b == BidiReference.LRO || b == BidiReference.RLO || b == BidiReference.BN) {
      return "";
    }
    String substring = str.substring(i,i+1);
    if ((substring.equals("<") || substring.equals(">")) && (b == BidiReference.L || b == BidiReference.R)) {
      return "";
    }
    return substring;
  }

  private static String showLevel(int level) {
    StringBuffer result = new StringBuffer();
    for (int i = 0; i < level; ++i) {
      result.append("<br>");
    }
    result.append("L").append(level);
    return result.toString();
  }



  public static BufferedReader openFile(Class class1, String file) throws IOException {
    //URL path = null;
    //String externalForm = null;
    try {
      //      //System.out.println("Reading:\t" + file1.getCanonicalPath());
      //      path = class1.getResource(file);
      //      externalForm = path.toExternalForm();
      //      if (externalForm.startsWith("file:")) {
      //        externalForm = externalForm.substring(5);
      //      }
      //      File file1 = new File(externalForm);
      //      boolean x = file1.canRead();
      //      final InputStream resourceAsStream = new FileInputStream(file1);
      final InputStream resourceAsStream = class1.getResourceAsStream(file);
      InputStreamReader reader = new InputStreamReader(resourceAsStream, UTF8);
      BufferedReader bufferedReader = new BufferedReader(reader,1024*64);
      return bufferedReader;
    } catch (Exception e) {
      File file1 = new File(file);
      throw (RuntimeException) new IllegalArgumentException("Bad file name: "
              //              + path + "\t" + externalForm + "\t" + 
              + file1.getCanonicalPath()
              + "\r\n" + new File(".").getCanonicalFile() + " => " + Arrays.asList(new File(".").getCanonicalFile().list())).initCause(e);
    }
  }


  public static String validateLanguageID(String input, String locale) {
    String result = LanguageCode.validate(input, new ULocale(locale));
    return result;
  }

  public static String getLanguageOptions(String locale) {
    return LanguageCode.getLanguageOptions(new ULocale(locale));
  }

}

/*
 * <% http://www.devshed.com/c/a/Java/Developing-JavaServer-Pages/ Enumeration
 * parameterNames = request.getParameterNames(); while
 * (parameterNames.hasMoreElements()){ String parameterName = (String)
 * parameterNames.nextElement(); String parameterValue =
 * request.getParameter(parameterName); %> <%= parameterName %> has value <%=
 * parameterValue %>. <br> <% } %>
 */