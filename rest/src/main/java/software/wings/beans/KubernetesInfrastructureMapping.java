package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;

/**
 * Created by anubhaw on 1/10/17.
 */
@JsonTypeName("AWS_KUBERNETES")
public class KubernetesInfrastructureMapping extends InfrastructureMapping {
  @Attributes(title = "Service cluster name") private String clusterName;

  /**
   * Instantiates a new Infrastructure mapping.
   */
  public KubernetesInfrastructureMapping() {
    super(InfrastructureMappingType.AWS_KUBERNETES.name());
  }

  /**
   * Gets cluster name.
   *
   * @return the cluster name
   */
  public String getClusterName() {
    return clusterName;
  }

  /**
   * Sets cluster name.
   *
   * @param clusterName the cluster name
   */
  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  @SchemaIgnore
  @Override
  @Attributes(title = "Connection Type")
  public String getHostConnectionAttrs() {
    return super.getHostConnectionAttrs();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String clusterName;
    private String computeProviderSettingId;
    private String envId;
    private String serviceTemplateId;
    private String serviceId;
    private String computeProviderType;
    private String deploymentType;
    private String hostConnectionAttrs;
    private String displayName;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    /**
     * A kubernetes infrastructure mapping builder.
     *
     * @return the builder
     */
    public static Builder aKubernetesInfrastructureMapping() {
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
     * With display name builder.
     *
     * @param displayName the display name
     * @return the builder
     */
    public Builder withDisplayName(String displayName) {
      this.displayName = displayName;
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
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aKubernetesInfrastructureMapping()
          .withClusterName(clusterName)
          .withComputeProviderSettingId(computeProviderSettingId)
          .withEnvId(envId)
          .withServiceTemplateId(serviceTemplateId)
          .withServiceId(serviceId)
          .withComputeProviderType(computeProviderType)
          .withDeploymentType(deploymentType)
          .withHostConnectionAttrs(hostConnectionAttrs)
          .withDisplayName(displayName)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    /**
     * Build kubernetes infrastructure mapping.
     *
     * @return the kubernetes infrastructure mapping
     */
    public KubernetesInfrastructureMapping build() {
      KubernetesInfrastructureMapping kubernetesInfrastructureMapping = new KubernetesInfrastructureMapping();
      kubernetesInfrastructureMapping.setClusterName(clusterName);
      kubernetesInfrastructureMapping.setComputeProviderSettingId(computeProviderSettingId);
      kubernetesInfrastructureMapping.setEnvId(envId);
      kubernetesInfrastructureMapping.setServiceTemplateId(serviceTemplateId);
      kubernetesInfrastructureMapping.setServiceId(serviceId);
      kubernetesInfrastructureMapping.setComputeProviderType(computeProviderType);
      kubernetesInfrastructureMapping.setDeploymentType(deploymentType);
      kubernetesInfrastructureMapping.setHostConnectionAttrs(hostConnectionAttrs);
      kubernetesInfrastructureMapping.setDisplayName(displayName);
      kubernetesInfrastructureMapping.setUuid(uuid);
      kubernetesInfrastructureMapping.setAppId(appId);
      kubernetesInfrastructureMapping.setCreatedBy(createdBy);
      kubernetesInfrastructureMapping.setCreatedAt(createdAt);
      kubernetesInfrastructureMapping.setLastUpdatedBy(lastUpdatedBy);
      kubernetesInfrastructureMapping.setLastUpdatedAt(lastUpdatedAt);
      return kubernetesInfrastructureMapping;
    }
  }
}
