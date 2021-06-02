package io.harness.pms.Dashboard;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
public class PipelineHealthInfo {
  private TotalHealthInfo total;
  private SuccessHealthInfo success;
  private MeanMedianInfo meanInfo;
  private MeanMedianInfo medianInfo;
}
