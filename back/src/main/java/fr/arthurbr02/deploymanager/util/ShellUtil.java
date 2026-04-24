package fr.arthurbr02.deploymanager.util;

public class ShellUtil {

    /**
     * Escapes a string for use in a single-quoted shell argument.
     * In POSIX shell, the only character that cannot appear inside a single-quoted string is the single quote itself.
     * To escape it, we close the single quote, add an escaped single quote, and reopen the single quote.
     * Example: "don't" becomes "'don'\''t'" (when wrapped in single quotes).
     * But since we are replacing in a command that might already have quotes or be passed as-is,
     * we will provide a version that can be safely embedded.
     */
    public static String escapeShell(String value) {
        if (value == null) return "";
        // Replace ' with '\''
        return value.replace("'", "'\\''");
    }

    /**
     * More restrictive sanitization for variables that should not contain shell metacharacters at all.
     */
    public static String sanitize(String value) {
        if (value == null) return "";
        // Remove characters that are often used for injection
        return value.replaceAll("[;&|><$`\\\\\"']", "");
    }
}
