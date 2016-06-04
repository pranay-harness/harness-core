package software.wings.beans;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

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

// TODO: Auto-generated Javadoc

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
  @NotEmpty
  private SettingAttribute hostConnAttr;

  @FormDataParam("bastionConnAttr")
  @Reference(idOnly = true, ignoreMissing = true)
  private SettingAttribute bastionConnAttr;

  @FormDataParam("tags") @Reference(idOnly = true, ignoreMissing = true) private List<Tag> tags = new ArrayList<>();

  @Transient @JsonProperty(access = WRITE_ONLY) private List<String> hostNames; // to support bulk add host API

  @Transient private List<ConfigFile> configFiles = new ArrayList<>();

  @Transient private HostConnectionCredential hostConnectionCredential;

  public String getInfraId() {
    return infraId;
  }

  public void setInfraId(String infraId) {
    this.infraId = infraId;
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public SettingAttribute getHostConnAttr() {
    return hostConnAttr;
  }

  public void setHostConnAttr(SettingAttribute hostConnAttr) {
    this.hostConnAttr = hostConnAttr;
  }

  public SettingAttribute getBastionConnAttr() {
    return bastionConnAttr;
  }

  public void setBastionConnAttr(SettingAttribute bastionConnAttr) {
    this.bastionConnAttr = bastionConnAttr;
  }

  public List<Tag> getTags() {
    return tags;
  }

  public void setTags(List<Tag> tags) {
    this.tags = tags;
  }

  public List<ConfigFile> getConfigFiles() {
    return configFiles;
  }

  public void setConfigFiles(List<ConfigFile> configFiles) {
    this.configFiles = configFiles;
  }

  public List<String> getHostNames() {
    return hostNames;
  }

  public void setHostNames(List<String> hostNames) {
    this.hostNames = hostNames;
  }

  public HostConnectionCredential getHostConnectionCredential() {
    return hostConnectionCredential;
  }

  public void setHostConnectionCredential(HostConnectionCredential hostConnectionCredential) {
    this.hostConnectionCredential = hostConnectionCredential;
  }

  public String getOsType() {
    return osType;
  }

  public void setOsType(String osType) {
    this.osType = osType;
  }

  /* (non-Javadoc)
   * @see software.wings.beans.Base#hashCode()
   */
  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(infraId, hostName, osType, hostConnAttr, bastionConnAttr, tags, hostNames, configFiles,
              hostConnectionCredential);
  }

  /* (non-Javadoc)
   * @see software.wings.beans.Base#equals(java.lang.Object)
   */
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
        && Objects.equals(this.hostNames, other.hostNames) && Objects.equals(this.configFiles, other.configFiles)
        && Objects.equals(this.hostConnectionCredential, other.hostConnectionCredential);
  }

  /**
   * The Class HostBuilder.
   */
  public static final class HostBuilder {
    private String infraId;
    private String hostName;
    private String osType;
    private SettingAttribute hostConnAttr;
    private SettingAttribute bastionConnAttr;
    private List<Tag> tags = new ArrayList<>();
    private List<String> hostNames; // to support bulk add host API
    private List<ConfigFile> configFiles = new ArrayList<>();
    private HostConnectionCredential hostConnectionCredential;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private HostBuilder() {}

    /**
     * A host.
     *
     * @return the host builder
     */
    public static HostBuilder aHost() {
      return new HostBuilder();
    }

    /**
     * With infra id.
     *
     * @param infraId the infra id
     * @return the host builder
     */
    public HostBuilder withInfraId(String infraId) {
      this.infraId = infraId;
      return this;
    }

    /**
     * With host name.
     *
     * @param hostName the host name
     * @return the host builder
     */
    public HostBuilder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    /**
     * With os type.
     *
     * @param osType the os type
     * @return the host builder
     */
    public HostBuilder withOsType(String osType) {
      this.osType = osType;
      return this;
    }

    /**
     * With host conn attr.
     *
     * @param hostConnAttr the host conn attr
     * @return the host builder
     */
    public HostBuilder withHostConnAttr(SettingAttribute hostConnAttr) {
      this.hostConnAttr = hostConnAttr;
      return this;
    }

    /**
     * With bastion conn attr.
     *
     * @param bastionConnAttr the bastion conn attr
     * @return the host builder
     */
    public HostBuilder withBastionConnAttr(SettingAttribute bastionConnAttr) {
      this.bastionConnAttr = bastionConnAttr;
      return this;
    }

    /**
     * With tags.
     *
     * @param tags the tags
     * @return the host builder
     */
    public HostBuilder withTags(List<Tag> tags) {
      this.tags = tags;
      return this;
    }

    /**
     * With host names.
     *
     * @param hostNames the host names
     * @return the host builder
     */
    public HostBuilder withHostNames(List<String> hostNames) {
      this.hostNames = hostNames;
      return this;
    }

    /**
     * With config files.
     *
     * @param configFiles the config files
     * @return the host builder
     */
    public HostBuilder withConfigFiles(List<ConfigFile> configFiles) {
      this.configFiles = configFiles;
      return this;
    }

    /**
     * With host connection credential.
     *
     * @param hostConnectionCredential the host connection credential
     * @return the host builder
     */
    public HostBuilder withHostConnectionCredential(HostConnectionCredential hostConnectionCredential) {
      this.hostConnectionCredential = hostConnectionCredential;
      return this;
    }

    /**
     * With uuid.
     *
     * @param uuid the uuid
     * @return the host builder
     */
    public HostBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id.
     *
     * @param appId the app id
     * @return the host builder
     */
    public HostBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by.
     *
     * @param createdBy the created by
     * @return the host builder
     */
    public HostBuilder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at.
     *
     * @param createdAt the created at
     * @return the host builder
     */
    public HostBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by.
     *
     * @param lastUpdatedBy the last updated by
     * @return the host builder
     */
    public HostBuilder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at.
     *
     * @param lastUpdatedAt the last updated at
     * @return the host builder
     */
    public HostBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With active.
     *
     * @param active the active
     * @return the host builder
     */
    public HostBuilder withActive(boolean active) {
      this.active = active;
      return this;
    }

    /**
     * But.
     *
     * @return the host builder
     */
    public HostBuilder but() {
      return aHost()
          .withInfraId(infraId)
          .withHostName(hostName)
          .withOsType(osType)
          .withHostConnAttr(hostConnAttr)
          .withBastionConnAttr(bastionConnAttr)
          .withTags(tags)
          .withHostNames(hostNames)
          .withConfigFiles(configFiles)
          .withHostConnectionCredential(hostConnectionCredential)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    /**
     * Builds the.
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
      host.setHostNames(hostNames);
      host.setConfigFiles(configFiles);
      host.setHostConnectionCredential(hostConnectionCredential);
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
