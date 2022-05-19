/*
 *   GEODynamics
 *   Author - James Suderman
 *   Date - 05/20/2019
 *
 *   This service reads data from the ADC and communicates
 *   key position data, shot plot data, Voltage, and Current to the Application controller
 */

package com.perf.shootingPanel.Services;

import com.perf.shootingPanel.controllers.AppController;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import javafx.application.Platform;
import javafx.scene.chart.XYChart;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ADCService implements Runnable {
    private static Logger adcLogger = Logger.getLogger(ADCService.class);
    private AppController controller;
    private volatile boolean running = true;
    private volatile boolean shooting = false;
    private boolean initialization = true;
    private List<XYChart.Data<Number, Number>> currentData = new ArrayList<>();
    private List<XYChart.Data<Number, Number>> voltageData = new ArrayList<>();

    private final double VOLTAGE_FACTOR = .0103;
    private final double CURRENT_FACTOR = 0.000076;
    private final double KEY_FACTOR = 0.000125;

    private double current = 0.0;
    private double voltage = 0.0;
    private double keyPosition = 0.0;
    private double ccl = 0.0;
    private double configOffset = 0.0;
    private double lastPosition = 0.0;

    private double i = 0;

    private I2CBus bus;
    private I2CDevice device;

    {
        try {
            bus = I2CFactory.getInstance(I2CBus.BUS_1);
            device = bus.getDevice(0x48);
        } catch (I2CFactory.UnsupportedBusNumberException | IOException e) {
            adcLogger.debug(e);
        }
    }

    public ADCService(AppController controller) { this.controller = controller; }

    @Override
    public void run() {
        while(running) {
            try {
                byte[] config = {(byte) 0xC2, (byte) 0xE3};
                // Select configuration register
                // AINP = AIN0 and AINN = GND, +/- 2.048V, Continuous conversion mode, 128 SPS
                current = adcSample(config) * CURRENT_FACTOR;

                if(initialization) {
                    // Use this configOffset to compensate for part tolerance influence
                    configOffset = current;
                    // This if statement should only run once on service startup
                    initialization = false;
                }

                current -= configOffset;

                byte[] config1 = {(byte) 0xD2, (byte) 0xE3};
                // Select configuration register
                // AINP = AIN1 and AINN = GND, +/- 2.048V, Continuous conversion mode, 128 SPS
                voltage = adcSample(config1) * VOLTAGE_FACTOR;

                if(!shooting) {
                    byte[] config2 = {(byte) 0xE2, (byte) 0xE3};
                    // Select configuration register
                    // AINP = AIN2 and AINN = GND, +/- 2.048V, Continuous conversion mode, 128 SPS
                    ccl = adcSample(config2);

                    byte[] config3 = {(byte) 0xF2, (byte) 0xE3};
                    // Select configuration register
                    // AINP = AIN3 and AINN = GND, +/- 2.048V, Continuous conversion mode, 128 SPS
                    keyPosition = adcSample(config3) * KEY_FACTOR;

                    if(keyPosition <= lastPosition - .5 || keyPosition >= lastPosition + .5) {
                        Platform.runLater(() -> {
                            controller.handleKeyPosition(keyPosition);
                        });
                    }
                }

                if(current < 0) {
                    current = 0;
                }

                if(voltage < 0) {
                    voltage = 0;
                }

            } catch (Exception e) {
                adcLogger.debug(e);
            }

            if(shooting) {
                currentData.add(new XYChart.Data<>(i * 6, current));
                voltageData.add(new XYChart.Data<>(i * 6, voltage));
                i++;
            } else {
                if(i > 0) {
                    controller.setChart(new ArrayList<>(voltageData), new ArrayList<>(currentData));
                }

                i = 0;

                Platform.runLater(() -> {
                    controller.updateADC(current, (int)voltage, ccl);
                });
            }
        }
    }

    private double adcSample(byte[] config) {
        try {
            device.write(0x01, config, 0, 2);
            Thread.sleep(2);

            // Read 2 bytes of data
            // raw_adc msb, raw_adc lsb
            byte[] data = new byte[2];
            device.read(0x00, data, 0, 2);

            // Convert the data
            double raw_adc = ((data[0] & 0xFF) * 256) + (data[1] & 0xFF);
            if(raw_adc > 32767) {
                raw_adc -= 65535;
            }

            return raw_adc;
        } catch (IOException | InterruptedException e) {
            adcLogger.debug(e);
        }

        return 0.0;
    }

    public void setShooting(boolean shooting) {
        this.shooting = shooting;

        if(shooting) {
            currentData.clear();
            voltageData.clear();
        }
    }

    public void setLastPosition(double lastPosition) {
        this.lastPosition = lastPosition;
    }

    public double getCurrent() {
        return current;
    }
}
