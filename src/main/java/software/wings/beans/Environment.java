package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;

import java.util.List;
import java.util.Objects;

// TODO: Auto-generated Javadoc

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

  /* (non-Javadoc)
   * @see software.wings.beans.Base#hashCode()
   */
  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(name, description, configFiles);
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
    final Environment other = (Environment) obj;
    return Objects.equals(this.name, other.name) && Objects.equals(this.description, other.description)
        && Objects.equals(this.configFiles, other.configFiles);
  }

  /**
   * The Class EnvironmentBuilder.
   */
  public static final class EnvironmentBuilder {
    private String name;
    private String description;
    private List<ConfigFile> configFiles;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private EnvironmentBuilder() {}

    /**
     * An environment.
     *
     * @return the environment builder
     */
    public static EnvironmentBuilder anEnvironment() {
      return new EnvironmentBuilder();
    }

    /**
     * With name.
     *
     * @param name the name
     * @return the environment builder
     */
    public EnvironmentBuilder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With description.
     *
     * @param description the description
     * @return the environment builder
     */
    public EnvironmentBuilder withDescription(String description) {
      this.description = description;
      return this;
    }

    /**
     * With config files.
     *
     * @param configFiles the config files
     * @return the environment builder
     */
    public EnvironmentBuilder withConfigFiles(List<ConfigFile> configFiles) {
      this.configFiles = configFiles;
      return this;
    }

    /**
     * With uuid.
     *
     * @param uuid the uuid
     * @return the environment builder
     */
    public EnvironmentBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id.
     *
     * @param appId the app id
     * @return the environment builder
     */
    public EnvironmentBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by.
     *
     * @param createdBy the created by
     * @return the environment builder
     */
    public EnvironmentBuilder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at.
     *
     * @param createdAt the created at
     * @return the environment builder
     */
    public EnvironmentBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by.
     *
     * @param lastUpdatedBy the last updated by
     * @return the environment builder
     */
    public EnvironmentBuilder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at.
     *
     * @param lastUpdatedAt the last updated at
     * @return the environment builder
     */
    public EnvironmentBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With active.
     *
     * @param active the active
     * @return the environment builder
     */
    public EnvironmentBuilder withActive(boolean active) {
      this.active = active;
      return this;
    }

    /**
     * But.
     *
     * @return the environment builder
     */
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

    /**
     * Builds the.
     *
     * @return the environment
     */
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
