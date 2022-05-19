/*
 *   GEODynamics
 *   Author - James Suderman
 *   Date - 05/04/2018
 *
 *   This service is responsible for switch communication to change the address of a switch
 */

package com.perf.shootingPanel.Services;

import com.perf.shootingPanel.Constants;
import com.perf.shootingPanel.ShootingPanel;
import com.perf.shootingPanel.controllers.AppController;
import com.perf.shootingPanel.models.Switch;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.serial.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;

import java.io.IOException;

public class ChangeAddressService implements Runnable {
    private static Logger changeAddressLogger = Logger.getLogger(ChangeAddressService.class);
    private AppController controller;
    private Serial serial = ShootingPanel.getSerial();
    private ObservableList<Switch> switches = FXCollections.observableArrayList();
    private int switchCount;
    private byte[] byteBuffer;
    private byte[] address = new byte[3];
    private volatile boolean running = true;
    private boolean found = false;
    private GpioPinDigitalInput shortPin = ShootingPanel.getShortPin();
    private SerialConfig config = new SerialConfig();
    private StringBuilder switchAddress = new StringBuilder();
    private String addressToChange, addressToChangeTo;
    private int timeout = 0, newFoundSwitch = 0, switchIndex = 0;
    private boolean isPassed = true;
    private String previousSwitch = Constants.BLANK_SPACE;

    public ChangeAddressService(AppController controller) {
        this.controller = controller;
    }

    @Override
    @SuppressWarnings("Duplicates")
    public void run() {
        switchCount = 0;

        // Listener to handle if a short is detected
        shortPin.addListener((GpioPinListenerDigital) event -> {
            if(event.getState() == PinState.HIGH) {
                Platform.runLater(() -> controller.handleShort());
            }
        });

        try {
            config.device(SerialPort.getDefaultPort())
                    .baud(Baud._9600)
                    .dataBits(DataBits._8)
                    .parity(Parity.NONE)
                    .stopBits(StopBits._1)
                    .flowControl(FlowControl.NONE);
        } catch (IOException | InterruptedException e) {
            changeAddressLogger.debug(e);
        } finally {
            try {
                serial.open(config);
            } catch (IOException e) {
                changeAddressLogger.debug(e);
            }
        }

        controller.setSwitchLinePower("ON");

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            changeAddressLogger.debug(e);
        }

        while (running) {
            timeout++;

            try {
                Thread.sleep(250);
                if(serial.isOpen()) {

                    if(serial.available() > 0) {

                        byteBuffer = serial.read();

                        switchAddress.append(String.format(Constants.HEX_FORMAT, byteBuffer[1]));
                        switchAddress.append(String.format(Constants.HEX_FORMAT, byteBuffer[2]));
                        switchAddress.append(String.format(Constants.HEX_FORMAT, byteBuffer[3]));

                        address[0] = byteBuffer[1];
                        address[1] = byteBuffer[2];
                        address[2] = byteBuffer[3];

                        // Buffer to use to calculate checksum from
                        byte[] checkBuffer = {byteBuffer[0], byteBuffer[1], byteBuffer[2],
                                byteBuffer[3], byteBuffer[4], byteBuffer[5],
                                byteBuffer[6], byteBuffer[7], byteBuffer[8]};

                        // Validate the checksum to make sure the switch is valid
                        if(calcChksum(checkBuffer) == byteBuffer[9]) {
                            found = true; // Used to show that a switch has been read in and wont allow a bypass if a switch isn't read
                            timeout = 0;

                        }
                    }
                }
            } catch (IOException | InterruptedException e) {
                changeAddressLogger.debug(e);
            }

            // If a switch is read in
            if (found) {

                newFoundSwitch++;

                Switch newSwitch = new Switch(Integer.toString(switchCount),
                        String.format(Constants.HEX_FORMAT, byteBuffer[1]) + String.format(Constants.HEX_FORMAT, byteBuffer[2]) + String.format(Constants.HEX_FORMAT, byteBuffer[3]),
                        false, isPassed, false, Constants.BLANK_SPACE, false, false, false, 0);

                if(newFoundSwitch % 2 == 0) {
                    switchCount++;

                    if((addressToChange.compareTo(switchAddress.toString()) == 0) && (switchCount == switchIndex)) {
                        changeAddress(addressToChange, addressToChangeTo);
                        isPassed = false;
                    }

                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        changeAddressLogger.debug(e);
                    }

                    switches.add(newSwitch);
                    bypass(byteBuffer);
                }

                found = false; // Reset so that another switch can be read

            }

            switchAddress.setLength(0);

            if (timeout == 5 || !isPassed) {
                stop();
            } else {
                isPassed = true; // Reset so that the next switch can potentially be passing
            }
        }
        shortPin.removeAllListeners();
        reset();

        controller.setSwitchLinePower("OFF");
    }

    public void stop() {
        running = false;

        try {
            if(serial.isOpen()) {
                serial.close();
            }
        } catch (IOException e) {
            changeAddressLogger.debug(e);
        }
    }

    private void bypass(byte[] address) {
        byte[] bypass = {0x09, address[1], address[2], address[3], 0x13, (byte)0xE5, 0x00, 0x00, 0x00, 0x00};

        try {
            bypass[9] = calcChksum(bypass);
            serial.write(bypass);
        } catch (IOException e) {
            changeAddressLogger.debug(e);
        }
    }

    private void sleep(byte[] address) {
        byte[] sleep = {0x09, address[0], address[1], address[2], (byte) 0xA9, (byte) 0x43, 0x00, 0x00, 0x00, 0x00};

        try {
            sleep[9] = calcChksum(sleep);
            serial.write(sleep);
        } catch (IOException e) {
            changeAddressLogger.debug(e);
        }
    }

    private byte calcChksum(byte[] bytes) {
        int sum = 0;

        for (byte b : bytes) {
            sum += b;
        }

        return (byte) (sum % 256);
    }

    private void changeAddress(String addressToChange, String addressToChangeTo) {
        try {
            byte[] oldAddress = Hex.decodeHex(addressToChange.toCharArray());
            byte[] newAddress = Hex.decodeHex(addressToChangeTo.toCharArray());

            byte[] changeAddress = {0x09, oldAddress[0], oldAddress[1], oldAddress[2], (byte) 0x0D, (byte) 0x80, newAddress[0], newAddress[1], newAddress[2], 0x00};

            changeAddress[9] = calcChksum(changeAddress);

            serial.write(changeAddress);

        } catch (DecoderException | IOException e) {
            changeAddressLogger.debug(e);
        }
    }

    public void setAddressToChange(String address) {
        addressToChange = address;
    }

    public void setAddressToChangeTo(String address) {
        addressToChangeTo = address;
    }

    public void setSwitchIndex(int index) { this.switchIndex = index; }

    public void reset() {
        running = true;
        timeout = 0;
        switchAddress.setLength(0);
        isPassed = true;
        found = false;
        newFoundSwitch = 0;
        switchIndex = 0;

        if(switches != null) {
            switches.removeAll();
        }
    }
}
