package ru.mirage.freerewards;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Разбор и проверка условий вида «%плейсхолдер% >= 5». Без зависимостей от Bukkit. */
public final class Conditions {
    /** Плейсхолдер наигранного времени (часы). */
    public static final String PLAYTIME = "%statistic_hours_played%";

    private static final Pattern EXPR = Pattern.compile("^(%[^%\\s]+%)\\s*(>=|<=|==|!=|>|<)\\s*(.+)$");
    private static final Pattern HOURS = Pattern.compile("^(\\d+(?:[.,]\\d+)?)\\s*(?:h|ч)?$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern NUMBER = Pattern.compile("^-?\\d+(?:\\.\\d+)?$");
    private static final Pattern UNRESOLVED = Pattern.compile("%[^%\\s]+%");

    private Conditions() {}

    /**
     * Разбирает условие в {плейсхолдер, оператор, значение}; null, если формат неверный.
     * Просто число («5», «5ч») означает «%statistic_hours_played% >= 5».
     */
    public static String[] parse(String in) {
        if (in == null) return null;
        String s = in.trim();
        if (s.isEmpty()) return null;
        Matcher h = HOURS.matcher(s);
        if (h.matches()) return new String[]{PLAYTIME, ">=", normalize(h.group(1))};
        Matcher m = EXPR.matcher(s);
        if (!m.matches()) return null;
        String value = m.group(3).trim();
        if (value.isEmpty() || "<>=!".indexOf(value.charAt(0)) >= 0) return null;
        return new String[]{m.group(1), m.group(2), value};
    }

    public static boolean validOperator(String op) {
        if (op == null) return false;
        return switch (op) {
            case ">=", "<=", "==", "!=", ">", "<" -> true;
            default -> false;
        };
    }

    /** "5.0" → "5", "2,5" → "2.5". */
    public static String normalize(String num) {
        try {
            return new BigDecimal(num.trim().replace(',', '.')).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException ex) {
            return num.trim();
        }
    }

    public static Double number(String s) {
        if (s == null) return null;
        String t = s.trim().replace(',', '.');
        if (!NUMBER.matcher(t).matches()) return null;
        try {
            return Double.valueOf(t);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** true, если в строке остался нераскрытый %плейсхолдер%. */
    public static boolean unresolved(String s) {
        return s == null || UNRESOLVED.matcher(s).find();
    }

    /** Числа сравниваются как числа, всё остальное — только == и != (без учёта регистра). */
    public static boolean compare(String current, String op, String value) {
        Double a = number(current);
        Double b = number(value);
        if (a != null && b != null) {
            int c = a.compareTo(b);
            return switch (op) {
                case ">=" -> c >= 0;
                case "<=" -> c <= 0;
                case ">" -> c > 0;
                case "<" -> c < 0;
                case "==" -> c == 0;
                case "!=" -> c != 0;
                default -> false;
            };
        }
        String x = current == null ? "" : current.trim();
        String y = value == null ? "" : value.trim();
        return switch (op) {
            case "==" -> x.equalsIgnoreCase(y);
            case "!=" -> !x.equalsIgnoreCase(y);
            default -> false;
        };
    }
}
