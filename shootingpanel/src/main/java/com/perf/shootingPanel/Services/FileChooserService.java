/*
 *   GEODynamics
 *   Author - James Suderman
 *   Date - 03/21/2019
 *
 *   This service builds the file tree view from the files stored on the pi and sends to controller
 */

package com.perf.shootingPanel.Services;

import com.perf.shootingPanel.Constants;
import com.perf.shootingPanel.controllers.AppController;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.log4j.Logger;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FileChooserService implements Runnable {
    private static Logger chooserLogger = Logger.getLogger(FileChooserService.class);
    private AppController controller;
    private List<File> jobFiles = new ArrayList<>();
    private String menu;
    private boolean wrt = false;

    public FileChooserService(AppController controller) {
        this.controller = controller;
    }

    private Image fileIcon = new Image(getClass().getResourceAsStream(Constants.FILE_IMAGE));
    private Image folderIcon = new Image(getClass().getResourceAsStream(Constants.FOLDER_IMAGE));
    private File activeFolder = new File(Paths.get(Constants.LINUX_JOB_PATH).toString());
    private TreeView<String> fileTree;
    private TreeItem<String> treeItemRoot;

    @Override
    public void run() {
        if("WELL".equals(menu)) {
            if(wrt) {
                treeItemRoot = new TreeItem<>("RELEASE");
                treeItemRoot.setExpanded(true);

                File[] wrtFiles = activeFolder.listFiles();

                if(wrtFiles != null) {
                    for(File file : wrtFiles) {
                        // Only display if it is a properties file
                        if(file.getName().contains(".properties")) {
                            ImageView fileImage = new ImageView(fileIcon);
                            fileImage.setFitHeight(25);
                            fileImage.setFitWidth(25);
                            // Create each leaf file and save to list
                            TreeItem<String> branch = new TreeItem<>(file.getName().replace(".properties", ""), fileImage);
                            treeItemRoot.getChildren().add(branch);
                            jobFiles.add(file);
                        }
                    }
                }

                menu = "WELL";
            } else {
                treeItemRoot = new TreeItem<>(menu);
                treeItemRoot.setExpanded(true);

                File[] wellFolders = activeFolder.listFiles();

                if(wellFolders != null) {
                    List<File> wells = Arrays.asList(wellFolders);
                    Collections.sort(wells);
                    // For all the folders in the directory
                    for(File folder : wells) {
                        // For all the files in each folder
                        File[] folderFiles = folder.listFiles();
                        if(folderFiles != null) {
                            for(File file : folderFiles) {
                                // Only display if it is a properties file
                                if(file.getName().contains(".properties")) {
                                    ImageView fileImage = new ImageView(fileIcon);
                                    fileImage.setFitHeight(25);
                                    fileImage.setFitWidth(25);
                                    // Create each leaf file and save to list
                                    TreeItem<String> branch = new TreeItem<>(file.getName().replace(".properties", ""), fileImage);
                                    treeItemRoot.getChildren().add(branch);
                                    jobFiles.add(file);
                                }
                            }
                        }
                    }
                }
            }
        } else {
            buildTree(activeFolder);
        }

        fileTree = new TreeView<>(treeItemRoot);

        Platform.runLater(() -> {
            controller.setTreeView(fileTree);
            controller.configTree(menu);
        });
    }

    private void buildTree(File activeFolder) {
        treeItemRoot = new TreeItem<>(menu);
        treeItemRoot.setExpanded(true);

        File[] jobFolders = activeFolder.listFiles();

        if(jobFolders != null) {
            List<File> jobs = Arrays.asList(jobFolders);
            Collections.sort(jobs);
            // For all the folders in the directory
            for(File folder : jobs) {
                ImageView folderImage = new ImageView(folderIcon);
                folderImage.setFitHeight(30);
                folderImage.setFitWidth(30);
                // Create a branch
                TreeItem<String> branch = new TreeItem<>(folder.getName(), folderImage);
                treeItemRoot.getChildren().add(branch);
            }
        }
    }

    public void setActiveMenu(String menu) {
        this.menu = menu;
    }

    public void setWrt(boolean wrt) { this.wrt = wrt; }

    public void setActiveFolder(File activeFolder) {
        this.activeFolder = activeFolder;
    }

    public File getFile(String name) {
        for(File file : jobFiles) {
            if((name + ".properties").equals(file.getName())) {
                return file;
            }
        }

        return null;
    }
}
