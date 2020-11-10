package io.harness.cvng.core.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SplunkImportStatus implements MonitoringSourceImportStatus {
  private String numberOfQueries;
}
