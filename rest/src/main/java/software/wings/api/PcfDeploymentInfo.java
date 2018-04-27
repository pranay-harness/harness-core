package software.wings.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class PcfDeploymentInfo extends DeploymentInfo {
  private Set<String> pcfApplicationNameSet;

  @Builder
  public PcfDeploymentInfo(String appId, String accountId, String infraMappingId, String workflowId,
      String workflowExecutionId, String workflowExecutionName, String pipelineExecutionId,
      String pipelineExecutionName, String stateExecutionInstanceId, String artifactStreamId, String artifactId,
      String artifactName, String artifactSourceName, String artifactBuildNum, String deployedById,
      String deployedByName, long deployedAt, Set<String> pcfApplicationNameSet) {
    super(appId, accountId, infraMappingId, workflowId, workflowExecutionId, workflowExecutionName, pipelineExecutionId,
        pipelineExecutionName, stateExecutionInstanceId, artifactStreamId, artifactId, artifactName, artifactSourceName,
        artifactBuildNum, deployedById, deployedByName, deployedAt);
    this.pcfApplicationNameSet = pcfApplicationNameSet;
  }
}
