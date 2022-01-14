package io.harness.pms.Dashboard;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
@Schema(
    name = "PipelineCount", description = "This is the view of the Pipeline Execution Count Info for a particular Date")
public class PipelineCountInfo {
  private long total;
  private long success;
  private long failure;
}
