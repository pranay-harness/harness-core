package io.harness.cvng.beans;

public enum DataSourceType {
  APP_DYNAMICS("Appdynamics"),
  SPLUNK("Splunk"),
  STACKDRIVER("Stackdriver");

  private String displayName;

  DataSourceType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
