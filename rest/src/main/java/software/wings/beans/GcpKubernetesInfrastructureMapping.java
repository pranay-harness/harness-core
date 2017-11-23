package software.wings.beans;

import static software.wings.beans.GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.utils.Util;

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

  /**
   * The type Yaml.
   */
  @Data
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends ContainerInfrastructureMapping.Yaml {
    private String namespace;

    /**
     * The type Builder.
     */
    public static final class Builder {
      private String namespace;
      private String cluster;
      private String computeProviderType;
      private String serviceName;
      private String infraMappingType;
      private String type;
      private String deploymentType;
      private String computeProviderName;
      private String name;

      private Builder() {}

      /**
       * A yaml builder.
       *
       * @return the builder
       */
      public static Builder aYaml() {
        return new Builder();
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
       * With cluster builder.
       *
       * @param cluster the cluster
       * @return the builder
       */
      public Builder withCluster(String cluster) {
        this.cluster = cluster;
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
       * With service name builder.
       *
       * @param serviceName the service name
       * @return the builder
       */
      public Builder withServiceName(String serviceName) {
        this.serviceName = serviceName;
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
       * With type builder.
       *
       * @param type the type
       * @return the builder
       */
      public Builder withType(String type) {
        this.type = type;
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
       * But builder.
       *
       * @return the builder
       */
      public Builder but() {
        return aYaml()
            .withNamespace(namespace)
            .withCluster(cluster)
            .withComputeProviderType(computeProviderType)
            .withServiceName(serviceName)
            .withInfraMappingType(infraMappingType)
            .withType(type)
            .withDeploymentType(deploymentType)
            .withComputeProviderName(computeProviderName)
            .withName(name);
      }

      /**
       * Build yaml.
       *
       * @return the yaml
       */
      public Yaml build() {
        Yaml yaml = new Yaml();
        yaml.setNamespace(namespace);
        yaml.setCluster(cluster);
        yaml.setComputeProviderType(computeProviderType);
        yaml.setServiceName(serviceName);
        yaml.setInfraMappingType(infraMappingType);
        yaml.setType(type);
        yaml.setDeploymentType(deploymentType);
        yaml.setComputeProviderName(computeProviderName);
        yaml.setName(name);
        return yaml;
      }
    }
  }

  @SchemaIgnore
  @Override
  public String getDefaultName() {
    return Util.normalize(String.format("%s (GCP/Kubernetes::%s) %s", this.getClusterName(),
        Optional.ofNullable(this.getComputeProviderName()).orElse(this.getComputeProviderType().toLowerCase()),
        Optional.ofNullable(this.getNamespace()).orElse("default")));
  }

  /**
   * Deep clone builder.
   *
   * @return the builder
   */
  public Builder deepClone() {
    return aGcpKubernetesInfrastructureMapping()
        .withName(getName())
        .withClusterName(getClusterName())
        .withNamespace(getNamespace())
        .withComputeProviderSettingId(getComputeProviderSettingId())
        .withEnvId(getEnvId())
        .withServiceTemplateId(getServiceTemplateId())
        .withServiceId(getServiceId())
        .withComputeProviderType(getComputeProviderType())
        .withDeploymentType(getDeploymentType())
        .withComputeProviderName(getComputeProviderName())
        .withUuid(getUuid())
        .withAppId(getAppId())
        .withCreatedBy(getCreatedBy())
        .withCreatedAt(getCreatedAt())
        .withLastUpdatedBy(getLastUpdatedBy())
        .withLastUpdatedAt(getLastUpdatedAt());
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
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aGcpKubernetesInfrastructureMapping()
          .withClusterName(clusterName)
          .withNamespace(namespace)
          .withUuid(uuid)
          .withAppId(appId)
          .withAccountId(accountId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withEntityYamlPath(entityYamlPath)
          .withComputeProviderSettingId(computeProviderSettingId)
          .withEnvId(envId)
          .withServiceTemplateId(serviceTemplateId)
          .withServiceId(serviceId)
          .withComputeProviderType(computeProviderType)
          .withInfraMappingType(infraMappingType)
          .withDeploymentType(deploymentType)
          .withComputeProviderName(computeProviderName)
          .withName(name)
          .withAutoPopulate(autoPopulate);
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
}
