package software.wings.api;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.infrastructure.instance.key.deployment.AwsAmiDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.AwsCodeDeployDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.ContainerDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.PcfDeploymentKey;

@Entity(value = "deploymentSummary", noClassnameStored = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class DeploymentSummary extends Base {
  private String accountId;
  @Indexed private String infraMappingId;
  private String workflowId;
  private String workflowExecutionId;
  private String workflowExecutionName;
  private String pipelineExecutionId;
  private String pipelineExecutionName;
  private String stateExecutionInstanceId;
  private String artifactId;
  private String artifactName;
  private String artifactSourceName;
  private String artifactStreamId;
  private String artifactBuildNum;
  private String deployedById;
  private String deployedByName;
  private long deployedAt;
  @Embedded private DeploymentInfo deploymentInfo;
  @Indexed @Embedded private PcfDeploymentKey pcfDeploymentKey;
  @Indexed @Embedded private AwsAmiDeploymentKey awsAmiDeploymentKey;
  @Indexed @Embedded private AwsCodeDeployDeploymentKey awsCodeDeployDeploymentKey;
  @Indexed @Embedded private ContainerDeploymentKey containerDeploymentKey;

  @Builder
  public DeploymentSummary(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, String accountId, String infraMappingId,
      String workflowId, String workflowExecutionId, String workflowExecutionName, String pipelineExecutionId,
      String pipelineExecutionName, String stateExecutionInstanceId, String artifactId, String artifactName,
      String artifactSourceName, String artifactStreamId, String artifactBuildNum, String deployedById,
      String deployedByName, long deployedAt, DeploymentInfo deploymentInfo, PcfDeploymentKey pcfDeploymentKey,
      AwsAmiDeploymentKey awsAmiDeploymentKey, AwsCodeDeployDeploymentKey awsCodeDeployDeploymentKey,
      ContainerDeploymentKey containerDeploymentKey) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, null, entityYamlPath);
    this.accountId = accountId;
    this.infraMappingId = infraMappingId;
    this.workflowId = workflowId;
    this.workflowExecutionId = workflowExecutionId;
    this.workflowExecutionName = workflowExecutionName;
    this.pipelineExecutionId = pipelineExecutionId;
    this.pipelineExecutionName = pipelineExecutionName;
    this.stateExecutionInstanceId = stateExecutionInstanceId;
    this.artifactId = artifactId;
    this.artifactName = artifactName;
    this.artifactSourceName = artifactSourceName;
    this.artifactStreamId = artifactStreamId;
    this.artifactBuildNum = artifactBuildNum;
    this.deployedById = deployedById;
    this.deployedByName = deployedByName;
    this.deployedAt = deployedAt;
    this.deploymentInfo = deploymentInfo;
    this.pcfDeploymentKey = pcfDeploymentKey;
    this.awsAmiDeploymentKey = awsAmiDeploymentKey;
    this.awsCodeDeployDeploymentKey = awsCodeDeployDeploymentKey;
    this.containerDeploymentKey = containerDeploymentKey;
  }
}