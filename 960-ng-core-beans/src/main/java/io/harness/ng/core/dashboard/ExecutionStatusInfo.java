package io.harness.ng.core.dashboard;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
public class ExecutionStatusInfo {
  private String pipelineName;
  private String pipelineIdentifier;
  private long startTs;
  private long endTs;
  private String status;
  private String planExecutionId;
  private GitInfo gitInfo;
  private String triggerType;
  private AuthorInfo author;
  private List<ServiceDeploymentInfo> serviceInfoList;
}
