package io.harness.pms.Dashboard;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
@Schema(name = "PipelineExecution", description = "This is the view of the Pipeline Executions for a particular Date")
public class PipelineExecutionInfo {
  private long date;
  private PipelineCountInfo count;
}
