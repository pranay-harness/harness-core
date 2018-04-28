package software.wings.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This holds container deployment info.
 * @author rktummala on 08/24/17
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public abstract class BaseContainerDeploymentInfo extends DeploymentInfo {
  private String clusterName;

  public BaseContainerDeploymentInfo(String appId, String accountId, String infraMappingId, String workflowId,
      String workflowExecutionId, String workflowExecutionName, String pipelineExecutionId,
      String pipelineExecutionName, String stateExecutionInstanceId, String artifactStreamId, String artifactId,
      String artifactName, String artifactSourceName, String artifactBuildNum, String deployedById,
      String deployedByName, long deployedAt, String clusterName) {
    super(appId, accountId, infraMappingId, workflowId, workflowExecutionId, workflowExecutionName, pipelineExecutionId,
        pipelineExecutionName, stateExecutionInstanceId, artifactStreamId, artifactId, artifactName, artifactSourceName,
        artifactBuildNum, deployedById, deployedByName, deployedAt);
    this.clusterName = clusterName;
  }
}
