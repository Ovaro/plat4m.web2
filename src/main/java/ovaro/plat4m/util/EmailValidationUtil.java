package ovaro.plat4m.util;

import java.util.regex.Pattern;

public final class EmailValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private EmailValidationUtil() {}

    public static boolean isEmail(String value) {
        return value != null && EMAIL_PATTERN.matcher(value).matches();
    }
}
