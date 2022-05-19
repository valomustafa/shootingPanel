/*
 *   GEODynamics
 *   Author - James Suderman
 *   Date - 04/09/2019
 *
 *   This service continuously monitors for a short circuit and signals to handle it when it occurs
 */

package com.perf.shootingPanel.Services;

import com.perf.shootingPanel.ShootingPanel;
import com.perf.shootingPanel.controllers.AppController;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import javafx.application.Platform;

public class ShortService implements Runnable {
    private GpioPinDigitalInput shortPin = ShootingPanel.getShortPin();
    private volatile boolean running = true, shooting = false, firstRun = true, bypass = false;
    private AppController controller;

    public ShortService(AppController controller) { this.controller = controller; }

    @Override
    public void run() {
        while(running) {
            if(!shooting && !firstRun && !bypass) {
                if(shortPin.isHigh()) {
                    Platform.runLater(() -> controller.handleShort());
                }
            }
        }
    }

    public void setShooting(boolean shooting) {
       this.shooting = shooting;
    }
    public void setFirstRun(boolean firstRun) { this.firstRun = firstRun; }
    public void setBypass(boolean bypass) { this.bypass = bypass; }
}
