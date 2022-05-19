/*
 *   GEODynamics
 *   Author - James Suderman
 *   Date - 12/18/2017
 *
 *   This service gets the correct files and exports them to USB
 */

package com.perf.shootingPanel.Services;

import com.perf.shootingPanel.Constants;
import com.perf.shootingPanel.controllers.AppController;
import javafx.application.Platform;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class FileExportService {
    private static Logger exportLogger = Logger.getLogger(FileExportService.class);
    private String button;
    private AppController controller;
    private List<File> files = new ArrayList<>();
    private File folder;
    private Calendar cal = Calendar.getInstance();
    private Calendar weekCal = Calendar.getInstance();
    private Calendar monthCal = Calendar.getInstance();
    private Calendar fileCal = Calendar.getInstance();
    private Calendar hourCal = Calendar.getInstance();
    private Calendar dayCal = Calendar.getInstance();
    private double fileCount = 0;

    public FileExportService(String button, AppController controller) {
        this.button = button;
        this.controller = controller;
    }

    public void export() {
        // found is referring to USB drive
        boolean found = false;

        controller.updateProgress(0);

        try {
            folder = new File(controller.getFileDir().toString());
        } catch (Exception e) {
            exportLogger.debug(Constants.NO_FOLDER_FOUND);
        }

        // 7 or 30 days prior to current day
        weekCal.add(Calendar.DAY_OF_YEAR, -7);
        monthCal.add(Calendar.DAY_OF_YEAR, -30);
        dayCal.add(Calendar.DAY_OF_YEAR, -2);

        if(folder.listFiles() != null) {
            for(File file : folder.listFiles()) {
                try {
                    String[] parts = file.getName().split("\\.");
                    if(parts.length > 2) {
                        String ext = parts[2];
                        if(ext.equals("txt")) {
                            parts = file.getName().split("-");
                            String date = parts[0];

                            try {
                                fileCal.setTime(new SimpleDateFormat(Constants.DATE_FORMAT_FILE).parse(date));
                            } catch (ParseException e) {
                                exportLogger.debug(e);
                            }

                            // Handle which files get added to the list of files based upon which button was pressed
                            if(button.equals("dayButton")) {
                                if((fileCal.after(dayCal) && fileCal.before(cal))) {
                                    files.add(file);
                                }
                            } else if(button.equals("weekButton")) {
                                if(fileCal.after(weekCal) && fileCal.before(cal)) {
                                    files.add(file);
                                }
                            } else if(button.equals("monthButton")) {
                                if(fileCal.after(monthCal) && fileCal.before(cal)) {
                                    files.add(file);
                                }
                            }
                        }
                    }

                    controller.updateProgress((++fileCount/2)/folder.listFiles().length);

                } catch(Exception e) {
                    exportLogger.debug(e);
                }
            }
        }

        if(new File(Constants.LINUX_DEVICE_PATH).exists()) {
            try {
                Process mount = Runtime.getRuntime().exec(Constants.MOUNT_COMMAND);
                Thread.sleep(250);
            } catch (InterruptedException | IOException  e) {
                exportLogger.debug(e);
            }

            for (File file : files) {
                try {
                    ProcessBuilder copy = new ProcessBuilder(Constants.SUPER_USER, Constants.COPY, file.getAbsolutePath(), Constants.LINUX_USB_PATH);
                    copy.start();
                    controller.updateProgress(((++fileCount/2) + .5)/files.size());
                    Thread.sleep(250);
                } catch (IOException | InterruptedException e) {
                    exportLogger.debug(e);
                }
            }
        }

        Platform.runLater(() -> {
            controller.hideSave();
            controller.updateProgress(0);
        });


        try {
            Process unmount =  Runtime.getRuntime().exec(Constants.UNMOUNT_COMMAND + Constants.LINUX_USB_PATH);
        } catch (IOException e) {
            exportLogger.debug(e);
        }
    }
}
