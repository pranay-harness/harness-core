package software.wings.beans.infrastructure.instance;

import io.harness.beans.EmbeddedUser;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.beans.infrastructure.instance.key.ContainerInstanceKey;
import software.wings.beans.infrastructure.instance.key.HostInstanceKey;
import software.wings.beans.infrastructure.instance.key.PcfInstanceKey;

import java.util.List;

/**
 * Represents the instance that the service get deployed onto.
 * We enforce unique constraint in code based on the instance key sub class.
 * @author rktummala
 */
@Entity(value = "instance", noClassnameStored = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class Instance extends Base {
  @Indexed @NotEmpty private InstanceType instanceType;
  @Indexed private HostInstanceKey hostInstanceKey;
  @Indexed private ContainerInstanceKey containerInstanceKey;
  @Indexed private PcfInstanceKey pcfInstanceKey;
  @Indexed private String envId;
  private String envName;
  private EnvironmentType envType;
  @Indexed private String accountId;
  @Indexed private String serviceId;
  private String serviceName;
  private String appName;

  @Indexed private String infraMappingId;
  @Indexed private String infraMappingType;

  private String computeProviderId;
  private String computeProviderName;

  private String lastArtifactStreamId;

  @Indexed private String lastArtifactId;
  private String lastArtifactName;
  @Indexed private String lastArtifactSourceName;
  @Indexed private String lastArtifactBuildNum;

  private String lastDeployedById;
  private String lastDeployedByName;
  private long lastDeployedAt;
  @Indexed private String lastWorkflowExecutionId;
  private String lastWorkflowExecutionName;

  @Indexed private String lastPipelineExecutionId;
  private String lastPipelineExecutionName;

  private InstanceInfo instanceInfo;

  private boolean isDeleted;
  private long deletedAt;

  @Builder
  public Instance(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, List<String> keywords, String entityYamlPath, InstanceType instanceType,
      HostInstanceKey hostInstanceKey, ContainerInstanceKey containerInstanceKey, PcfInstanceKey pcfInstanceKey,
      String envId, String envName, EnvironmentType envType, String accountId, String serviceId, String serviceName,
      String appName, String infraMappingId, String infraMappingType, String computeProviderId,
      String computeProviderName, String lastArtifactStreamId, String lastArtifactId, String lastArtifactName,
      String lastArtifactSourceName, String lastArtifactBuildNum, String lastDeployedById, String lastDeployedByName,
      long lastDeployedAt, String lastWorkflowExecutionId, String lastWorkflowExecutionName,
      String lastPipelineExecutionId, String lastPipelineExecutionName, InstanceInfo instanceInfo, boolean isDeleted,
      long deletedAt) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath);
    this.instanceType = instanceType;
    this.hostInstanceKey = hostInstanceKey;
    this.containerInstanceKey = containerInstanceKey;
    this.pcfInstanceKey = pcfInstanceKey;
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
