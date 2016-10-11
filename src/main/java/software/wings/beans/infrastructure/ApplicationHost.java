package software.wings.beans.infrastructure;

import com.google.common.base.MoreObjects;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;
import software.wings.beans.ConfigFile;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 9/15/16.
 */
@Entity(value = "applicationHosts", noClassnameStored = true)
@Indexes(@Index(fields = { @Field("infraId")
                           , @Field("appId"), @Field("envId"), @Field("hostName") },
    options = @IndexOptions(unique = true)))
public class ApplicationHost extends Base {
  @NotEmpty private String envId;
  @NotEmpty private String infraId;
  @NotEmpty private String hostName;

  @Reference(idOnly = true, ignoreMissing = true) @NotNull private Host host;

  @Property("configTag") private String configTagId;

  @Transient private List<ConfigFile> configFiles = new ArrayList<>();

  /**
   * Gets host.
   *
   * @return the host
   */
  public Host getHost() {
    return host;
  }

  /**
   * Sets host.
   *
   * @param host the host
   */
  public void setHost(Host host) {
    this.host = host;
  }

  /**
   * Gets env id.
   *
   * @return the env id
   */
  public String getEnvId() {
    return envId;
  }

  /**
   * Sets env id.
   *
   * @param envId the env id
   */
  public void setEnvId(String envId) {
    this.envId = envId;
  }

  /**
   * Gets config tag.
   *
   * @return the config tag
   */
  public String getConfigTagId() {
    return configTagId;
  }

  /**
   * Sets config tag.
   *
   * @param configTagId the config tag
   */
  public void setConfigTagId(String configTagId) {
    this.configTagId = configTagId;
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

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(envId, infraId, hostName, host, configTagId, configFiles);
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
    final ApplicationHost other = (ApplicationHost) obj;
    return Objects.equals(this.envId, other.envId) && Objects.equals(this.infraId, other.infraId)
        && Objects.equals(this.hostName, other.hostName) && Objects.equals(this.host, other.host)
        && Objects.equals(this.configTagId, other.configTagId) && Objects.equals(this.configFiles, other.configFiles);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("envId", envId)
        .add("infraId", infraId)
        .add("hostName", hostName)
        .add("host", host)
        .add("configTagId", configTagId)
        .add("configFiles", configFiles)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String envId;
    private String infraId;
    private String hostName;
    private Host host;
    private String configTagId;
    private List<ConfigFile> configFiles = new ArrayList<>();
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    /**
     * An application host builder.
     *
     * @return the builder
     */
    public static Builder anApplicationHost() {
      return new Builder();
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
     * With host builder.
     *
     * @param host the host
     * @return the builder
     */
    public Builder withHost(Host host) {
      this.host = host;
      return this;
    }

    /**
     * With config tag builder.
     *
     * @param configTag the config tag
     * @return the builder
     */
    public Builder withConfigTag(Tag configTag) {
      this.configTagId = configTag.getUuid();
      return this;
    }

    public Builder withConfigTagId(String configTag) {
      this.configTagId = configTag;
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
      return anApplicationHost()
          .withEnvId(envId)
          .withInfraId(infraId)
          .withHostName(hostName)
          .withHost(host)
          .withConfigTagId(configTagId)
          .withConfigFiles(configFiles)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    /**
     * Build application host.
     *
     * @return the application host
     */
    public ApplicationHost build() {
      ApplicationHost applicationHost = new ApplicationHost();
      applicationHost.setEnvId(envId);
      applicationHost.setInfraId(infraId);
      applicationHost.setHostName(hostName);
      applicationHost.setHost(host);
      applicationHost.setConfigTagId(configTagId);
      applicationHost.setConfigFiles(configFiles);
      applicationHost.setUuid(uuid);
      applicationHost.setAppId(appId);
      applicationHost.setCreatedBy(createdBy);
      applicationHost.setCreatedAt(createdAt);
      applicationHost.setLastUpdatedBy(lastUpdatedBy);
      applicationHost.setLastUpdatedAt(lastUpdatedAt);
      applicationHost.setActive(active);
      return applicationHost;
    }
  }
}
