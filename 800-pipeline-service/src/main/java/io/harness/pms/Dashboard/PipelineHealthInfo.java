package io.harness.pms.Dashboard;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
@Schema(name = "PipelineHealth",
    description = "This is the view of the Pipeline Executions Stats Info for a given Interval")
public class PipelineHealthInfo {
  private TotalHealthInfo total;
  private SuccessHealthInfo success;
  private MeanMedianInfo meanInfo;
  private MeanMedianInfo medianInfo;
}
