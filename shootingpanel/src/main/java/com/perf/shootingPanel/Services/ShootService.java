/*
 *   GEODynamics
 *   Author - James Suderman
 *   Date - 04/09/2019
 *
 *   This service handles the 45 second countdown when shooting
 */

package com.perf.shootingPanel.Services;

import com.perf.shootingPanel.ShootingPanel;
import com.perf.shootingPanel.controllers.AppController;
import com.perf.shootingPanel.models.Switch;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.serial.*;
import javafx.application.Platform;
import org.apache.log4j.Logger;

import java.io.IOException;

public class ShootService implements Runnable {
    private static Logger shootLogger = Logger.getLogger(ShootService.class);
    private AppController controller;
    private final Serial serial = ShootingPanel.getSerial();
    private SerialConfig config = new SerialConfig();
    private GpioPinDigitalOutput firePowerPin = ShootingPanel.getFirePowerPin();
    private Switch currentSwitch, settingSwitch;
    private volatile boolean running = true;
    private int i;

    public ShootService(AppController controller, Switch settingSwitch) {
        this.controller = controller;
        this.settingSwitch = settingSwitch;
    }

    @Override
    @SuppressWarnings("Duplicates")
    public void run() {
        i = 45;

        try {
            serial.discardAll();
            serial.flush();
            if(serial.available() == 10) {
                serial.read(10);
            }
        } catch (IOException e) {
            shootLogger.debug(e);
        }

        // Powers the switch string on
        firePowerPin.setState(PinState.HIGH);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            shootLogger.debug(e);
        }

        // Send fire command
        fire(this.currentSwitch);

        while(running) {
            try {
                Thread.sleep(250);
                if(serial.available() > 0) {

                    byte[] byteBuffer = serial.read();

                    i = byteBuffer[8];

                    Platform.runLater(() -> {
                        controller.countdown(i + 1);

                        if(i == 44) {
                            controller.armPanel();
                        }
                    });

                    if(i == 1) {
                        Platform.runLater(() -> {
                            try {
                                Thread.sleep(250);
                                controller.countdown(1);
                                Thread.sleep(250);
                                controller.countdown(0);
                                running = false;
                            } catch (InterruptedException e) {
                                shootLogger.debug(e);
                            }
                        });
                    }
                }
            } catch (IOException | InterruptedException e) {
                shootLogger.debug(e);
            }
        }
    }

    public void setCurrentSwitch(Switch currentSwitch) {
        this.currentSwitch = currentSwitch;
    }

    private void fire(Switch currentSwitch) {
        byte[] fire = new byte[10];
        fire[0] = 0x09;
        fire[1] = (byte)Integer.parseInt(currentSwitch.getAddress().substring(0,2), 16);
        fire[2] = (byte)Integer.parseInt(currentSwitch.getAddress().substring(2,4), 16);
        fire[3] = (byte)Integer.parseInt(currentSwitch.getAddress().substring(4), 16);

        // If setting switch, use set plug command, else use fire command
        if(settingSwitch != null && settingSwitch.isSetting() && currentSwitch.getAddress().equals(settingSwitch.getAddress())) {
            fire[4] = (byte) 0x15;
            fire[5] = (byte) 0x63;
        } else {
            fire[4] = (byte) 0xEC;
            fire[5] = (byte) 0x64;
        }

        fire[6] = 0x00;
        fire[7] = 0x00;
        fire[8] = 0x00;
        fire[9] = 0x00;

        try {
            fire[9] = calcChksum(fire);
            serial.write(fire);
            Thread.sleep(500);
        } catch (IOException | InterruptedException e) {
            shootLogger.debug(e);
        }
    }

    private byte calcChksum(byte[] bytes) {
        int sum = 0;

        for (byte b : bytes) {
            sum += b;
        }

        return (byte) (sum % 256);
    }

    public void stop() {
        i = 45;
        running = false;
    }
}
