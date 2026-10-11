package japlearn.demo.Service;

import java.util.regex.Pattern;

/**
 * Plain-text rules for names and passwords. Emojis, symbols, invisible characters and
 * markup are rejected so every account holds a real, readable name and a typeable password.
 */
public final class TextInput {
    private TextInput() {}

    public static final String NAME_RULE = "Names can only use letters, spaces, hyphens (-), apostrophes (') and periods. Emojis, numbers and symbols are not allowed.";
    public static final String PASSWORD_RULE = "Passwords must be 8 to 128 characters and use only letters, numbers and keyboard symbols. Emojis and other special characters are not allowed.";

    /** Letters in any language (Latin, Japanese, ...) with spaces, hyphens, apostrophes, periods and the Japanese middle dot between them. */
    private static final Pattern NAME = Pattern.compile("^[\\p{L}\\p{M}](?:[\\p{L}\\p{M} .'’\\-・]{0,58}[\\p{L}\\p{M}.])?$");
    /** Printable keyboard characters only (ASCII 0x20-0x7E). */
    private static final Pattern PASSWORD = Pattern.compile("^[\\x20-\\x7E]{8,128}$");

    /** Returns the cleaned name (trimmed, single spaces) or null when it is not a plain name. */
    public static String name(Object raw) {
        if (!(raw instanceof String s)) return null;
        String v = s.trim().replaceAll("\\s+", " ");
        return NAME.matcher(v).matches() ? v : null;
    }

    public static boolean validPassword(Object raw) {
        return raw instanceof String s && PASSWORD.matcher(s).matches();
    }
}
