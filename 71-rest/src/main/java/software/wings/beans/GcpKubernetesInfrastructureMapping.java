package software.wings.beans;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.lang.String.format;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.EmbeddedUser;
import io.harness.exception.InvalidRequestException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType;
import software.wings.utils.Util;

import java.util.Map;
import java.util.Optional;

/**
 * Created by brett on 2/27/17
 */
@JsonTypeName("GCP_KUBERNETES")
@Data
@EqualsAndHashCode(callSuper = true)
public class GcpKubernetesInfrastructureMapping extends ContainerInfrastructureMapping {
  @Attributes(title = "Namespace") private String namespace;

  /**
   * Instantiates a new Infrastructure mapping.
   */
  public GcpKubernetesInfrastructureMapping() {
    super(InfrastructureMappingType.GCP_KUBERNETES.name());
  }

  @Override
  public void applyProvisionerVariables(Map<String, Object> map, NodeFilteringType nodeFilteringType) {
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      switch (entry.getKey()) {
        case "clusterName":
          setClusterName((String) entry.getValue());
          break;
        case "namespace":
          setNamespace((String) entry.getValue());
          break;
        default:
          throw new InvalidRequestException("Incorrect mapping " + entry.getKey() + " added in the provisioner ");
      }
    }
    if (getClusterName() == null) {
      throw new InvalidRequestException("Cluster Name is mandatory");
    }
  }

  @SchemaIgnore
  @Override
  public String getDefaultName() {
    return Util.normalize(
        format("%s (GCP/Kubernetes::%s) %s", isEmpty(this.getProvisionerId()) ? this.getClusterName() : "",
            Optional.ofNullable(this.getComputeProviderName()).orElse(this.getComputeProviderType().toLowerCase()),
            Optional.ofNullable(this.getNamespace()).orElse("default")));
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    /**
     * The Entity yaml path.
     */
    public transient String entityYamlPath; // TODO:: remove it with changeSet batching
    /**
     * The App id.
     */
    protected String appId;
    private String accountId;
    private String clusterName;
    private String namespace;
    private String uuid;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private String computeProviderSettingId;
    private String envId;
    private String serviceTemplateId;
    private String serviceId;
    private String computeProviderType;
    private String infraMappingType;
    private String deploymentType;
    private String computeProviderName;
    private String name;
    // auto populate name
    private boolean autoPopulate = true;

    private Builder() {}

    /**
     * A gcp kubernetes infrastructure mapping builder.
     *
     * @return the builder
     */
    public static Builder aGcpKubernetesInfrastructureMapping() {
      return new Builder();
    }

    /**
     * With cluster name builder.
     *
     * @param clusterName the cluster name
     * @return the builder
     */
    public Builder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    /**
     * With namespace builder.
     *
     * @param namespace the namespace
     * @return the builder
     */
    public Builder withNamespace(String namespace) {
      this.namespace = namespace;
      return this;
    }

    /**
     * With uuid builder.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    /**
     * With created by builder.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at builder.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With entity yaml path builder.
     *
     * @param entityYamlPath the entity yaml path
     * @return the builder
     */
    public Builder withEntityYamlPath(String entityYamlPath) {
      this.entityYamlPath = entityYamlPath;
      return this;
    }

    /**
     * With compute provider setting id builder.
     *
     * @param computeProviderSettingId the compute provider setting id
     * @return the builder
     */
    public Builder withComputeProviderSettingId(String computeProviderSettingId) {
      this.computeProviderSettingId = computeProviderSettingId;
      return this;
    }

    /**
     * With env id builder.
     *
     * @param envId the env id
     * @return the builder
     */
    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    /**
     * With service template id builder.
     *
     * @param serviceTemplateId the service template id
     * @return the builder
     */
    public Builder withServiceTemplateId(String serviceTemplateId) {
      this.serviceTemplateId = serviceTemplateId;
      return this;
    }

    /**
     * With service id builder.
     *
     * @param serviceId the service id
     * @return the builder
     */
    public Builder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    /**
     * With compute provider type builder.
     *
     * @param computeProviderType the compute provider type
     * @return the builder
     */
    public Builder withComputeProviderType(String computeProviderType) {
      this.computeProviderType = computeProviderType;
      return this;
    }

    /**
     * With infra mapping type builder.
     *
     * @param infraMappingType the infra mapping type
     * @return the builder
     */
    public Builder withInfraMappingType(String infraMappingType) {
      this.infraMappingType = infraMappingType;
      return this;
    }

    /**
     * With deployment type builder.
     *
     * @param deploymentType the deployment type
     * @return the builder
     */
    public Builder withDeploymentType(String deploymentType) {
      this.deploymentType = deploymentType;
      return this;
    }

    /**
     * With compute provider name builder.
     *
     * @param computeProviderName the compute provider name
     * @return the builder
     */
    public Builder withComputeProviderName(String computeProviderName) {
      this.computeProviderName = computeProviderName;
      return this;
    }

    /**
     * With name builder.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With auto populate builder.
     *
     * @param autoPopulate the auto populate
     * @return the builder
     */
    public Builder withAutoPopulate(boolean autoPopulate) {
      this.autoPopulate = autoPopulate;
      return this;
    }

    /**
     * Build gcp kubernetes infrastructure mapping.
     *
     * @return the gcp kubernetes infrastructure mapping
     */
    public GcpKubernetesInfrastructureMapping build() {
      GcpKubernetesInfrastructureMapping gcpKubernetesInfrastructureMapping = new GcpKubernetesInfrastructureMapping();
      gcpKubernetesInfrastructureMapping.setClusterName(clusterName);
      gcpKubernetesInfrastructureMapping.setNamespace(namespace);
      gcpKubernetesInfrastructureMapping.setUuid(uuid);
      gcpKubernetesInfrastructureMapping.setAppId(appId);
      gcpKubernetesInfrastructureMapping.setCreatedBy(createdBy);
      gcpKubernetesInfrastructureMapping.setCreatedAt(createdAt);
      gcpKubernetesInfrastructureMapping.setLastUpdatedBy(lastUpdatedBy);
      gcpKubernetesInfrastructureMapping.setLastUpdatedAt(lastUpdatedAt);
      gcpKubernetesInfrastructureMapping.setEntityYamlPath(entityYamlPath);
      gcpKubernetesInfrastructureMapping.setComputeProviderSettingId(computeProviderSettingId);
      gcpKubernetesInfrastructureMapping.setEnvId(envId);
      gcpKubernetesInfrastructureMapping.setServiceTemplateId(serviceTemplateId);
      gcpKubernetesInfrastructureMapping.setServiceId(serviceId);
      gcpKubernetesInfrastructureMapping.setComputeProviderType(computeProviderType);
      gcpKubernetesInfrastructureMapping.setInfraMappingType(infraMappingType);
      gcpKubernetesInfrastructureMapping.setDeploymentType(deploymentType);
      gcpKubernetesInfrastructureMapping.setComputeProviderName(computeProviderName);
      gcpKubernetesInfrastructureMapping.setName(name);
      gcpKubernetesInfrastructureMapping.setAutoPopulate(autoPopulate);
      gcpKubernetesInfrastructureMapping.setAccountId(accountId);
      return gcpKubernetesInfrastructureMapping;
    }
  }

  /**
   * The type Yaml.
   */
  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends ContainerInfrastructureMapping.YamlWithComputeProvider {
    private String namespace;

    @lombok.Builder
    public Yaml(String type, String harnessApiVersion, String computeProviderType, String serviceName,
        String infraMappingType, String deploymentType, String computeProviderName, String cluster, String namespace) {
      super(type, harnessApiVersion, computeProviderType, serviceName, infraMappingType, deploymentType,
          computeProviderName, cluster);
      this.namespace = namespace;
    }
  }
}
