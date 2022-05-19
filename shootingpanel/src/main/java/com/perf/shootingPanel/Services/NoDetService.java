/*
 *   GEODynamics
 *   Author - James Suderman
 *   Date - 07/23/2019
 *
 *   This service blinks the total to give visual indication to the user that detonators are missing
 */

package com.perf.shootingPanel.Services;

import com.perf.shootingPanel.controllers.AppController;
import javafx.application.Platform;
import org.apache.log4j.Logger;

public class NoDetService implements Runnable {
    private static Logger notDetLogger = Logger.getLogger(NoDetService.class);
    private AppController controller;
    private volatile boolean running = true;
    private boolean blink = true;

    public NoDetService(AppController controller) {
        this.controller = controller;
    }

    @Override
    public void run() {
        while(running) {
            Platform.runLater(() -> {
                controller.changeTotalColor("red");
            });
            try {
                Thread.sleep(500);
                Platform.runLater(() -> {
                    blink = !blink;
                    controller.blinkTotal(blink);
                });
            } catch (InterruptedException e) {
                notDetLogger.debug(e);
            }
        }
    }

    public void stop() {
        running = false;
    }
    public void start() { running = true; }
}
