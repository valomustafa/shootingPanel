/*
 *   GEODynamics
 *   Author - James Suderman
 *   Date - 08/21/2018
 *
 *   This service switches the hat board to the correct mode by adjusting the appropriate pin
 */

package com.perf.shootingPanel.Services;

import com.perf.shootingPanel.Constants;
import com.perf.shootingPanel.ShootingPanel;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import org.apache.log4j.Logger;

public class ModeService implements Runnable {
    private static Logger modeLogger = Logger.getLogger(ModeService.class);
    private GpioPinDigitalOutput hybridPin = ShootingPanel.getModeSelectPin();
    private GpioPinDigitalOutput resetPin = ShootingPanel.getResetPin();
    private String mode;

    public ModeService(String mode) {
        this.mode = mode;
    }

    @Override
    public void run() {
        try {

            resetPin.setState(PinState.LOW);

            Thread.sleep(1);

            if(mode.equals(Constants.ADDRESSABLE_MODE)) {
                hybridPin.setState(PinState.HIGH);
            } else if(mode.equals(Constants.HYBRID_MODE)) {
                hybridPin.setState(PinState.LOW);
            }

            Thread.sleep(1);

            resetPin.setState(PinState.HIGH);
        } catch (InterruptedException e) {
            modeLogger.debug(e);
        }
    }
}
