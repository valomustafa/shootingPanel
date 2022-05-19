/*
 *   GEODynamics
 *   Author - James Suderman
 *   Date - 06/03/2019
 *
 *   This service will poll the Release tool and check existence and also release
 */

package com.perf.shootingPanel.Services;

import com.perf.shootingPanel.Constants;
import com.perf.shootingPanel.ShootingPanel;
import com.perf.shootingPanel.controllers.AppController;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.serial.*;
import javafx.application.Platform;
import org.apache.log4j.Logger;

import java.io.IOException;

public class ReleaseService implements Runnable {
    private static Logger releaseLogger = Logger.getLogger(ReleaseService.class);
    private AppController controller;
    private volatile boolean running = true, countdown = false;
    private final Serial serial = ShootingPanel.getSerial();
    private GpioPinDigitalOutput ignitorModePin = ShootingPanel.getIgnitorModePin();
    private StringBuilder switchAddress = new StringBuilder();
    private byte[] byteBuffer;
    private int timeout = 0;
    private String mode;
    private byte[] releaseCommand = {0x09, (byte)0xFF, (byte)0xFD, 0x00, (byte)0x49, (byte)0x73, 0x00, 0x00, 0x00, 0x00};

    public ReleaseService(AppController controller) {
        this.controller = controller;
    }

    @Override
    @SuppressWarnings("Duplicates")
    public void run() {
        try {
            Platform.runLater(() -> {
                controller.releaseCountdown("WAIT");
                controller.setSwitchLinePower("OFF");
            });

            Thread.sleep(50);

            if(mode.equals(Constants.HYBRID_MODE)) {
                controller.changeToMode(Constants.ADDRESSABLE_MODE);
            }

            // Powers the switch string on to 35 V
            controller.setNinetyVoltLine("LOW");
            controller.setSwitchLinePower("ON");

            Thread.sleep(250);

            releaseCommand[9] = calcChksum(releaseCommand);
            serial.write(releaseCommand);

            Thread.sleep(800);
        } catch (IOException | InterruptedException e) {
            releaseLogger.debug(e);
        }

        while(!Thread.interrupted() && running) {
            timeout++;
            try {
                if(serial.available() >= 10) {
                    byteBuffer = serial.read(10);

                    switchAddress.append(String.format(Constants.HEX_FORMAT, byteBuffer[1]));
                    switchAddress.append(String.format(Constants.HEX_FORMAT, byteBuffer[2]));
                    switchAddress.append(String.format(Constants.HEX_FORMAT, byteBuffer[3]));

                    if(switchAddress.toString().contains("FFFD")) {

                        ignitorModePin.setState(PinState.HIGH);
                        countdown = true;
                        Platform.runLater(() -> {
                            controller.armPanel();
                        });
                        while(countdown) {
                            byteBuffer = serial.read(10);

                            Platform.runLater(() -> {
                                controller.releaseCountdown(Integer.toString(byteBuffer[8] + 1));
                            });

                            //Thread.sleep(250);
                            if(byteBuffer[8] == 0) {
                                countdown = false;
                            }
                        }

                        timeout = 20;
                    }
                }
                Thread.sleep(250);
                switchAddress.setLength(0);
            } catch (IOException | InterruptedException e) {
                releaseLogger.debug(e);
                stop();
                reset();
            }

            if(timeout == 20) {
                if(mode.equals(Constants.HYBRID_MODE)) {
                    Platform.runLater(() -> {
                        controller.changeToMode(Constants.HYBRID_MODE);
                    });
                }

                stop();
            }
        }

        reset();
    }

    public void reset() {
        controller.setSwitchLinePower("OFF");
        controller.setNinetyVoltLine("HIGH");
        ignitorModePin.setState(PinState.LOW);
        switchAddress.setLength(0);
        countdown = false;
        timeout = 0;
        running = true;

        try {
            serial.flush();
        } catch (Exception e) {
            releaseLogger.debug(e);
        }
    }

    public void stop() {
        running = false;
    }

    private byte calcChksum(byte[] bytes) {
        int sum = 0;

        for (byte b : bytes) {
            sum += b;
        }

        return (byte) (sum % 256);
    }

    public void setMode(String mode) { this.mode = mode; }
}
