package software.wings.beans;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// TODO: Auto-generated Javadoc

/**
 * Application bean class.
 *
 * @author Rishi
 */
@Entity(value = "applications", noClassnameStored = true)
public class Application extends Base {
  private String name;
  private String description;

  @Reference(idOnly = true, ignoreMissing = true) private List<Service> services;

  @Reference(idOnly = true, ignoreMissing = true) private List<Environment> environments;

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

  public List<Service> getServices() {
    return services;
  }

  public void setServices(List<Service> services) {
    this.services = services;
  }

  /**
   * Adds the service.
   *
   * @param service the service
   */
  public void addService(Service service) {
    if (this.services == null) {
      this.services = new ArrayList<>();
    }
    this.services.add(service);
  }

  public List<Environment> getEnvironments() {
    return environments;
  }

  public void setEnvironments(List<Environment> environments) {
    this.environments = environments;
  }

  /**
   * Adds the environment.
   *
   * @param environment the environment
   */
  public void addEnvironment(Environment environment) {
    if (this.environments == null) {
      this.environments = new ArrayList<>();
    }
    this.environments.add(environment);
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
    Application that = (Application) obj;
    return Objects.equals(name, that.name) && Objects.equals(description, that.description)
        && Objects.equals(services, that.services);
  }

  /* (non-Javadoc)
   * @see software.wings.beans.Base#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), name, description, services);
  }

  /* (non-Javadoc)
   * @see software.wings.beans.Base#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("uuid", getUuid())
        .add("createdBy", getCreatedBy())
        .add("createdAt", getCreatedAt())
        .add("lastUpdatedBy", getLastUpdatedBy())
        .add("lastUpdatedAt", getLastUpdatedAt())
        .add("active", isActive())
        .add("name", name)
        .add("description", description)
        .add("services", services)
        .toString();
  }

  /**
   * The Class Builder.
   */
  public static class Builder {
    private String name;
    private String description;
    private List<Service> services;
    private String uuid;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    /**
     * An application.
     *
     * @return the builder
     */
    public static Builder anApplication() {
      return new Builder();
    }

    /**
     * With name.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With description.
     *
     * @param description the description
     * @return the builder
     */
    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }

    /**
     * With services.
     *
     * @param services the services
     * @return the builder
     */
    public Builder withServices(List<Service> services) {
      this.services = services;
      return this;
    }

    /**
     * With uuid.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With created by.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With active.
     *
     * @param active the active
     * @return the builder
     */
    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    /**
     * But.
     *
     * @return the builder
     */
    public Builder but() {
      return anApplication()
          .withName(name)
          .withDescription(description)
          .withServices(services)
          .withUuid(uuid)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    /**
     * Builds the.
     *
     * @return the application
     */
    public Application build() {
      Application application = new Application();
      application.setName(name);
      application.setDescription(description);
      application.setServices(services);
      application.setUuid(uuid);
      application.setCreatedBy(createdBy);
      application.setCreatedAt(createdAt);
      application.setLastUpdatedBy(lastUpdatedBy);
      application.setLastUpdatedAt(lastUpdatedAt);
      application.setActive(active);
      return application;
    }
  }
}
