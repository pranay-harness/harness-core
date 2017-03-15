package software.wings.beans;

import com.google.common.base.MoreObjects;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by anubhaw on 4/4/16.
 */
@Entity(value = "serviceTemplates", noClassnameStored = true)
@Indexes(@Index(fields = { @Field("appId")
                           , @Field("envId"), @Field("name") }, options = @IndexOptions(unique = true)))
public class ServiceTemplate extends Base {
  @NotEmpty private String envId;
  @NotEmpty private String name;
  private String description;
  @NotEmpty private String serviceId;

  @Transient private Service service;
  @Transient private List<ConfigFile> configFilesOverrides = new ArrayList<>();
  @Transient private List<ServiceVariable> serviceVariablesOverrides = new ArrayList<>();
  @Transient private List<InfrastructureMapping> infrastructureMappings = new ArrayList<>();
  private boolean defaultServiceTemplate = false;

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
   * Gets env id.
   *
   * @return the env id
   */
  public String getEnvId() {
    return envId;
  }

  /**
   * Sets env id.
   *
   * @param envId the env id
   */
  public void setEnvId(String envId) {
    this.envId = envId;
  }

  /**
   * Gets service.
   *
   * @return the service
   */
  public Service getService() {
    return service;
  }

  /**
   * Sets service.
   *
   * @param service the service
   */
  public void setService(Service service) {
    this.service = service;
  }

  /**
   * Gets config files.
   *
   * @return the config files
   */
  public List<ConfigFile> getConfigFilesOverrides() {
    return configFilesOverrides;
  }

  /**
   * Sets config files.
   *
   * @param configFilesOverrides the config files
   */
  public void setConfigFilesOverrides(List<ConfigFile> configFilesOverrides) {
    this.configFilesOverrides = configFilesOverrides;
  }

  /**
   * Is default service template boolean.
   *
   * @return the boolean
   */
  public boolean isDefaultServiceTemplate() {
    return defaultServiceTemplate;
  }

  /**
   * Sets default service template.
   *
   * @param defaultServiceTemplate the default service template
   */
  public void setDefaultServiceTemplate(boolean defaultServiceTemplate) {
    this.defaultServiceTemplate = defaultServiceTemplate;
  }

  /**
   * Gets service id.
   *
   * @return the service id
   */
  public String getServiceId() {
    return serviceId;
  }

  /**
   * Sets service id.
   *
   * @param serviceId the service id
   */
  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  /**
   * Gets service variables.
   *
   * @return the service variables
   */
  public List<ServiceVariable> getServiceVariablesOverrides() {
    return serviceVariablesOverrides;
  }

  /**
   * Sets service variables.
   *
   * @param serviceVariablesOverrides the service variables
   */
  public void setServiceVariablesOverrides(List<ServiceVariable> serviceVariablesOverrides) {
    this.serviceVariablesOverrides = serviceVariablesOverrides;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(envId, name, description, serviceId, service, configFilesOverrides, serviceVariablesOverrides,
              defaultServiceTemplate);
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
    final ServiceTemplate other = (ServiceTemplate) obj;
    return Objects.equals(this.envId, other.envId) && Objects.equals(this.name, other.name)
        && Objects.equals(this.description, other.description) && Objects.equals(this.serviceId, other.serviceId)
        && Objects.equals(this.service, other.service)
        && Objects.equals(this.configFilesOverrides, other.configFilesOverrides)
        && Objects.equals(this.serviceVariablesOverrides, other.serviceVariablesOverrides)
        && Objects.equals(this.defaultServiceTemplate, other.defaultServiceTemplate);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("envId", envId)
        .add("name", name)
        .add("description", description)
        .add("serviceId", serviceId)
        .add("service", service)
        .add("configFilesOverrides", configFilesOverrides)
        .add("serviceVariablesOverrides", serviceVariablesOverrides)
        .add("defaultServiceTemplate", defaultServiceTemplate)
        .toString();
  }

  public List<InfrastructureMapping> getInfrastructureMappings() {
    return infrastructureMappings;
  }

  public void setInfrastructureMappings(List<InfrastructureMapping> infrastructureMappings) {
    this.infrastructureMappings = infrastructureMappings;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String envId;
    private String name;
    private String description;
    private String serviceId;
    private Service service;
    private List<ConfigFile> configFilesOverrides = new ArrayList<>();
    private List<ServiceVariable> serviceVariablesOverrides = new ArrayList<>();
    private boolean defaultServiceTemplate = false;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    /**
     * A service template builder.
     *
     * @return the builder
     */
    public static Builder aServiceTemplate() {
      return new Builder();
    }

    /**
     * With env id builder.
     *
     * @param envId the env id
     * @return the builder
     */
    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
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
     * With service id builder.
     *
     * @param serviceId the service id
     * @return the builder
     */
    public Builder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    /**
     * With service builder.
     *
     * @param service the service
     * @return the builder
     */
    public Builder withService(Service service) {
      this.service = service;
      return this;
    }

    /**
     * With config files builder.
     *
     * @param configFilesOverrides the config files
     * @return the builder
     */
    public Builder withConfigFilesOverrides(List<ConfigFile> configFilesOverrides) {
      this.configFilesOverrides = configFilesOverrides;
      return this;
    }

    /**
     * With service variables builder.
     *
     * @param serviceVariablesOverrides the service variables
     * @return the builder
     */
    public Builder withServiceVariablesOverrides(List<ServiceVariable> serviceVariablesOverrides) {
      this.serviceVariablesOverrides = serviceVariablesOverrides;
      return this;
    }

    /**
     * With default service template builder.
     *
     * @param defaultServiceTemplate the default service template
     * @return the builder
     */
    public Builder withDefaultServiceTemplate(boolean defaultServiceTemplate) {
      this.defaultServiceTemplate = defaultServiceTemplate;
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
      return aServiceTemplate()
          .withEnvId(envId)
          .withName(name)
          .withDescription(description)
          .withServiceId(serviceId)
          .withService(service)
          .withConfigFilesOverrides(configFilesOverrides)
          .withServiceVariablesOverrides(serviceVariablesOverrides)
          .withDefaultServiceTemplate(defaultServiceTemplate)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    /**
     * Build service template.
     *
     * @return the service template
     */
    public ServiceTemplate build() {
      ServiceTemplate serviceTemplate = new ServiceTemplate();
      serviceTemplate.setEnvId(envId);
      serviceTemplate.setName(name);
      serviceTemplate.setDescription(description);
      serviceTemplate.setServiceId(serviceId);
      serviceTemplate.setService(service);
      serviceTemplate.setConfigFilesOverrides(configFilesOverrides);
      serviceTemplate.setServiceVariablesOverrides(serviceVariablesOverrides);
      serviceTemplate.setDefaultServiceTemplate(defaultServiceTemplate);
      serviceTemplate.setUuid(uuid);
      serviceTemplate.setAppId(appId);
      serviceTemplate.setCreatedBy(createdBy);
      serviceTemplate.setCreatedAt(createdAt);
      serviceTemplate.setLastUpdatedBy(lastUpdatedBy);
      serviceTemplate.setLastUpdatedAt(lastUpdatedAt);
      return serviceTemplate;
    }
  }
}
