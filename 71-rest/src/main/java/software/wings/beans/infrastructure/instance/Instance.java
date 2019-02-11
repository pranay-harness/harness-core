package software.wings.beans.infrastructure.instance;

import io.harness.beans.EmbeddedUser;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.beans.infrastructure.instance.key.ContainerInstanceKey;
import software.wings.beans.infrastructure.instance.key.HostInstanceKey;
import software.wings.beans.infrastructure.instance.key.PcfInstanceKey;
import software.wings.beans.infrastructure.instance.key.PodInstanceKey;

/**
 * Represents the instance that the service get deployed onto.
 * We enforce unique constraint in code based on the instance key sub class.
 * @author rktummala
 */
@Entity(value = "instance", noClassnameStored = true)
@Data
@EqualsAndHashCode(callSuper = true)
@Indexes({
  @Index(fields = { @Field("appId")
                    , @Field("isDeleted"), @Field("deletedAt") },
      options = @IndexOptions(name = "instance_index1", background = true))
  ,
      @Index(fields = {
        @Field("appId"), @Field("infraMappingId"), @Field("isDeleted"), @Field("deletedAt")
      }, options = @IndexOptions(name = "instance_index2"), background = true), @Index(fields = {
        @Field("accountId"), @Field("createdAt"), @Field("isDeleted"), @Field("deletedAt")
      }, options = @IndexOptions(name = "instance_index3", background = true)), @Index(fields = {
        @Field("appId"), @Field("createdAt"), @Field("isDeleted"), @Field("deletedAt")
      }, options = @IndexOptions(name = "instance_index4", background = true)), @Index(fields = {
        @Field("appId"), @Field("serviceId"), @Field("createdAt"), @Field("isDeleted"), @Field("deletedAt")
      }, options = @IndexOptions(name = "instance_index5", background = true)), @Index(fields = {
        @Field("appId"), @Field("isDeleted")
      }, options = @IndexOptions(name = "instance_index6", background = true)), @Index(fields = {
        @Field("accountId"), @Field("isDeleted")
      }, options = @IndexOptions(name = "instance_index7", background = true)), @Index(fields = {
        @Field("appId"), @Field("serviceId"), @Field("isDeleted")
      }, options = @IndexOptions(name = "instance_index8", background = true))
})
public class Instance extends Base {
  @NotEmpty private InstanceType instanceType;
  private HostInstanceKey hostInstanceKey;
  private ContainerInstanceKey containerInstanceKey;
  private PcfInstanceKey pcfInstanceKey;
  private PodInstanceKey podInstanceKey;
  private String envId;
  private String envName;
  private EnvironmentType envType;
  private String accountId;
  private String serviceId;
  private String serviceName;
  private String appName;

  private String infraMappingId;
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

  private InstanceInfo instanceInfo;

  @Indexed(background = true) private boolean isDeleted;
  private long deletedAt;

  @Builder
  public Instance(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, String entityYamlPath, InstanceType instanceType, HostInstanceKey hostInstanceKey,
      ContainerInstanceKey containerInstanceKey, PcfInstanceKey pcfInstanceKey, PodInstanceKey podInstanceKey,
      String envId, String envName, EnvironmentType envType, String accountId, String serviceId, String serviceName,
      String appName, String infraMappingId, String infraMappingType, String computeProviderId,
      String computeProviderName, String lastArtifactStreamId, String lastArtifactId, String lastArtifactName,
      String lastArtifactSourceName, String lastArtifactBuildNum, String lastDeployedById, String lastDeployedByName,
      long lastDeployedAt, String lastWorkflowExecutionId, String lastWorkflowExecutionName,
      String lastPipelineExecutionId, String lastPipelineExecutionName, InstanceInfo instanceInfo, boolean isDeleted,
      long deletedAt) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.instanceType = instanceType;
    this.hostInstanceKey = hostInstanceKey;
    this.containerInstanceKey = containerInstanceKey;
    this.pcfInstanceKey = pcfInstanceKey;
    this.podInstanceKey = podInstanceKey;
    this.envId = envId;
    this.envName = envName;
    this.envType = envType;
    this.accountId = accountId;
    this.serviceId = serviceId;
    this.serviceName = serviceName;
    this.appName = appName;
    this.infraMappingId = infraMappingId;
    this.infraMappingType = infraMappingType;
    this.computeProviderId = computeProviderId;
    this.computeProviderName = computeProviderName;
    this.lastArtifactStreamId = lastArtifactStreamId;
    this.lastArtifactId = lastArtifactId;
    this.lastArtifactName = lastArtifactName;
    this.lastArtifactSourceName = lastArtifactSourceName;
    this.lastArtifactBuildNum = lastArtifactBuildNum;
    this.lastDeployedById = lastDeployedById;
    this.lastDeployedByName = lastDeployedByName;
    this.lastDeployedAt = lastDeployedAt;
    this.lastWorkflowExecutionId = lastWorkflowExecutionId;
    this.lastWorkflowExecutionName = lastWorkflowExecutionName;
    this.lastPipelineExecutionId = lastPipelineExecutionId;
    this.lastPipelineExecutionName = lastPipelineExecutionName;
    this.instanceInfo = instanceInfo;
    this.isDeleted = isDeleted;
    this.deletedAt = deletedAt;
  }
}
