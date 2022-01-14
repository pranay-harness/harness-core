package io.harness.pms.Dashboard;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
@Schema(name = "TotalHealth", description = "This is the view of the total count of Executions for given Time Interval")
public class TotalHealthInfo {
  private long count;
  private double rate;
}
