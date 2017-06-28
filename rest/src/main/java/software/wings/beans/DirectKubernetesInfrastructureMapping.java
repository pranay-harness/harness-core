package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;

/**
 * Created by brett on 2/27/17
 */
@JsonTypeName("DIRECT_KUBERNETES")
public class DirectKubernetesInfrastructureMapping extends InfrastructureMapping {
  @Attributes(title = "Master URL", required = true) private String masterUrl;
  @Attributes(title = "Username", required = true) private String username;
  @Attributes(title = "Password", required = true) private String password;
  @Attributes(title = "Display Name", required = true) private String clusterName;

  /**
   * Instantiates a new Infrastructure mapping.
   */
  public DirectKubernetesInfrastructureMapping() {
    super(InfrastructureMappingType.DIRECT_KUBERNETES.name());
  }

  public String getMasterUrl() {
    return masterUrl;
  }

  public void setMasterUrl(String masterUrl) {
    this.masterUrl = masterUrl;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
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
    return null;
  }

  @SchemaIgnore
  @Override
  public String getDisplayName() {
    return clusterName;
  }

  public static final class DirectKubernetesInfrastructureMappingBuilder {
    private String masterUrl;
    private String username;
    private String password;
    private String clusterName;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private String computeProviderSettingId;
    private String envId;
    private String serviceTemplateId;
    private String serviceId;
    private String computeProviderType;
    private String computeProviderName;
    private String displayName;

    private DirectKubernetesInfrastructureMappingBuilder() {}

    public static DirectKubernetesInfrastructureMappingBuilder aDirectKubernetesInfrastructureMapping() {
      return new DirectKubernetesInfrastructureMappingBuilder();
    }

    public DirectKubernetesInfrastructureMappingBuilder withMasterUrl(String masterUrl) {
      this.masterUrl = masterUrl;
      return this;
    }

    public DirectKubernetesInfrastructureMappingBuilder withUsername(String username) {
      this.username = username;
      return this;
    }

    public DirectKubernetesInfrastructureMappingBuilder withPassword(String password) {
      this.password = password;
      return this;
    }

    public DirectKubernetesInfrastructureMappingBuilder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public DirectKubernetesInfrastructureMappingBuilder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public DirectKubernetesInfrastructureMappingBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public DirectKubernetesInfrastructureMappingBuilder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public DirectKubernetesInfrastructureMappingBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public DirectKubernetesInfrastructureMappingBuilder withComputeProviderSettingId(String computeProviderSettingId) {
      this.computeProviderSettingId = computeProviderSettingId;
      return this;
    }

    public DirectKubernetesInfrastructureMappingBuilder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public DirectKubernetesInfrastructureMappingBuilder withServiceTemplateId(String serviceTemplateId) {
      this.serviceTemplateId = serviceTemplateId;
      return this;
    }

    public DirectKubernetesInfrastructureMappingBuilder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public DirectKubernetesInfrastructureMappingBuilder withComputeProviderType(String computeProviderType) {
      this.computeProviderType = computeProviderType;
      return this;
    }

    public DirectKubernetesInfrastructureMappingBuilder withComputeProviderName(String computeProviderName) {
      this.computeProviderName = computeProviderName;
      return this;
    }

    public DirectKubernetesInfrastructureMappingBuilder withDisplayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public DirectKubernetesInfrastructureMappingBuilder but() {
      return aDirectKubernetesInfrastructureMapping()
          .withMasterUrl(masterUrl)
          .withUsername(username)
          .withPassword(password)
          .withClusterName(clusterName)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withComputeProviderSettingId(computeProviderSettingId)
          .withEnvId(envId)
          .withServiceTemplateId(serviceTemplateId)
          .withServiceId(serviceId)
          .withComputeProviderType(computeProviderType)
          .withComputeProviderName(computeProviderName)
          .withDisplayName(displayName);
    }

    public DirectKubernetesInfrastructureMapping build() {
      DirectKubernetesInfrastructureMapping directKubernetesInfrastructureMapping =
          new DirectKubernetesInfrastructureMapping();
      directKubernetesInfrastructureMapping.setMasterUrl(masterUrl);
      directKubernetesInfrastructureMapping.setUsername(username);
      directKubernetesInfrastructureMapping.setPassword(password);
      directKubernetesInfrastructureMapping.setClusterName(clusterName);
      directKubernetesInfrastructureMapping.setCreatedBy(createdBy);
      directKubernetesInfrastructureMapping.setCreatedAt(createdAt);
      directKubernetesInfrastructureMapping.setLastUpdatedBy(lastUpdatedBy);
      directKubernetesInfrastructureMapping.setLastUpdatedAt(lastUpdatedAt);
      directKubernetesInfrastructureMapping.setComputeProviderSettingId(computeProviderSettingId);
      directKubernetesInfrastructureMapping.setEnvId(envId);
      directKubernetesInfrastructureMapping.setServiceTemplateId(serviceTemplateId);
      directKubernetesInfrastructureMapping.setServiceId(serviceId);
      directKubernetesInfrastructureMapping.setComputeProviderType(computeProviderType);
      directKubernetesInfrastructureMapping.setComputeProviderName(computeProviderName);
      directKubernetesInfrastructureMapping.setDisplayName(displayName);
      return directKubernetesInfrastructureMapping;
    }
  }
}
