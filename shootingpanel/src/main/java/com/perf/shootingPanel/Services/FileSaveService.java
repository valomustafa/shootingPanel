/*
 *   GEODynamics
 *   Author - James Suderman
 *   Date - 03/25/2019
 *
 *   This service will save a properties file to represent a new job as format: company_well_stage_timestamp.properties
 *   Also will create a directory on the Jobs root if one is not present
 */

package com.perf.shootingPanel.Services;

import com.perf.shootingPanel.Constants;
import com.perf.shootingPanel.controllers.AppController;
import com.perf.shootingPanel.helpers.PropertyHelper;
import com.perf.shootingPanel.models.Job;
import javafx.application.Platform;
import org.apache.log4j.Logger;

import java.io.*;

public class FileSaveService implements Runnable {
    private static Logger chooserLogger = Logger.getLogger(FileChooserService.class);
    private PropertyHelper saveFileHelper;
    private AppController controller;
    private Job currentJob;

    public FileSaveService(AppController controller) {
        this.controller = controller;
    }

    @Override
    public void run() {
        try {
            File jobDir = new File(Constants.LINUX_JOB_PATH +
                    File.separator + currentJob.getCompany().replace(' ', '-') +
                    File.separator + currentJob.getJobName().replace(' ', '-') +
                    File.separator + currentJob.getWell().replace(' ', '-')
            );

            Process dirAccess = Runtime.getRuntime().exec(Constants.GIVE_DIR_ACCESS + Constants.LINUX_JOB_PATH);

            // Create the job directory if it does not exist
            jobDir.mkdirs();

            Process folder = Runtime.getRuntime().exec(Constants.GIVE_DIR_ACCESS + jobDir.getAbsolutePath());

            // Format: home/pi/Files/Jobs/Company/Job/Well/ <companyName>/<jobName>/<wellName>.properties
            String path = jobDir + File.separator + currentJob.getCompany() + "_" +
                    currentJob.getJobName() + "_" + currentJob.getWell() + Constants.PROPERTIES_EXTENSION;

            // Use replace to change all spaces to hyphens so no naming issues for Linux OS
            String modifiedPath = path.replace(' ', '-');

            controller.setJobDirPath(jobDir.getAbsolutePath());

            File file = new File(modifiedPath);

            // Save the properties
            saveFileHelper = new PropertyHelper(file.getAbsolutePath());

            saveFileHelper.addProp(Constants.JOB_PROPERTY, currentJob.getJobName());
            saveFileHelper.addProp(Constants.COMPANY_PROPERTY, currentJob.getCompany());
            saveFileHelper.addProp(Constants.WELL_PROPERTY, currentJob.getWell());
            saveFileHelper.addProp(Constants.MODE_PROPERTY, currentJob.getMode());
            saveFileHelper.addProp(Constants.COLOR_PROPERTY, currentJob.getColor());
            saveFileHelper.addProp(Constants.ENGINEER_PROPERTY, currentJob.getEngineer());
            saveFileHelper.addProp(Constants.STAGE_PROPERTY, String.valueOf(currentJob.getStage()));
            saveFileHelper.updateProps();

            Platform.runLater(() -> controller.initializeJobProperties(file.getAbsolutePath()));

        } catch (IOException e) {
            chooserLogger.debug(e);
        }

    }

    public void setJob(Job currentJob) {
        this.currentJob = currentJob;
    }
}
