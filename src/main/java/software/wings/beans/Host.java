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
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Entity(value = "hosts", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Indexes(@Index(fields = { @Field("infraId")
                           , @Field("hostName") }, options = @IndexOptions(unique = true)))
public class Host extends Base implements ContextElement {
  private static final long serialVersionUID = 1189183137783838598L;

  private String infraId;
  private String hostName;

  @FormDataParam("hostConnAttrs")
  @Reference(idOnly = true, ignoreMissing = true)
  private SettingAttribute hostConnAttrs;

  @FormDataParam("bastionConnAttrs")
  @Reference(idOnly = true, ignoreMissing = true)
  private SettingAttribute bastionConnAttrs;

  @FormDataParam("tags") @Reference(idOnly = true, ignoreMissing = true) private List<Tag> tags = new ArrayList<>();

  @Transient @JsonProperty(access = WRITE_ONLY) private List<String> hostNames; // to support bulk add host API

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

  public SettingAttribute getHostConnAttrs() {
    return hostConnAttrs;
  }

  public void setHostConnAttrs(SettingAttribute hostConnAttrs) {
    this.hostConnAttrs = hostConnAttrs;
  }

  public SettingAttribute getBastionConnAttrs() {
    return bastionConnAttrs;
  }

  public void setBastionConnAttrs(SettingAttribute bastionConnAttrs) {
    this.bastionConnAttrs = bastionConnAttrs;
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
  public ContextElementType getElementType() {
    return ContextElementType.HOST;
  }
  @Override
  public Map<String, Object> paramMap() {
    Map<String, Object> map = new HashMap<>();
    map.put(ContextElementType.HOST.getDisplayName(), this);
    return map;
  }

  @Override
  public String getName() {
    return hostName;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(infraId, hostName, hostConnAttrs, bastionConnAttrs, tags, hostNames, configFiles);
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
        && Objects.equals(this.hostConnAttrs, other.hostConnAttrs)
        && Objects.equals(this.bastionConnAttrs, other.bastionConnAttrs) && Objects.equals(this.tags, other.tags)
        && Objects.equals(this.hostNames, other.hostNames) && Objects.equals(this.configFiles, other.configFiles);
  }

  public static final class HostBuilder {
    private String infraId;
    private String hostName;
    private SettingAttribute hostConnAttrs;
    private SettingAttribute bastionConnAttrs;
    private List<Tag> tags = new ArrayList<>();
    private List<String> hostNames; // to support bulk add host API
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

    public HostBuilder withHostConnAttrs(SettingAttribute hostConnAttrs) {
      this.hostConnAttrs = hostConnAttrs;
      return this;
    }

    public HostBuilder withBastionConnAttrs(SettingAttribute bastionConnAttrs) {
      this.bastionConnAttrs = bastionConnAttrs;
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
          .withHostConnAttrs(hostConnAttrs)
          .withBastionConnAttrs(bastionConnAttrs)
          .withTags(tags)
          .withHostNames(hostNames)
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
      host.setHostConnAttrs(hostConnAttrs);
      host.setBastionConnAttrs(bastionConnAttrs);
      host.setTags(tags);
      host.setHostNames(hostNames);
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
