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
  @Transient private List<ServiceTemplate> serviceTemplates;
  @Transient private List<ConfigFile> configFiles;
  @Transient private List<Orchestration> orchestrations;
  @Transient private Setup setup;

  /**
   * Gets name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets name.
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets description.
   *
   * @param description the description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Gets environment type.
   *
   * @return the environment type
   */
  public EnvironmentType getEnvironmentType() {
    return environmentType;
  }

  /**
   * Sets environment type.
   *
   * @param environmentType the environment type
   */
  public void setEnvironmentType(EnvironmentType environmentType) {
    this.environmentType = environmentType;
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
   * Gets service templates.
   *
   * @return the service templates
   */
  public List<ServiceTemplate> getServiceTemplates() {
    return serviceTemplates;
  }

  /**
   * Sets service templates.
   *
   * @param serviceTemplates the service templates
   */
  public void setServiceTemplates(List<ServiceTemplate> serviceTemplates) {
    this.serviceTemplates = serviceTemplates;
  }

  /**
   * Gets orchestrations.
   *
   * @return the orchestrations
   */
  public List<Orchestration> getOrchestrations() {
    return orchestrations;
  }

  /**
   * Sets orchestrations.
   *
   * @param orchestrations the orchestrations
   */
  public void setOrchestrations(List<Orchestration> orchestrations) {
    this.orchestrations = orchestrations;
  }

  /**
   * Gets setup.
   *
   * @return the setup
   */
  public Setup getSetup() {
    return setup;
  }

  /**
   * Sets setup.
   *
   * @param setup the setup
   */
  public void setSetup(Setup setup) {
    this.setup = setup;
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
   * The enum Environment type.
   */
  public enum EnvironmentType {
    /**
     * Prod environment type.
     */
    PROD, /**
           * Other environment type.
           */
    OTHER
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

    /**
     * An environment builder.
     *
     * @return the builder
     */
    public static Builder anEnvironment() {
      return new Builder();
    }

    /**
     * With name builder.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With description builder.
     *
     * @param description the description
     * @return the builder
     */
    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }

    /**
     * With environment type builder.
     *
     * @param environmentType the environment type
     * @return the builder
     */
    public Builder withEnvironmentType(EnvironmentType environmentType) {
      this.environmentType = environmentType;
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
    public Builder withCreatedBy(User createdBy) {
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
    public Builder withLastUpdatedBy(User lastUpdatedBy) {
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

    /**
     * Build environment.
     *
     * @return the environment
     */
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
