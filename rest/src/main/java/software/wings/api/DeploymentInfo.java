package software.wings.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * This is used as request for capturing deployment and instance information.
 * @author rktummala on 02/04/18
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode
public abstract class DeploymentInfo {
  private String appId;
  private String accountId;
  private String infraMappingId;
  private String workflowId;
  private String workflowExecutionId;
  private String workflowExecutionName;
  private String pipelineExecutionId;
  private String pipelineExecutionName;
  private String stateExecutionInstanceId;
  private String artifactStreamId;
  private String artifactId;
  private String artifactName;
  private String artifactSourceName;
  private String artifactBuildNum;
  private String deployedById;
  private String deployedByName;
  private long deployedAt;

  public DeploymentInfo(String appId, String accountId, String infraMappingId, String workflowId,
      String workflowExecutionId, String workflowExecutionName, String pipelineExecutionId,
      String pipelineExecutionName, String stateExecutionInstanceId, String artifactStreamId, String artifactId,
      String artifactName, String artifactSourceName, String artifactBuildNum, String deployedById,
      String deployedByName, long deployedAt) {
    this.appId = appId;
    this.accountId = accountId;
    this.infraMappingId = infraMappingId;
    this.workflowId = workflowId;
    this.workflowExecutionId = workflowExecutionId;
    this.workflowExecutionName = workflowExecutionName;
    this.pipelineExecutionId = pipelineExecutionId;
    this.pipelineExecutionName = pipelineExecutionName;
    this.stateExecutionInstanceId = stateExecutionInstanceId;
    this.artifactStreamId = artifactStreamId;
    this.artifactId = artifactId;
    this.artifactName = artifactName;
    this.artifactSourceName = artifactSourceName;
    this.artifactBuildNum = artifactBuildNum;
    this.deployedById = deployedById;
    this.deployedByName = deployedByName;
    this.deployedAt = deployedAt;
  }
}
