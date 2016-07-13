package software.wings.beans;

import com.google.common.base.MoreObjects;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Transient;

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
@Indexes(@Index(fields = { @Field("name") }, options = @IndexOptions(unique = true)))
public class Application extends Base {
  @NotEmpty private String name;
  private String description;

  @Reference(idOnly = true, ignoreMissing = true) private List<Service> services = new ArrayList<>();
  @Reference(idOnly = true, ignoreMissing = true) private List<Environment> environments = new ArrayList<>();

  @Transient private Setup setup;
  @Transient private List<WorkflowExecution> recentExecutions;

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
   * Gets services.
   *
   * @return the services
   */
  public List<Service> getServices() {
    return services;
  }

  /**
   * Sets services.
   *
   * @param services the services
   */
  public void setServices(List<Service> services) {
    this.services = services;
  }

  /**
   * Gets environments.
   *
   * @return the environments
   */
  public List<Environment> getEnvironments() {
    return environments;
  }

  /**
   * Sets environments.
   *
   * @param environments the environments
   */
  public void setEnvironments(List<Environment> environments) {
    this.environments = environments;
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

  public List<WorkflowExecution> getRecentExecutions() {
    return recentExecutions;
  }

  public void setRecentExecutions(List<WorkflowExecution> recentExecutions) {
    this.recentExecutions = recentExecutions;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(name, description, services, environments);
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
    final Application other = (Application) obj;
    return Objects.equals(this.name, other.name) && Objects.equals(this.description, other.description)
        && Objects.equals(this.services, other.services) && Objects.equals(this.environments, other.environments);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("description", description)
        .add("services", services)
        .add("environments", environments)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String name;
    private String description;
    private List<Service> services = new ArrayList<>();
    private List<Environment> environments = new ArrayList<>();
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    /**
     * An application builder.
     *
     * @return the builder
     */
    public static Builder anApplication() {
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
     * With services builder.
     *
     * @param services the services
     * @return the builder
     */
    public Builder withServices(List<Service> services) {
      this.services = services;
      return this;
    }

    /**
     * With environments builder.
     *
     * @param environments the environments
     * @return the builder
     */
    public Builder withEnvironments(List<Environment> environments) {
      this.environments = environments;
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
      return anApplication()
          .withName(name)
          .withDescription(description)
          .withServices(services)
          .withEnvironments(environments)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    /**
     * Build application.
     *
     * @return the application
     */
    public Application build() {
      Application application = new Application();
      application.setName(name);
      application.setDescription(description);
      application.setServices(services);
      application.setEnvironments(environments);
      application.setUuid(uuid);
      application.setAppId(appId);
      application.setCreatedBy(createdBy);
      application.setCreatedAt(createdAt);
      application.setLastUpdatedBy(lastUpdatedBy);
      application.setLastUpdatedAt(lastUpdatedAt);
      application.setActive(active);
      return application;
    }
  }
}
