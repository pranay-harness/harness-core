package io.harness.pms.Dashboard;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
@Schema(name = "DashboardPipelineHealth",
    description = "This is the view of the Pipeline Executions Stats Info for a given Interval")
public class DashboardPipelineHealthInfo {
  private PipelineHealthInfo executions;
}
