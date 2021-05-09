package io.harness.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dto.deploymentinfo.DeploymentInfo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.DX)
public class DeploymentSummary {
  private String accountId;
  private String orgId;
  private String projectId;
  private String pipelineExecutionId;
  private String pipelineExecutionName;
  private String artifactId;
  private String artifactName;
  private String artifactBuildNum;
  private String deployedById;
  private String deployedByName;
  private String infrastructureMappingId;
  private long deployedAt;
  private DeploymentInfo deploymentInfo;
}
