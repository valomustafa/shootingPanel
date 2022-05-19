/*
 *   GEODynamics
 *   Author - James Suderman
 *   Date - 12/18/2017
 *
 *   App start
 */

package com.perf.shootingPanel;

import com.perf.shootingPanel.controllers.AppController;
import com.perf.shootingPanel.models.DS3231;
import com.pi4j.io.gpio.*;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.serial.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.apache.log4j.Logger;

import java.io.IOException;

public class ShootingPanel extends Application {
    private static final Logger panelLogger = Logger.getLogger(ShootingPanel.class);
    private static String os;
    private static Serial serial = SerialFactory.createInstance();
    private static SerialConfig config = new SerialConfig();
    // GPIO setup
    private static final GpioController gpio = GpioFactory.getInstance();
    // firePowerPin powers the line for switch comms - works in conjunction with sixtyVoltPin
    private static final GpioPinDigitalOutput firePowerPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_25, Constants.FIRE_POWER_PIN, PinState.LOW);
    private static final GpioPinDigitalInput shortPin = gpio.provisionDigitalInputPin(RaspiPin.GPIO_01, Constants.SHORT_PIN, PinPullResistance.PULL_DOWN);
    // armPin flags PIC to begin shoot sequence
    private static final GpioPinDigitalOutput armPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_11, Constants.ARM_PIN, PinState.LOW);
    // modeSelectPin LOW:HYBRID HIGH:ADDRESSABLE
    private static final GpioPinDigitalOutput modeSelectPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_26, Constants.HYBRID_PIN, PinState.LOW);
    private static final GpioPinDigitalOutput resetPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_27, Constants.RESET_PIN, PinState.HIGH);
    // shootPin HIGH: shooting LOW: not shooting
    private static final GpioPinDigitalInput shootPin = gpio.provisionDigitalInputPin(RaspiPin.GPIO_10, Constants.SHOOT_PIN, PinPullResistance.PULL_DOWN);
    // sixtyVoltPin LOW:35 volts HIGH:90 volts
    private static final GpioPinDigitalOutput ninetyVoltPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_29, Constants.SIXTY_VOLT_PIN, PinState.HIGH);
    // ignitorModePin LOW:Det Mode  HIGH:Ignitor Mode (WRT & Ignitors)
    private static final GpioPinDigitalOutput ignitorModePin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05, Constants.IGNITOR_PIN, PinState.LOW);

    // I2C setup
    private static I2CBus i2c = null;
    private static I2CDevice device = null;
    private static DS3231 rtc = null;
    static {
        try {
            i2c = I2CFactory.getInstance(I2CBus.BUS_1);
            device = i2c.getDevice(0x68);
            rtc = new DS3231(device);
        } catch (I2CFactory.UnsupportedBusNumberException | IOException e) {
            panelLogger.debug(e);
        }
    }

    // Serial Setup
    static {
        try {
            config.device(SerialPort.getDefaultPort())
                    .baud(Baud._9600)
                    .dataBits(DataBits._8)
                    .parity(Parity.NONE)
                    .stopBits(StopBits._1)
                    .flowControl(FlowControl.NONE);
        } catch (IOException | InterruptedException e) {
            panelLogger.debug(e);
        } finally {
            try {
                if(!serial.isOpen()) {
                    serial.open(config);
                }

                serial.discardAll();
                serial.flush();
            } catch (IOException e) {
                panelLogger.debug(e);
            }
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Rectangle2D screenBounds = Screen.getPrimary().getBounds();
        FXMLLoader appLoader = new FXMLLoader(getClass().getResource(Constants.APPLICATION_VIEW));
        Parent root = appLoader.load();
        AppController appController = appLoader.getController();
        Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());

        scene.setCursor(Cursor.NONE);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        os = System.getProperty(Constants.OS_PROPERTY);
        launch(args);
    }
    
    /**
     * Global method to shutdown and exit the application completely
     */
    public static void shutdown() {
        exit();
        try {
            Process shutdown = Runtime.getRuntime().exec(Constants.SHUTDOWN_COMMAND);
        } catch (IOException e) {
            panelLogger.debug(e);
        }
    }

    /**
     * Global method to cleanup the gpio before shutdown
     */
    public static void exit() {
        gpio.shutdown();
        Platform.exit();
    }

    public static String getOS() {
        return os;
    }
    public static GpioPinDigitalOutput getFirePowerPin() {
        return firePowerPin;
    }
    public static GpioPinDigitalInput getShortPin() { return shortPin; }
    public static GpioPinDigitalOutput getArmPin() { return armPin; }
    public static GpioPinDigitalInput getShootPin() { return shootPin; }
    public static GpioPinDigitalOutput getResetPin() { return resetPin; }
    public static GpioPinDigitalOutput getModeSelectPin() { return modeSelectPin; }
    public static GpioPinDigitalOutput getNinetyVoltPin() { return ninetyVoltPin; }
    public static GpioPinDigitalOutput getIgnitorModePin() { return ignitorModePin; }

    public static Serial getSerial() { return serial; }
    public static DS3231 getRtc() { return rtc; }
}
