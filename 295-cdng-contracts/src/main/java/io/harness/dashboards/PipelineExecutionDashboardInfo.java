package io.harness.dashboards;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PipelineExecutionDashboardInfo {
  private String name;
  private String identifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private String accountIdentifier;

  private String planExecutionId;
  private long startTs;
}
