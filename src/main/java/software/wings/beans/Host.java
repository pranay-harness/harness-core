package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Transient;
import software.wings.sm.RepeatElementType;
import software.wings.sm.Repeatable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Entity(value = "hosts", noClassnameStored = true)
public class Host extends Base implements Repeatable {
  public enum AccessType { SSH, SSH_KEY, SSH_USER_PASSWD, SSH_SU_APP_ACCOUNT, SSH_SUDO_APP_ACCOUNT }

  public enum ConnectionType { SSH }

  private String infraId;

  private String hostName;

  private String osType;

  private AccessType accessType;

  private ConnectionType connectionType;

  @Reference(idOnly = true, ignoreMissing = true) private List<Tag> tags = new ArrayList<>();

  @Transient private List<ConfigFile> configFiles = new ArrayList<>();

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

  public String getOsType() {
    return osType;
  }

  public void setOsType(String osType) {
    this.osType = osType;
  }

  public AccessType getAccessType() {
    return accessType;
  }

  public void setAccessType(AccessType accessType) {
    this.accessType = accessType;
  }

  public ConnectionType getConnectionType() {
    return connectionType;
  }

  public void setConnectionType(ConnectionType connectionType) {
    this.connectionType = connectionType;
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

  public String getTagsString() {
    return tags.stream().map(Tag::getTagString).collect(Collectors.joining(","));
  }

  @Override
  public RepeatElementType getRepeatElementType() {
    return RepeatElementType.HOST;
  }

  @Override
  public String getName() {
    return hostName;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(infraId, hostName, osType, accessType, connectionType, tags, configFiles);
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
        && Objects.equals(this.osType, other.osType) && Objects.equals(this.accessType, other.accessType)
        && Objects.equals(this.connectionType, other.connectionType) && Objects.equals(this.tags, other.tags)
        && Objects.equals(this.configFiles, other.configFiles);
  }

  public static final class HostBuilder {
    private String infraId;
    private String hostName;
    private String osType;
    private AccessType accessType;
    private ConnectionType connectionType;
    private List<Tag> tags = new ArrayList<>();
    private List<ConfigFile> configFiles = new ArrayList<>();
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private HostBuilder() {}

    public static HostBuilder aHost() {
      return new HostBuilder();
    }

    public HostBuilder withInfraId(String infraId) {
      this.infraId = infraId;
      return this;
    }

    public HostBuilder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    public HostBuilder withOsType(String osType) {
      this.osType = osType;
      return this;
    }

    public HostBuilder withAccessType(AccessType accessType) {
      this.accessType = accessType;
      return this;
    }

    public HostBuilder withConnectionType(ConnectionType connectionType) {
      this.connectionType = connectionType;
      return this;
    }

    public HostBuilder withTags(List<Tag> tags) {
      this.tags = tags;
      return this;
    }

    public HostBuilder withConfigFiles(List<ConfigFile> configFiles) {
      this.configFiles = configFiles;
      return this;
    }

    public HostBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public HostBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public HostBuilder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public HostBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public HostBuilder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public HostBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public HostBuilder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public HostBuilder but() {
      return aHost()
          .withInfraId(infraId)
          .withHostName(hostName)
          .withOsType(osType)
          .withAccessType(accessType)
          .withConnectionType(connectionType)
          .withTags(tags)
          .withConfigFiles(configFiles)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    public Host build() {
      Host host = new Host();
      host.setInfraId(infraId);
      host.setHostName(hostName);
      host.setOsType(osType);
      host.setAccessType(accessType);
      host.setConnectionType(connectionType);
      host.setTags(tags);
      host.setConfigFiles(configFiles);
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
