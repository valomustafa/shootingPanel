/*
 *   GEODynamics
 *   Author - James Suderman
 *   Date - 05/08/2018
 *
 *   This service communicates with Addressable Switches
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

public class SwitchService implements Runnable {
    private static Logger switchLogger = Logger.getLogger(SwitchService.class);
    private ObservableList<Switch> switches = FXCollections.observableArrayList();
    private ObservableList<String> addresses = FXCollections.observableArrayList();
    private AppController controller;
    private Serial serial = ShootingPanel.getSerial();
    private byte[] byteBuffer;
    private int timeout = 0, switchCount, volts, masterString = 0;
    private volatile boolean running = true;
    private boolean isPassed = true;
    private boolean commError = false;
    private boolean found = false;
    private boolean isSetting, notSet = true;
    private boolean inventoryOnly = true;
    private StringBuilder switchAddress = new StringBuilder();
    private String message = Constants.NONE, previousSwitch = Constants.BLANK_SPACE;
    private Switch currentSwitch = null;
    private Switch newSwitch = null;
    private double amps;

    public SwitchService(AppController controller) {
        this.controller = controller;
    }

    @Override
    @SuppressWarnings("Duplicates")
    public void run() {
        controller.setSwitchLinePower("OFF");

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            switchLogger.debug(e);
        }

        addresses.clear();
        switches.clear();

        switchCount = 0;

        try {
            serial.discardAll();
            serial.flush();
            if(serial.available() == 10) {
                serial.read(10);
            }
        } catch (IOException e) {
            switchLogger.debug(e);
        }

        // Powers the switch string on
        controller.setNinetyVoltLine("HIGH");
        controller.setSwitchLinePower("ON");

        try {
            Thread.sleep(800);

            volts = controller.getVolts();
            amps = controller.getAmps();
        } catch (InterruptedException e) {
            switchLogger.debug(e);
        }

        while (running) {
            timeout++;
            try {
                if(serial.available() >= 10) {
                    byteBuffer = serial.read(10);

                    switchAddress.append(String.format(Constants.HEX_FORMAT, byteBuffer[1]));
                    switchAddress.append(String.format(Constants.HEX_FORMAT, byteBuffer[2]));
                    switchAddress.append(String.format(Constants.HEX_FORMAT, byteBuffer[3]));

                    // Buffer to use to calculate checksum from
                    byte[] checkBuffer = {
                            byteBuffer[0], byteBuffer[1], byteBuffer[2],
                            byteBuffer[3], byteBuffer[4], byteBuffer[5],
                            byteBuffer[6], byteBuffer[7], byteBuffer[8]
                    };

                    // Validate the checksum to make sure the switch is valid
                    if(calcChksum(checkBuffer) == byteBuffer[9]) {
                        found = true; // Used to show that a switch has been read in and wont allow a bypass if a switch isn't read
                        timeout = 0;
                    } else {
                        commError = true;
                    }
                }
            } catch (IOException e) {
                switchLogger.debug(e);
            } catch (IndexOutOfBoundsException e) {
                stop();
                Platform.runLater(() -> {
                    controller.setHasPassed(false);
                    controller.setTestError(Constants.COMM_ERROR);
                });
                found = false;
            }

            try {
                // If a switch is read in
                if(found || commError) {
                    // Get the status bits for feed through and detonator voltage sense
                    boolean feedthrough = ((byteBuffer[8] >> 6) & 0b01) == 1;
                    boolean detonator = ((byteBuffer[8] >> 7) & 0b1) == 1;
                    boolean shorted = ((byteBuffer[8] >> 5) & 0b001) == 1;
                    int version = byteBuffer[7];

                    // Determines whether the switch is in the setting tool or release tool address range
                    isSetting = switchAddress.toString().compareTo("FFFC00") >= 0 && switchAddress.toString().compareTo("FFFCFF") <= 0;
                    boolean isRelease = switchAddress.toString().contains("FFFD");

                    if(isRelease && switchCount != 1) {
                        // This is the case when release tool is not the first switch in the string
                        Platform.runLater(() -> controller.setHasPassed(false));
                        isPassed = false;
                        message = Constants.RELEASE_TOOL;
                    }

                    newSwitch = new Switch(Integer.toString(switchCount),
                            String.format(Constants.HEX_FORMAT, byteBuffer[1]) + String.format(Constants.HEX_FORMAT, byteBuffer[2]) + String.format(Constants.HEX_FORMAT, byteBuffer[3]),
                            isSetting, isPassed, isRelease, message, feedthrough, detonator, shorted, version);

                    // Checks to see if it is the first uplink
                    if(byteBuffer[6] == 0) {
                        if(newSwitch.getAddress().equals("000000")) {
                            newSwitch.setPassed(false);
                            newSwitch.setMessage(Constants.BAD_ADDRESS);
                        }

                        if(commError) {
                            newSwitch.setPassed(false);
                            newSwitch.setMessage(Constants.COMM_ERROR);
                        }

                        if(switchCount > 0) {
                            previousSwitch = switches.get(switchCount - 1).getAddress();
                        }

                        if(version < 10) {
                            Thread.sleep(300);
                        }

                        if(newSwitch.getAddress().equals(previousSwitch) || addresses.contains(newSwitch.getAddress())) {
                            // If duplicate switches is detected, kill service and notify user of possible short
                            isPassed = false;
                            Platform.runLater(() -> {
                                controller.setHasPassed(false);
                                controller.setTestError(Constants.SHORT_CIRCUIT);
                            });
                        } else {
                            // Every switch above 1 bypasses, if switch 1 is shorted no bypass, first inventory allow bypass
                            if(switchCount >= 0 || !shorted || masterString == 0) {
                                // If straight inventory we want to BYPASS all the way down
                                if(inventoryOnly) {
                                    bypass(byteBuffer);
                                } else {
                                    // If we are doing something other than inventory, BYPASS only if it's not address in question
                                    if(!newSwitch.getAddress().equals(currentSwitch.getAddress())) {
                                        bypass(byteBuffer);
                                    }
                                }

                                switchCount++;

                                newSwitch.setOriginalIndex(Integer.toString(switchCount));
                                newSwitch.setId(Integer.toString(switchCount));
                                switches.add(newSwitch);
                                // Only add the switch if no duplicates
                                addresses.add(newSwitch.getAddress());

                                Platform.runLater(() -> {
                                    controller.incrementTotal(switchCount);

                                    if(newSwitch.isSetting()) {
                                        controller.setSettingSwitch(newSwitch, notSet);
                                    }
                                });
                            }
                        }

                        Thread.sleep(850);

                    }
                    found = false; // Reset so that another switch can be read
                }
            } catch (InterruptedException e) {
                switchLogger.debug(e);
            }

            commError = false;
            switchAddress.setLength(0);

            if(timeout == 20|| !isPassed) {
                stop();
            } else {
                isPassed = true; // Reset so that the next switch can potentially be passing
            }
        }

        reset();

        Platform.runLater(() -> {
            controller.addSwitches(switches);
            controller.showResult((isSetting && notSet),switchCount, volts, amps);
        });
    }

    public void stop() {
        running = false;
    }

    private void bypass(byte[] address) {
        byte[] bypass = {0x09, address[1], address[2], address[3], 0x13, (byte)0xE5, 0x00, 0x00, 0x00, 0x00};

        try {
            bypass[9] = calcChksum(bypass);
            serial.write(bypass);
        } catch (IOException e) {
            switchLogger.debug(e);
        }
    }

    private byte calcChksum(byte[] bytes) {
        int sum = 0;

        for (byte b : bytes) {
            sum += b;
        }

        return (byte) (sum % 256);
    }

    public void reset() {
        running = true;
        timeout = 0;
        switchAddress.setLength(0);
        commError = false;
        isPassed = true;
        found = false;
        previousSwitch = Constants.BLANK_SPACE;
        message = Constants.NONE;
        currentSwitch = null;
        inventoryOnly = true;
        newSwitch = null;

        try {
            serial.flush();
        } catch (Exception e) {
            switchLogger.debug(e);
        }
    }

    public void setCurrentSwitch(Switch currentSwitch, boolean inventoryOnly) {
        this.currentSwitch = currentSwitch;
        this.inventoryOnly = inventoryOnly;
    }

    public void setNotSet(boolean notSet) {
        this.notSet = notSet;
    }

    /**
     * masterString being the original gun string number
     * @param masterString which will be 0 for initial inventory, and non-zero for valid gun string
     */
    public void setMasterString(int masterString) {
        this.masterString = masterString;
    }
}
