package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
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

  @Indexed(unique = true) private String hostName;

  private String osType;

  private String ipAddress;

  private int sshPort;

  private String hostAlias;
  private String envUuid;
  private AccessType accessType;

  @Reference(idOnly = true, ignoreMissing = true) private List<Tag> tags = new ArrayList<>();

  private String infraId;

  @Transient private List<ConfigFile> configFiles = new ArrayList<>();

  public Host() {}

  public Host(String infraId, String hostName, String osType, AccessType accessType) {
    this.infraId = infraId;
    this.hostName = hostName;
    this.osType = osType;
    this.accessType = accessType;
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public String getHostAlias() {
    return hostAlias;
  }

  public void setHostAlias(String hostAlias) {
    this.hostAlias = hostAlias;
  }

  public String getEnvUuid() {
    return envUuid;
  }

  public void setEnvUuid(String envUuid) {
    this.envUuid = envUuid;
  }

  public AccessType getAccessType() {
    return accessType;
  }

  public void setAccessType(AccessType accessType) {
    this.accessType = accessType;
  }

  public int getSshPort() {
    return sshPort;
  }

  public void setSshPort(int sshPort) {
    this.sshPort = sshPort;
  }

  public String getInfraId() {
    return infraId;
  }

  public void setInfraId(String infraId) {
    this.infraId = infraId;
  }

  public List<Tag> getTags() {
    return tags;
  }

  public void setTags(List<Tag> tags) {
    this.tags = tags;
  }

  public String getOsType() {
    return osType;
  }

  public void setOsType(String osType) {
    this.osType = osType;
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
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(
              hostName, osType, ipAddress, sshPort, hostAlias, envUuid, accessType, tags, infraId, configFiles);
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
    return Objects.equals(this.hostName, other.hostName) && Objects.equals(this.osType, other.osType)
        && Objects.equals(this.ipAddress, other.ipAddress) && Objects.equals(this.sshPort, other.sshPort)
        && Objects.equals(this.hostAlias, other.hostAlias) && Objects.equals(this.envUuid, other.envUuid)
        && Objects.equals(this.accessType, other.accessType) && Objects.equals(this.tags, other.tags)
        && Objects.equals(this.infraId, other.infraId) && Objects.equals(this.configFiles, other.configFiles);
  }

  @Override
  public RepeatElementType getRepeatElementType() {
    return RepeatElementType.HOST;
  }

  @Override
  public String getName() {
    return hostName;
  }

  public static final class HostBuilder {
    private String hostName;
    private String osType;
    private String ipAddress;
    private int sshPort;
    private String hostAlias;
    private String envUuid;
    private AccessType accessType;
    private List<Tag> tags = new ArrayList<>();
    private String infraId;
    private List<ConfigFile> configFiles = new ArrayList<>();
    private String uuid;
    //@NotNull
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

    public HostBuilder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    public HostBuilder withOsType(String osType) {
      this.osType = osType;
      return this;
    }

    public HostBuilder withIpAddress(String ipAddress) {
      this.ipAddress = ipAddress;
      return this;
    }

    public HostBuilder withSshPort(int sshPort) {
      this.sshPort = sshPort;
      return this;
    }

    public HostBuilder withHostAlias(String hostAlias) {
      this.hostAlias = hostAlias;
      return this;
    }

    public HostBuilder withEnvUuid(String envUuid) {
      this.envUuid = envUuid;
      return this;
    }

    public HostBuilder withAccessType(AccessType accessType) {
      this.accessType = accessType;
      return this;
    }

    public HostBuilder withTags(List<Tag> tags) {
      this.tags = tags;
      return this;
    }

    public HostBuilder withInfraId(String infraId) {
      this.infraId = infraId;
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
          .withHostName(hostName)
          .withOsType(osType)
          .withIpAddress(ipAddress)
          .withSshPort(sshPort)
          .withHostAlias(hostAlias)
          .withEnvUuid(envUuid)
          .withAccessType(accessType)
          .withTags(tags)
          .withInfraId(infraId)
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
      host.setHostName(hostName);
      host.setOsType(osType);
      host.setIpAddress(ipAddress);
      host.setSshPort(sshPort);
      host.setHostAlias(hostAlias);
      host.setEnvUuid(envUuid);
      host.setAccessType(accessType);
      host.setTags(tags);
      host.setInfraId(infraId);
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
