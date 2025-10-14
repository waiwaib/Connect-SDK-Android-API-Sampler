package org.apache.http.conn.util;

import java.util.regex.Pattern;

/**
 * Minimal backport of InetAddressUtils for legacy libraries that depend on it.
 * (Used by Amazon WhisperLink inside Connect SDK)
 */
public class InetAddressUtils {
    private static final Pattern IPV4_PATTERN =
            Pattern.compile("^(25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})(\\.(25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})){3}$");

    private static final Pattern IPV6_STD_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{1,4}(:[0-9a-fA-F]{1,4}){7}$");

    private static final Pattern IPV6_HEX_COMPRESSED_PATTERN =
            Pattern.compile("^(([0-9A-Fa-f]{1,4})(:|::)){1,7}([0-9A-Fa-f]{1,4})?$");

    public static boolean isIPv4Address(String input) {
        return input != null && IPV4_PATTERN.matcher(input).matches();
    }

    public static boolean isIPv6StdAddress(String input) {
        return input != null && IPV6_STD_PATTERN.matcher(input).matches();
    }

    public static boolean isIPv6HexCompressedAddress(String input) {
        return input != null && IPV6_HEX_COMPRESSED_PATTERN.matcher(input).matches();
    }

    public static boolean isIPv6Address(String input) {
        return isIPv6StdAddress(input) || isIPv6HexCompressedAddress(input);
    }
}
