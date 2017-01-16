package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.List;

/**
 * Created by anubhaw on 1/10/17.
 */
@JsonTypeName("PHYSICAL_DATA_CENTER")
public class PhysicalInfrastructureMapping extends InfrastructureMapping {
  @Attributes(title = "Host Names") private List<String> hostnames;
  @Attributes(title = "Connection Type") private String hostConnectionAttrs;

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
   * Gets host connection attrs.
   *
   * @return the host connection attrs
   */
  public String getHostConnectionAttrs() {
    return hostConnectionAttrs;
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
   * The type Builder.
   */
  public static final class Builder {
    private String computeProviderSettingId;
    private List<String> hostnames;
    private String envId;
    private String serviceTemplateId;
    private String hostConnectionAttrs;
    private String computeProviderType;
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
          .withHostnames(hostnames)
          .withEnvId(envId)
          .withServiceTemplateId(serviceTemplateId)
          .withHostConnectionAttrs(hostConnectionAttrs)
          .withComputeProviderType(computeProviderType)
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
      physicalInfrastructureMapping.setHostnames(hostnames);
      physicalInfrastructureMapping.setEnvId(envId);
      physicalInfrastructureMapping.setServiceTemplateId(serviceTemplateId);
      physicalInfrastructureMapping.setHostConnectionAttrs(hostConnectionAttrs);
      physicalInfrastructureMapping.setComputeProviderType(computeProviderType);
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
