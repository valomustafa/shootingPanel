/*
 *   GEODynamics
 *   Author - James Suderman
 *   Date - 12/18/2017
 *
 *   This service writes every INVENTORY and SHOT to a file for the user to export later
 */

package com.perf.shootingPanel.Services;

import com.perf.shootingPanel.Constants;
import com.perf.shootingPanel.controllers.AppController;
import com.perf.shootingPanel.models.Job;
import com.perf.shootingPanel.models.Switch;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import org.apache.log4j.Logger;

import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GEOWriterService implements Runnable {
    private static Logger writeLogger = Logger.getLogger(GEOWriterService.class);
    private AppController controller;
    private SimpleDateFormat formatter = new SimpleDateFormat(Constants.DATE_FORMAT_FILE_LONG);
    private XYChart.Series<Number, Number> voltageData;
    private XYChart.Series<Number, Number> currentData;
    private String message, mode, type;
    private boolean arm;
    private Job currentJob;
    private int switchCount, volts;
    private double amps;
    private DecimalFormat df = new DecimalFormat("###.##");

    public GEOWriterService(AppController controller, int volts, double amps, int switchCount, String mode, String message,
                            Job currentJob, XYChart.Series<Number, Number> voltageData, XYChart.Series<Number, Number> currentData,
                            String type, boolean arm) {
        this.controller = controller;
        this.volts = volts;
        this.amps = amps;
        this.switchCount = switchCount;
        this.mode = mode;
        this.message = message;
        this.currentJob = currentJob;
        this.voltageData = voltageData;
        this.currentData = currentData;
        this.type = type;
        this.arm = arm;
    }

    public void run() {
        ObservableList<Switch> switches = controller.getSwitches();
        String filePath = currentJob.getJobDirPath();
        FileWriter fw;
        BufferedWriter bw;

        if(currentJob.getCompany().equals("WRT")) {
            filePath = "/home/pi/Files/Jobs/:GEODynamics-RELEASE/:WRT";
        }

        try {
            if(type.equals("INVENTORY")) {
                if(arm) {
                    filePath += File.separator + formatter.format(new Date()) + "_Stage(" + currentJob.getStage() + ")_ARM_" + switchCount + Constants.TEXT_EXTENSION;
                } else {
                    filePath += File.separator + formatter.format(new Date()) + "_Stage(" + currentJob.getStage() + ")_INVENTORY_" + switchCount + Constants.TEXT_EXTENSION;
                }
            } else {
                filePath += File.separator + formatter.format(new Date()) + "_Stage(" + currentJob.getStage() + ")_SHOT_" + switchCount + Constants.TEXT_EXTENSION;
            }

            fw = new FileWriter(new File(filePath));
            bw = new BufferedWriter(fw);

            if(controller.isHasPassed()) {
                bw.write(currentJob.getCompany() + " - " + currentJob.getJobName() + " - " + currentJob.getWell() + "\n");
                bw.write("Engineer - " + currentJob.getEngineer() + "\n");
                bw.write("STAGE " + currentJob.getStage() + "\n");

                if(type.equals("INVENTORY")) {
                    bw.write("Volts " + volts + " : Amps " + new DecimalFormat("#.000").format(amps) + "\n");
                }

                if(arm) {
                    bw.write(mode.split(" ")[0] + " ARM SUCCESSFUL\n");
                } else {
                    if(currentJob.getCompany().equals("WRT")) {
                        bw.write( "RELEASE\n");
                    } else {
                        bw.write(mode.split(" ")[0] + " " + type + " SUCCESSFUL\n");
                    }

                }
            } else {
                bw.write("SHOT FAILED\n");
                bw.write("ERROR: " + controller.getTestError() + "\n");
            }

            bw.write(controller.getDate() + "\n-----------------\n");

            if(mode.equals(Constants.ADDRESSABLE_MODE) && !type.equals("CHECK")) {
                for(Switch i : switches) {
                    bw.write(i.getInfo());
                    bw.write("-----------------\n");
                }

                if(!message.equals(" ")) {
                    bw.write(message + "\n");
                    bw.write("-----------------\n");
                }

                if(!currentJob.getCompany().equals("WRT")) {
                    if(switches.size() <= 0) {
                        bw.write("No switches detected\n");
                    }
                }
            } else if(mode.equals(Constants.HYBRID_MODE) && !type.equals("CHECK")) {
                String detected = switchCount == 1 ? "switch" : "switches";
                bw.write(switchCount + " " + detected + "\n");
                bw.write("-----------------\n");

                if(!message.equals(" ")) {
                    bw.write(message + "\n");
                    bw.write("-----------------\n");
                }
            }

            // Only write shot data if shot
            if((type.equals("SHOT") && switches.size() > 0) || type.equals("CHECK") || currentJob.getCompany().equals("WRT")) {
                bw.write("DATA" + "\n");
                bw.write("-----------------\n");
                bw.write("Time,V,I\n");

                for(int i = 0; i < voltageData.getData().size(); i++) {
                    bw.write(voltageData.getData().get(i).getXValue() + "," +
                            df.format(voltageData.getData().get(i).getYValue()) + "," +
                            df.format(currentData.getData().get(i).getYValue()) + "\n");
                }
            }

            bw.close();
            fw.close();
        } catch(FileNotFoundException e) {
            System.out.println(e);
            writeLogger.debug(Constants.COULD_NOT_WRITE_FILE);
        } catch (IOException e) {
            System.out.println(e);
            writeLogger.debug(e);
        }

        try {
            Process flushPiFileBuffers =  Runtime.getRuntime().exec("sudo sync");
        } catch (IOException e) {
            writeLogger.debug(e);
        }
    }

    public void setMode(String mode) { this.mode = mode; }
}
