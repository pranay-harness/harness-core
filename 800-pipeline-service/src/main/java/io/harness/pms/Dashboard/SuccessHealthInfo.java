package io.harness.pms.Dashboard;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
@Schema(name = "SuccessHealth",
    description = "This is the view of the successful  count of Executions for given Time Interval")
public class SuccessHealthInfo {
  private double percent;
  private double rate;
}
