/*
 *   GEODynamics
 *   Author - James Suderman
 *   Date - 12/18/2017
 */

package com.perf.shootingPanel.controllers;

import com.perf.shootingPanel.Constants;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Line;

public class SwitchController {

    @FXML
    private ImageView switchIcon, throughIcon, detIcon;

    @FXML
    private Label switchLabel;

    @FXML
    private AnchorPane settingOverlay, releaseOverlay, cell;

    @FXML
    private Line wireline;

    public void setLabel(String label) {
        switchLabel.setText(label);
    }

    public void setIconColor(String color) {
        Image dot;

        if(color.equals(Constants.RED_COLOR)) {
            dot = new Image("/images/circle_r.png");
        } else {
            dot = new Image("/images/circle_g.png");
        }

        switchIcon.setImage(dot);
    }

    public void setErrorBackground() {
        cell.setStyle(Constants.STYLE_BACKGROUND_RED);
    }

    /**
     * Changes the color and makes the icon visible for a pass through
     * @param isThrough indicates if a pass through is detected
     * @param isSetting indicates if the switch is a setting switch
     */
    void setThrough(boolean isThrough, boolean isSetting) {
        Image thru;

        if(isSetting) {
            if(isThrough) {
                thru = new Image("/images/down_g.png");
            } else {
                thru = new Image("/images/down_r.png");
            }

            throughIcon.setImage(thru);
        } else {
            throughIcon.setVisible(false);
        }
    }

    /**
     * Changes the color of the detonator icon based upon if present or not
     * @param isDet indicates whether a detonator is seen or not
     */
    public void setDet(boolean isDet) {
        Image det;

        if (isDet) {
            det = new Image("/images/det_g.png");
        } else {
            det = new Image("/images/det_r.png");
        }

        detIcon.setImage(det);
    }

    /**
     * Changes the icon to a setting icon
     * @param isSetting indicates whether it is a setting switch or not
     */
    public void addSetting(boolean isSetting) {
        settingOverlay.setVisible(isSetting);
        // Shortens the wire line in the list cell to look like it's the end of the line
        if(isSetting) {
            wireline.setEndY(40);
        }
    }

    /**
     * Changes the icon to a release icon
     * @param isRelease indicates whether it is a release address or not
     */
    public void addRelease(boolean isRelease) {
        releaseOverlay.setVisible(isRelease);
        // Shortens the wire line in the list cell to look like it's the beginning of the line
        if(isRelease) {
            wireline.setStartY(40);
            wireline.setTranslateY(40);
        }
    }
}
