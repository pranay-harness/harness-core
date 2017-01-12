package software.wings.beans;

import static software.wings.beans.EntityType.HOST;

import com.google.common.base.MoreObjects;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.infrastructure.ApplicationHost;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.validation.constraints.NotNull;

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
  @NotNull private EntityType mappedBy = HOST;
  @NotEmpty private String serviceId;
  private List<String> hostIds = new ArrayList<>();

  @Transient private Service service;
  @Transient private List<ApplicationHost> hosts = new ArrayList<>();
  @Transient private List<ApplicationHost> taggedHosts = new ArrayList<>();
  @Transient private List<ConfigFile> configFiles = new ArrayList<>();
  @Transient private List<ServiceVariable> serviceVariables = new ArrayList<>();
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
   * Gets mapped by.
   *
   * @return the mapped by
   */
  public EntityType getMappedBy() {
    return mappedBy;
  }

  /**
   * Sets mapped by.
   *
   * @param mappedBy the mapped by
   */
  public void setMappedBy(EntityType mappedBy) {
    this.mappedBy = mappedBy;
  }

  /**
   * Gets hosts.
   *
   * @return the hosts
   */
  public List<ApplicationHost> getHosts() {
    return hosts;
  }

  /**
   * Sets hosts.
   *
   * @param hosts the hosts
   */
  public void setHosts(List<ApplicationHost> hosts) {
    this.hosts = hosts;
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
   * Gets tagged hosts.
   *
   * @return the tagged hosts
   */
  public List<ApplicationHost> getTaggedHosts() {
    return taggedHosts;
  }

  /**
   * Sets tagged hosts.
   *
   * @param taggedHosts the tagged hosts
   */
  public void setTaggedHosts(List<ApplicationHost> taggedHosts) {
    this.taggedHosts = taggedHosts;
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
   * Gets host ids.
   *
   * @return the host ids
   */
  public List<String> getHostIds() {
    return hostIds;
  }

  /**
   * Sets host ids.
   *
   * @param hostIds the host ids
   */
  public void setHostIds(List<String> hostIds) {
    this.hostIds = hostIds;
  }

  /**
   * Gets service variables.
   *
   * @return the service variables
   */
  public List<ServiceVariable> getServiceVariables() {
    return serviceVariables;
  }

  /**
   * Sets service variables.
   *
   * @param serviceVariables the service variables
   */
  public void setServiceVariables(List<ServiceVariable> serviceVariables) {
    this.serviceVariables = serviceVariables;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(envId, name, description, mappedBy, serviceId, hostIds, service, hosts, taggedHosts, configFiles,
              serviceVariables, defaultServiceTemplate);
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
        && Objects.equals(this.description, other.description) && Objects.equals(this.mappedBy, other.mappedBy)
        && Objects.equals(this.serviceId, other.serviceId) && Objects.equals(this.hostIds, other.hostIds)
        && Objects.equals(this.service, other.service) && Objects.equals(this.hosts, other.hosts)
        && Objects.equals(this.taggedHosts, other.taggedHosts) && Objects.equals(this.configFiles, other.configFiles)
        && Objects.equals(this.serviceVariables, other.serviceVariables)
        && Objects.equals(this.defaultServiceTemplate, other.defaultServiceTemplate);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("envId", envId)
        .add("name", name)
        .add("description", description)
        .add("mappedBy", mappedBy)
        .add("serviceId", serviceId)
        .add("hostIds", hostIds)
        .add("service", service)
        .add("hosts", hosts)
        .add("taggedHosts", taggedHosts)
        .add("configFiles", configFiles)
        .add("serviceVariables", serviceVariables)
        .add("defaultServiceTemplate", defaultServiceTemplate)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String envId;
    private String name;
    private String description;
    private EntityType mappedBy = HOST;
    private String serviceId;
    private List<String> hostIds = new ArrayList<>();
    private String uuid;
    private Service service;
    private String appId;
    private List<ApplicationHost> hosts = new ArrayList<>();
    private EmbeddedUser createdBy;
    private long createdAt;
    private List<ApplicationHost> taggedHosts = new ArrayList<>();
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private List<ConfigFile> configFiles = new ArrayList<>();
    private List<ServiceVariable> serviceVariables = new ArrayList<>();
    private boolean defaultServiceTemplate = false;

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
     * With mapped by builder.
     *
     * @param mappedBy the mapped by
     * @return the builder
     */
    public Builder withMappedBy(EntityType mappedBy) {
      this.mappedBy = mappedBy;
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
     * With host ids builder.
     *
     * @param hostIds the host ids
     * @return the builder
     */
    public Builder withHostIds(List<String> hostIds) {
      this.hostIds = hostIds;
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
     * With hosts builder.
     *
     * @param hosts the hosts
     * @return the builder
     */
    public Builder withHosts(List<ApplicationHost> hosts) {
      this.hosts = hosts;
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
     * With tagged hosts builder.
     *
     * @param taggedHosts the tagged hosts
     * @return the builder
     */
    public Builder withTaggedHosts(List<ApplicationHost> taggedHosts) {
      this.taggedHosts = taggedHosts;
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
     * With service variables builder.
     *
     * @param serviceVariables the service variables
     * @return the builder
     */
    public Builder withServiceVariables(List<ServiceVariable> serviceVariables) {
      this.serviceVariables = serviceVariables;
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
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aServiceTemplate()
          .withEnvId(envId)
          .withName(name)
          .withDescription(description)
          .withMappedBy(mappedBy)
          .withServiceId(serviceId)
          .withHostIds(hostIds)
          .withUuid(uuid)
          .withService(service)
          .withAppId(appId)
          .withHosts(hosts)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withTaggedHosts(taggedHosts)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withConfigFiles(configFiles)
          .withServiceVariables(serviceVariables)
          .withDefaultServiceTemplate(defaultServiceTemplate);
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
      serviceTemplate.setMappedBy(mappedBy);
      serviceTemplate.setServiceId(serviceId);
      serviceTemplate.setHostIds(hostIds);
      serviceTemplate.setUuid(uuid);
      serviceTemplate.setService(service);
      serviceTemplate.setAppId(appId);
      serviceTemplate.setHosts(hosts);
      serviceTemplate.setCreatedBy(createdBy);
      serviceTemplate.setCreatedAt(createdAt);
      serviceTemplate.setTaggedHosts(taggedHosts);
      serviceTemplate.setLastUpdatedBy(lastUpdatedBy);
      serviceTemplate.setLastUpdatedAt(lastUpdatedAt);
      serviceTemplate.setConfigFiles(configFiles);
      serviceTemplate.setServiceVariables(serviceVariables);
      serviceTemplate.setDefaultServiceTemplate(defaultServiceTemplate);
      return serviceTemplate;
    }
  }
}
