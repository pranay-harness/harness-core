package software.wings.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

/**
 * This holds controllers info about containers.
 * @author rktummala on 08/24/17
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class ContainerDeploymentInfoWithNames extends BaseContainerDeploymentInfo {
  /**
   * In case of ECS, this would be a list of taskDefinitionArns.
   * In case of Kubernetes, this would be a list of controllerNames.
   */
  private Set<String> containerSvcNameSet;

  @Builder
  public ContainerDeploymentInfoWithNames(String appId, String accountId, String infraMappingId, String workflowId,
      String workflowExecutionId, String workflowExecutionName, String pipelineExecutionId,
      String pipelineExecutionName, String stateExecutionInstanceId, String artifactStreamId, String artifactId,
      String artifactName, String artifactSourceName, String artifactBuildNum, String deployedById,
      String deployedByName, long deployedAt, String clusterName, Set<String> containerSvcNameSet) {
    super(appId, accountId, infraMappingId, workflowId, workflowExecutionId, workflowExecutionName, pipelineExecutionId,
        pipelineExecutionName, stateExecutionInstanceId, artifactStreamId, artifactId, artifactName, artifactSourceName,
        artifactBuildNum, deployedById, deployedByName, deployedAt, clusterName);
    this.containerSvcNameSet = containerSvcNameSet;
  }
}
