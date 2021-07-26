package io.harness.ng.cdOverview.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
public class DeploymentStatusInfo {
  private String pipelineName;
  private String pipelineIdentifier;
  private String branch;
  private String commit;
  private String commitID;
  private String repoName;
  private AuthorInfo author;
  private long startTs;
  private long endTs;
  private String status;
  private String planExecutionId;
  private List<ServiceDeploymentInfo> serviceInfoList;
}
