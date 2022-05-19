/*
 *   GEODynamics
 *   Author - James Suderman
 *   Date - 12/18/2017
 *
 *   This service works with the time service and handles setting the pi time and date
 */

package com.perf.shootingPanel.Services;

import com.perf.shootingPanel.Constants;
import com.perf.shootingPanel.ShootingPanel;
import com.perf.shootingPanel.controllers.AppController;
import com.pi4j.io.gpio.GpioController;
import org.apache.log4j.Logger;

public class DateService implements Runnable {
    private static Logger dateLogger = Logger.getLogger(DateService.class);
    private volatile boolean running = true;
    private AppController controller;

    public DateService(AppController controller) {
        this.controller = controller;
    }

    @Override
    public void run() {
        TimeService timeService = controller.getTimeService();

        // Comes back around every minute
        while(running) {
            try {
                // This thread is for the RTC to set the time on the pi at application start
                timeService.setMode(Constants.READ_MODE);
                Thread timeThread = new Thread(timeService);
                timeThread.setDaemon(true);
                timeThread.start();

                Thread.sleep(1000);
            } catch (Exception e) {
                dateLogger.debug(e);
            }
        }
    }

    public void stop() {
        running = false;
    }
}
