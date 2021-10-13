package io.harness.overviewdashboard.dtos;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PL)
public class PipelineExecutionInfo {
    PipelineInfo pipelineInfo;
    ProjectInfo projectInfo;
    OrgInfo orgInfo;
    AccountInfo accountInfo;

    String planExecutionId;
    long startTs;
}
