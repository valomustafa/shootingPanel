/*
 *   GEODynamics
 *   Author - James Suderman
 *   Date - 05/08/2018
 *
 *   This service communicates with Hybrid Switches
 */

package com.perf.shootingPanel.Services;

import com.perf.shootingPanel.Constants;
import com.perf.shootingPanel.ShootingPanel;
import com.perf.shootingPanel.controllers.AppController;
import com.perf.shootingPanel.models.Switch;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.serial.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.log4j.Logger;

import java.io.IOException;

public class HybridService implements Runnable {
    private static Logger hybridLogger = Logger.getLogger(HybridService.class);
    private final Serial serial = ShootingPanel.getSerial();
    private ObservableList<Switch> switches = FXCollections.observableArrayList();
    private SerialConfig config = new SerialConfig();
    private AppController controller;
    private byte[] byteBuffer;
    private double amps;
    private int switchCount, timeout, volts;
    private boolean isSetPresent, finalSwitch;
    private volatile boolean running = true, firing = false;

    public HybridService(AppController controller) {
        this.controller = controller;
    }

    @Override
    @SuppressWarnings("Duplicates")
    public void run() {
        controller.setSwitchLinePower("OFF");

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            hybridLogger.debug(e);
        }

        Switch newSwitch = null;
        isSetPresent = false;
        finalSwitch = false;
        switchCount = 0;
        timeout = 0;

        try {
            serial.discardAll();
            serial.flush();
            if(serial.available() == 10) {
                serial.read(10);
            }

            controller.setSwitchLinePower("ON");

            Thread.sleep(50);

            volts = controller.getVolts();
            amps = controller.getAmps();
        } catch (InterruptedException | IOException e) {
            hybridLogger.debug(e);
        }


        while(running) {
            timeout++;
            try {
                Thread.sleep(200);
                if(serial.available() > 0) {
                    byteBuffer = serial.read(10);

                    if(byteBuffer[0] >= 19 && byteBuffer[0] <= 21) {
                        // INLINE SWITCH
                        switchCount++;
                        newSwitch = new Switch(Integer.toString(switchCount), Constants.BLANK_SPACE,
                                false, true, false, Constants.BLANK_SPACE, true, true, false, 0);
                        timeout = 0;
                        switches.add(newSwitch);
                        isSetPresent = false;
                        Platform.runLater(() -> controller.incrementTotal(switchCount));
                    } else if(byteBuffer[0] >= 9 && byteBuffer[0] <= 11) {
                        // LAST SWITCH
                        switchCount++;
                        newSwitch = new Switch(Integer.toString(switchCount), Constants.BLANK_SPACE,
                                false, true, false, Constants.BLANK_SPACE, true, true, false, 0);
                        timeout = 0;

                        switches.add(newSwitch);
                        Platform.runLater(() -> controller.incrementTotal(switchCount));
                        finalSwitch = true;
                        isSetPresent = false;

                        Thread.sleep(200);
                        if(serial.available() > 0) {
                            byteBuffer = serial.read(10);
                            if(byteBuffer[0] >= 14 && byteBuffer[0] <= 16) {
                                // SET SWITCH
                                switchCount++;
                                isSetPresent = true;
                                newSwitch = new Switch(Integer.toString(switchCount), Constants.SET_FIRE_ADDRESS,
                                        isSetPresent, true, false, Constants.BLANK_SPACE, true, true, false, 0);
                                switches.add(newSwitch);
                                Switch finalNewSwitch = newSwitch;
                                Platform.runLater(() -> {
                                    controller.incrementTotal(switchCount);
                                    controller.setSettingSwitch(finalNewSwitch, isSetPresent);
                                });
                            }
                        }
                        stop();
                    }
                }
            } catch (IOException | InterruptedException e) {
                hybridLogger.debug(e);
            } catch (IndexOutOfBoundsException e) {
                stop();
                controller.setHasPassed(false);
                controller.setTestError(Constants.COMM_ERROR);
            }

            if(timeout == 4) {
                stop();
            }
        }

        if(!finalSwitch && switchCount != 0) {
            controller.setHasPassed(false);
            controller.setTestError(Constants.NO_BOTTOM_SWITCH);
            controller.setSwitchLinePower("OFF");
        } else {
            controller.setHasPassed(true);
        }

        if(firing && (isSetPresent || finalSwitch)) {
            controller.armPanel();
        }

        Platform.runLater(() -> {
            controller.addSwitches(switches);
            controller.showResult(isSetPresent, switchCount, volts, amps);
        });
    }

    public void setFiring(boolean firing) {
        this.firing = firing;
    }

    private void stop() {
        running = false;
    }
}
