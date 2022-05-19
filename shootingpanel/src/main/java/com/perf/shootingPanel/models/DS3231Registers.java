/*
 *   GEODynamics
 *   Author - James Suderman
 *   Date - --/--/----
 */

package com.perf.shootingPanel.models;

import com.perf.shootingPanel.Constants;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static com.perf.shootingPanel.helpers.BCDKit.fromBCD;
import static com.perf.shootingPanel.helpers.BCDKit.toBCD;

public class DS3231Registers {
    private static final int maxSeconds = 59;
    private static final int maxMinutes = 59;

    private static final int minDayNumber = 1;
    private static final int maxDayNumber = 7;

    private static final int minDate = 1;
    private static final int maxDate = 31;

    private static final int minMonth = 1;
    private static final int maxMonth = 12;

    private static final int maxYear = 99;
    private final static byte ampmMask = 0x40;

    private byte bcdSeconds; // 00.59
    private byte bcdMinutes; // 00..59
    private byte bcdHours; // 00..23 (01..12 in AM/PM mode)
    private byte dayNumber; // 01.07
    private byte bcdDate; // 01..31
    private byte bcdMonth; // 01..12
    private byte bcdYear; // 00..99
    private byte controlByte;

    private Calendar calendar = Calendar.getInstance();


    public DS3231Registers() {
        dayNumber = bcdDate = bcdMonth = 1;
    }

    public void setRegisters(final byte[] bytes) {
        if (bytes.length < 8) {
            throw new IllegalArgumentException("Byte array length too short (needs to be at least 8): " + bytes.length);
        }

        int i = 0;

        bcdSeconds = bytes[i++];
        bcdMinutes = bytes[i++];
        bcdHours = bytes[i++];
        dayNumber = bytes[i++];
        bcdDate = bytes[i++];
        bcdMonth = bytes[i++];
        bcdYear = bytes[i++];
        controlByte = bytes[i++];
    }

    public byte[] asByteArray() {

        final byte[] bytes = new byte[8];
        bytes[0] = bcdSeconds;
        bytes[1] = bcdMinutes;
        bytes[2] = bcdHours;
        bytes[3] = dayNumber;
        bytes[4] = bcdDate;
        bytes[5] = bcdMonth;
        bytes[6] = bcdYear;
        bytes[7] = controlByte;

        return bytes;
    }

    public int getSeconds() {
        return fromBCD(bcdSeconds);
    }

    public void setSeconds(final int seconds) {
        bcdSeconds = toBCD(seconds);
    }

    public int getMinutes() {
        return fromBCD(bcdMinutes);
    }

    public void setMinutes(final int minutes) {
        bcdMinutes = toBCD(minutes);
    }

    public int getHours() {
        return fromBCD(bcdHours);
    }

    public void setHours(final int hours) {
        bcdHours = toBCD(hours);
    }

    public boolean isAMPMMode() {
        return (bcdHours & ampmMask) != 0;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(final int dayNumber) {
        this.dayNumber = (byte) dayNumber;
    }

    public int getDate() {
        return fromBCD(bcdDate);
    }

    public void setDate(final int date) {
        bcdDate = toBCD(date);
    }

    public int getMonth() {
        return fromBCD(bcdMonth) - 1;
    }

    public void setMonth(final int month) {
        bcdMonth = toBCD(month);
    }

    public int getYear() {
        return fromBCD(bcdYear) + 2000;
    }

    public void setYear(final int year) {
        bcdYear = toBCD(year);
    }

    public byte getControlByte() {
        return controlByte;
    }

    public void set(Date date) {
        calendar.setTime(date);
        int seconds = calendar.get(Calendar.SECOND);
        int minutes = calendar.get(Calendar.MINUTE);
        int hours = calendar.get(Calendar.HOUR_OF_DAY);

        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR) - 2000;

        setSeconds(seconds);
        setMinutes(minutes);
        setHours(hours);

        setDate(day);
        setMonth(month + 1);
        setYear(year);
    }

    public String asPrettyString() {
        SimpleDateFormat format = new SimpleDateFormat(Constants.DATE_FORMAT_SHORT);
        return formattedBy(format);
    }

    private String formattedBy(SimpleDateFormat dateFormat) {
        calendar.set(getYear(), getMonth(), getDate(), getHours(), getMinutes(), getSeconds());
        return dateFormat.format(calendar.getTime());
    }

    @Override
    public String toString() {
        return "DS3231[" + //
                hex(bcdSeconds) + hex(bcdMinutes) + hex(bcdHours) + "-" + //
                hex(dayNumber) + "-" + //
                hex(bcdDate) + hex(bcdMonth) + hex(bcdYear) + "-" + //
                hex(controlByte) + //
                "]";
    }

    private static String hex(final int byteValue) {
        return Integer.toHexString(byteValue | 0x80000000).substring(6, 8).toUpperCase();
    }
}
