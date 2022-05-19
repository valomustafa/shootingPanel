/*
 *   GEODynamics
 *   Author - James Suderman
 *   Date - 02/11/2019
 */

package com.perf.shootingPanel.helpers;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PropertyHelper {
    private static final Logger propLogger = Logger.getLogger(PropertyHelper.class);
    private HashMap<String, String> propMap = new HashMap<>();
    private String path;

    public PropertyHelper(String path) {
        this.path = path;
    }

    // Adding to props puts the key into the hash map which only allows for unique keys
    public void addProp(String key, String value) {
        propMap.put(key, value);
    }

    public HashMap<String, String> getPropMap() {
        return propMap;
    }

    public String getProp(String property) {
        Properties props = new Properties();
        String value = null;

        // Use try with resources so that the stream and the file get closed
        try(InputStream input = new FileInputStream(path)) {
            props.load(input);
            value =  props.getProperty(property);
        } catch (IOException e) {
            propLogger.debug(e);
        }

        return value;
    }

    public void updateProps() {
        Properties props = new Properties();

        // Use try with resources so that the stream and the file get closed
        try(OutputStream output = new FileOutputStream(path, false)) {
            for(Map.Entry<String, String> i : propMap.entrySet()) {
                props.setProperty(i.getKey(), i.getValue());
            }

            props.store(output, null);
        } catch (IOException e) {
            propLogger.debug(e);
        }
    }
}
