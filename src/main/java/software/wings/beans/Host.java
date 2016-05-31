package software.wings.beans;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.glassfish.jersey.media.multipart.FormDataParam;
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

@Entity(value = "hosts", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Indexes(@Index(fields = { @Field("infraId")
                           , @Field("hostName") }, options = @IndexOptions(unique = true)))
public class Host extends Base {
  private String infraId;
  private String hostName;

  @FormDataParam("hostConnAttr") @Reference(idOnly = true, ignoreMissing = true) private SettingAttribute hostConnAttr;

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

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(infraId, hostName, hostConnAttr, bastionConnAttr, tags, hostNames, configFiles);
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
        && Objects.equals(this.hostConnAttr, other.hostConnAttr)
        && Objects.equals(this.bastionConnAttr, other.bastionConnAttr) && Objects.equals(this.tags, other.tags)
        && Objects.equals(this.hostNames, other.hostNames) && Objects.equals(this.configFiles, other.configFiles);
  }

  public HostConnectionCredential getHostConnectionCredential() {
    return hostConnectionCredential;
  }

  public void setHostConnectionCredential(HostConnectionCredential hostConnectionCredential) {
    this.hostConnectionCredential = hostConnectionCredential;
  }

  public static final class HostBuilder {
    private String infraId;
    private String hostName;
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

    public HostBuilder withHostConnAttr(SettingAttribute hostConnAttr) {
      this.hostConnAttr = hostConnAttr;
      return this;
    }

    public HostBuilder withBastionConnAttr(SettingAttribute bastionConnAttr) {
      this.bastionConnAttr = bastionConnAttr;
      return this;
    }

    public HostBuilder withTags(List<Tag> tags) {
      this.tags = tags;
      return this;
    }

    public HostBuilder withHostNames(List<String> hostNames) {
      this.hostNames = hostNames;
      return this;
    }

    public HostBuilder withConfigFiles(List<ConfigFile> configFiles) {
      this.configFiles = configFiles;
      return this;
    }

    public HostBuilder withHostConnectionCredential(HostConnectionCredential hostConnectionCredential) {
      this.hostConnectionCredential = hostConnectionCredential;
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

    public Host build() {
      Host host = new Host();
      host.setInfraId(infraId);
      host.setHostName(hostName);
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
