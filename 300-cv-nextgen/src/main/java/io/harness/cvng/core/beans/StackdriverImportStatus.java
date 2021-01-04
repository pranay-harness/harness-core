package io.harness.cvng.core.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StackdriverImportStatus implements MonitoringSourceImportStatus {
  private int numberOfDashboards;
  private int totalNumberOfDashboards;
}
