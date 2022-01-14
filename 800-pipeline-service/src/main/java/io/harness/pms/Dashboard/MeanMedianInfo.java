package io.harness.pms.Dashboard;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
@Schema(name = "MeanMedianInfo",
    description = "This is the view of the Mean and Median info for the Executions for given Time Interval")
public class MeanMedianInfo {
  private long duration;
  private double rate;
}
