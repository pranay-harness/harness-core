package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by anubhaw on 4/4/16.
 */
@Entity(value = "serviceTemplates", noClassnameStored = true)
public class ServiceTemplate extends Base {
  private String envId;
  private String name;
  private String description;
  @Reference(idOnly = true, ignoreMissing = true) private Service service;
  @Reference(idOnly = true, ignoreMissing = true) private List<Tag> tags = new ArrayList<>();
  @Reference(idOnly = true, ignoreMissing = true) private List<Host> hosts = new ArrayList<>();

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

  public List<Tag> getTags() {
    return tags;
  }

  public void setTags(List<Tag> tags) {
    this.tags = tags;
  }

  public List<Host> getHosts() {
    return hosts;
  }

  public void setHosts(List<Host> hosts) {
    this.hosts = hosts;
  }

  public String getEnvId() {
    return envId;
  }

  public void setEnvId(String envId) {
    this.envId = envId;
  }

  public Service getService() {
    return service;
  }

  public void setService(Service service) {
    this.service = service;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(envId, name, description, service, tags, hosts);
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
        && Objects.equals(this.description, other.description) && Objects.equals(this.service, other.service)
        && Objects.equals(this.tags, other.tags) && Objects.equals(this.hosts, other.hosts);
  }

  public static final class ServiceTemplateBuilder {
    private String envId;
    private String name;
    private String description;
    private Service service;
    private List<Tag> tags = new ArrayList<>();
    private List<Host> hosts = new ArrayList<>();
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private ServiceTemplateBuilder() {}

    public static ServiceTemplateBuilder aServiceTemplate() {
      return new ServiceTemplateBuilder();
    }

    public ServiceTemplateBuilder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public ServiceTemplateBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public ServiceTemplateBuilder withDescription(String description) {
      this.description = description;
      return this;
    }

    public ServiceTemplateBuilder withService(Service service) {
      this.service = service;
      return this;
    }

    public ServiceTemplateBuilder withTags(List<Tag> tags) {
      this.tags = tags;
      return this;
    }

    public ServiceTemplateBuilder withHosts(List<Host> hosts) {
      this.hosts = hosts;
      return this;
    }

    public ServiceTemplateBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public ServiceTemplateBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public ServiceTemplateBuilder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public ServiceTemplateBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public ServiceTemplateBuilder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public ServiceTemplateBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public ServiceTemplateBuilder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public ServiceTemplateBuilder but() {
      return aServiceTemplate()
          .withEnvId(envId)
          .withName(name)
          .withDescription(description)
          .withService(service)
          .withTags(tags)
          .withHosts(hosts)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    public ServiceTemplate build() {
      ServiceTemplate serviceTemplate = new ServiceTemplate();
      serviceTemplate.setEnvId(envId);
      serviceTemplate.setName(name);
      serviceTemplate.setDescription(description);
      serviceTemplate.setService(service);
      serviceTemplate.setTags(tags);
      serviceTemplate.setHosts(hosts);
      serviceTemplate.setUuid(uuid);
      serviceTemplate.setAppId(appId);
      serviceTemplate.setCreatedBy(createdBy);
      serviceTemplate.setCreatedAt(createdAt);
      serviceTemplate.setLastUpdatedBy(lastUpdatedBy);
      serviceTemplate.setLastUpdatedAt(lastUpdatedAt);
      serviceTemplate.setActive(active);
      return serviceTemplate;
    }
  }
}
