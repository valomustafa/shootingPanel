/*
 *   GEODynamics
 *   Author - James Suderman
 *   Date - 12/18/2017
 *
 *   This service will handle the updating of the software
 */

package com.perf.shootingPanel.Services;

import com.perf.shootingPanel.Constants;
import com.perf.shootingPanel.controllers.AppController;
import com.perf.shootingPanel.helpers.PropertyHelper;
import javafx.application.Platform;
import org.apache.log4j.Logger;
import soot.util.Cons;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

public class UpdateService implements Runnable {
    private static Logger updateLogger = Logger.getLogger(UpdateService.class);
    private AppController controller;
    private PropertyHelper propertyHelper;
    private String oldVersion, currentVersion, serialNumber;
    private StringBuilder sb = new StringBuilder();
    private boolean updated = false;

    public UpdateService(AppController controller, PropertyHelper propertyHelper) {
        this.propertyHelper = propertyHelper;
        this.controller = controller;

        currentVersion = this.propertyHelper.getProp(Constants.CURRENT_VERSION_PROPERTY);
        oldVersion = this.propertyHelper.getProp(Constants.OLD_VERSION_PROPERTY);
        serialNumber = this.propertyHelper.getProp(Constants.SERIAL_NUMBER_PROPERTY);
    }

    @Override
    public void run() {
        Platform.runLater(() -> controller.setUpdateProgressVisible(true));

        File oldAppFile = null;
        File currentAppFile = null;
        Path appPath = controller.getAppPath();             //Where the current app is located
        Path targetPath = controller.getTargetPath();       //Where the new app is located

        if (appPath != null) {
            File appFolder = new File(appPath.toString());
            File[] listOfFiles = appFolder.listFiles();
            // Get the application version currently in use
            currentAppFile = new File(appPath.toString() + File.separator + Constants.APPLICATION_PREPEND + propertyHelper.getProp(Constants.CURRENT_VERSION_PROPERTY) + Constants.APPLICATION_POSTPEND);
            // Get the previous version
            oldAppFile = new File(appPath.toString() + File.separator + Constants.APPLICATION_PREPEND + propertyHelper.getProp(Constants.OLD_VERSION_PROPERTY) + Constants.APPLICATION_POSTPEND);

            if (oldAppFile.exists()) {
                oldAppFile.delete();
            }

            // Gets the name of the current application and turn it into the previous version
            if(listOfFiles != null) {
                for(File file : listOfFiles) {
                    if(file.getName().equals(currentAppFile.getName())) {
                        oldVersion = ((currentAppFile.toString()).split("-"))[1]; // Just the version number
                    }
                }
            }
        }

        //If there is a USB and a file in it that is a ShootingPanel file other than the current one installed
        if (new File(Constants.LINUX_DEVICE_PATH).exists()) {
            try {
                Process mount = Runtime.getRuntime().exec(Constants.MOUNT_COMMAND);
                Thread.sleep(1000);
            } catch (InterruptedException | IOException  e) {
                updateLogger.debug(e);
            }

            File targetFolder = new File(targetPath.toString());
            File[] listOfFiles = targetFolder.listFiles();

            if(listOfFiles != null) {
                for (File file : listOfFiles) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        updateLogger.debug(e);
                    }

                    String fileType;
                    String[] fileParts = file.getName().split("\\.");
                    if(file.getName().startsWith(Constants.APPLICATION)) {
                        fileType = fileParts[2];
                    } else {
                        fileType = fileParts[1];
                    }

                    if(fileType.equals("jar")) {
                        String version = ((file.toString()).split("-"))[1]; // Just the version number

                        if(Double.parseDouble(version) >= Double.parseDouble(oldVersion)) {
                            try {
                                if(Double.parseDouble(oldVersion) == Double.parseDouble(version)) {
                                    sb.append(Constants.UP_TO_DATE).append("\n");
                                } else {
                                    Path from = Paths.get(file.getAbsolutePath());
                                    Path to = Paths.get(appPath.toString() + File.separator + file.getName());

                                    moveFile(from, to);

                                    currentVersion = version;

                                    updated = true;
                                    sb.append(Constants.UPDATED).append("\n");
                                }
                            } catch (IOException e) {
                                updateLogger.debug(e);
                                update(Constants.UPDATE_FAILED, updated, true);
                                return;
                            }
                        } else {
                            sb.append(Constants.INVALID_UPDATE).append("\n");
                        }
                    } else if(fileType.equals("properties")) {
                        try {
                            Path from = Paths.get(file.getAbsolutePath());
                            Path to = Paths.get(appPath.toString() + File.separator + Constants.PROPERTIES_FILE_NAME);

                            moveFile(from, to);

                            propertyHelper = new PropertyHelper(appPath.toString() + File.separator + Constants.PROPERTIES_FILE_NAME);
                        } catch (IOException e) {
                            updateLogger.debug(e);
                            update(Constants.UPDATE_FAILED, updated, true);
                            return;
                        }

                        sb.append(Constants.PROP_UPDATE).append("\n");
                    } else {
                        try {
                            File assets = new File(Constants.LINUX_ASSET_PATH);
                            if(!assets.exists()) {
                                assets.mkdirs();
                                Process dirAccess = Runtime.getRuntime().exec(Constants.GIVE_DIR_ACCESS + assets);
                            }

                            Path from = Paths.get(file.getAbsolutePath());
                            Path to = Paths.get(Constants.LINUX_ASSET_PATH + File.separator + file.getName());

                            moveFile(from, to);

                            if(!sb.toString().contains(Constants.RESOURCES)) {
                                sb.append(Constants.RESOURCES).append("\n");
                            }
                        } catch (Exception e) {
                            updateLogger.debug(e);
                            update(Constants.UPDATE_FAILED, updated, true);
                            return;
                        }
                    }
                }
            }
        } else {
            sb.append(Constants.NO_FILES).append("\n");
        }

        propertyHelper.addProp(Constants.CURRENT_VERSION_PROPERTY, currentVersion);
        propertyHelper.addProp(Constants.OLD_VERSION_PROPERTY, oldVersion);
        propertyHelper.addProp(Constants.SERIAL_NUMBER_PROPERTY, serialNumber);
        propertyHelper.updateProps();
        String message = sb.toString().substring(0, sb.toString().length() - 1);
        update(message, updated, false);

        try {
            Process unmount = Runtime.getRuntime().exec(Constants.UNMOUNT_COMMAND);
        } catch (IOException e) {
            updateLogger.debug(e);
        }
    }

    private void moveFile(Path from, Path to) throws IOException {
        try {
            Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
            Thread.sleep(3000);
            Process giveFileAccess = Runtime.getRuntime().exec(Constants.GIVE_FILE_ACCESS + to);
            Thread.sleep(500);
        } catch (InterruptedException e) {
            updateLogger.debug(e.getMessage());
        }
    }

    private void update(String updateMessage, boolean updated, boolean failed) {
        Platform.runLater(() -> {
            controller.showUpdateInfo(updateMessage, updated, failed);
        });
    }
}
