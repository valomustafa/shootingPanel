/*
 *   GEODynamics
 *   Author - James Suderman
 *   Date - 05/08/2018
 */

package com.perf.shootingPanel;

public class Constants {
    // Global Application --------------------------------------------------
    public static String APPLICATION = "ShootingPanel";
    public static String APPLICATION_PREPEND = "ShootingPanel-";
    public static String APPLICATION_POSTPEND = "-jar-with-dependencies.jar";
    public static String IP_ADDRESS = "10.158.237";
    // System Command Line Commands ----------------------------------------
    public static String SUPER_USER = "sudo";
    public static String MOVE = "mv";
    public static String COPY = "cp";
    public static String CLOCK = "hwclock";
    public static String DATE = "date";
    public static String SAVE_FLAG = "-s";
    public static String WRITE_FLAG = "-w";
    public static String SHUTDOWN_COMMAND = "sudo shutdown -h now";
    public static String REBOOT_COMMAND = "sudo reboot";
    public static String MOUNT_COMMAND = "/home/pi/Documents/mount.sh";
    public static String GIVE_FILE_ACCESS = "sudo chmod 777 ";
    public static String GIVE_DIR_ACCESS = "sudo chmod -R 777 ";
    public static String UNMOUNT_COMMAND = "sudo umount ";
    // System Paths --------------------------------------------------------
    //      LINUX ----------------------------------------------------------
    public static String LINUX = "Linux";
    public static String LINUX_APP_PATH = "/home/pi/Developer";
    public static String BACKUP_PROP_PATH = "/home/pi/Documents";
    public static String LINUX_USB_PATH = "/media/usb";
    public static String LINUX_DEVICE_PATH = "/dev/sda";
    public static String LINUX_JOB_PATH = "/home/pi/Files/Jobs";
    public static String LINUX_ARCHIVE_PATH = "/home/pi/Files/Archive";
    public static String LINUX_ASSET_PATH = "/home/pi/Files/Assets";
    //      MAC ------------------------------------------------------------
    public static String MAC = "MacOS";
    public static String MAC_APP_PATH = "/Users/jamessuderman/Developer/GEODynamics/ShootingPanel/shooting-panel/src/main/resources/app";
    // Properties ----------------------------------------------------------
    public static String PROPERTIES_FILE_NAME = "app.properties";
    public static String CURRENT_VERSION_PROPERTY = "currentVersion";
    public static String OLD_VERSION_PROPERTY = "oldVersion";
    public static String PANEL_VERSION_PROPERTY = "panelVersion";
    public static String SERIAL_NUMBER_PROPERTY = "serialNumber";
    public static String MODE_PROPERTY = "mode";
    public static String OS_PROPERTY = "os.name";
    public static String JOB_PROPERTY = "jobName";
    public static String COMPANY_PROPERTY = "company";
    public static String WELL_PROPERTY = "well";
    public static String COLOR_PROPERTY = "color";
    public static String ENGINEER_PROPERTY = "engineer";
    public static String STAGE_PROPERTY = "stage";
    // Modes ---------------------------------------------------------------
    public static String WRITE_MODE = "write";
    public static String READ_MODE = "read";
    public static String HYBRID_MODE = "HYBRID MODE";
    public static String ADDRESSABLE_MODE = "ADDRESSABLE MODE";
    public static String TEST_MODE = "TEST";
    public static String FIRE_MODE = "FIRE";
    public static String UPDATED_MODE = "updated";
    public static String UP_TO_DATE_MODE = "upToDate";
    public static String NOT_UPDATED = "notUpdated";
    // Resources ---------------------------------------------------------------
    public static String SWITCH_CELL_VIEW = "/views/switchCell.fxml";
    public static String APPLICATION_VIEW = "/views/application.fxml";
    public static String FILE_IMAGE = "/images/file.png";
    public static String FOLDER_IMAGE = "/images/folder-closed-black-shape.png";
    // Formats -------------------------------------------------------------
    public static String DATE_FORMAT_LONG = "MM-dd-yyyy - h:mm a";
    public static String DATE_FORMAT_SHORT = "yyyy-MM-dd HH:mm:ss";
    public static String DATE_FORMAT_FILE = "MMddyyyy";
    public static String DATE_FORMAT_FILE_LONG = "MMddyyyy-HHmmss";
    public static String PI_DATE_FORMAT = "MM/dd/yyyy HH:mm";
    public static String HEX_FORMAT = "%02X";
    public static String THREE_DIGIT_FORMAT = "%03d";
    public static String TEXT_EXTENSION = ".txt";
    public static String PROPERTIES_EXTENSION = ".properties";
    // Set Strings ---------------------------------------------------------
    public static String BLANK_SPACE = " ";
    public static String NO_STRING = "";
    public static String NONE = "None";
    public static String ZERO = "0";
    public static String ONE = "1";
    public static String THIRTY = "30";
    // Messages ------------------------------------------------------------
    public static String NO_USB = "NO USB DETECTED";
    public static String SHORT_DETECTED = "Short Detected";
    public static String NO_BOTTOM_SWITCH = "Final Switch Error";
    public static String NOT_SETTING_SWITCH = "Not a setting switch";
    public static String SETTING_SWITCH = "Setting Switch Detected";
    public static String MISPLACED_SETTING_SWITCH = "Misplaced setting switch";
    public static String NO_FOLDER_FOUND = "No folder found";
    public static String COULD_NOT_WRITE_FILE = "Could not write file";
    public static String DUPLICATE_SWITCH = "Duplicate Switch";
    public static String SHORT_CIRCUIT = "Possible Short Circuit";
    public static String NO_DET = "No Detonator";
    public static String RELEASE_TOOL = "Release Tool";
    public static String UP_TO_DATE = "SOFTWARE UP TO DATE";
    public static String INCOMPATIBLE_UPDATE = "SOFTWARE AND PANEL ARE INCOMPATIBLE";
    public static String INVALID_UPDATE = "INVALID SOFTWARE UPDATE";
    public static String UPDATED = "SOFTWARE UPDATED";
    public static String NO_FILES = "NO FILES TO LOAD OR UPDATE";
    public static String PROP_UPDATE = "PROPERTIES UPDATED";
    public static String RESOURCES = "RESOURCES LOADED";
    public static String UPDATE_FAILED = "UPDATE FAILED";
    public static String COMM_ERROR = "COMMUNICATION ERROR";
    public static String BAD_ADDRESS = "CHANGE ADDRESS";
    public static String VOLTS = "V ";
    public static String AMPS = " A";
    public static String CONFIRM = "CONFIRM";
    public static String RELEASE = "RELEASE";
    public static String INVENTORY = "INVENTORY";
    public static String CONTINUE = "CONTINUE";
    public static String RESET = "RESET";
    public static String SHOT = "SHOT";
    public static String FIRE = "FIRE";
    public static String CHECK = "CHECK";
    public static String SET_PLUG = "SET PLUG";
    public static String SHOT_EVENT = "SHOT EVENT";
    public static String DO_NOTHING = "Catching action to do nothing with";
    public static String RELEASE_SEQUENCE = "Initiating the release sequence WILL release the cable head. Are you sure you want to continue?";
    public static String CHECK_OPEN = "Check Open";
    public static String CHECK_SHORT = "Check Short";
    public static String CHECK_OPEN_MESSAGE = "Press FIRE to check for OPEN";
    public static String CHECK_SHORT_MESSAGE = "Press FIRE to check for SHORT";
    public static String GUN_COUNT_ERROR = "GUN COUNT ERROR";
    public static String COUNT_ERROR = "There was a gun count error. Possible solution is another inventory.";
    public static String SKIP_COUNT = "\n IF skipping a gun press CONTINUE";
    public static String GUN_ADDRESS_ERROR = "GUN ADDRESS ERROR";
    public static String FAILED_STRING = "Gun address errors. Possible solution is another inventory.";
    public static String DELETE_STRING = "Are you sure you want to delete";
    // Names ----------------------------------------------------------------
    public static String FIRE_POWER_PIN = "firePowerPin";
    public static String SHORT_PIN = "shortPin";
    public static String RESET_PIN = "resetPin";
    public static String SIXTY_VOLT_PIN = "sixtyVoltPin";
    public static String ARM_PIN = "armPin";
    public static String SHOOT_PIN = "shootPin";
    public static String HYBRID_PIN = "hybridPin";
    public static String IGNITOR_PIN = "ignitorModePin";
    public static String MONTH = "month";
    public static String DAY = "day";
    public static String YEAR = "year";
    public static String HOUR = "hour";
    public static String MINUTE = "minute";
    public static String AMPM = "ampm";
    public static String SET_FIRE_ADDRESS = "FFFCFF";
    public static String SUCCESS_ICON = "CHECK";
    public static String FAIL_ICON = "CLOSE";
    // IDs ------------------------------------------------------------------
    public static String SAVE_CLOSE = "saveClose";
    public static String GEAR_CLOSE = "gearClose";
    public static String MONTH_FIELD = "monthField";
    public static String DAY_FIELD = "dayField";
    public static String YEAR_FIELD = "yearField";
    public static String HOUR_FIELD = "hourField";
    public static String MINUTE_FIELD = "minuteField";
    public static String AMPM_FIELD = "ampmField";
    public static String QR_CLOSE = "qrClose";
    // Styles ---------------------------------------------------------------
    public static String STYLE_BACKGROUND_RED = "-fx-background-color: #E82C0C;";
    public static String STYLE_BACKGROUND_GRAY = "-fx-background-color: #5f6265;";
    public static String STYLE_BACKGROUND_BLACK = "-fx-background-color: #000000;";
    public static String STYLE_TEXT_BLACK = "-fx-text-fill: black;";
    public static String STYLE_TEXT_RED = "-fx-text-fill: #E82C0C;";
    public static String BLACK_COLOR = "#000000";
    public static String RED_COLOR = "#E82C0C";
    public static String GREEN_COLOR = "#5cc941";
    public static String STYLE_BUTTON_SELECTED = "buttonSelected";
    public static String STYLE_BUTTON_UNSELECTED = "buttonUnselected";
    public static String STYLE_DELETE_ON = "deleteOn";
    public static String STYLE_DELETE_OFF = "deleteOff";
    public static String INVALID = "invalid";
    public static String VALID = "valid";
    public static String STYLE_RELEASE_CONFIRM = "-fx-background-color: white; -fx-text-fill: #E82C0C; -fx-border-color: white;";
    public static String STYLE_RELEASE_CANCEL = "-fx-background-color: #E82C0C; -fx-text-fill: white; -fx-border-color: white;";
    public static String STYLE_RELEASE_INSTRUCTIONS = "-fx-text-fill: black; -fx-font-size: 1.8em;";
}