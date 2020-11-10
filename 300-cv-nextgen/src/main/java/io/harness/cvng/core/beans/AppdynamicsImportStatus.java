package io.harness.cvng.core.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AppdynamicsImportStatus implements MonitoringSourceImportStatus {
  private int numberOfApplications;
  private int totalNumberOfApplications;
  private int numberOfEnvironments;
  private int totalNumberOfEnvironments;
}
