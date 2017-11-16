package software.wings.beans;

import static software.wings.beans.PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping;

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;
import software.wings.stencils.EnumData;
import software.wings.utils.Util;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Created by anubhaw on 1/10/17.
 */
@JsonTypeName("PHYSICAL_DATA_CENTER_SSH")
public class PhysicalInfrastructureMapping extends InfrastructureMapping {
  @EnumData(enumDataProvider = HostConnectionAttributesDataProvider.class)
  @Attributes(title = "Connection Type", required = true)
  @NotEmpty
  private String hostConnectionAttrs;
  @Attributes(title = "Host Names", required = true) private List<String> hostNames;

  @Attributes(title = "Load Balancer") private String loadBalancerId;
  @Transient @SchemaIgnore private String loadBalancerName;

  /**
   * Instantiates a new Infrastructure mapping.
   */
  public PhysicalInfrastructureMapping() {
    super(InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH.name());
  }

  /**
   * The type Yaml.
   */
  @Data
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends InfrastructureMapping.Yaml {
    // maps to hostConnectionAttrs
    // This would either be a username/password / ssh key id
    private String connection;
    private List<String> hostNames;
    private String loadBalancer;

    /**
     * The type Builder.
     */
    public static final class Builder {
      private String computeProviderType;
      // maps to hostConnectionAttrs
      // This would either be a username/password / ssh key id
      private String connection;
      private String serviceName;
      private List<String> hostNames;
      private String infraMappingType;
      private String type;
      private String loadBalancer;
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
       * With connection builder.
       *
       * @param connection the connection
       * @return the builder
       */
      public Builder withConnection(String connection) {
        this.connection = connection;
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
       * With host names builder.
       *
       * @param hostNames the host names
       * @return the builder
       */
      public Builder withHostNames(List<String> hostNames) {
        this.hostNames = hostNames;
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
       * With load balancer builder.
       *
       * @param loadBalancer the load balancer
       * @return the builder
       */
      public Builder withLoadBalancer(String loadBalancer) {
        this.loadBalancer = loadBalancer;
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
            .withComputeProviderType(computeProviderType)
            .withConnection(connection)
            .withServiceName(serviceName)
            .withHostNames(hostNames)
            .withInfraMappingType(infraMappingType)
            .withType(type)
            .withLoadBalancer(loadBalancer)
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
        yaml.setComputeProviderType(computeProviderType);
        yaml.setConnection(connection);
        yaml.setServiceName(serviceName);
        yaml.setHostNames(hostNames);
        yaml.setInfraMappingType(infraMappingType);
        yaml.setType(type);
        yaml.setLoadBalancer(loadBalancer);
        yaml.setDeploymentType(deploymentType);
        yaml.setComputeProviderName(computeProviderName);
        yaml.setName(name);
        return yaml;
      }
    }
  }

  /**
   * Gets hostNames.
   *
   * @return the hostNames
   */
  public List<String> getHostNames() {
    return hostNames;
  }

  /**
   * Sets hostNames.
   *
   * @param hostNames the hostNames
   */
  public void setHostNames(List<String> hostNames) {
    this.hostNames = hostNames;
  }

  @Override
  public String getHostConnectionAttrs() {
    return hostConnectionAttrs;
  }

  @SchemaIgnore
  @Override
  public String getDefaultName() {
    return Util.normalize(String.format(
        "%s (Data Center_SSH)", Optional.ofNullable(this.getComputeProviderName()).orElse("data-center")));
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    final PhysicalInfrastructureMapping other = (PhysicalInfrastructureMapping) obj;
    return Objects.equals(this.hostNames, other.hostNames);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("hostNames", hostNames).toString();
  }

  /**
   * Sets host connection attrs.
   *
   * @param hostConnectionAttrs the host connection attrs
   */
  public void setHostConnectionAttrs(String hostConnectionAttrs) {
    this.hostConnectionAttrs = hostConnectionAttrs;
  }

  /**
   * Gets load balancer id.
   *
   * @return the load balancer id
   */
  public String getLoadBalancerId() {
    return loadBalancerId;
  }

  /**
   * Sets load balancer id.
   *
   * @param loadBalancerId the load balancer id
   */
  public void setLoadBalancerId(String loadBalancerId) {
    this.loadBalancerId = loadBalancerId;
  }

  /**
   * Gets load balancer name.
   *
   * @return the load balancer name
   */
  public String getLoadBalancerName() {
    return loadBalancerName;
  }

  /**
   * Sets load balancer name.
   *
   * @param loadBalancerName the load balancer name
   */
  public void setLoadBalancerName(String loadBalancerName) {
    this.loadBalancerName = loadBalancerName;
  }

  /**
   * Deep clone builder.
   *
   * @return the builder
   */
  public Builder deepClone() {
    return aPhysicalInfrastructureMapping()
        .withHostConnectionAttrs(getHostConnectionAttrs())
        .withHostNames(getHostNames())
        .withLoadBalancerId(getLoadBalancerId())
        .withUuid(getUuid())
        .withAppId(getAppId())
        .withCreatedBy(getCreatedBy())
        .withCreatedAt(getCreatedAt())
        .withLastUpdatedBy(getLastUpdatedBy())
        .withLastUpdatedAt(getLastUpdatedAt())
        .withComputeProviderSettingId(getComputeProviderSettingId())
        .withEnvId(getEnvId())
        .withServiceTemplateId(getServiceTemplateId())
        .withServiceId(getServiceId())
        .withComputeProviderType(getComputeProviderType())
        .withDeploymentType(getDeploymentType())
        .withComputeProviderName(getComputeProviderName())
        .withName(getName());
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
    private String hostConnectionAttrs;
    private List<String> hostNames;
    private String loadBalancerId;
    private String loadBalancerName;
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
     * A physical infrastructure mapping builder.
     *
     * @return the builder
     */
    public static Builder aPhysicalInfrastructureMapping() {
      return new Builder();
    }

    /**
     * With host connection attrs builder.
     *
     * @param hostConnectionAttrs the host connection attrs
     * @return the builder
     */
    public Builder withHostConnectionAttrs(String hostConnectionAttrs) {
      this.hostConnectionAttrs = hostConnectionAttrs;
      return this;
    }

    /**
     * With host names builder.
     *
     * @param hostNames the host names
     * @return the builder
     */
    public Builder withHostNames(List<String> hostNames) {
      this.hostNames = hostNames;
      return this;
    }

    /**
     * With load balancer id builder.
     *
     * @param loadBalancerId the load balancer id
     * @return the builder
     */
    public Builder withLoadBalancerId(String loadBalancerId) {
      this.loadBalancerId = loadBalancerId;
      return this;
    }

    /**
     * With load balancer name builder.
     *
     * @param loadBalancerName the load balancer name
     * @return the builder
     */
    public Builder withLoadBalancerName(String loadBalancerName) {
      this.loadBalancerName = loadBalancerName;
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
      return aPhysicalInfrastructureMapping()
          .withHostConnectionAttrs(hostConnectionAttrs)
          .withHostNames(hostNames)
          .withLoadBalancerId(loadBalancerId)
          .withLoadBalancerName(loadBalancerName)
          .withUuid(uuid)
          .withAppId(appId)
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
     * Build physical infrastructure mapping.
     *
     * @return the physical infrastructure mapping
     */
    public PhysicalInfrastructureMapping build() {
      PhysicalInfrastructureMapping physicalInfrastructureMapping = new PhysicalInfrastructureMapping();
      physicalInfrastructureMapping.setHostConnectionAttrs(hostConnectionAttrs);
      physicalInfrastructureMapping.setHostNames(hostNames);
      physicalInfrastructureMapping.setLoadBalancerId(loadBalancerId);
      physicalInfrastructureMapping.setLoadBalancerName(loadBalancerName);
      physicalInfrastructureMapping.setUuid(uuid);
      physicalInfrastructureMapping.setAppId(appId);
      physicalInfrastructureMapping.setCreatedBy(createdBy);
      physicalInfrastructureMapping.setCreatedAt(createdAt);
      physicalInfrastructureMapping.setLastUpdatedBy(lastUpdatedBy);
      physicalInfrastructureMapping.setLastUpdatedAt(lastUpdatedAt);
      physicalInfrastructureMapping.setEntityYamlPath(entityYamlPath);
      physicalInfrastructureMapping.setComputeProviderSettingId(computeProviderSettingId);
      physicalInfrastructureMapping.setEnvId(envId);
      physicalInfrastructureMapping.setServiceTemplateId(serviceTemplateId);
      physicalInfrastructureMapping.setServiceId(serviceId);
      physicalInfrastructureMapping.setComputeProviderType(computeProviderType);
      physicalInfrastructureMapping.setInfraMappingType(infraMappingType);
      physicalInfrastructureMapping.setDeploymentType(deploymentType);
      physicalInfrastructureMapping.setComputeProviderName(computeProviderName);
      physicalInfrastructureMapping.setName(name);
      physicalInfrastructureMapping.setAutoPopulate(autoPopulate);
      return physicalInfrastructureMapping;
    }
  }
}
