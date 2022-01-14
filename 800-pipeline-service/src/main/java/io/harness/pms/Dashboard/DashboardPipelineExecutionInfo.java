package io.harness.pms.Dashboard;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
@Schema(name = "DashboardPipelineExecution",
    description = "This is the view of the Pipeline Executions for given Time Interval presented in day wise format")
public class DashboardPipelineExecutionInfo {
  private List<PipelineExecutionInfo> pipelineExecutionInfoList;
}
