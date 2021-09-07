package io.harness.cvng.beans;

import io.harness.cvng.models.VerificationType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public enum DataSourceType {
  APP_DYNAMICS("Appdynamics", VerificationType.TIME_SERIES),
  SPLUNK("Splunk", VerificationType.LOG),
  STACKDRIVER("Stackdriver", VerificationType.TIME_SERIES),
  STACKDRIVER_LOG("Stackdriver Log", VerificationType.LOG),
  KUBERNETES("Kubernetes", VerificationType.TIME_SERIES),
  NEW_RELIC("New Relic", VerificationType.TIME_SERIES),
  PROMETHEUS("Prometheus", VerificationType.TIME_SERIES);

  private String displayName;
  private VerificationType verificationType;

  DataSourceType(String displayName, VerificationType verificationType) {
    this.displayName = displayName;
    this.verificationType = verificationType;
  }

  public String getDisplayName() {
    return displayName;
  }

  public VerificationType getVerificationType() {
    return verificationType;
  }

  public static List<DataSourceType> getTimeSeriesTypes() {
    return new ArrayList<>(EnumSet.of(APP_DYNAMICS, STACKDRIVER, NEW_RELIC, PROMETHEUS));
  }
}
