/*
 *   GEODynamics
 *   Author - James Suderman
 *   Date - 12/18/2017
 */

package com.perf.shootingPanel.controllers;

import com.perf.shootingPanel.Constants;
import com.perf.shootingPanel.ShootingPanel;
import com.perf.shootingPanel.Services.*;
import com.perf.shootingPanel.Services.ADCService;
import com.perf.shootingPanel.helpers.PropertyHelper;
import com.perf.shootingPanel.models.Job;
import com.perf.shootingPanel.models.Switch;
import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import eu.hansolo.medusa.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class AppController implements Initializable {
    private static final Logger appLogger = Logger.getLogger(AppController.class);

    @FXML
    private AnchorPane rootPane, totalPane, startPane, saveBackground, gearBackground, testPane, setTimePane,
            colorBackground, navPane, jobInfoPane, fileChooserPane, testContentPane, cclPane, safePane,
            wrtPane, releaseLabelPane, setFirePane, checkFirePane, gunCountPane, updatePane, updateMessagePane,
            postReleasePane, deletePane;
    @FXML
    private StackPane meterPane;
    @FXML
    private GridPane minutePane, dayPane, monthPane, yearPane, hourPane, ampmPane;
    @FXML
    private Label infoAddressLabel, date, voltLabel, ampLabel, totalLabel, messageLabel, ipLabel,
            countdownLabel, companyInfoLabel, wellInfoLabel, jobInfoLabel, totalSwitchLabel, releaseInstructionLabel,
            checkFireInstructionLabel, updateMessage, startSafeLabel, gunCountInstructionLabel, deleteInstructionLabel;
    @FXML
    private ListView<Switch> switchList;
    @FXML
    LineChart<Number, Number> voltageChart, currentChart, checkVoltageChart, checkCurrentChart;
    @FXML
    NumberAxis vAxis, cAxis, t1Axis, t2Axis, checkVAxis, checkCAxis, checkT1Axis, checkT2Axis;
    @FXML
    private ProgressBar saveProgressBar, updateProgressBar;
    @FXML
    private Button dayButton, weekButton, monthButton, hybridButton, addressableButton, colorButton, fireButton,
            inventoryButton, armButton, releaseConfirmButton, releaseCancelButton, newJobButton, resumeJobButton,
            updateButton, exportButton, exitButton, fileOKButton, jobInfoButton, checkReleaseButton, checkFireButton,
            gunCountResetButton, deleteFileButton, checkConfirmButton, plugYesButton, plugNoButton;
    @FXML
    private TextField monthField, dayField, yearField, hourField, minuteField, ampmField, jobNameField, companyField,
            wellField, engineerField, stageField;
    @FXML
    private TreeView<String> fileTree;
    @FXML
    private ScrollPane fileScrollPane;
    @FXML
    private ImageView logoImage, statusIcon, qrView, indicator;

    private Path appPath, targetPath;   // FIle directory - could be incoming or outgoing based on usage
    private ObservableList<Switch> switches = FXCollections.observableArrayList();
    private ObservableList<Switch> previousSwitches = FXCollections.observableArrayList();
    private ObservableList<Switch> readSwitches = FXCollections.observableArrayList(); // For reading because of concurrency
    private SimpleDateFormat formatter = new SimpleDateFormat(Constants.DATE_FORMAT_LONG);
    private FXMLLoader loader;
    private GpioPinDigitalOutput firePowerPin = ShootingPanel.getFirePowerPin();
    private GpioPinDigitalOutput ninetyVoltPin = ShootingPanel.getNinetyVoltPin();
    private GpioPinDigitalOutput ignitorModePin = ShootingPanel.getIgnitorModePin();
    private GpioPinDigitalOutput armPin = ShootingPanel.getArmPin();
    private GpioPinDigitalInput shootPin = ShootingPanel.getShootPin();
    private boolean hasPassed = true, updated = false, shorted = false, changedDuplicate = false,
            changedSetting = false, activeJob = false, isShooting = false, isArmed = false, hybridFire = false,
            releasing = false, checkOpen = false, checkShort = false, detMissing = false, arm = false,
            plugSet = false, canRelease = false, canDelete = false, canDoJob = false, isInSafe;
    private StringBuilder testError = new StringBuilder();
    private String message = Constants.BLANK_SPACE, fieldFocused, serialNumber, version, oldVersion, deleteInstructions;
    private String selectionCustomer, selectionJob, selectionWell;
    private Switch selectedSwitch = new Switch(), settingSwitch;
    private Thread switchThread = null, fileChooserThread = null;
    private PropertyHelper appPropertyHelper, jobPropertyHelper, backupPropertyHelper;
    private int gunString = 0, switchCount = 0, expectedIndex = 0, volts = 0;
    private XYChart.Series<Number, Number> voltageData = new XYChart.Series<>();
    private XYChart.Series<Number, Number> currentData = new XYChart.Series<>();
    private XYChart.Series<Number, Number> checkVoltageData = new XYChart.Series<>();
    private XYChart.Series<Number, Number> checkCurrentData = new XYChart.Series<>();
    private Gauge cclMeter;
    private double amps = 0.0;
    private File activeFile = null;
    private Job currentJob = new Job();

    private HashMap<String, String> months = new HashMap<>();
    private enum TreeMenu { CUSTOMER, JOB, WELL, PROPS }

    //-------- SERVICES ---------//
    private DateService dateService = new DateService(this);
    private SwitchService switchService;
    private HybridService hybridService;
    private TimeService timeService = new TimeService(this);
    private GEOWriterService geoWriter;
    private ShortService shortService = new ShortService(this);
    private FileChooserService chooserService = new FileChooserService(this);
    private FileSaveService fileSaveService = new FileSaveService(this);
    private ADCService adcService = new ADCService(this);
    private ShootService shootService;
    private ReleaseService releaseService ;
    private CheckService checkService;
    private NoDetService noDetService = new NoDetService(this);

    Thread writerThread;
    Thread wrtThread;
    private TreeMenu menu = TreeMenu.CUSTOMER;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            File archive = new File(Constants.LINUX_ARCHIVE_PATH);
            File assets = new File(Constants.LINUX_ASSET_PATH);
            File startScript = new File(Constants.LINUX_ASSET_PATH + File.separator + "startScript.sh");

            if(!archive.exists()) {
                archive.mkdirs();
                Process dirAccess = Runtime.getRuntime().exec(Constants.GIVE_DIR_ACCESS + archive);
            }

            if(!assets.exists()) {
                assets.mkdirs();
                Process dirAccess = Runtime.getRuntime().exec(Constants.GIVE_DIR_ACCESS + assets);
            }

            if(startScript.exists()) {
                try {
                    Files.copy(Paths.get("/home/pi/Developer/app.properties"),
                                Paths.get("/home/pi/Documents/app.properties"),
                                StandardCopyOption.REPLACE_EXISTING);
                    Thread.sleep(500);
                    Files.move(Paths.get("/home/pi/Files/Assets/startScript.sh"),
                                Paths.get("/home/pi/Documents/startScript.sh"),
                                StandardCopyOption.REPLACE_EXISTING);
                } catch (InterruptedException e) {
                    appLogger.debug(e.getMessage());
                }
            }
        } catch (Exception e) {
            appLogger.debug(e);
        }

        startPane.setVisible(true);
        totalPane.setVisible(false);
        testPane.setVisible(false);
        countdownVisible(false);
        totalSwitchLabel.setVisible(false);
        fireButton.setDisable(true);
        armButton.setDisable(true);
        newJobButton.setVisible(false);
        resumeJobButton.setVisible(false);
        startSafeLabel.setVisible(true);

        months.put("January", "1");
        months.put("February", "2");
        months.put("March", "3");
        months.put("April", "4");
        months.put("May", "5");
        months.put("June", "6");
        months.put("July", "7");
        months.put("August", "8");
        months.put("September", "9");
        months.put("October", "10");
        months.put("November", "11");
        months.put("December", "12");

        logoImage.fitWidthProperty().bind(rootPane.widthProperty());
        checkFirePane.setVisible(false);
        getIPAddress();
        setAppPath();
        setTargetPath();

        initializeListeners();

        initializeCCLMeter();

        initializeChart(voltageChart, voltageData, vAxis, t1Axis, 400);
        initializeChart(currentChart, currentData, cAxis, t2Axis, 2);
        initializeChart(checkVoltageChart, checkVoltageData, checkVAxis, checkT1Axis, 400);
        initializeChart(checkCurrentChart, checkCurrentData, checkCAxis, checkT2Axis, 2);

        initializeAppProperties(appPath.toString() + File.separator + Constants.PROPERTIES_FILE_NAME);

        try {
            ProcessBuilder getTime = new ProcessBuilder(Constants.SUPER_USER, Constants.CLOCK, Constants.SAVE_FLAG);
            getTime.start();
        } catch (IOException e) {
            appLogger.debug(e);
        }

        try {
            // This thread is to keep time and keep application alive
            Thread dateThread = new Thread(dateService);
            dateThread.setDaemon(true);
            dateThread.start();

            // This thread is for capturing Voltage, Current, and Key position from ADC
            Thread adcThread = new Thread(adcService);
            adcThread.setDaemon(true);
            adcThread.start();

            Thread shortThread = new Thread(shortService);
            shortThread.setDaemon(true);
            shortThread.start();
        } catch (Exception e) {
            appLogger.debug(e);
        }

        // This is so it wont fail
        currentJob.setMode(Constants.ADDRESSABLE_MODE);

        try {
            backupPropertyHelper.addProp(Constants.OLD_VERSION_PROPERTY, oldVersion);
            backupPropertyHelper.addProp(Constants.CURRENT_VERSION_PROPERTY, version);
            backupPropertyHelper.addProp(Constants.SERIAL_NUMBER_PROPERTY, serialNumber);
            backupPropertyHelper.updateProps();

            Process flushPiFileBuffers =  Runtime.getRuntime().exec("sudo sync");
        } catch (IOException e) {
            System.out.println(e);
        }

        setModeButtonSelected(currentJob.getMode());

        changeToMode(currentJob.getMode());
    }

    private void initializeListeners() {
        shootPin.addListener((GpioPinListenerDigital) event -> {
            if(event.getState() == PinState.HIGH) {
                isShooting = true;
                isArmed = false;
                adcService.setShooting(true);
                shortService.setShooting(true);

                if(!checkOpen && !checkShort) {
                    if(settingSwitch != null && settingSwitch.isSetting()) {
                        ignitorModePin.setState(PinState.HIGH);
                    }

                    if(shootService != null) {
                        shootService.stop();
                    }
                    AnchorPane.setBottomAnchor(testContentPane, 190.0);
                    countdownVisible(false);
                }
            } else if(isShooting && event.getState() == PinState.LOW) {
                isShooting = false;
                armButton.setDisable(false);

                adcService.setShooting(false);

                setSwitchLinePower("OFF");
                setNinetyVoltLine("HIGH");
                // Sleep the thread to let the line settle
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    appLogger.debug(e);
                }

                if(!checkOpen && !checkShort) {
                    expectedIndex--;

                    Platform.runLater(() -> {
                        totalLabel.setText(Constants.ZERO);
                    });

                    geoWrite(Constants.SHOT, arm, volts, amps);

                    try {
                        writerThread.join();
                    } catch (Exception e) {
                        appLogger.debug(e);
                    }

                    if(releasing) {
                        wrtThread.interrupt();
                        resetReleaseView();
                        armPin.setState(PinState.LOW);
                        fileChooserPane.setVisible(false);
                        postReleasePane.setVisible(true);
                        testPane.setVisible(true);
                        releasing = false;
                    } else {
                        if(settingSwitch != null && settingSwitch.isSetting()) {
                            armPin.setState(PinState.LOW);
                            setFirePane.setVisible(true);
                        } else {
                            if(!shorted) {
                                plugSet = true;
                                settingSwitch = null;
                                shortService.setShooting(false);
                                inventorySwitches("SHOT EVENT");
                            }
                        }
                    }

                } else {
                    geoWrite(Constants.CHECK, arm, volts, amps);

                    try {
                        writerThread.join();
                    } catch (Exception e) {
                        appLogger.debug(e);
                    }

                    if(checkShort) {
                        checkOpen = true;
                        checkShort = false;
                        Platform.runLater(() -> {
                            checkFireInstructionLabel.setText(Constants.CHECK_OPEN_MESSAGE);
                        });
                    } else {
                        checkOpen = false;
                    }

                    armPin.setState(PinState.LOW);
                }
            }
        });
    }

    private void initializeAppProperties(String path) {
        appPropertyHelper = new PropertyHelper(path);
        backupPropertyHelper = new PropertyHelper(Constants.BACKUP_PROP_PATH);

        try {
            serialNumber = appPropertyHelper.getProp(Constants.SERIAL_NUMBER_PROPERTY);
            version = appPropertyHelper.getProp(Constants.CURRENT_VERSION_PROPERTY);
            oldVersion = appPropertyHelper.getProp(Constants.OLD_VERSION_PROPERTY);
        } catch (Exception e) {
            serialNumber = backupPropertyHelper.getProp(Constants.SERIAL_NUMBER_PROPERTY);
            version = backupPropertyHelper.getProp(Constants.CURRENT_VERSION_PROPERTY);
            oldVersion = backupPropertyHelper.getProp(Constants.OLD_VERSION_PROPERTY);
        }

        appPropertyHelper.addProp(Constants.OLD_VERSION_PROPERTY, oldVersion);
        appPropertyHelper.addProp(Constants.CURRENT_VERSION_PROPERTY, version);
        appPropertyHelper.addProp(Constants.SERIAL_NUMBER_PROPERTY, serialNumber);
        appPropertyHelper.updateProps();

        try {
            Process flushPiFileBuffers =  Runtime.getRuntime().exec("sudo sync");
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public void initializeJobProperties(String path) {
        jobPropertyHelper = new PropertyHelper(path);

        currentJob.setMode(jobPropertyHelper.getProp(Constants.MODE_PROPERTY));
        currentJob.setJobName(jobPropertyHelper.getProp(Constants.JOB_PROPERTY));
        currentJob.setCompany(jobPropertyHelper.getProp(Constants.COMPANY_PROPERTY));
        currentJob.setWell(jobPropertyHelper.getProp(Constants.WELL_PROPERTY));
        currentJob.setEngineer(jobPropertyHelper.getProp(Constants.ENGINEER_PROPERTY));
        currentJob.setColor(jobPropertyHelper.getProp(Constants.COLOR_PROPERTY));
        currentJob.setStage(Double.parseDouble(jobPropertyHelper.getProp(Constants.STAGE_PROPERTY)));

        Thread modeThread = new Thread(new ModeService(currentJob.getMode()));
        modeThread.setDaemon(true);
        modeThread.start();

        try {
            Process flushPiFileBuffers =  Runtime.getRuntime().exec("sudo sync");
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    private void initializeCCLMeter() {
        cclMeter = GaugeBuilder.create()
                .foregroundBaseColor(Color.rgb(53, 53, 53))
                .barBackgroundColor(Color.rgb(210, 210, 210))
                .unit(" ")
                .minValue(0)
                .maxValue(30000)
                .startAngle(270)
                .angleRange(180)
                .needleShape(Gauge.NeedleShape.FLAT)
                .needleSize(Gauge.NeedleSize.STANDARD)
                .needleColor(Color.rgb(53, 53, 53))
                .knobType(Gauge.KnobType.FLAT)
                .knobColor(Color.rgb(53, 53, 53))
                .barColor(Color.RED)
                .barEffectEnabled(true)
                .sectionIconsVisible(true)
                .tickLabelColor(Color.WHITE)
                .minWidth(1600)
                .translateY(10)
                .build();

        meterPane.getChildren().addAll(cclMeter);
    }

    private void initializeChart(LineChart<Number, Number> chart, XYChart.Series<Number, Number> data,
                                 NumberAxis yAxis, NumberAxis xAxis, int upperBound) {
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setVerticalGridLinesVisible(false);
        chart.setHorizontalGridLinesVisible(false);
        chart.legendVisibleProperty().setValue(false);
        chart.getData().addAll(data);

        yAxis.setLowerBound(0);
        yAxis.setUpperBound(upperBound);
        yAxis.setTickUnit(1);
        yAxis.setAutoRanging(false);
        yAxis.setTickMarkVisible(false);

        xAxis.setAutoRanging(true);
        xAxis.setLowerBound(0);
        xAxis.setTickUnit(10);

    }

    public void setChart(List<XYChart.Data<Number, Number>> voltage, List<XYChart.Data<Number, Number>> current) {
        List<XYChart.Data<Number, Number>> v = new ArrayList<>(voltage);
        List<XYChart.Data<Number, Number>> c = new ArrayList<>(current);

        if(!checkFirePane.isVisible()) {
            voltageData.getData().addAll(v);
            currentData.getData().addAll(c);
        } else {
            checkVoltageData.getData().addAll(v);
            checkCurrentData.getData().addAll(c);
        }
    }

    private void clearChart() {
        AnchorPane.setBottomAnchor(testContentPane, 5.0);
        voltageData.getData().clear();
        currentData.getData().clear();
        checkVoltageData.getData().clear();
        checkCurrentData.getData().clear();
    }

    private void start() {
        messageLabel.setText("STAGE " + currentJob.getStage());
        config();
    }

    private void config() {
        messageLabel.setStyle(Constants.STYLE_BACKGROUND_GRAY);

        Platform.runLater(() -> {
            messageLabel.setText("STAGE " + currentJob.getStage());
            totalSwitchLabel.setVisible(false);
        });

        if(detMissing) {
            detMissing = false;
            noDetService.stop();
            changeTotalColor("black");
        }

        countdownVisible(false);
        fileChooserPane.setVisible(false);
        infoAddressLabel.setVisible(false);
        indicator.setVisible(false);

        if(isInSafe) {
            inventoryButton.setDisable(true);
        }

        testPane.setVisible(true);
        messageLabel.setVisible(true);
        jobInfoPane.setVisible(false);
        totalPane.setVisible(true);
        fireButton.setDisable(true);
        armButton.setDisable(true);
    }

    private void reset() {
        Platform.runLater(() -> {
            if(!hasPassed) {
                gunString = 0;
                expectedIndex = 0;
            }
            hasPassed = true;

            if(switches != null) {
                switches.clear();
            }

            if(readSwitches != null) {
                readSwitches.clear();
            }

            switchList.getItems().clear();
            switchList.getItems().removeAll();
            gearBackground.setVisible(false);
            testError = new StringBuilder();
            totalLabel.setText(Constants.ZERO);
            shorted = false;
            message = Constants.BLANK_SPACE;
            changedDuplicate = false;
            changedSetting = false;
            countdownLabel.setText(Constants.BLANK_SPACE);
            releaseInstructionLabel.setText(Constants.RELEASE_SEQUENCE);
            releaseInstructionLabel.setStyle("-fx-font-size: 1.8em;");
            releasing = false;
        });
    }

    @FXML
    void inventorySwitches() {
        inventorySwitches(Constants.INVENTORY);
    }

    private void inventorySwitches(String origin) {
        reset();

        if(shootService != null) {
            shootService.stop();
        }

        switch(origin) {
            case "ARM":
                isArmed = true;
                break;
            case "INVENTORY":
                isArmed = false;
                hybridFire = false;
                break;
            case "HYBRID FIRE":
                hybridFire = true;
                break;
            case "SHOT EVENT":
                hybridFire = false;
                break;
        }

        armPin.setState(PinState.LOW);
        inventoryButton.setDisable(true);

        config();
        setNinetyVoltLine("HIGH");

        // This thread is effectively the testing of the switches
        // Adjust UI to match test mode
        if(currentJob.getMode().equals(Constants.ADDRESSABLE_MODE)) {
            switchThread = new Thread(switchService);
        } else if(currentJob.getMode().equals(Constants.HYBRID_MODE)) {
            hybridService  = new HybridService(this);
            hybridService.setFiring(hybridFire);
            switchThread = new Thread(hybridService);
        }

        switchThread.setDaemon(true);
        switchThread.start();
    }

    @FXML
    void newJobButtonPressed() {
        jobInfoChange(false);
    }

    @FXML
    void jobInfoButtonPressed() {
        jobInfoChange(true);
    }

    private void jobInfoChange(boolean active) {
        // This method is also accessed from job info in settings
        // So if active job, certain values can't be changed
        jobNameField.getStyleClass().add(Constants.VALID);
        companyField.getStyleClass().add(Constants.VALID);
        wellField.getStyleClass().add(Constants.VALID);

        if(active) {
            jobNameField.setText(currentJob.getJobName());
            jobNameField.setDisable(true);
            companyField.setText(currentJob.getCompany());
            companyField.setDisable(true);

            wellField.setText(currentJob.getWell());
            engineerField.setText(currentJob.getEngineer());
            stageField.setText("" + currentJob.getStage());
        } else {
            jobNameField.clear();
            jobNameField.setDisable(false);
            companyField.clear();
            companyField.setDisable(false);
            wellField.clear();
            engineerField.clear();
        }

        setModeButtonSelected(currentJob.getMode());

        testPane.setVisible(true);
        jobInfoPane.setVisible(true);

        ignitorModePin.setState(PinState.HIGH);
    }

    private void setModeButtonSelected(String mode) {
        hybridButton.getStyleClass().clear();
        addressableButton.getStyleClass().clear();

        if(mode.equals(Constants.ADDRESSABLE_MODE)) {
            hybridButton.getStyleClass().add(Constants.STYLE_BUTTON_UNSELECTED);
            addressableButton.getStyleClass().add(Constants.STYLE_BUTTON_SELECTED);
        } else if(mode.equals(Constants.HYBRID_MODE)) {
            hybridButton.getStyleClass().add(Constants.STYLE_BUTTON_SELECTED);
            addressableButton.getStyleClass().add(Constants.STYLE_BUTTON_UNSELECTED);
        }
    }

    public void changeToMode(String mode) {
        Thread modeThread = new Thread(new ModeService(mode));
        modeThread.setDaemon(true);
        modeThread.start();
    }

    public void configTree(String tm) {
        switch(tm) {
            case "CUSTOMER":
                fileTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                    activeFile = new File(Constants.LINUX_JOB_PATH + File.separator + newValue.getValue());
                    selectionCustomer = newValue.getValue();
                    menu = TreeMenu.JOB;

                    deleteInstructions = Constants.DELETE_STRING + "\n" + selectionCustomer;

                    if(!selectionCustomer.equals(":GEODynamics-RELEASE")) {
                        deleteInstructionLabel.setText(deleteInstructions);
                        deleteFileButton.setVisible(true);
                    } else {
                        deleteFileButton.setVisible(false);
                    }

                    fileOKButton.setDisable(false);
                });
                break;
            case "JOB":
                fileTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                    activeFile = new File(Constants.LINUX_JOB_PATH + File.separator + selectionCustomer + File.separator +  newValue.getValue());
                    selectionJob = newValue.getValue();
                    menu = TreeMenu.WELL;

                    deleteInstructions = Constants.DELETE_STRING + "\n" + selectionCustomer + File.separator + selectionJob;

                    if(!selectionJob.equals(":WRT")) {
                        deleteInstructionLabel.setText(deleteInstructions);
                        deleteFileButton.setVisible(true);
                    }

                    if(newValue.getValue().equals(":WRT")) {
                        chooserService.setWrt(true);
                    } else {
                        deleteFileButton.setVisible(false);
                    }

                    fileOKButton.setDisable(false);
                });
                break;
            case "WELL":
                fileTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                    activeFile = new File(Constants.LINUX_JOB_PATH + File.separator + selectionCustomer + File.separator + selectionJob + File.separator +  newValue.getValue());
                    selectionWell = newValue.getValue();
                    menu = TreeMenu.CUSTOMER;
                    canDoJob = true;

                    deleteInstructions = Constants.DELETE_STRING + "\n" + selectionCustomer + File.separator + selectionJob + File.separator + selectionWell;

                    fileOKButton.setDisable(false);
                });
                break;
            default:
        }
    }

    private void selectTree(TreeMenu tm, File file) {
        chooserService.setActiveMenu(tm.name());
        chooserService.setActiveFolder(file);

        fileChooserThread = new Thread(chooserService);
        fileChooserThread.setDaemon(true);
        fileChooserThread.start();
    }

    @FXML
    void fileDeleteButtonPressed() {
        deletePane.setVisible(true);
    }

    @FXML
    void deleteConfirm() {
        SimpleDateFormat formatter = new SimpleDateFormat(Constants.DATE_FORMAT_FILE_LONG);
        deletePane.setVisible(false);
        fileChooserPane.setVisible(false);
        try {
            if(activeFile.isDirectory()) {
                FileUtils.moveDirectory(activeFile, new File(Constants.LINUX_ARCHIVE_PATH + File.separator + formatter.format(new Date())));
            } else {
                FileUtils.moveFileToDirectory(activeFile, new File(Constants.LINUX_ARCHIVE_PATH + File.separator + formatter.format(new Date())), false);
            }
        } catch (IOException e) {
            appLogger.debug(e);
        }
    }

    @FXML
    void deleteCancel() {
        deletePane.setVisible(false);
    }

    @FXML
    void resumeJobButtonPressed() {
        deleteFileButton.setVisible(false);
        fileOKButton.setDisable(true);
        canDoJob = false;

        menu = TreeMenu.CUSTOMER;
        chooserService.setWrt(false);
        selectTree(menu, new File(Constants.LINUX_JOB_PATH));

        fileChooserPane.setVisible(true);
        ignitorModePin.setState(PinState.HIGH);
    }

    @FXML
    void okButtonPressed() {
        if(jobInfoPane.isVisible()) {
            if(!activeJob) {
                if(jobNameField.getText().isEmpty() || companyField.getText().isEmpty() || wellField.getText().isEmpty()) {
                    // Structured this way so that only the fields that are invalid are red
                    if(jobNameField.getText().isEmpty()) {
                        jobNameField.getStyleClass().add(Constants.INVALID);
                    }

                    if(companyField.getText().isEmpty()) {
                        companyField.getStyleClass().add(Constants.INVALID);
                    }

                    if(wellField.getText().isEmpty()) {
                        wellField.getStyleClass().add(Constants.INVALID);
                    }
                } else {
                    // Make the fields valid again
                    jobNameField.getStyleClass().add(Constants.VALID);
                    companyField.getStyleClass().add(Constants.VALID);
                    wellField.getStyleClass().add(Constants.VALID);
                    // Save the data
                    saveFile();
                    switchService = new SwitchService(this);
                    activeJob = true;
                    start();
                }
            } else {
                String newDirPath = Constants.LINUX_JOB_PATH +
                        File.separator + currentJob.getCompany() +
                        File.separator + currentJob.getJobName() +
                        File.separator + currentJob.getWell();

                setJobDirPath(newDirPath);

                if(!currentJob.getWell().equals(wellField.getText())) {
                    currentJob.setJobDirPath(newDirPath);
                    saveFile();
                } else {
                    currentJob.setWell(wellField.getText());
                    currentJob.setEngineer(engineerField.getText());
                    currentJob.setStage(Double.parseDouble(stageField.getText()));

                    messageLabel.setText("STAGE " + currentJob.getStage());

                    wellInfoLabel.setText(wellField.getText());

                    jobPropertyHelper.addProp(Constants.ENGINEER_PROPERTY, currentJob.getEngineer());
                    jobPropertyHelper.addProp(Constants.STAGE_PROPERTY, Double.toString(currentJob.getStage()));
                    jobPropertyHelper.addProp(Constants.JOB_PROPERTY, currentJob.getJobName());
                    jobPropertyHelper.addProp(Constants.COMPANY_PROPERTY, currentJob.getCompany());
                    jobPropertyHelper.addProp(Constants.WELL_PROPERTY, currentJob.getWell());
                    jobPropertyHelper.addProp(Constants.MODE_PROPERTY, currentJob.getMode());
                    jobPropertyHelper.addProp(Constants.COLOR_PROPERTY, currentJob.getColor());
                    jobPropertyHelper.updateProps();

                    saveFile();
                }

                stringReset();
                jobInfoPane.setVisible(false);
            }
        } else if(fileChooserPane.isVisible()) {
            fileOKButton.setDisable(true);
            deleteFileButton.setVisible(false);
            if(canDoJob) {
                try {
                    // Change properties file to represent the resumed job file
                    initializeJobProperties(chooserService.getFile(fileTree.getSelectionModel().getSelectedItem().getValue()).getAbsolutePath());
                    // Set the current Job dir path
                    String[] parts = chooserService.getFile(fileTree.getSelectionModel().getSelectedItem().getValue()).getAbsolutePath().split("/");
                    activeJob = true;

                    setJobDirPath("/" + parts[1] + "/" + parts[2] + "/" + parts[3] + "/" + parts[4] +
                            "/" + parts[5] + "/" + parts[6] + "/" + parts[7]);

                    if(parts[6].equals(":WRT")) {
                        wrtPane.setVisible(true);
                    } else {
                        start();
                    }

                    switchService.setNotSet(true);
                } catch (NullPointerException e) {
                    appLogger.debug(Constants.DO_NOTHING);
                }

                adcService.setLastPosition(0.0);

                // Make sure the hat board is set to the right mode
                changeToMode(currentJob.getMode());

                if(currentJob.getMode().equals(Constants.HYBRID_MODE)) {
                    armButton.setVisible(false);
                    fireButton.setDisable(false);
                } else {
                    armButton.setVisible(true);
                    fireButton.setDisable(true);
                }

                jobInfoLabel.setText(currentJob.getJobName());
                companyInfoLabel.setText(currentJob.getCompany());
                wellInfoLabel.setText(currentJob.getWell());

                switchService = new SwitchService(this);
                stringReset();
                fileChooserPane.setVisible(false);
            } else {
                selectTree(menu, activeFile);
            }
        }
    }

    private void saveFile() {
        currentJob.setJobName(jobNameField.getText());
        currentJob.setCompany(companyField.getText());
        currentJob.setWell(wellField.getText());
        currentJob.setEngineer(engineerField.getText());
        currentJob.setStage(Double.parseDouble(stageField.getText()));
        currentJob.setColor((colorButton.getBackground().getFills().get(0).getFill()).toString());

        fileSaveService.setJob(currentJob);

        Thread fileSaveThread = new Thread(fileSaveService);
        fileSaveThread.setDaemon(true);
        fileSaveThread.start();
    }

    /**
     * Resets the system for new gun string
     */
    private void stringReset() {
        if(switches != null) { switches.clear(); }
        if(readSwitches != null) { readSwitches.clear(); }
        if(previousSwitches != null) { previousSwitches.clear(); }

        ignitorModePin.setState(PinState.HIGH);
        settingSwitch = null;
        gunString = 0;
        expectedIndex = 0;

        if(switchService != null) {
            switchService.setNotSet(true);
            switchService.setMasterString(0);
        }

        shortService.setFirstRun(true);
        clearChart();
        totalLabel.setText(Constants.ZERO);
        inventoryButton.setDisable(false);
        armButton.setDisable(true);
        plugSet = false;
        messageLabel.setText("STAGE " + currentJob.getStage());
    }

    @FXML
    void cancelButtonPressed() {
        if(jobInfoPane.isVisible()) {
            testPane.setVisible(false);
            jobInfoPane.setVisible(false);
        } else if(fileChooserPane.isVisible()) {
            menu = TreeMenu.CUSTOMER;
            chooserService.setWrt(false);
            fileChooserPane.setVisible(false);
        }
    }

    public void countdown(int i) {
        countdownLabel.setText(Constants.NO_STRING + i);

        if(i == 0) {
            countdownVisible(false);
            armButton.setDisable(false);
            fireButton.setDisable(true);
            inventoryButton.setDisable(false);
            armPin.setState(PinState.LOW);
        }
    }

    private void countdownVisible(boolean visible) {
        infoAddressLabel.setVisible(!visible);
        indicator.setVisible(!visible);
        countdownLabel.setVisible(visible);
    }

    @FXML
    void fireButtonPressed() {
        if(currentJob.getMode().equals(Constants.ADDRESSABLE_MODE)) {
            countdownVisible(true);
            shootService = new ShootService(this, settingSwitch);
            Thread countdownThread = new Thread(shootService);
            // Set the switch to the selected switch so that fire command can be sent
            shootService.setCurrentSwitch(switchList.getSelectionModel().getSelectedItem());

            countdownThread.setDaemon(true);
            countdownThread.start();
        } else {
            clearChart();
            inventorySwitches("HYBRID FIRE");
        }

        fireButton.setDisable(true);
    }

    public void armPanel() {
        armPin.setState(PinState.HIGH);
    }

    @FXML
    void armButtonPressed() {
        try {
            selectedSwitch = switchList.getSelectionModel().getSelectedItem();
        } catch (NullPointerException e) {
            selectedSwitch = null;
        }

        switchService.setCurrentSwitch(selectedSwitch, false);

        clearChart();
        fireButton.setDisable(false);
        armButton.setDisable(true);
        isArmed = true;
        inventorySwitches("ARM");
    }

    @FXML
    void abortPressed() {
        qrView.setVisible(true);
        companyInfoLabel.setVisible(false);
        wellInfoLabel.setVisible(false);
        jobInfoLabel.setVisible(false);
        activeJob = false;
        shorted = false;
        releasing = false;
        exportButton.setDisable(true);
        gearBackground.setVisible(false);
        statusIcon.setVisible(false);
        totalPane.setVisible(false);
        testPane.setVisible(false);
        newJobButton.setVisible(false);
        resumeJobButton.setVisible(false);

        if(!isInSafe) {
            newJobButton.setVisible(false);
            resumeJobButton.setVisible(false);
            startSafeLabel.setVisible(true);
        } else {
            newJobButton.setVisible(true);
            resumeJobButton.setVisible(true);
            startSafeLabel.setVisible(false);
        }

        startPane.setVisible(true);
        resetReleaseView();
    }

    public void releaseCountdown(String i) {
        Platform.runLater(() -> releaseInstructionLabel.setText(i));
    }

    public void resetReleaseView() {
        Platform.runLater(() -> {
            canRelease = false;
            activeJob = false;
            shorted = false;
            releasing = false;
            AnchorPane.setBottomAnchor(releaseLabelPane, 220.0);
            releaseInstructionLabel.setText(Constants.RELEASE_SEQUENCE);
            releaseInstructionLabel.setStyle(Constants.STYLE_RELEASE_INSTRUCTIONS);
            releaseConfirmButton.setStyle(Constants.STYLE_BUTTON_SELECTED);
            releaseCancelButton.setStyle(Constants.STYLE_BUTTON_UNSELECTED);
            releaseConfirmButton.setText(Constants.CONFIRM);
            releaseConfirmButton.setDisable(false);
            wrtPane.setVisible(false);
        });
    }

    @FXML
    void releaseConfirmPressed() {
        AnchorPane.setBottomAnchor(releaseLabelPane, 0.0);
        releaseInstructionLabel.setStyle("-fx-text-fill: white;");
        releaseConfirmButton.setStyle(Constants.STYLE_RELEASE_CONFIRM);
        releaseCancelButton.setStyle(Constants.STYLE_RELEASE_CANCEL);

        if(releaseConfirmButton.getText().equals(Constants.RELEASE)) {
            if(canRelease) {
                releaseConfirmButton.setDisable(true);
                releasing = true;
                releaseService = new ReleaseService(this);
                releaseService.setMode(Constants.ADDRESSABLE_MODE);
                wrtThread = new Thread(releaseService);
                wrtThread.setDaemon(true);
                wrtThread.start();

                releaseInstructionLabel.setText(" ");
                releaseInstructionLabel.setStyle("-fx-font-size: 6em; -fx-text-fill: white");
            } else {
                releaseInstructionLabel.setText("Put panel in ARM and PRESS RELEASE");
                releaseInstructionLabel.setStyle("-fx-font-size: 2em; -fx-text-fill: white");
            }
        } else {
            releaseConfirmButton.setText(Constants.RELEASE);
        }
    }

    @FXML
    void releaseCancelPressed() {
        releaseInstructionLabel.setText(Constants.RELEASE_SEQUENCE);
        releasing = false;

        if(releaseService != null) {
            releaseService.stop();
            releaseService.reset();
        }

        resetReleaseView();
        armPin.setState(PinState.LOW);
    }

    @FXML
    void postReleaseOk() {
        postReleasePane.setVisible(false);
        abortPressed();
    }

    @FXML
    void checkCancelPressed() {
        shortService.setBypass(false);
        checkOpen = false;
        checkShort = false;
        stringReset();
        checkFireButton.setDisable(false);
        checkReleaseButton.setDisable(false);
        checkFirePane.setVisible(false);
        armPin.setState(PinState.LOW);
    }

    @FXML
    void checkFirePressed() {
        shortService.setBypass(true);
        clearChart();

        if(!checkShort) {
            checkShort = true;
            message = Constants.CHECK_SHORT;
        } else {
            checkOpen = true;
            message = Constants.CHECK_OPEN;
        }

        armPanel();
    }

    @FXML
    void checkFire() {
        checkFireButton.setDisable(true);
        checkReleaseButton.setDisable(true);
        checkFireInstructionLabel.setText(Constants.CHECK_SHORT_MESSAGE);
        checkFirePane.setVisible(true);
    }

    @FXML
    void checkReleaseTool() {
        checkFireButton.setDisable(true);
        checkReleaseButton.setDisable(true);
        statusIcon.setVisible(false);

        checkService = new CheckService(this);
        checkService.setMode(currentJob.getMode());
        Thread checkThread = new Thread(checkService);
        checkThread.start();
    }

    public void showWrtStatus(boolean isPresent) {
        checkFireButton.setDisable(false);
        checkReleaseButton.setDisable(false);

        Image check;

        if(isPresent) {
            check = new Image("/images/check_g.png");
        } else {
            check = new Image("/images/x_r.png");
        }

        statusIcon.setImage(check);
        statusIcon.setVisible(true);
        setSwitchLinePower("OFF");
    }

    @FXML
    void plugYesPressed() {
        switchService.setNotSet(false);
        setFirePane.setVisible(false);
        fireButton.setText(Constants.FIRE);
        ignitorModePin.setState(PinState.LOW);
        expectedIndex++;
        plugSet = true;
        inventorySwitches(Constants.SHOT_EVENT);
        if(currentJob.getMode().equals(Constants.HYBRID_MODE)) {
            settingSwitch = null;
        }
    }

    @FXML
    void plugNoPressed() {
        switchService.setNotSet(true);
        setFirePane.setVisible(false);
        expectedIndex++;
        inventorySwitches(Constants.SHOT_EVENT);
    }

    @FXML
    void dismissGunCountModal() {
        gunCountPane.setVisible(false);
        gunCountInstructionLabel.setText(Constants.COUNT_ERROR);
        resetMessage();
        inventorySwitches();
    }

    @FXML
    void gunCountReset() {
        if(!isArmed) {
            stringReset();
            resetMessage();
            gunCountPane.setVisible(false);
            gunCountInstructionLabel.setText(Constants.COUNT_ERROR);
            inventorySwitches();
        } else {
            // This is for the event of skipping a gun
            expectedIndex = Integer.parseInt(selectedSwitch.getOriginalIndex());
            // null here because the assumption is that you've fired the setting switch (prevents setting switch prompt)
            settingSwitch = null;
            resetMessage();
            gunCountPane.setVisible(false);
            gunCountInstructionLabel.setText(Constants.COUNT_ERROR);
            gunCountResetButton.setText(Constants.RESET);
        }
    }

    @FXML
    void gunCountNewExpected() {
        expectedIndex = switchCount;
        gunCountPane.setVisible(false);
        gunCountInstructionLabel.setText(Constants.COUNT_ERROR);
        resetMessage();
        inventorySwitches();
    }

    @FXML
    void switchClicked() {
        if(!isArmed) {
            selectedSwitch = switchList.getSelectionModel().getSelectedItem();

            if(selectedSwitch.isSetting()) {
                fireButton.setText(Constants.SET_PLUG);
            } else {
                fireButton.setText(Constants.FIRE);
            }

            if(selectedSwitch != null) {
                // Do not display address if communication error
                if(selectedSwitch.getMessage().equals(Constants.COMM_ERROR)) {
                    infoAddressLabel.setText(Constants.BLANK_SPACE);
                    indicator.setVisible(false);
                } else {
                    infoAddressLabel.setText(selectedSwitch.getAddress());

                    if(selectedSwitch.isShorted()) {
                        indicator.setImage(new Image("/images/shortIndicator.png"));
                    } else if(selectedSwitch.isFeedthrough()) {
                        indicator.setImage(new Image("/images/thruIndicator.png"));
                    } else {
                        indicator.setImage(new Image("/images/openIndicator.png"));
                    }

                    indicator.setVisible(true);
                }

                if(!selectedSwitch.isPassed()) {
                    testError = new StringBuilder();
                    testError.append(selectedSwitch.getMessage());
                    hasPassed = false;
                } else {
                    resetMessage();
                }

                // Displays an error message in the switch info pane if one exists
                // "None" is default, so if anything else, display the error message
                if(!(selectedSwitch.getMessage().equals(Constants.NONE))) {
                    messageLabel.setText(selectedSwitch.getMessage());
                    messageLabel.setStyle(Constants.STYLE_BACKGROUND_RED);
                    messageLabel.setVisible(true);
                }
            }
        }
    }

    @FXML
    void saveTest() {
        saveBackground.setVisible(true);
    }

    @FXML
    void modalClose(MouseEvent event) {
        String button = ((ImageView) event.getSource()).getId();
        // This switch is here because this method is used among varying modals
        if(button.equals(Constants.SAVE_CLOSE)) {
            hideSave();
            saveBackground.setVisible(false);
        } else if(button.equals(Constants.GEAR_CLOSE)) {
            updateButton.setDisable(false);
            gearBackground.setVisible(false);
            setTimePane.setVisible(false);
            statusIcon.setVisible(false);
            if(releasing) {
                postReleasePane.setVisible(true);
            }
        }
    }

    @FXML
    void colorSelect() {
        colorBackground.setVisible(true);
    }

    @FXML
    void colorConfirm(ActionEvent event) {
        // Dismiss the view
        colorBackground.setVisible(false);
        // Set appropriate UI elements to the color selected
        colorButton.setBackground(new Background(new BackgroundFill(((Button)event.getSource()).getBackground().getFills().get(0).getFill(), new CornerRadii(15), Insets.EMPTY)));
        navPane.setBackground(new Background(new BackgroundFill(((Button)event.getSource()).getBackground().getFills().get(0).getFill(), CornerRadii.EMPTY, Insets.EMPTY)));
    }

    @FXML
    void gearButtonPressed() {
        // Displays application and job information
        getIPAddress();

        if(testPane.isVisible()) {
            jobInfoLabel.setText(currentJob.getJobName());
            companyInfoLabel.setText(currentJob.getCompany());
            wellInfoLabel.setText(currentJob.getWell());
            qrView.setVisible(false);
        } else {
            jobInfoLabel.setText(" ");
            companyInfoLabel.setText(" ");
            wellInfoLabel.setText(" ");
            qrView.setVisible(true);
        }

        postReleasePane.setVisible(false);

        if(activeJob) {
            exportButton.setDisable(false);
            jobInfoButton.setDisable(false);
        }

        gearBackground.setVisible(true);
    }

    @FXML
    void exitButtonPressed() {
        ShootingPanel.exit();
    }

    @FXML
    void dateTimeClose() {
        fieldFocused = Constants.BLANK_SPACE;
        hideGridPanes();
        setTimePane.setVisible(false);
    }

    @FXML
    void setTimeButtonPressed() {
        // Locks the current year in because the pi clock has issues with past dates, and no reason to set future
        yearField.setText(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
        setTimePane.setVisible(true);
    }

    @FXML
    void saveButtonPressed(ActionEvent event) {
        dayButton.setVisible(false);
        weekButton.setVisible(false);
        monthButton.setVisible(false);
        saveProgressBar.setVisible(true);

        // Gets the ID of the event to determine which files to save
        FileExportService exportService = new FileExportService(((Control) event.getSource()).getId(), this);

        Task<Void> exportTask = new Task<Void>() {
            public Void call() {
                exportService.export();
                return null;
            }
        };

        new Thread(exportTask).start();
    }

    @FXML
    void textFieldFocused(MouseEvent event) {
        String textField = ((Control) event.getSource()).getId();
        // Hides all the grid panes
        hideGridPanes();
        // So that only the one selected will be visible
        if(textField.equals(Constants.MONTH_FIELD)) {
            fieldFocused = Constants.MONTH;
            monthPane.setVisible(true);
        } else if(textField.equals(Constants.DAY_FIELD)) {
            fieldFocused = Constants.DAY;
            dayPane.setVisible(true);
        } else if(textField.equals(Constants.YEAR_FIELD)) {
            fieldFocused = Constants.YEAR;
            yearPane.setVisible(true);
        } else if(textField.equals(Constants.HOUR_FIELD)) {
            fieldFocused = Constants.HOUR;
            hourPane.setVisible(true);
        } else if(textField.equals(Constants.MINUTE_FIELD)) {
            fieldFocused = Constants.MINUTE;
            minutePane.setVisible(true);
        } else if(textField.equals(Constants.AMPM_FIELD)) {
            fieldFocused = Constants.AMPM;
            ampmPane.setVisible(true);
        }
    }

    @FXML
    void dateTimeButtonPressed(ActionEvent event) {
        // Get the text off the button
        Button button = (Button) event.getSource();
        String buttonText = button.getText();

        // Put the text in the field that is focused
        if(fieldFocused.equals(Constants.MONTH)) {
            monthField.setText(months.get(buttonText));
        } else if(fieldFocused.equals(Constants.DAY)) {
            dayField.setText(buttonText);
        } else if(fieldFocused.equals(Constants.YEAR)) {
            yearField.setText(buttonText);
        } else if(fieldFocused.equals(Constants.HOUR)) {
            hourField.setText(buttonText);
        } else if(fieldFocused.equals(Constants.MINUTE)) {
            minuteField.setText(buttonText);
        } else if(fieldFocused.equals(Constants.AMPM)) {
            ampmField.setText(buttonText);
        }
    }

    @FXML
    void saveTimeDateButtonPressed() {
        if(monthField.getText().isEmpty() || dayField.getText().isEmpty() || yearField.getText().isEmpty() ||
                hourField.getText().isEmpty() || minuteField.getText().isEmpty() || ampmField.getText().isEmpty()) {
            monthField.getStyleClass().add(Constants.INVALID);
            dayField.getStyleClass().add(Constants.INVALID);
            yearField.getStyleClass().add(Constants.INVALID);
            hourField.getStyleClass().add(Constants.INVALID);
            minuteField.getStyleClass().add(Constants.INVALID);
            ampmField.getStyleClass().add(Constants.INVALID);
            return;
        }

        String hour;

        if(ampmField.getText().equals("PM")) {
            if(Integer.parseInt(hourField.getText()) == 12) {
                hour = "12";
            } else {
                hour = Integer.toString(Integer.parseInt(hourField.getText()) + 12);
            }
        } else {
            if(Integer.parseInt(hourField.getText()) == 12) {
                hour = "0";
            } else {
                hour = hourField.getText();
            }
        }

        SimpleDateFormat formatter = new SimpleDateFormat(Constants.DATE_FORMAT_SHORT);

        String month = " ";
        for(Map.Entry<String, String> entry : months.entrySet()) {
            if(entry.getValue().equals(monthField.getText())) {
                month = entry.getValue();
            }
        }

        String newDate = yearField.getText() + "-" + month + "-" + dayField.getText() + " " +
                hour + ":" + minuteField.getText() + ":" + "00";

        try {
            Date selectedDate = formatter.parse(newDate);

            timeService.setMode(Constants.WRITE_MODE);
            timeService.setDate(selectedDate);
            Thread timeThread = new Thread(timeService);
            timeThread.setDaemon(true);
            timeThread.start();
        } catch (ParseException e) {
            appLogger.debug(e);
        }

        hideGridPanes();
        setTimePane.setVisible(false);
        gearBackground.setVisible(false);
    }

    @FXML
    void selectMode(ActionEvent event) {
        // Get the text off the button
        Button button = (Button) event.getSource();
        String buttonText = button.getText();

        hybridButton.getStyleClass().clear();
        addressableButton.getStyleClass().clear();

        if(buttonText.equals(Constants.ADDRESSABLE_MODE)) {
            hybridButton.getStyleClass().add(Constants.STYLE_BUTTON_UNSELECTED);
            addressableButton.getStyleClass().add(Constants.STYLE_BUTTON_SELECTED);
            currentJob.setMode(Constants.ADDRESSABLE_MODE);
        } else if(buttonText.equals(Constants.HYBRID_MODE)) {
            hybridButton.getStyleClass().add(Constants.STYLE_BUTTON_SELECTED);
            addressableButton.getStyleClass().add(Constants.STYLE_BUTTON_UNSELECTED);
            currentJob.setMode(Constants.HYBRID_MODE);
        }
    }

    @FXML
    void resetMessage() {
        messageLabel.setText("STAGE " + currentJob.getStage());
        messageLabel.setStyle(Constants.STYLE_BACKGROUND_GRAY);
        hasPassed = true;
    }

    /**
     * Sets the visibility of the progress bar when updating the application
     * @param visible is the boolean to determine whether the bar is visible or not
     */
    public void setUpdateProgressVisible(boolean visible) {
        updateProgressBar.setVisible(visible);
    }

    /**
     * Sets the visibility of the Settings view
     * @param visible is the boolean to determine whether the Settings view is visible or not
     */
    public void setGearBackgroundVisible(boolean visible) {
        gearBackground.setVisible(visible);
    }

    /**
     * Sets the modal message and look & feel accordingly (Red: Error, Green: Application updated)
     * @param message is the message to be displayed in the modal
     * @param updated is the flag to indicate if the application was updated or not
     * @param failure is the flag to indicate if the application experienced a problem updating
     */
    public void showUpdateInfo(String message, boolean updated, boolean failure) {
        this.updated = updated;
        updateMessage.setText(message);
        setUpdateProgressVisible(false);
        updateButton.setDisable(false);
        setGearBackgroundVisible(false);

        if(failure) {
            updateMessagePane.setStyle("-fx-background-color: #E82C0C;");
        }

        updatePane.setVisible(true);
    }

    /**
     * Called from inside the FileExportService
     * @param work represents how many files are being saved
     */
    public void updateProgress(double work) {
        saveProgressBar.setProgress(work);
    }

    @FXML
    void updateButtonPressed() {
        updateButton.setDisable(true);
        Thread updateThread = new Thread(new UpdateService(this, appPropertyHelper));
        updateThread.setDaemon(true);
        updateThread.start();
    }

    @FXML
    void updateOKPressed() {
        try {
            updatePane.setVisible(false);

            if(this.updated) {
                Process reboot = Runtime.getRuntime().exec(Constants.REBOOT_COMMAND);
            }
        } catch (IOException e) {
            appLogger.debug(e);
        }
    }

    /**
     * Called from inside the ADCService
     * @param current is the amperage measured from the adc
     * @param volts is the voltage measured from the adc
     * @param collars is the collar kicks
     */
    public void updateADC(double current, int volts, double collars) {
        Platform.runLater(() -> {
            if(cclPane.isVisible()) {
                if(collars >= 13500 && collars <= 16500) {
                    cclMeter.setValue(15000);
                } else {
                    cclMeter.setValue(collars);
                }
            } else {
                this.volts = volts;
                this.amps = current;
                ampLabel.setText(new DecimalFormat("#.000").format(current) + Constants.AMPS);
                voltLabel.setText(Constants.VOLTS + volts);
            }
        });
    }

    public int getVolts() {
        return this.volts;
    }

    public double getAmps() {
        return this.amps;
    }

    /**
     * @param piDate is actually the RTC date
     */
    public void setDate(Date piDate, boolean dateChanged) {
        date.setText(formatter.format(piDate));

        if(dateChanged) {
            // Causes a reboot after date/time change
            try {
                Process reboot = Runtime.getRuntime().exec(Constants.REBOOT_COMMAND);
            } catch (IOException e) {
                appLogger.debug(e);
            }
        }
    }

    /**
     * @return the date currently on the UI
     */
    public String getDate() {
        return date.getText();
    }

    /**
     * @param hasPassed indicates if any switch has failed, which will set this to false
     */
    public void setHasPassed(boolean hasPassed) {
        this.hasPassed = hasPassed;
    }

    public boolean isHasPassed() {
        return hasPassed;
    }

    /**
     * @return the active string of switches
     */
    public ObservableList<Switch> getSwitches() {
        return readSwitches;
    }

    public void hideSave() {
        resetSave();
        saveBackground.setVisible(false);
    }

    /**
     * Hides the save progress bar and shows the buttons again
     */
    private void resetSave() {
        dayButton.setVisible(true);
        weekButton.setVisible(true);
        monthButton.setVisible(true);
        saveProgressBar.setVisible(false);
    }

    /**
     * Called from the SwitchService
     * @param count is the number of switches actually seen
     */
    public void incrementTotal(int count) {
        Platform.runLater(() -> {
            totalLabel.setText(Integer.toString(count));
        });
    }

    /**
     * Gets the version of the application that is in the properties file
     * @return the current version number of the application
     */
    private String getVersion() {
        return version;
    }

    /**
     * Sets the path to the application based on operating system
     */
    private void setAppPath() {
        if(ShootingPanel.getOS().equals(Constants.LINUX)) {
            this.appPath = Paths.get(Constants.LINUX_APP_PATH);
        } else if(ShootingPanel.getOS().equals(Constants.MAC)) {
            this.appPath = Paths.get(Constants.MAC_APP_PATH);
        }
    }

    /**
     * Sets the path to the USB based on operating system
     */
    private void setTargetPath() {
        if(ShootingPanel.getOS().equals(Constants.LINUX)) {
            this.targetPath = Paths.get(Constants.LINUX_USB_PATH);
        }
    }

    /**
     * This method reverses the switch array and updates the custom cells of the List View for the switches
     * @param switchCount is the number of switches that the Switch Service found
     * @param isSetPresent is indication of a set plug - used for the Hybrid switches
     */
    public void showResult(boolean isSetPresent, int switchCount, int volts, double amps) {
            if(previousSwitches.size() > 0 && switches.size() <= previousSwitches.size()) {
                for(int i = 0; i < switches.size() - 1; i++) {
                    if(!switches.get(i).getAddress().equals(previousSwitches.get(i).getAddress())) {
                        gunCountInstructionLabel.setText(Constants.FAILED_STRING);
                        message = Constants.GUN_ADDRESS_ERROR;
                        gunCountPane.setVisible(true);
                    }
                }
            }

            arm = false;
            this.switchCount = switchCount;

            if(isSetPresent) {
                messageLabel.setText(Constants.SETTING_SWITCH);
                ignitorModePin.setState(PinState.HIGH);
            } else {
                ignitorModePin.setState(PinState.LOW);
            }

            // gunString starts out as 0 so this will set gun string on first run
            if(gunString == 0) {
                gunString = switchCount;
                expectedIndex = gunString;
                previousSwitches.addAll(switches);
                switchService.setMasterString(gunString);
                shortService.setFirstRun(false);
            }

            // Create i so that gunstring does not decrement
            int i = gunString;

            // This reverses the switch IDs to show bottom switch as switch 1
            for(Switch s : switches) {
                s.setOriginalIndex(s.getId());
                s.setId(Integer.toString(i--));

                if((s.getId().equals(Constants.ONE)) && !(s.isSetting()) && !plugSet) {
                    // This is the case when it is switch 1, but not a setting switch address
                    s.setPassed(false);
                    s.setMessage(Constants.NOT_SETTING_SWITCH);
                } else if((s.isSetting()) && !(s.getId().equals(Constants.ONE))) {
                    // This is the case when setting address is encountered and it is not switch 1
                    s.setPassed(false);
                    s.setMessage(Constants.MISPLACED_SETTING_SWITCH);
                } else if(s.getId().equals(Constants.ONE) && changedSetting) {
                    // When switch was changed to setting switch, this turns the node from red to green
                    s.setPassed(true);
                }

                // Shorted setting switch is missing ignitor
                if((s.isSetting() && !s.isFeedthrough()) || (!s.isDetonator() && switchCount > 0)) {
                    detMissing = true;
                }

                if(changedDuplicate) {
                    s.setPassed(true);
                }
            }

            switchList.setItems(switches);

            if(currentJob.getMode().equals(Constants.ADDRESSABLE_MODE)) {
                switchList.setCellFactory(lv -> new ListCell<Switch>() {
                    Node view;
                    SwitchController controller;

                    @Override
                    protected void updateItem(Switch item, boolean empty) {
                        Platform.runLater(() -> {
                            super.updateItem(item, empty);

                            try {
                                loader = new FXMLLoader(getClass().getResource(Constants.SWITCH_CELL_VIEW));
                                view = loader.load();
                                controller = loader.getController();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                            if(empty) {
                                setGraphic(null);
                            } else {
                                if(!item.isPassed()) {
                                    // Sets the switch color to red if it is a failed switch
                                    controller.setIconColor(Constants.RED_COLOR);
                                }

                                // If communication error, cell background is red and no icons
                                if(item.getMessage().equals(Constants.COMM_ERROR)) {
                                    controller.setErrorBackground();
                                } else {
                                    controller.addSetting(item.isSetting());
                                    controller.addRelease(item.isRelease());
                                    controller.setThrough(item.isFeedthrough(), item.isSetting());
                                    controller.setDet(item.isDetonator());
                                }

                                controller.setLabel(item.getId());
                                setGraphic(view);
                            }
                        });
                    }
                });

                if(switches.size() > 0) {
                    switchList.scrollTo(switchList.getItems().get(switches.size() - 1));
                    switchList.getSelectionModel().select(switches.size() - 1);
                }
            }

            if(hasPassed && !shorted) {
                messageLabel.setStyle(Constants.STYLE_BACKGROUND_GRAY);
            } else {
                //messageLabel.setStyle(Constants.STYLE_BACKGROUND_RED);
            }

            inventoryButton.setDisable(false);

            try {
                if(currentJob.getMode().equals(Constants.ADDRESSABLE_MODE)) {
                    infoAddressLabel.setText(switchList.getSelectionModel().getSelectedItem().getAddress());
                    infoAddressLabel.setVisible(true);

                    if(switchList.getSelectionModel().getSelectedItem().isShorted()) {
                        indicator.setImage(new Image("/images/shortIndicator.png"));
                    } else if(switchList.getSelectionModel().getSelectedItem().isFeedthrough()) {
                        indicator.setImage(new Image("/images/thruIndicator.png"));
                    } else {
                        indicator.setImage(new Image("/images/openIndicator.png"));
                    }

                    indicator.setVisible(true);
                }
            } catch (NullPointerException e) {
                appLogger.debug(e);
            }

            // This will leave the line powered up after arming for the ability to send fire command
            // If hybrid then either have line on or off
            if(currentJob.getMode().equals(Constants.ADDRESSABLE_MODE)) {
                if(isArmed) {
                    armButton.setDisable(true);
                    fireButton.setDisable(false);
                    arm = true;
                } else {
                    armButton.setDisable(false);
                    fireButton.setDisable(true);
                    arm = false;

                    setSwitchLinePower("OFF");
                    // Sleep the thread to let the line settle
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        appLogger.debug(e);
                    }
                }

                // Keep track of the current gun count that it is correct
                if(switchCount >= 0 && switchCount != expectedIndex) {
                    if(isArmed) {
                        gunCountInstructionLabel.setText(Constants.COUNT_ERROR + Constants.SKIP_COUNT);
                        gunCountResetButton.setText(Constants.CONTINUE);
                    }
                    message = Constants.GUN_COUNT_ERROR;
                    gunCountPane.setVisible(true);
                }
            } else if(currentJob.getMode().equals(Constants.HYBRID_MODE) && !hybridFire) {
                // This scenario is Hybrid mode and regular inventory
                fireButton.setDisable(false);
                setSwitchLinePower("OFF");
                // Sleep the thread to let the line settle
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    appLogger.debug(e);
                }
            }

            if(switchCount == 0) {
                setSwitchLinePower("OFF");
                // Sleep the thread to let the line settle
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    appLogger.debug(e);
                }
            }

            Platform.runLater(() -> {
                if(currentJob.getMode().equals(Constants.ADDRESSABLE_MODE))
                    totalPane.setVisible(false);

                totalSwitchLabel.setText(Constants.NO_STRING + switchCount);
                totalSwitchLabel.setVisible(true);
            });

            // This will blink the total red if a detonator is missing as indication to the user
            if(detMissing) {
                Thread noDetThread = new Thread(noDetService);
                noDetService.start();
                noDetThread.setDaemon(true);
                noDetThread.start();
            }

            readSwitches.addAll(switches);

            // This writes to file for inventory and arm is whether it is an arm inventory or regular
            Platform.runLater(() -> {
                geoWrite(Constants.INVENTORY, arm, volts, amps);
                arm = false;
            });
    }

    private void geoWrite(String type, boolean arm, int volts, double amps) {
        if(type.equals("CHECK")) {
            geoWriter = new GEOWriterService(this, volts, amps, switchCount, currentJob.getMode(),
                    message, currentJob, checkVoltageData, checkCurrentData, type, arm);
        } else {
            geoWriter = new GEOWriterService(this, volts, amps, switchCount, currentJob.getMode(),
                    message, currentJob, voltageData, currentData, type, arm);
        }

        writerThread = new Thread(geoWriter);
        writerThread.setDaemon(true);
        writerThread.start();
    }

    public void setSwitchLinePower(String power) {
        switch (power) {
            case "ON":
                firePowerPin.setState(PinState.HIGH);
                break;
            case "OFF":
                firePowerPin.setState(PinState.LOW);
                break;
        }
    }

    public void setNinetyVoltLine(String position) {
        switch (position) {
            case "HIGH":
                ninetyVoltPin.setState(PinState.HIGH);
                break;
            case "LOW":
                ninetyVoltPin.setState(PinState.LOW);
                break;
        }
    }

    /**
     * @param switches is the switch list from the Switch Service
     */
    public void addSwitches(ObservableList<Switch> switches) {
        this.switches = switches;
    }

    public Path getFileDir() {
        return Paths.get(currentJob.getJobDirPath());
    }

    /**
     * This method will shut down the switch power line and reset services if a short is detected
     */
    public void handleShort() {
        setSwitchLinePower("OFF");
        switchService.stop();
        messageLabel.setVisible(true);

        if(switchCount == expectedIndex || switchCount - 1 == expectedIndex) {
            messageLabel.setStyle(Constants.STYLE_BACKGROUND_GRAY);
        } else {
            messageLabel.setStyle(Constants.STYLE_BACKGROUND_RED);
            messageLabel.setText(Constants.SHORT_DETECTED);
        }

        switchService.reset();
        shorted = true;
    }

    public TimeService getTimeService() {
        return timeService;
    }

    public Path getAppPath() {
        return this.appPath;
    }

    /**
     * @return the path the USB
     */
    public Path getTargetPath() {
        return this.targetPath;
    }

    public void setJobDirPath(String path) {
        currentJob.setJobDirPath(path);
    }

    /**
     * @param error is the error message from a failed test (currently a shorted line)
     */
    public void setTestError(String error) {
        testError.append(error);
        messageLabel.setText(error);
        messageLabel.setStyle(Constants.STYLE_BACKGROUND_RED);
        messageLabel.setVisible(true);
    }

    public String getTestError() {
        return testError.toString();
    }

    /**
     * This method displays the IP Address for local network 10.158.237
     */
    private void getIPAddress() {
        Enumeration interfaces = null;
        boolean found = false;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            appLogger.debug(e);
        }

        if(interfaces != null) {
            while(interfaces.hasMoreElements()) {
                NetworkInterface network = (NetworkInterface) interfaces.nextElement();
                Enumeration addresses = network.getInetAddresses();
                while(addresses.hasMoreElements() && !found) {
                    InetAddress address = (InetAddress) addresses.nextElement();
                    if(address.getHostAddress().startsWith("10.158.237")) {
                        found = true;
                        Platform.runLater(() -> ipLabel.setText(address.getHostAddress()));
                        // Secret button only accessible on GEO local network
                        exitButton.setDisable(false);
                    } else {
                        ipLabel.setText("" + getVersion());
                    }
                }
            }
        }
    }

    private void hideGridPanes() {
        monthPane.setVisible(false);
        dayPane.setVisible(false);
        yearPane.setVisible(false);
        hourPane.setVisible(false);
        minutePane.setVisible(false);
        ampmPane.setVisible(false);
    }

    public void setTreeView(TreeView<String> tree) {
        fileTree = tree;
        fileScrollPane.setContent(fileTree);
    }

    public void setSettingSwitch(Switch settingSwitch, boolean isSetting) {
        if(isSetting) {
            fireButton.setText(Constants.SET_PLUG);
        } else {
            fireButton.setText(Constants.FIRE);
        }

        this.settingSwitch = settingSwitch;
        this.settingSwitch.setSetting(isSetting);
    }

    /**
     * This method gets called only when a change is detected from the key
     * Handled from the ADCService
     * @param keyPosition is the ADC reading for the position of the front panel key
     */
    public void handleKeyPosition(double keyPosition) {
        int LOG = 1, SAFE = 2, CCL = 3, FIRE = 4;
        Platform.runLater(() -> {
            if((keyPosition < .5) && testPane.isVisible()) {
                messageLabel.setStyle(Constants.STYLE_BACKGROUND_BLACK);
                messageLabel.setText("AUX");
                checkReleaseButton.setDisable(true);
                checkFireButton.setDisable(true);
                safePane.setVisible(true);
                cclPane.setVisible(false);
                canRelease = false;
                inventoryButton.setDisable(true);
                checkConfirmButton.setDisable(true);
                isInSafe = false;
            } else if((keyPosition >= LOG - .5 && keyPosition < LOG + .5) && testPane.isVisible()) {
                messageLabel.setStyle(Constants.STYLE_BACKGROUND_BLACK);
                messageLabel.setText("LOG");
                checkReleaseButton.setDisable(true);
                checkFireButton.setDisable(true);
                safePane.setVisible(true);
                cclPane.setVisible(false);
                canRelease = false;
                inventoryButton.setDisable(true);
                checkConfirmButton.setDisable(true);
                isInSafe = false;
            } else if((keyPosition >= SAFE - .5 && keyPosition < SAFE + .5) && testPane.isVisible()) {
                messageLabel.setStyle(Constants.STYLE_BACKGROUND_BLACK);
                messageLabel.setText("SAFE");
                checkReleaseButton.setDisable(true);
                checkFireButton.setDisable(true);
                safePane.setVisible(true);
                cclPane.setVisible(false);
                canRelease = false;
                inventoryButton.setDisable(true);
                checkConfirmButton.setDisable(true);
                newJobButton.setVisible(true);
                resumeJobButton.setVisible(true);
                startSafeLabel.setVisible(false);
                isInSafe = true;
                plugYesButton.setDisable(true);
                plugNoButton.setDisable(true);
            } else if((keyPosition >= CCL - .5 && keyPosition < CCL + .5) && testPane.isVisible()) {
                messageLabel.setStyle(Constants.STYLE_BACKGROUND_BLACK);
                checkReleaseButton.setDisable(true);
                checkFireButton.setDisable(true);
                safePane.setVisible(false);
                cclPane.setVisible(true);
                canRelease = false;

                if(shootService != null) {
                    shootService.stop();
                }

                armPin.setState(PinState.LOW);
                setSwitchLinePower("OFF");
                inventoryButton.setDisable(true);
                checkConfirmButton.setDisable(true);
                isInSafe = false;
            } else if((keyPosition >= CCL - .5 && keyPosition < CCL + .5) && wrtPane.isVisible()) {
                if(shootService != null) {
                    shootService.stop();
                }

                armPin.setState(PinState.LOW);
                setSwitchLinePower("OFF");
                canRelease = false;
                inventoryButton.setDisable(true);
                checkConfirmButton.setDisable(true);
                isInSafe = false;
            } else if((keyPosition >= FIRE - .5 && keyPosition < FIRE + .5) && (testPane.isVisible() || wrtPane.isVisible())) {
                if(hasPassed && !shorted) {
                    messageLabel.setStyle(Constants.STYLE_BACKGROUND_GRAY);
                    messageLabel.setText("STAGE " + currentJob.getStage());
                } else if(!hasPassed) {
                    messageLabel.setText(testError.toString());
                    messageLabel.setStyle(Constants.STYLE_BACKGROUND_RED);
                }

                checkReleaseButton.setDisable(false);
                checkFireButton.setDisable(false);
                safePane.setVisible(false);
                cclPane.setVisible(false);
                canRelease = true;
                inventoryButton.setDisable(false);
                checkConfirmButton.setDisable(false);
                isInSafe = false;
                plugYesButton.setDisable(false);
                plugNoButton.setDisable(false);
            } else {
                if(keyPosition >= SAFE - .5 && keyPosition < SAFE + .5) {
                    newJobButton.setVisible(true);
                    resumeJobButton.setVisible(true);
                    startSafeLabel.setVisible(false);
                    isInSafe = true;
                } else {
                    newJobButton.setVisible(false);
                    resumeJobButton.setVisible(false);
                    startSafeLabel.setVisible(true);
                    isInSafe = false;
                }

                checkFireButton.setDisable(false);
                canRelease = false;
            }
        });

        adcService.setLastPosition(keyPosition);
    }

    /**
     * This method toggles the total from noDetService to blink it
     */
    public void blinkTotal(boolean blink) {
        Platform.runLater(() -> totalSwitchLabel.setVisible(blink));
    }

    /**
     * This method changes the total red or black depending on whether there is a short
     */
    public void changeTotalColor(String color) {
        if(color.equals("black")) {
            totalSwitchLabel.setStyle(Constants.STYLE_TEXT_BLACK);
        } else if(color.equals("red")) {
            totalSwitchLabel.setStyle(Constants.STYLE_TEXT_RED);
        }

    }
}