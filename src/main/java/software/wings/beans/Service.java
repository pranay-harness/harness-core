package software.wings.beans;

import static java.util.Arrays.asList;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.ArtifactSource.ArtifactType;

import java.util.List;
import java.util.Objects;

// TODO: Auto-generated Javadoc

/**
 * Component bean class.
 *
 * @author Rishi
 */
@Entity(value = "services", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Service extends Base {
  private String name;
  private String description;
  private ArtifactType artifactType;
  private List<Command> commands = Lists.newArrayList();

  @Reference(idOnly = true, ignoreMissing = true) private AppContainer appContainer;

  @Transient private List<ConfigFile> configFiles;

  @Transient private Activity lastDeploymentActivity;
  @Transient private Activity lastProdDeploymentActivity;

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
   * Gets artifact type.
   *
   * @return the artifact type
   */
  public ArtifactType getArtifactType() {
    return artifactType;
  }

  /**
   * Sets artifact type.
   *
   * @param artifactType the artifact type
   */
  public void setArtifactType(ArtifactType artifactType) {
    this.artifactType = artifactType;
  }

  /**
   * Gets commands.
   *
   * @return the commands
   */
  public List<Command> getCommands() {
    return commands;
  }

  /**
   * Sets commands.
   *
   * @param commands the commands
   */
  public void setCommands(List<Command> commands) {
    this.commands = commands;
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
   * Gets config files.
   *
   * @return the config files
   */
  public List<ConfigFile> getConfigFiles() {
    return configFiles;
  }

  /**
   * Gets app container.
   *
   * @return the app container
   */
  public AppContainer getAppContainer() {
    return appContainer;
  }

  /**
   * Sets app container.
   *
   * @param appContainer the app container
   */
  public void setAppContainer(AppContainer appContainer) {
    this.appContainer = appContainer;
  }

  public Activity getLastDeploymentActivity() {
    return lastDeploymentActivity;
  }

  public void setLastDeploymentActivity(Activity lastDeploymentActivity) {
    this.lastDeploymentActivity = lastDeploymentActivity;
  }

  public Activity getLastProdDeploymentActivity() {
    return lastProdDeploymentActivity;
  }

  public void setLastProdDeploymentActivity(Activity lastProdDeploymentActivity) {
    this.lastProdDeploymentActivity = lastProdDeploymentActivity;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(name, description, artifactType, commands, appContainer, configFiles, lastDeploymentActivity,
              lastProdDeploymentActivity);
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
    final Service other = (Service) obj;
    return Objects.equals(this.name, other.name) && Objects.equals(this.description, other.description)
        && Objects.equals(this.artifactType, other.artifactType) && Objects.equals(this.commands, other.commands)
        && Objects.equals(this.appContainer, other.appContainer) && Objects.equals(this.configFiles, other.configFiles)
        && Objects.equals(this.lastDeploymentActivity, other.lastDeploymentActivity)
        && Objects.equals(this.lastProdDeploymentActivity, other.lastProdDeploymentActivity);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("description", description)
        .add("artifactType", artifactType)
        .add("commands", commands)
        .add("appContainer", appContainer)
        .add("configFiles", configFiles)
        .add("lastDeploymentActivity", lastDeploymentActivity)
        .add("lastProdDeploymentActivity", lastProdDeploymentActivity)
        .toString();
  }

  public static final class Builder {
    private String name;
    private String description;
    private ArtifactType artifactType;
    private List<Command> commands = Lists.newArrayList();
    private AppContainer appContainer;
    private List<ConfigFile> configFiles;
    private Activity lastDeploymentActivity;
    private Activity lastProdDeploymentActivity;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    public static Builder aService() {
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

    public Builder withArtifactType(ArtifactType artifactType) {
      this.artifactType = artifactType;
      return this;
    }

    public Builder addCommands(Command... commands) {
      this.commands.addAll(asList(commands));
      return this;
    }

    public Builder withCommands(List<Command> commands) {
      this.commands = commands;
      return this;
    }

    public Builder withAppContainer(AppContainer appContainer) {
      this.appContainer = appContainer;
      return this;
    }

    public Builder withConfigFiles(List<ConfigFile> configFiles) {
      this.configFiles = configFiles;
      return this;
    }

    public Builder withLastDeploymentActivity(Activity lastDeploymentActivity) {
      this.lastDeploymentActivity = lastDeploymentActivity;
      return this;
    }

    public Builder withLastProdDeploymentActivity(Activity lastProdDeploymentActivity) {
      this.lastProdDeploymentActivity = lastProdDeploymentActivity;
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
      return aService()
          .withName(name)
          .withDescription(description)
          .withArtifactType(artifactType)
          .withCommands(commands)
          .withAppContainer(appContainer)
          .withConfigFiles(configFiles)
          .withLastDeploymentActivity(lastDeploymentActivity)
          .withLastProdDeploymentActivity(lastProdDeploymentActivity)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    public Service build() {
      Service service = new Service();
      service.setName(name);
      service.setDescription(description);
      service.setArtifactType(artifactType);
      service.setCommands(commands);
      service.setAppContainer(appContainer);
      service.setConfigFiles(configFiles);
      service.setLastDeploymentActivity(lastDeploymentActivity);
      service.setLastProdDeploymentActivity(lastProdDeploymentActivity);
      service.setUuid(uuid);
      service.setAppId(appId);
      service.setCreatedBy(createdBy);
      service.setCreatedAt(createdAt);
      service.setLastUpdatedBy(lastUpdatedBy);
      service.setLastUpdatedAt(lastUpdatedAt);
      service.setActive(active);
      return service;
    }
  }
}
