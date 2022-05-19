/*
 *   GEODynamics
 *   Author - James Suderman
 *   Date - 12/18/2017
 */

package com.perf.shootingPanel.models;

public class Switch {
    private String id, address = "000000", message, originalIndex;
    private int firmwareVersion;
    private boolean isSetting;
    private boolean isRelease;
    private boolean isPassed;
    private boolean feedthrough;
    private boolean detonator;
    private boolean shorted;
    private boolean duplicate = false;

    public Switch() {}

    public Switch(String id, String address, boolean isSetting, boolean isPassed, boolean isRelease,
                  String message, boolean feedthrough, boolean detonator, boolean shorted, int firmwareVersion) {
        this.id = id;
        this.address = address;
        this.isSetting = isSetting;
        this.isPassed = isPassed;
        this.isRelease = isRelease;
        this.message = message;
        this.feedthrough = feedthrough;
        this.detonator = detonator;
        this.shorted = shorted;
        this.firmwareVersion = firmwareVersion;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isSetting() {
        return isSetting;
    }

    public void setSetting(boolean setting) {
        isSetting = setting;
    }

    public boolean isPassed() {
        return isPassed;
    }

    public void setPassed(boolean passed) {
        isPassed = passed;
    }

    public boolean isRelease() { return isRelease; }

    public void setRelease(boolean release) { isRelease = release; }

    public boolean isFeedthrough() { return feedthrough; }

    public void setFeedthrough(boolean feedthrough) { this.feedthrough = feedthrough; }

    public boolean isDetonator() { return detonator; }

    public void setDetonator(boolean detonator) { this.detonator = detonator; }

    public boolean isShorted() { return shorted; }

    public void setShorted(boolean shorted) { this.shorted = shorted; }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean getDuplicate() {
        return duplicate;
    }

    public void setDuplicate() {
        this.duplicate = true;
    }

    public String getOriginalIndex() {
        return originalIndex;
    }

    public void setOriginalIndex(String originalIndex) {
        this.originalIndex = originalIndex;
    }

    public int getFirmwareVersion() { return firmwareVersion; }

    public void setFirmwareVersion(int firmwareVersion) { this.firmwareVersion = firmwareVersion; }

    public String getInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Switch: ");
        sb.append(id);
        sb.append("\n");
        sb.append("Address: ");
        sb.append(address);
        sb.append("\n");
        sb.append("Setting: ");
        sb.append(isSetting);
        sb.append("\n");
        sb.append("Release: ");
        sb.append(isRelease);
        sb.append("\n");
        sb.append("Message: ");
        sb.append(message);
        sb.append("\n");
        sb.append("Feed Through: ");
        sb.append(feedthrough);
        sb.append("\n");
        sb.append("Detonator: ");
        sb.append(detonator);
        sb.append("\n");
        sb.append("Shorted: ");
        sb.append(shorted);
        sb.append("\n");
        sb.append("Version: ");
        sb.append(firmwareVersion);
        sb.append("\n");

        return sb.toString();
    }
}
