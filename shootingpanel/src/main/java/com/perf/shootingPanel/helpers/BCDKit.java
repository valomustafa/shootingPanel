/*
 *   GEODynamics
 *   Author - James Suderman
 *   Date - --/--/----
 */

package com.perf.shootingPanel.helpers;

public class BCDKit {
    private BCDKit() {
    }

    public static byte toBCD(final int n) {
        return (byte) ((n / 10 * 16) + (n % 10));
    }

    public static byte fromBCD(final int bcdByte) {
        final int b = bcdByte & 0xFF;
        return (byte) ((b / 16 * 10) + (b % 16));
    }
}
