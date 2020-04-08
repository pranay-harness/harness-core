package software.wings.api;

import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.utils.IndexType;
import software.wings.api.DeploymentSummary.DeploymentSummaryKeys;
import software.wings.beans.Base;
import software.wings.beans.infrastructure.instance.key.deployment.AwsAmiDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.AwsCodeDeployDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.AwsLambdaDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.ContainerDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.K8sDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.PcfDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.SpotinstAmiDeploymentKey;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity(value = "deploymentSummary", noClassnameStored = true)
@HarnessEntity(exportable = false)
@Indexes({
  @Index(fields =
      {
        @Field(DeploymentSummaryKeys.accountId)
        , @Field(DeploymentSummaryKeys.CLUSTER_NAME_CONTAINER_DEPLOYMENT_INFO_WITH_NAMES),
            @Field(DeploymentSummaryKeys.CONTAINER_SVC_NAME_CONTAINER_DEPLOYMENT_INFO_WITH_NAMES),
            @Field(value = DeploymentSummaryKeys.CREATED_AT, type = IndexType.DESC)
      },
      options = @IndexOptions(name = "accountId_containerDeploymentInfo", background = true))
  ,
      @Index(fields =
          {
            @Field(DeploymentSummaryKeys.CONTAINER_KEY_SERVICE_NAME)
            , @Field(DeploymentSummaryKeys.infraMappingId),
                @Field(value = DeploymentSummaryKeys.CREATED_AT, type = IndexType.DESC)
          },
          options = @IndexOptions(name = "containerSvcName_inframappingId_createdAt", background = true)),

      @Index(fields =
          {
            @Field(DeploymentSummaryKeys.accountId)
            , @Field(DeploymentSummaryKeys.RELEASE_NAME_K8S_DEPLOYMENT_INFO),
                @Field(value = DeploymentSummaryKeys.CREATED_AT, type = IndexType.DESC)
          },
          options = @IndexOptions(name = "accountId_k8sDeploymentInfo", background = true)),

      @Index(fields =
          {
            @Field(DeploymentSummaryKeys.accountId)
            , @Field(value = DeploymentSummaryKeys.CREATED_AT, type = IndexType.ASC)
          },
          options = @IndexOptions(name = "accountId_createdAtAsc", background = true)),

      @Index(fields =
          {
            @Field(DeploymentSummaryKeys.accountId)
            , @Field(DeploymentSummaryKeys.infraMappingId),
                @Field(value = DeploymentSummaryKeys.CREATED_AT, type = IndexType.DESC)
          },
          options = @IndexOptions(name = "accountId_infraMappingId_createdAtDesc", background = true)),

      @Index(fields = {
        @Field(DeploymentSummaryKeys.infraMappingId)
        , @Field(DeploymentSummaryKeys.CONTAINER_KEY_LABELS), @Field(DeploymentSummaryKeys.CONTAINER_KEY_NEW_VERSION),
            @Field(value = DeploymentSummaryKeys.CREATED_AT, type = IndexType.DESC)
      }, options = @IndexOptions(name = "inframappingId_containerlabelsAndVersion", background = true))
})
@FieldNameConstants(innerTypeName = "DeploymentSummaryKeys")
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
  private DeploymentInfo deploymentInfo;
  private PcfDeploymentKey pcfDeploymentKey;
  private AwsAmiDeploymentKey awsAmiDeploymentKey;
  private AwsCodeDeployDeploymentKey awsCodeDeployDeploymentKey;
  private ContainerDeploymentKey containerDeploymentKey;
  private K8sDeploymentKey k8sDeploymentKey;
  private SpotinstAmiDeploymentKey spotinstAmiDeploymentKey;
  private AwsLambdaDeploymentKey awsLambdaDeploymentKey;

  @Builder
  public DeploymentSummary(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, String accountId, String infraMappingId,
      String workflowId, String workflowExecutionId, String workflowExecutionName, String pipelineExecutionId,
      String pipelineExecutionName, String stateExecutionInstanceId, String artifactId, String artifactName,
      String artifactSourceName, String artifactStreamId, String artifactBuildNum, String deployedById,
      String deployedByName, long deployedAt, DeploymentInfo deploymentInfo, PcfDeploymentKey pcfDeploymentKey,
      AwsAmiDeploymentKey awsAmiDeploymentKey, AwsCodeDeployDeploymentKey awsCodeDeployDeploymentKey,
      ContainerDeploymentKey containerDeploymentKey, SpotinstAmiDeploymentKey spotinstAmiDeploymentKey,
      AwsLambdaDeploymentKey awsLambdaDeploymentKey, K8sDeploymentKey k8sDeploymentKey) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
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
    this.spotinstAmiDeploymentKey = spotinstAmiDeploymentKey;
    this.awsLambdaDeploymentKey = awsLambdaDeploymentKey;
    this.k8sDeploymentKey = k8sDeploymentKey;
  }

  public static final class DeploymentSummaryKeys {
    private DeploymentSummaryKeys() {}

    public static final String CREATED_AT = "createdAt";
    public static final String CONTAINER_SVC_NAME_CONTAINER_DEPLOYMENT_INFO_WITH_NAMES =
        "deploymentInfo.containerSvcName";
    public static final String CLUSTER_NAME_CONTAINER_DEPLOYMENT_INFO_WITH_NAMES = "deploymentInfo.clusterName";
    public static final String RELEASE_NAME_K8S_DEPLOYMENT_INFO = "deploymentInfo.releaseName";
    public static final String CONTAINER_KEY_SERVICE_NAME = "containerDeploymentKey.containerServiceName";
    public static final String CONTAINER_KEY_LABELS = "containerDeploymentKey.labels";
    public static final String CONTAINER_KEY_NEW_VERSION = "containerDeploymentKey.newVersion";
  }
}