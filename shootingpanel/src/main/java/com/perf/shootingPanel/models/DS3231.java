/*
 *   GEODynamics
 *   Author - James Suderman
 *   Date - --/--/----
 */

package com.perf.shootingPanel.models;

import com.pi4j.io.i2c.I2CDevice;

import java.io.IOException;
import java.util.Date;

public class DS3231 extends DS3231Registers {
    private static final int RTC_STRUCT_SIZE = 8;
    private static final byte[] RTC_STRUCT = new byte[RTC_STRUCT_SIZE];
    private final I2CDevice rtc;

    public DS3231(final I2CDevice rtc) {
        this.rtc = rtc;
    }

    public void readClock() throws IOException {
        rtc.read(0, RTC_STRUCT, 0, RTC_STRUCT_SIZE);
        setRegisters(RTC_STRUCT);
    }

    public void writeClock(Date date) throws IOException {
        set(date);
        final byte[] rawRegisterBytes = asByteArray();
        rtc.write(0, rawRegisterBytes, 0, RTC_STRUCT_SIZE);
    }
}
