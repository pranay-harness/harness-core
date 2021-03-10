package io.harness.cvng.beans;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public enum DataSourceType {
  APP_DYNAMICS("Appdynamics"),
  SPLUNK("Splunk"),
  STACKDRIVER("Stackdriver"),
  KUBERNETES("Kubernetes");

  private String displayName;

  DataSourceType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public static List<DataSourceType> getTimeSeriesThresholds() {
    return new ArrayList<>(EnumSet.of(APP_DYNAMICS, STACKDRIVER));
  }
}
