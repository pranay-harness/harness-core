package software.wings.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

/**
 * This is a wrapper class of ContainerDeploymentDeploymentInfo to make it extend queuable.
 * This is used as request for capturing container deployment information.
 * @author rktummala on 08/24/17
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class ContainerDeploymentInfo extends DeploymentInfo {
  private String clusterName;
  /**
   * In case of ECS, this would be a list of taskDefinitionArns belonging to the same family (in other words, same
   * containerSvcNameNoRevision). In case of Kubernetes, this would be a list of replicationControllerNames belonging to
   * the same family (in other words, same containerSvcNameNoRevision). This has the revision number in it.
   */
  private Set<String> containerSvcNameSet;

  @Builder
  public ContainerDeploymentInfo(String appId, String accountId, String infraMappingId, String workflowId,
      String workflowExecutionId, String workflowExecutionName, String pipelineExecutionId,
      String pipelineExecutionName, String stateExecutionInstanceId, String artifactStreamId, String artifactId,
      String artifactName, String artifactSourceName, String artifactBuildNum, String deployedById,
      String deployedByName, long deployedAt, String clusterName, Set<String> containerSvcNameSet) {
    super(appId, accountId, infraMappingId, workflowId, workflowExecutionId, workflowExecutionName, pipelineExecutionId,
        pipelineExecutionName, stateExecutionInstanceId, artifactStreamId, artifactId, artifactName, artifactSourceName,
        artifactBuildNum, deployedById, deployedByName, deployedAt);
    this.clusterName = clusterName;
    this.containerSvcNameSet = containerSvcNameSet;
  }
}
