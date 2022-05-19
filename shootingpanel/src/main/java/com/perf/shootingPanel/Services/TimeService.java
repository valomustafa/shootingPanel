/*
 *   GEODynamics
 *   Author - James Suderman
 *   Date - 05/08/2018
 *
 *   This service works with the Date service to make sure the date and time on the pi are correct
 */

package com.perf.shootingPanel.Services;

import com.perf.shootingPanel.Constants;
import com.perf.shootingPanel.ShootingPanel;
import com.perf.shootingPanel.controllers.AppController;
import com.perf.shootingPanel.models.DS3231;
import javafx.application.Platform;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class TimeService implements Runnable {
    private static Logger timeLogger = Logger.getLogger(TimeService.class);
    private AppController controller;
    private SimpleDateFormat formatter = new SimpleDateFormat(Constants.DATE_FORMAT_SHORT);
    private DS3231 rtc = ShootingPanel.getRtc();
    private Date date, newDate;
    private String mode;
    private boolean dateChanged = false;

    public TimeService(AppController controller) { this.controller = controller;}

    @Override
    public void run() {
        try {
            if(mode.equals(Constants.WRITE_MODE)) {
                write();
                Thread.sleep(2000);
                read();
            } else if(mode.equals(Constants.READ_MODE)) {
                read();
            }
        } catch (InterruptedException e) {
            timeLogger.debug(e);
        }
    }

    /**
     * @param mode - determines if the service will READ or WRITE
     */
    public void setMode(String mode) {
        this.mode = mode;
    }

    /**
     * @param date - This is what actually gets set in the RTC
     */
    public void setDate(Date date) { this.date = date; }

    /**
     * Writes the value from the date variable to the RTC
     * Saves RTC time to Pi system
     */
    private void write() {
        SimpleDateFormat piDateFormatter = new SimpleDateFormat(Constants.PI_DATE_FORMAT);

        try {
            ProcessBuilder setPiTime = new ProcessBuilder(Constants.SUPER_USER, Constants.DATE, Constants.SAVE_FLAG, piDateFormatter.format(date));
            setPiTime.start();

            Thread.sleep(1000);

            ProcessBuilder saveTime = new ProcessBuilder(Constants.SUPER_USER, Constants.CLOCK, Constants.WRITE_FLAG);
            saveTime.start();

            Thread.sleep(1000);

        } catch (IOException | InterruptedException e) {
            timeLogger.debug(e);
        }

        dateChanged = true;
    }

    /**
     * Read the clock time from the pi as string
     * Parses the string into a date
     */
    private void read() {
        LocalDateTime now = LocalDateTime.now();
        Instant instant = now.atZone(ZoneId.systemDefault()).toInstant();
        newDate = Date.from(instant);

        Platform.runLater(() -> controller.setDate(newDate, dateChanged));
    }
}
