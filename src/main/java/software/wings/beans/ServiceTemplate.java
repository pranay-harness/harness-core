package software.wings.beans;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Reference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 4/4/16.
 */
@Entity(value = "serviceTemplates", noClassnameStored = true)
@Indexes(@Index(fields = { @Field("appId")
                           , @Field("envId"), @Field("name") }, options = @IndexOptions(unique = true)))
public class ServiceTemplate extends Base {
  private String envId;
  private String name;
  private String description;
  @Reference(idOnly = true, ignoreMissing = true) private Service service;
  @Reference(idOnly = true, ignoreMissing = true) private List<Tag> tags = new ArrayList<>();
  @Reference(idOnly = true, ignoreMissing = true) private List<Host> hosts = new ArrayList<>();
  @Reference(idOnly = true, ignoreMissing = true) private Set<Tag> leafTags = new HashSet<Tag>();

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
   * Gets tags.
   *
   * @return the tags
   */
  public List<Tag> getTags() {
    return tags;
  }

  /**
   * Sets tags.
   *
   * @param tags the tags
   */
  public void setTags(List<Tag> tags) {
    this.tags = tags;
  }

  /**
   * Gets hosts.
   *
   * @return the hosts
   */
  public List<Host> getHosts() {
    return hosts;
  }

  /**
   * Sets hosts.
   *
   * @param hosts the hosts
   */
  public void setHosts(List<Host> hosts) {
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
   * Gets leaf tags.
   *
   * @return the leaf tags
   */
  public Set<Tag> getLeafTags() {
    return leafTags;
  }

  /**
   * Sets leaf tags.
   *
   * @param leafTags the leaf tags
   */
  public void setLeafTags(Set<Tag> leafTags) {
    this.leafTags = leafTags;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(envId, name, description, service, tags, hosts, leafTags);
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
        && Objects.equals(this.tags, other.tags) && Objects.equals(this.hosts, other.hosts)
        && Objects.equals(this.leafTags, other.leafTags);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("envId", envId)
        .add("name", name)
        .add("description", description)
        .add("service", service)
        .add("tags", tags)
        .add("hosts", hosts)
        .add("leafTags", leafTags)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String envId;
    private String name;
    private String description;
    private Service service;
    private List<Tag> tags = new ArrayList<>();
    private List<Host> hosts = new ArrayList<>();
    private Set<Tag> leafTags = new HashSet<Tag>();
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    public static Builder aServiceTemplate() {
      return new Builder();
    }

    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder withService(Service service) {
      this.service = service;
      return this;
    }

    public Builder withTags(List<Tag> tags) {
      this.tags = tags;
      return this;
    }

    public Builder withHosts(List<Host> hosts) {
      this.hosts = hosts;
      return this;
    }

    public Builder withLeafTags(Set<Tag> leafTags) {
      this.leafTags = leafTags;
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
      return aServiceTemplate()
          .withEnvId(envId)
          .withName(name)
          .withDescription(description)
          .withService(service)
          .withTags(tags)
          .withHosts(hosts)
          .withLeafTags(leafTags)
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
      serviceTemplate.setLeafTags(leafTags);
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
