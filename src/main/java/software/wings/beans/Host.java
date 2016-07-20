package software.wings.beans;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Transient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.validation.constraints.NotNull;

/**
 * The Class Host.
 */
@Entity(value = "hosts", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Indexes(@Index(fields = { @Field("infraId")
                           , @Field("hostName") }, options = @IndexOptions(unique = true)))
public class Host extends Base {
  @NotEmpty private String infraId;
  private String hostName;
  private String osType;

  @FormDataParam("hostConnAttr")
  @Reference(idOnly = true, ignoreMissing = true)
  @NotNull
  private SettingAttribute hostConnAttr;

  @FormDataParam("bastionConnAttr")
  @Reference(idOnly = true, ignoreMissing = true)
  private SettingAttribute bastionConnAttr;

  @FormDataParam("tags") @Reference(idOnly = true, ignoreMissing = true) private List<Tag> tags = new ArrayList<>();

  @Transient private List<ConfigFile> configFiles = new ArrayList<>();

  @Transient private HostConnectionCredential hostConnectionCredential; // TODO: remove

  @Transient @JsonProperty(access = WRITE_ONLY) private List<String> hostNames; // to support bulk add host API

  @FormDataParam("serviceTemplates")
  @Transient
  @JsonProperty(access = WRITE_ONLY)
  private List<ServiceTemplate> serviceTemplates; // to support bulk add host API

  /**
   * Gets infra id.
   *
   * @return the infra id
   */
  public String getInfraId() {
    return infraId;
  }

  /**
   * Sets infra id.
   *
   * @param infraId the infra id
   */
  public void setInfraId(String infraId) {
    this.infraId = infraId;
  }

  /**
   * Gets host name.
   *
   * @return the host name
   */
  public String getHostName() {
    return hostName;
  }

  /**
   * Sets host name.
   *
   * @param hostName the host name
   */
  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  /**
   * Gets host conn attr.
   *
   * @return the host conn attr
   */
  public SettingAttribute getHostConnAttr() {
    return hostConnAttr;
  }

  /**
   * Sets host conn attr.
   *
   * @param hostConnAttr the host conn attr
   */
  public void setHostConnAttr(SettingAttribute hostConnAttr) {
    this.hostConnAttr = hostConnAttr;
  }

  /**
   * Gets bastion conn attr.
   *
   * @return the bastion conn attr
   */
  public SettingAttribute getBastionConnAttr() {
    return bastionConnAttr;
  }

  /**
   * Sets bastion conn attr.
   *
   * @param bastionConnAttr the bastion conn attr
   */
  public void setBastionConnAttr(SettingAttribute bastionConnAttr) {
    this.bastionConnAttr = bastionConnAttr;
  }

  /**
   * Gets tags.
   *
   * @return the tags
   */
  public List<Tag> getTags() {
    return tags;
  }

  /**
   * Sets tags.
   *
   * @param tags the tags
   */
  public void setTags(List<Tag> tags) {
    this.tags = tags;
  }

  /**
   * Gets config files.
   *
   * @return the config files
   */
  public List<ConfigFile> getConfigFiles() {
    return configFiles;
  }

  /**
   * Sets config files.
   *
   * @param configFiles the config files
   */
  public void setConfigFiles(List<ConfigFile> configFiles) {
    this.configFiles = configFiles;
  }

  /**
   * Gets host names.
   *
   * @return the host names
   */
  public List<String> getHostNames() {
    return hostNames;
  }

  /**
   * Sets host names.
   *
   * @param hostNames the host names
   */
  public void setHostNames(List<String> hostNames) {
    this.hostNames = hostNames;
  }

  /**
   * Gets host connection credential.
   *
   * @return the host connection credential
   */
  public HostConnectionCredential getHostConnectionCredential() {
    return hostConnectionCredential;
  }

  /**
   * Sets host connection credential.
   *
   * @param hostConnectionCredential the host connection credential
   */
  public void setHostConnectionCredential(HostConnectionCredential hostConnectionCredential) {
    this.hostConnectionCredential = hostConnectionCredential;
  }

  /**
   * Gets os type.
   *
   * @return the os type
   */
  public String getOsType() {
    return osType;
  }

  /**
   * Sets os type.
   *
   * @param osType the os type
   */
  public void setOsType(String osType) {
    this.osType = osType;
  }

  /**
   * Gets service templates.
   *
   * @return the service templates
   */
  public List<ServiceTemplate> getServiceTemplates() {
    return serviceTemplates;
  }

  /**
   * Sets service templates.
   *
   * @param serviceTemplates the service templates
   */
  public void setServiceTemplates(List<ServiceTemplate> serviceTemplates) {
    this.serviceTemplates = serviceTemplates;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(infraId, hostName, osType, hostConnAttr, bastionConnAttr, tags, configFiles,
              hostConnectionCredential, hostNames, serviceTemplates);
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
    final Host other = (Host) obj;
    return Objects.equals(this.infraId, other.infraId) && Objects.equals(this.hostName, other.hostName)
        && Objects.equals(this.osType, other.osType) && Objects.equals(this.hostConnAttr, other.hostConnAttr)
        && Objects.equals(this.bastionConnAttr, other.bastionConnAttr) && Objects.equals(this.tags, other.tags)
        && Objects.equals(this.configFiles, other.configFiles)
        && Objects.equals(this.hostConnectionCredential, other.hostConnectionCredential)
        && Objects.equals(this.hostNames, other.hostNames)
        && Objects.equals(this.serviceTemplates, other.serviceTemplates);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("infraId", infraId)
        .add("hostName", hostName)
        .add("osType", osType)
        .add("hostConnAttr", hostConnAttr)
        .add("bastionConnAttr", bastionConnAttr)
        .add("tags", tags)
        .add("configFiles", configFiles)
        .add("hostConnectionCredential", hostConnectionCredential)
        .add("hostNames", hostNames)
        .add("serviceTemplates", serviceTemplates)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String infraId;
    private String hostName;
    private String osType;
    private SettingAttribute hostConnAttr;
    private SettingAttribute bastionConnAttr;
    private List<Tag> tags = new ArrayList<>();
    private List<ConfigFile> configFiles = new ArrayList<>();
    private HostConnectionCredential hostConnectionCredential; // TODO: remove
    private List<String> hostNames; // to support bulk add host API
    private List<ServiceTemplate> serviceTemplates; // to support bulk add host API
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    /**
     * A host builder.
     *
     * @return the builder
     */
    public static Builder aHost() {
      return new Builder();
    }

    /**
     * With infra id builder.
     *
     * @param infraId the infra id
     * @return the builder
     */
    public Builder withInfraId(String infraId) {
      this.infraId = infraId;
      return this;
    }

    /**
     * With host name builder.
     *
     * @param hostName the host name
     * @return the builder
     */
    public Builder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    /**
     * With os type builder.
     *
     * @param osType the os type
     * @return the builder
     */
    public Builder withOsType(String osType) {
      this.osType = osType;
      return this;
    }

    /**
     * With host conn attr builder.
     *
     * @param hostConnAttr the host conn attr
     * @return the builder
     */
    public Builder withHostConnAttr(SettingAttribute hostConnAttr) {
      this.hostConnAttr = hostConnAttr;
      return this;
    }

    /**
     * With bastion conn attr builder.
     *
     * @param bastionConnAttr the bastion conn attr
     * @return the builder
     */
    public Builder withBastionConnAttr(SettingAttribute bastionConnAttr) {
      this.bastionConnAttr = bastionConnAttr;
      return this;
    }

    /**
     * With tags builder.
     *
     * @param tags the tags
     * @return the builder
     */
    public Builder withTags(List<Tag> tags) {
      this.tags = tags;
      return this;
    }

    /**
     * With config files builder.
     *
     * @param configFiles the config files
     * @return the builder
     */
    public Builder withConfigFiles(List<ConfigFile> configFiles) {
      this.configFiles = configFiles;
      return this;
    }

    /**
     * With host connection credential builder.
     *
     * @param hostConnectionCredential the host connection credential
     * @return the builder
     */
    public Builder withHostConnectionCredential(HostConnectionCredential hostConnectionCredential) {
      this.hostConnectionCredential = hostConnectionCredential;
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
     * With service templates builder.
     *
     * @param serviceTemplates the service templates
     * @return the builder
     */
    public Builder withServiceTemplates(List<ServiceTemplate> serviceTemplates) {
      this.serviceTemplates = serviceTemplates;
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
    public Builder withCreatedBy(User createdBy) {
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
    public Builder withLastUpdatedBy(User lastUpdatedBy) {
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
     * With active builder.
     *
     * @param active the active
     * @return the builder
     */
    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aHost()
          .withInfraId(infraId)
          .withHostName(hostName)
          .withOsType(osType)
          .withHostConnAttr(hostConnAttr)
          .withBastionConnAttr(bastionConnAttr)
          .withTags(tags)
          .withConfigFiles(configFiles)
          .withHostConnectionCredential(hostConnectionCredential)
          .withHostNames(hostNames)
          .withServiceTemplates(serviceTemplates)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    /**
     * Build host.
     *
     * @return the host
     */
    public Host build() {
      Host host = new Host();
      host.setInfraId(infraId);
      host.setHostName(hostName);
      host.setOsType(osType);
      host.setHostConnAttr(hostConnAttr);
      host.setBastionConnAttr(bastionConnAttr);
      host.setTags(tags);
      host.setConfigFiles(configFiles);
      host.setHostConnectionCredential(hostConnectionCredential);
      host.setHostNames(hostNames);
      host.setServiceTemplates(serviceTemplates);
      host.setUuid(uuid);
      host.setAppId(appId);
      host.setCreatedBy(createdBy);
      host.setCreatedAt(createdAt);
      host.setLastUpdatedBy(lastUpdatedBy);
      host.setLastUpdatedAt(lastUpdatedAt);
      host.setActive(active);
      return host;
    }
  }
}
