package software.wings.beans.infrastructure.instance;

import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.entityinterface.ApplicationAccess;
import software.wings.beans.infrastructure.instance.info.ServerlessInstanceInfo;
import software.wings.beans.infrastructure.instance.key.AwsLambdaInstanceKey;

import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(of = {"uuid", "appId"}, callSuper = false)
@Indexes({
  @Index(fields = { @Field("appId")
                    , @Field("isDeleted"), @Field("deletedAt") },
      options = @IndexOptions(name = "serverless_instance_index1", background = true))
  ,
      @Index(fields = {
        @Field("appId"), @Field("infraMappingId"), @Field("isDeleted"), @Field("deletedAt")
      }, options = @IndexOptions(name = "serverless_instance_index2", background = true)), @Index(fields = {
        @Field("accountId"), @Field("createdAt"), @Field("isDeleted"), @Field("deletedAt")
      }, options = @IndexOptions(name = "serverless_instance_index3", background = true)), @Index(fields = {
        @Field("appId"), @Field("serviceId"), @Field("createdAt"), @Field("isDeleted"), @Field("deletedAt")
      }, options = @IndexOptions(name = "serverless_instance_index5", background = true)), @Index(fields = {
        @Field("accountId"), @Field("isDeleted")
      }, options = @IndexOptions(name = "serverless_instance_index7", background = true)), @Index(fields = {
        @Field("appId"), @Field("serviceId"), @Field("isDeleted")
      }, options = @IndexOptions(name = "serverless_instance_index8", background = true))
})
@FieldNameConstants(innerTypeName = "ServerlessInstanceKeys")
@Entity(value = "serverless-instance", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class ServerlessInstance implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, UpdatedAtAware,
                                           UpdatedByAware, ApplicationAccess {
  @Id @NotNull(groups = {Update.class}) private String uuid;

  @Indexed @NotNull protected String appId;

  private EmbeddedUser createdBy;

  @Indexed private long createdAt;

  private EmbeddedUser lastUpdatedBy;

  @NotNull private long lastUpdatedAt;

  @NotEmpty private ServerlessInstanceType instanceType;
  private AwsLambdaInstanceKey lambdaInstanceKey;
  private String envId;
  private String envName;
  private EnvironmentType envType;
  private String accountId;
  private String serviceId;
  private String serviceName;
  private String appName;

  private String infraMappingId;
  private String infraMappingName;
  private String infraMappingType;

  private String computeProviderId;
  private String computeProviderName;

  private String lastArtifactStreamId;

  private String lastArtifactId;
  private String lastArtifactName;
  private String lastArtifactSourceName;
  private String lastArtifactBuildNum;

  private String lastDeployedById;
  private String lastDeployedByName;
  private long lastDeployedAt;
  private String lastWorkflowExecutionId;
  private String lastWorkflowExecutionName;

  private String lastPipelineExecutionId;
  private String lastPipelineExecutionName;

  private ServerlessInstanceInfo instanceInfo;

  @Indexed(options = @IndexOptions(background = true)) private boolean isDeleted;
  private long deletedAt;

  @Builder(toBuilder = true)
  public ServerlessInstance(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, ServerlessInstanceType instanceType,
      AwsLambdaInstanceKey lambdaInstanceKey, String envId, String envName, EnvironmentType envType, String accountId,
      String serviceId, String serviceName, String appName, String infraMappingId, String infraMappingName,
      String infraMappingType, String computeProviderId, String computeProviderName, String lastArtifactStreamId,
      String lastArtifactId, String lastArtifactName, String lastArtifactSourceName, String lastArtifactBuildNum,
      String lastDeployedById, String lastDeployedByName, long lastDeployedAt, String lastWorkflowExecutionId,
      String lastWorkflowExecutionName, String lastPipelineExecutionId, String lastPipelineExecutionName,
      ServerlessInstanceInfo instanceInfo, boolean isDeleted, long deletedAt) {
    this.uuid = uuid;
    this.isDeleted = isDeleted;
    this.deletedAt = deletedAt;
    this.appId = appId;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
    this.serviceId = serviceId;
    this.serviceName = serviceName;
    this.appName = appName;
    this.lastUpdatedBy = lastUpdatedBy;
    this.lastUpdatedAt = lastUpdatedAt;
    this.instanceType = instanceType;
    this.lambdaInstanceKey = lambdaInstanceKey;
    this.envId = envId;
    this.envName = envName;
    this.envType = envType;
    this.lastWorkflowExecutionId = lastWorkflowExecutionId;
    this.lastWorkflowExecutionName = lastWorkflowExecutionName;
    this.lastPipelineExecutionId = lastPipelineExecutionId;
    this.lastPipelineExecutionName = lastPipelineExecutionName;
    this.accountId = accountId;
    this.lastArtifactId = lastArtifactId;
    this.lastArtifactName = lastArtifactName;
    this.lastArtifactSourceName = lastArtifactSourceName;
    this.lastArtifactBuildNum = lastArtifactBuildNum;

    this.computeProviderId = computeProviderId;
    this.computeProviderName = computeProviderName;
    this.lastArtifactStreamId = lastArtifactStreamId;

    this.lastDeployedById = lastDeployedById;
    this.lastDeployedByName = lastDeployedByName;
    this.lastDeployedAt = lastDeployedAt;

    this.instanceInfo = instanceInfo;

    this.infraMappingId = infraMappingId;
    this.infraMappingName = infraMappingName;
    this.infraMappingType = infraMappingType;
  }
}
