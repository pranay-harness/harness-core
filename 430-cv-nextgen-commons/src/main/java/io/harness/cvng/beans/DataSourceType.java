package io.harness.cvng.beans;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public enum DataSourceType {
  APP_DYNAMICS("Appdynamics"),
  SPLUNK("Splunk"),
  STACKDRIVER("Stackdriver"),
  STACKDRIVER_LOG("Stackdriver Log"),
  KUBERNETES("Kubernetes"),
  NEW_RELIC("New Relic"),
  PROMETHEUS("Prometheus");

  private String displayName;

  DataSourceType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public static List<DataSourceType> getTimeSeriesTypes() {
    return new ArrayList<>(EnumSet.of(APP_DYNAMICS, STACKDRIVER, NEW_RELIC, PROMETHEUS));
  }
}
