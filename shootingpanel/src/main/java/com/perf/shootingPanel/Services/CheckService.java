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
import com.pi4j.io.serial.Serial;
import javafx.application.Platform;
import org.apache.log4j.Logger;

import java.io.IOException;

public class CheckService implements Runnable {
    private static Logger checkLogger = Logger.getLogger(CheckService.class);
    private AppController controller;
    private volatile boolean running = true, countdown = false;
    private final Serial serial = ShootingPanel.getSerial();
    private GpioPinDigitalOutput ignitorModePin = ShootingPanel.getIgnitorModePin();
    private StringBuilder switchAddress = new StringBuilder();
    private byte[] byteBuffer;
    private int timeout = 0;
    private String mode;
    private boolean found = false;
    private byte[] headVoltageCommand = {0x09, (byte)0xFF, (byte)0xFD, 0x00, (byte)0xB2, (byte)0xD9, 0x00, 0x00, 0x00, 0x00};

    public CheckService(AppController controller) {
        this.controller = controller;
    }

    @Override
    @SuppressWarnings("Duplicates")
    public void run() {
        try {
            controller.setSwitchLinePower("OFF");

            Thread.sleep(50);

            if(mode.equals(Constants.HYBRID_MODE)) {
                controller.changeToMode(Constants.ADDRESSABLE_MODE);
            }

            // Powers the switch string on to 35 V
            controller.setNinetyVoltLine("LOW");
            controller.setSwitchLinePower("ON");

            Thread.sleep(800);

            headVoltageCommand[9] = calcChksum(headVoltageCommand);
            serial.write(headVoltageCommand);

            Thread.sleep(250);
        } catch (IOException | InterruptedException e) {
            checkLogger.debug(e);
        }

        while(running) {
            timeout++;
            try {
                if(serial.available() >= 10) {
                    byteBuffer = serial.read(10);

                    // Buffer to use to calculate checksum from
                    byte[] checkBuffer = {
                            byteBuffer[0], byteBuffer[1], byteBuffer[2],
                            byteBuffer[3], byteBuffer[4], byteBuffer[5],
                            byteBuffer[6], byteBuffer[7], byteBuffer[8]
                    };

                    switchAddress.append(String.format(Constants.HEX_FORMAT, byteBuffer[1]));
                    switchAddress.append(String.format(Constants.HEX_FORMAT, byteBuffer[2]));
                    switchAddress.append(String.format(Constants.HEX_FORMAT, byteBuffer[3]));

                    // Validate the checksum to make sure the switch is valid
                    if(calcChksum(checkBuffer) == byteBuffer[9] && switchAddress.toString().contains("FFFD")) {
                        found = true;
                        timeout = 6;
                        stop();
                    } else {
                        found = false;
                    }
                }
                Thread.sleep(250);
                switchAddress.setLength(0);
            } catch (IOException | InterruptedException e) {
                checkLogger.debug(e);
            }

            if(timeout == 6) {
                Platform.runLater(() -> {
                    controller.showWrtStatus(found);
                });

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
        timeout = 0;
        countdown = false;
        running = true;

        try {
            serial.flush();
        } catch (Exception e) {
            checkLogger.debug(e);
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
