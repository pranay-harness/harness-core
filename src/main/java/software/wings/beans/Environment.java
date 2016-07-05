package software.wings.beans;

import static software.wings.beans.Environment.EnvironmentType.OTHER;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;

import java.util.List;
import java.util.Objects;
import javax.validation.constraints.NotNull;

// TODO: Auto-generated Javadoc

/**
 * Environment bean class.
 *
 * @author Rishi
 */
@Entity(value = "environments", noClassnameStored = true)
@Indexes(@Index(fields = { @Field("appId")
                           , @Field("name") }, options = @IndexOptions(unique = true)))
public class Environment extends Base {
  @NotEmpty private String name;
  private String description;
  @NotNull private EnvironmentType environmentType = OTHER;
  @Transient private List<ConfigFile> configFiles;

  /**
   * The enum Environment type.
   */
  public enum EnvironmentType {
    /**
     * Prod environment type.
     */
    PROD,
    /**
     * Other environment type.
     */
    OTHER
  }

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

  public EnvironmentType getEnvironmentType() {
    return environmentType;
  }

  public void setEnvironmentType(EnvironmentType environmentType) {
    this.environmentType = environmentType;
  }

  public List<ConfigFile> getConfigFiles() {
    return configFiles;
  }

  public void setConfigFiles(List<ConfigFile> configFiles) {
    this.configFiles = configFiles;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(name, description, environmentType, configFiles);
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
        && Objects.equals(this.environmentType, other.environmentType)
        && Objects.equals(this.configFiles, other.configFiles);
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String name;
    private String description;
    private EnvironmentType environmentType = OTHER;
    private List<ConfigFile> configFiles;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    public static Builder anEnvironment() {
      return new Builder();
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder withEnvironmentType(EnvironmentType environmentType) {
      this.environmentType = environmentType;
      return this;
    }

    public Builder withConfigFiles(List<ConfigFile> configFiles) {
      this.configFiles = configFiles;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public Builder but() {
      return anEnvironment()
          .withName(name)
          .withDescription(description)
          .withEnvironmentType(environmentType)
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
      environment.setEnvironmentType(environmentType);
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
