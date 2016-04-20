package software.wings.beans;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
   * Adds a service to application.
   *
   * @param service service to be added to application.
   */
  public void addService(Service service) {
    if (services == null) {
      services = new ArrayList<>();
    }
    services.add(service);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), name, description, services);
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
    Application that = (Application) obj;
    return Objects.equals(name, that.name) && Objects.equals(description, that.description)
        && Objects.equals(services, that.services);
  }

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
     * copy method for the builder.
     * @return a copy of the builder.
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

    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withServices(List<Service> services) {
      this.services = services;
      return this;
    }

    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public static Builder anApplication() {
      return new Builder();
    }

    /**
     * builds an application object.
     * @return newly created application object.
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
