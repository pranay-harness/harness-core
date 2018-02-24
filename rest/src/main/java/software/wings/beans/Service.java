package software.wings.beans;

import static java.util.Arrays.asList;
import static software.wings.beans.Service.Builder.aService;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.annotations.Version;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.ServiceCommand;
import software.wings.utils.ArtifactType;
import software.wings.yaml.BaseEntityYaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Component bean class.
 *
 * @author Rishi
 */
@Entity(value = "services", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Indexes(@Index(fields = { @Field("appId")
                           , @Field("name") }, options = @IndexOptions(unique = true)))
public class Service extends Base {
  private String name;
  private String description;
  private ArtifactType artifactType;

  @Version private long version;

  @Reference(idOnly = true, ignoreMissing = true) private AppContainer appContainer;

  @Transient private List<ConfigFile> configFiles = Lists.newArrayList();
  @Transient private List<ServiceVariable> serviceVariables = Lists.newArrayList();
  @Transient private List<ArtifactStream> artifactStreams = Lists.newArrayList();
  @Transient private List<ServiceCommand> serviceCommands = Lists.newArrayList();

  @Transient private Activity lastDeploymentActivity;
  @Transient private Activity lastProdDeploymentActivity;
  @Transient private Setup setup;

  public Service clone() {
    return aService()
        .withAppId(getAppId())
        .withName(getName())
        .withDescription(getDescription())
        .withArtifactType(getArtifactType())
        .withAppContainer(getAppContainer())
        .build();
  }

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

  /**
   * Gets last deployment activity.
   *
   * @return the last deployment activity
   */
  public Activity getLastDeploymentActivity() {
    return lastDeploymentActivity;
  }

  /**
   * Sets last deployment activity.
   *
   * @param lastDeploymentActivity the last deployment activity
   */
  public void setLastDeploymentActivity(Activity lastDeploymentActivity) {
    this.lastDeploymentActivity = lastDeploymentActivity;
  }

  /**
   * Gets last prod deployment activity.
   *
   * @return the last prod deployment activity
   */
  public Activity getLastProdDeploymentActivity() {
    return lastProdDeploymentActivity;
  }

  /**
   * Sets last prod deployment activity.
   *
   * @param lastProdDeploymentActivity the last prod deployment activity
   */
  public void setLastProdDeploymentActivity(Activity lastProdDeploymentActivity) {
    this.lastProdDeploymentActivity = lastProdDeploymentActivity;
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

  /**
   * Getter for property 'version'.
   *
   * @return Value for property 'version'.
   */
  public long getVersion() {
    return version;
  }

  /**
   * Setter for property 'version'.
   *
   * @param version Value to set for property 'version'.
   */
  public void setVersion(long version) {
    this.version = version;
  }

  /**
   * Getter for property 'serviceVariables'.
   *
   * @return Value for property 'serviceVariables'.
   */
  public List<ServiceVariable> getServiceVariables() {
    return serviceVariables;
  }

  /**
   * Setter for property 'serviceVariables'.
   *
   * @param serviceVariables Value to set for property 'serviceVariables'.
   */
  public void setServiceVariables(List<ServiceVariable> serviceVariables) {
    this.serviceVariables = serviceVariables;
  }

  /**
   * Get Service Commands
   * @return
   */
  public List<ServiceCommand> getServiceCommands() {
    return serviceCommands;
  }

  /**
   * Set service commands
   * @param serviceCommands
   */
  public void setServiceCommands(List<ServiceCommand> serviceCommands) {
    this.serviceCommands = serviceCommands;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(name, description, artifactType, appContainer, configFiles, lastDeploymentActivity,
              lastProdDeploymentActivity);
  }

  /**
   * {@inheritDoc}
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
    final Service other = (Service) obj;
    return Objects.equals(this.name, other.name) && Objects.equals(this.description, other.description)
        && Objects.equals(this.artifactType, other.artifactType)
        && Objects.equals(this.appContainer, other.appContainer) && Objects.equals(this.configFiles, other.configFiles)
        && Objects.equals(this.lastDeploymentActivity, other.lastDeploymentActivity)
        && Objects.equals(this.lastProdDeploymentActivity, other.lastProdDeploymentActivity);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("description", description)
        .add("artifactType", artifactType)
        .add("appContainer", appContainer)
        .add("configFiles", configFiles)
        .add("lastDeploymentActivity", lastDeploymentActivity)
        .add("lastProdDeploymentActivity", lastProdDeploymentActivity)
        .toString();
  }

  public List<ArtifactStream> getArtifactStreams() {
    return artifactStreams;
  }

  public void setArtifactStreams(List<ArtifactStream> artifactStreams) {
    this.artifactStreams = artifactStreams;
  }

  public Builder toBuilder() {
    return aService()
        .withName(getName())
        .withDescription(getDescription())
        .withArtifactType(getArtifactType())
        .withVersion(getVersion())
        .withAppContainer(getAppContainer())
        .withConfigFiles(Lists.newArrayList(getConfigFiles()))
        .withLastDeploymentActivity(getLastDeploymentActivity())
        .withLastProdDeploymentActivity(getLastProdDeploymentActivity())
        .withSetup(getSetup())
        .withUuid(getUuid())
        .withAppId(getAppId())
        .withCreatedBy(getCreatedBy())
        .withCreatedAt(getCreatedAt())
        .withLastUpdatedBy(getLastUpdatedBy())
        .withLastUpdatedAt(getLastUpdatedAt());
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String name;
    private String description;
    private ArtifactType artifactType;
    private List<ServiceCommand> serviceCommands = Lists.newArrayList();
    private long version;
    private AppContainer appContainer;
    private List<ConfigFile> configFiles = Lists.newArrayList();
    private Activity lastDeploymentActivity;
    private Activity lastProdDeploymentActivity;
    private Setup setup;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    /**
     * A service builder.
     *
     * @return the builder
     */
    public static Builder aService() {
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
     * With artifact type builder.
     *
     * @param artifactType the artifact type
     * @return the builder
     */
    public Builder withArtifactType(ArtifactType artifactType) {
      this.artifactType = artifactType;
      return this;
    }

    /**
     * With serviceCommands builder.
     *
     * @param commands the serviceCommands
     * @return the builder
     */
    public Builder withCommands(List<ServiceCommand> commands) {
      this.serviceCommands = commands;
      return this;
    }

    /**
     * Add serviceCommands builder.
     *
     * @param commands the serviceCommands
     * @return the builder
     */
    public Builder addCommands(ServiceCommand... commands) {
      this.serviceCommands.addAll(asList(commands));
      return this;
    }

    /**
     * With version builder.
     *
     * @param version the version
     * @return the builder
     */
    public Builder withVersion(long version) {
      this.version = version;
      return this;
    }

    /**
     * With app container builder.
     *
     * @param appContainer the app container
     * @return the builder
     */
    public Builder withAppContainer(AppContainer appContainer) {
      this.appContainer = appContainer;
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
     * With last deployment activity builder.
     *
     * @param lastDeploymentActivity the last deployment activity
     * @return the builder
     */
    public Builder withLastDeploymentActivity(Activity lastDeploymentActivity) {
      this.lastDeploymentActivity = lastDeploymentActivity;
      return this;
    }

    /**
     * With last prod deployment activity builder.
     *
     * @param lastProdDeploymentActivity the last prod deployment activity
     * @return the builder
     */
    public Builder withLastProdDeploymentActivity(Activity lastProdDeploymentActivity) {
      this.lastProdDeploymentActivity = lastProdDeploymentActivity;
      return this;
    }

    /**
     * With setup builder.
     *
     * @param setup the setup
     * @return the builder
     */
    public Builder withSetup(Setup setup) {
      this.setup = setup;
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
    public Builder withCreatedBy(EmbeddedUser createdBy) {
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
    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
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
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aService()
          .withName(name)
          .withDescription(description)
          .withArtifactType(artifactType)
          .withCommands(Lists.newArrayList(serviceCommands))
          .withVersion(version)
          .withAppContainer(appContainer)
          .withConfigFiles(Lists.newArrayList(configFiles))
          .withLastDeploymentActivity(lastDeploymentActivity)
          .withLastProdDeploymentActivity(lastProdDeploymentActivity)
          .withSetup(setup)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    /**
     * Build service.
     *
     * @return the service
     */
    public Service build() {
      Service service = new Service();
      service.setName(name);
      service.setDescription(description);
      service.setArtifactType(artifactType);
      service.setVersion(version);
      service.setAppContainer(appContainer);
      service.setConfigFiles(configFiles);
      service.setLastDeploymentActivity(lastDeploymentActivity);
      service.setLastProdDeploymentActivity(lastProdDeploymentActivity);
      service.setSetup(setup);
      service.setUuid(uuid);
      service.setAppId(appId);
      service.setCreatedBy(createdBy);
      service.setCreatedAt(createdAt);
      service.setLastUpdatedBy(lastUpdatedBy);
      service.setLastUpdatedAt(lastUpdatedAt);
      service.setServiceCommands(serviceCommands);
      return service;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends BaseEntityYaml {
    private String description;
    private String artifactType;
    private String applicationStack;
    private List<NameValuePair.Yaml> configVariables = new ArrayList<>();

    @lombok.Builder
    public Yaml(String harnessApiVersion, String description, String artifactType, String applicationStack,
        List<NameValuePair.Yaml> configVariables) {
      super(EntityType.SERVICE.name(), harnessApiVersion);
      this.description = description;
      this.artifactType = artifactType;
      this.applicationStack = applicationStack;
      this.configVariables = configVariables;
    }
  }
}
