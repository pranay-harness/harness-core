package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;

import java.util.List;
import java.util.Objects;

/**
 * Environment bean class.
 *
 * @author Rishi
 */
@Entity(value = "environments", noClassnameStored = true)
public class Environment extends Base {
  private String name;
  private String description;
  @Transient private List<ConfigFile> configFiles;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setConfigFiles(List<ConfigFile> configFiles) {
    this.configFiles = configFiles;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(name, description, configFiles);
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
    final Environment other = (Environment) obj;
    return Objects.equals(this.name, other.name) && Objects.equals(this.description, other.description)
        && Objects.equals(this.configFiles, other.configFiles);
  }

  public static final class EnvironmentBuilder {
    private String name;
    private String description;
    private List<ConfigFile> configFiles;
    private String uuid;
    //@NotNull
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private EnvironmentBuilder() {}

    public static EnvironmentBuilder anEnvironment() {
      return new EnvironmentBuilder();
    }

    public EnvironmentBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public EnvironmentBuilder withDescription(String description) {
      this.description = description;
      return this;
    }

    public EnvironmentBuilder withConfigFiles(List<ConfigFile> configFiles) {
      this.configFiles = configFiles;
      return this;
    }

    public EnvironmentBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public EnvironmentBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public EnvironmentBuilder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public EnvironmentBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public EnvironmentBuilder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public EnvironmentBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public EnvironmentBuilder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public EnvironmentBuilder but() {
      return anEnvironment()
          .withName(name)
          .withDescription(description)
          .withConfigFiles(configFiles)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    public Environment build() {
      Environment environment = new Environment();
      environment.setName(name);
      environment.setDescription(description);
      environment.setConfigFiles(configFiles);
      environment.setUuid(uuid);
      environment.setAppId(appId);
      environment.setCreatedBy(createdBy);
      environment.setCreatedAt(createdAt);
      environment.setLastUpdatedBy(lastUpdatedBy);
      environment.setLastUpdatedAt(lastUpdatedAt);
      environment.setActive(active);
      return environment;
    }
  }
}
