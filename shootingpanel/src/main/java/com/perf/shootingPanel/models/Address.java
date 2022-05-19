/*
 *   GEODynamics
 *   Author - James Suderman
 *   Date - 05/04/2018
 */

package com.perf.shootingPanel.models;

import com.perf.shootingPanel.Constants;
import javafx.collections.ObservableList;

public class Address {
    private String[] addresses =   {"FFFBFF", "FFFBFE", "FFFBFD", "FFFBFC",
                                    "FFFBFB", "FFFBFA", "FFFBF9", "FFFBF8",
                                    "FFFBF7", "FFFBF6", "FFFBF5", "FFFBF4",
                                    "FFFBF3", "FFFBF2", "FFFBF1", "FFFBF0",
                                    "FFFBEF", "FFFBEE", "FFFBED", "FFFBEC"};

    public String getAddress(ObservableList<Switch> switches, boolean isSetting) {
        String newAddress = Constants.BLANK_SPACE;
        boolean foundAddress = false;

        if(isSetting) {
            newAddress = Constants.SET_FIRE_ADDRESS;
        } else {
            for(String address : addresses) {
                for(Switch i : switches) {
                    if((i.getAddress().compareTo(address) == 0)) {
                       foundAddress = true;
                    }
                }

                if(!foundAddress) {
                    return address;
                } else {
                    foundAddress = false;
                }
            }
        }

        return newAddress;
    }
}
