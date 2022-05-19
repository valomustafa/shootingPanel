/*
 *   GEODynamics
 *   Author - James Suderman
 *   Date - 03/26/2019
 */

package com.perf.shootingPanel.models;

import com.perf.shootingPanel.Constants;

public class Job {

    private String mode;
    private String color;
    private String jobName;
    private String company;
    private String well;
    private String engineer;
    private String jobDirPath;
    private double stage = 1;

    public Job() {}

    public Job(String mode, String jobName, String company, String well, String color, String engineer, double stage) {
        this.mode = mode;
        this.jobName = jobName;
        this.company = company;
        this.well = well;
        this.color = color;
        this.engineer = engineer;
        this.stage = stage;
    }

    public String getMode() {
        if(mode == null) {
            return Constants.HYBRID_MODE;
        } else {
            return mode;
        }
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getWell() {
        return well;
    }

    public void setWell(String well) {
        this.well = well;
    }

    public String getColor() {
        if(color == null) {
            return Constants.BLACK_COLOR;
        } else {
            return color;
        }
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getEngineer() {
        return engineer;
    }

    public void setEngineer(String engineer) {
        this.engineer = engineer;
    }

    public double getStage() { return stage; }

    public void setStage(double stage) {
        this.stage = stage;
    }

    public String getJobDirPath() { return jobDirPath; }

    public void setJobDirPath(String jobDirPath) { this.jobDirPath = jobDirPath; }

    @Override
    public String toString() {
        return "Job{" +
                "jobName='" + jobName + '\'' +
                ", company='" + company + '\'' +
                ", well='" + well + '\'' +
                ", color='" + color + '\'' +
                ", engineer='" + engineer + '\'' +
                ", mode='" + mode + '\'' +
                ", stage=" + stage +
                '}';
    }
}
