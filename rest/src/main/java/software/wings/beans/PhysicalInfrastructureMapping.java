package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import software.wings.beans.HostConnectionAttributes.AccessType;
import software.wings.beans.HostConnectionAttributes.ConnectionType;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.List;

/**
 * Created by anubhaw on 1/10/17.
 */
@JsonTypeName("PHYSICAL_DATA_CENTER")
public class PhysicalInfrastructureMapping extends InfrastructureMapping {
  @Attributes(title = "Host Names") private List<String> hostnames;
  @Attributes(title = "Connection Type") private ConnectionType connectionType;
  @Attributes(title = "Access Type") private AccessType accessType;

  /**
   * Instantiates a new Infrastructure mapping.
   */
  public PhysicalInfrastructureMapping() {
    super(SettingVariableTypes.PHYSICAL_DATA_CENTER.name());
  }

  /**
   * Gets hostnames.
   *
   * @return the hostnames
   */
  public List<String> getHostnames() {
    return hostnames;
  }

  /**
   * Sets hostnames.
   *
   * @param hostnames the hostnames
   */
  public void setHostnames(List<String> hostnames) {
    this.hostnames = hostnames;
  }

  /**
   * Gets connection type.
   *
   * @return the connection type
   */
  public ConnectionType getConnectionType() {
    return connectionType;
  }

  /**
   * Sets connection type.
   *
   * @param connectionType the connection type
   */
  public void setConnectionType(ConnectionType connectionType) {
    this.connectionType = connectionType;
  }

  /**
   * Gets access type.
   *
   * @return the access type
   */
  public AccessType getAccessType() {
    return accessType;
  }

  /**
   * Sets access type.
   *
   * @param accessType the access type
   */
  public void setAccessType(AccessType accessType) {
    this.accessType = accessType;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String computeProviderSettingId;
    private String envId;
    private String serviceTemplateId;
    private String computeProviderType;
    private List<String> hostnames;
    private ConnectionType connectionType;
    private AccessType accessType;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

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
     * With hostnames builder.
     *
     * @param hostnames the hostnames
     * @return the builder
     */
    public Builder withHostnames(List<String> hostnames) {
      this.hostnames = hostnames;
      return this;
    }

    /**
     * With connection type builder.
     *
     * @param connectionType the connection type
     * @return the builder
     */
    public Builder withConnectionType(ConnectionType connectionType) {
      this.connectionType = connectionType;
      return this;
    }

    /**
     * With access type builder.
     *
     * @param accessType the access type
     * @return the builder
     */
    public Builder withAccessType(AccessType accessType) {
      this.accessType = accessType;
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
      return aPhysicalInfrastructureMapping()
          .withComputeProviderSettingId(computeProviderSettingId)
          .withEnvId(envId)
          .withServiceTemplateId(serviceTemplateId)
          .withComputeProviderType(computeProviderType)
          .withHostnames(hostnames)
          .withConnectionType(connectionType)
          .withAccessType(accessType)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    /**
     * Build physical infrastructure mapping.
     *
     * @return the physical infrastructure mapping
     */
    public PhysicalInfrastructureMapping build() {
      PhysicalInfrastructureMapping physicalInfrastructureMapping = new PhysicalInfrastructureMapping();
      physicalInfrastructureMapping.setComputeProviderSettingId(computeProviderSettingId);
      physicalInfrastructureMapping.setEnvId(envId);
      physicalInfrastructureMapping.setServiceTemplateId(serviceTemplateId);
      physicalInfrastructureMapping.setComputeProviderType(computeProviderType);
      physicalInfrastructureMapping.setHostnames(hostnames);
      physicalInfrastructureMapping.setConnectionType(connectionType);
      physicalInfrastructureMapping.setAccessType(accessType);
      physicalInfrastructureMapping.setUuid(uuid);
      physicalInfrastructureMapping.setAppId(appId);
      physicalInfrastructureMapping.setCreatedBy(createdBy);
      physicalInfrastructureMapping.setCreatedAt(createdAt);
      physicalInfrastructureMapping.setLastUpdatedBy(lastUpdatedBy);
      physicalInfrastructureMapping.setLastUpdatedAt(lastUpdatedAt);
      return physicalInfrastructureMapping;
    }
  }
}
