package software.wings.beans;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Transient;
import software.wings.sm.RepeatElementType;
import software.wings.sm.Repeatable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity(value = "hosts", noClassnameStored = true)
@Indexes(@Index(fields = { @Field("infraId")
                           , @Field("hostName") }, options = @IndexOptions(unique = true)))
public class Host extends Base implements Repeatable {
  private String infraId;
  private String hostName;

  @FormDataParam("hostAttributes")
  @Reference(idOnly = true, ignoreMissing = true)
  private EnvironmentAttribute hostAttributes;

  @FormDataParam("bastionHostAttributes")
  @Reference(idOnly = true, ignoreMissing = true)
  private EnvironmentAttribute bastionHostAttributes;

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

  public EnvironmentAttribute getHostAttributes() {
    return hostAttributes;
  }

  public void setHostAttributes(EnvironmentAttribute hostAttributes) {
    this.hostAttributes = hostAttributes;
  }

  public EnvironmentAttribute getBastionHostAttributes() {
    return bastionHostAttributes;
  }

  public void setBastionHostAttributes(EnvironmentAttribute bastionHostAttributes) {
    this.bastionHostAttributes = bastionHostAttributes;
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
        + Objects.hash(infraId, hostName, hostAttributes, bastionHostAttributes, tags, configFiles);
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
        && Objects.equals(this.hostAttributes, other.hostAttributes)
        && Objects.equals(this.bastionHostAttributes, other.bastionHostAttributes)
        && Objects.equals(this.tags, other.tags) && Objects.equals(this.configFiles, other.configFiles);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("infraId", infraId)
        .add("hostName", hostName)
        .add("hostAttributes", hostAttributes)
        .add("bastionHostAttributes", bastionHostAttributes)
        .add("tags", tags)
        .add("configFiles", configFiles)
        .toString();
  }

  public static final class HostBuilder {
    private String infraId;
    private String hostName;
    private EnvironmentAttribute hostAttributes;
    private EnvironmentAttribute bastionHostAttributes;
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

    public HostBuilder withHostAttributes(EnvironmentAttribute hostAttributes) {
      this.hostAttributes = hostAttributes;
      return this;
    }

    public HostBuilder withBastionHostAttributes(EnvironmentAttribute bastionHostAttributes) {
      this.bastionHostAttributes = bastionHostAttributes;
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
          .withHostAttributes(hostAttributes)
          .withBastionHostAttributes(bastionHostAttributes)
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
      host.setHostAttributes(hostAttributes);
      host.setBastionHostAttributes(bastionHostAttributes);
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
