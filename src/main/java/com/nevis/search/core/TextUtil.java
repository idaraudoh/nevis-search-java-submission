package com.nevis.search.core;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small text helpers shared by every search component. */
public final class TextUtil {

    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "has", "have", "in",
            "is", "it", "its", "of", "on", "or", "that", "the", "to", "was", "were", "will", "with",
            "this", "these", "those", "i", "we", "you", "your", "our", "my", "their");

    private static final Pattern TOKEN = Pattern.compile("[a-z0-9]+");
    private static final Pattern SENTENCE = Pattern.compile("(?<=[.!?])\\s+|\\n+");
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]");

    private TextUtil() {}

    /** Lowercase and strip accents. */
    public static String normalize(String text) {
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFKD);
        return MARKS.matcher(decomposed).replaceAll("").toLowerCase(Locale.ROOT);
    }

    /** Very light plural stemmer: bills -> bill, utilities -> utility. */
    public static String stem(String token) {
        int n = token.length();
        if (n > 4 && token.endsWith("ies")) return token.substring(0, n - 3) + "y";
        if (n > 5 && token.endsWith("sses")) return token.substring(0, n - 2);
        if (n > 3 && token.endsWith("s") && !token.endsWith("ss")) return token.substring(0, n - 1);
        return token;
    }

    public static List<String> tokenize(String text) {
        return tokenize(text, true, true);
    }

    public static List<String> tokenize(String text, boolean dropStopwords, boolean doStem) {
        List<String> tokens = new ArrayList<>();
        Matcher m = TOKEN.matcher(normalize(text));
        while (m.find()) tokens.add(m.group());
        if (dropStopwords) {
            List<String> kept = tokens.stream().filter(t -> !STOPWORDS.contains(t)).toList();
            if (!kept.isEmpty()) tokens = new ArrayList<>(kept); // never return nothing for a real query
        }
        if (doStem) tokens = new ArrayList<>(tokens.stream().map(TextUtil::stem).toList());
        return tokens;
    }

    /** Drop everything except letters and digits: "Nevis Wealth" -> "neviswealth". */
    public static String compact(String text) {
        return NON_ALNUM.matcher(normalize(text)).replaceAll("");
    }

    public static List<String> splitSentences(String text) {
        List<String> out = new ArrayList<>();
        for (String s : SENTENCE.split(text)) {
            String t = s.strip();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    /** Damerau-Levenshtein (optimal string alignment) similarity in [0, 1]. */
    public static double similarity(String a, String b) {
        int max = Math.max(a.length(), b.length());
        if (max == 0) return 1.0;
        int[][] d = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) d[i][0] = i;
        for (int j = 0; j <= b.length(); j++) d[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
                if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1)) {
                    d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
                }
            }
        }
        return 1.0 - (double) d[a.length()][b.length()] / max;
    }
}
