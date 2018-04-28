package software.wings.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * This holds deploymentInfo of helm based deployments.
 * @author rktummala on 08/24/17
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class ContainerDeploymentInfoWithLabels extends BaseContainerDeploymentInfo {
  private Map<String, String> labels;

  @Builder
  public ContainerDeploymentInfoWithLabels(String appId, String accountId, String infraMappingId, String workflowId,
      String workflowExecutionId, String workflowExecutionName, String pipelineExecutionId,
      String pipelineExecutionName, String stateExecutionInstanceId, String artifactStreamId, String artifactId,
      String artifactName, String artifactSourceName, String artifactBuildNum, String deployedById,
      String deployedByName, long deployedAt, String clusterName, Map<String, String> labels) {
    super(appId, accountId, infraMappingId, workflowId, workflowExecutionId, workflowExecutionName, pipelineExecutionId,
        pipelineExecutionName, stateExecutionInstanceId, artifactStreamId, artifactId, artifactName, artifactSourceName,
        artifactBuildNum, deployedById, deployedByName, deployedAt, clusterName);
    this.labels = labels;
  }
}
