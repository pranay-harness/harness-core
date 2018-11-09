package software.wings.beans;

import static java.util.Arrays.asList;

import com.google.common.base.MoreObjects;

import io.harness.beans.EmbeddedUser;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.yaml.BaseEntityYaml;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Application bean class.
 *
 * @author Rishi
 */
@Entity(value = "applications", noClassnameStored = true)
@Indexes(
    @Index(options = @IndexOptions(name = "yaml", unique = true), fields = { @Field("accountId")
                                                                             , @Field("name") }))
public class Application extends Base {
  public static final String NAME_KEY = "name";

  @NotEmpty private String name;
  private String description;

  @Indexed @NotEmpty private String accountId;

  @Transient private List<Service> services = new ArrayList<>();
  @Transient private List<Environment> environments = new ArrayList<>();

  @Transient private Setup setup;
  @Transient private List<WorkflowExecution> recentExecutions;
  @Transient private List<Notification> notifications;
  @Transient private long nextDeploymentOn;
  @Getter @Setter private transient YamlGitConfig yamlGitConfig;

  private transient Map<String, String> defaults = new HashMap<>();

  public Map<String, String> getDefaults() {
    return defaults;
  }

  public void setDefaults(Map<String, String> defaults) {
    this.defaults = defaults;
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
    if (services == null) {
      this.services = new ArrayList<>();
      return;
    }
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
    if (environments == null) {
      this.environments = new ArrayList<>();
      return;
    }
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

  /**
   * Gets recent executions.
   *
   * @return the recent executions
   */
  public List<WorkflowExecution> getRecentExecutions() {
    return recentExecutions;
  }

  /**
   * Sets recent executions.
   *
   * @param recentExecutions the recent executions
   */
  public void setRecentExecutions(List<WorkflowExecution> recentExecutions) {
    this.recentExecutions = recentExecutions;
  }

  /**
   * Gets notifications.
   *
   * @return the notifications
   */
  public List<Notification> getNotifications() {
    return notifications;
  }

  /**
   * Sets notifications.
   *
   * @param notifications the notifications
   */
  public void setNotifications(List<Notification> notifications) {
    this.notifications = notifications;
  }

  /**
   * Gets next deployment on.
   *
   * @return the next deployment on
   */
  public long getNextDeploymentOn() {
    return nextDeploymentOn;
  }

  /**
   * Sets next deployment on.
   *
   * @param nextDeploymentOn the next deployment on
   */
  public void setNextDeploymentOn(long nextDeploymentOn) {
    this.nextDeploymentOn = nextDeploymentOn;
  }

  /**
   * Getter for property 'accountId'.
   *
   * @return Value for property 'accountId'.
   */
  public String getAccountId() {
    return accountId;
  }

  /**
   * Setter for property 'accountId'.
   *
   * @param accountId Value to set for property 'accountId'.
   */
  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(
              name, description, accountId, services, environments, recentExecutions, notifications, nextDeploymentOn);
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
        && Objects.equals(this.accountId, other.accountId) && Objects.equals(this.services, other.services)
        && Objects.equals(this.environments, other.environments)
        && Objects.equals(this.recentExecutions, other.recentExecutions)
        && Objects.equals(this.notifications, other.notifications)
        && Objects.equals(this.nextDeploymentOn, other.nextDeploymentOn);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("description", description)
        .add("accountId", accountId)
        .add("services", services)
        .add("environments", environments)
        .add("setup", setup)
        .add("recentExecutions", recentExecutions)
        .add("notifications", notifications)
        .add("nextDeploymentOn", nextDeploymentOn)
        .add("yamlGitConfig", yamlGitConfig)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String name;
    private String description;
    private String accountId;
    private List<Service> services = new ArrayList<>();
    private List<Environment> environments = new ArrayList<>();
    private Setup setup;
    private List<WorkflowExecution> recentExecutions;
    private List<Notification> notifications;
    private long nextDeploymentOn;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private Map<String, String> defaults;
    private YamlGitConfig yamlGitConfig;

    private Builder() {}

    public static Builder anApplication() {
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

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder withServices(List<Service> services) {
      this.services = services;
      return this;
    }

    public Builder withEnvironments(List<Environment> environments) {
      this.environments = environments;
      return this;
    }

    public Builder withSetup(Setup setup) {
      this.setup = setup;
      return this;
    }

    public Builder withRecentExecutions(List<WorkflowExecution> recentExecutions) {
      this.recentExecutions = recentExecutions;
      return this;
    }

    public Builder withNotifications(List<Notification> notifications) {
      this.notifications = notifications;
      return this;
    }

    public Builder withNextDeploymentOn(long nextDeploymentOn) {
      this.nextDeploymentOn = nextDeploymentOn;
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

    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withDefaults(Map<String, String> defaults) {
      this.defaults = defaults;
      return this;
    }

    public Builder withYamlGitConfig(YamlGitConfig yamlGitConfig) {
      this.yamlGitConfig = yamlGitConfig;
      return this;
    }

    public Builder but() {
      return anApplication()
          .withName(name)
          .withDescription(description)
          .withAccountId(accountId)
          .withServices(services)
          .withEnvironments(environments)
          .withSetup(setup)
          .withRecentExecutions(recentExecutions)
          .withNotifications(notifications)
          .withNextDeploymentOn(nextDeploymentOn)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withYamlGitConfig(yamlGitConfig);
    }

    public Application build() {
      Application application = new Application();
      application.setName(name);
      application.setDescription(description);
      application.setAccountId(accountId);
      application.setServices(services);
      application.setEnvironments(environments);
      application.setSetup(setup);
      application.setRecentExecutions(recentExecutions);
      application.setNotifications(notifications);
      application.setNextDeploymentOn(nextDeploymentOn);
      application.setUuid(uuid);
      application.setAppId(appId);
      application.setCreatedBy(createdBy);
      application.setCreatedAt(createdAt);
      application.setLastUpdatedBy(lastUpdatedBy);
      application.setLastUpdatedAt(lastUpdatedAt);
      application.setDefaults(defaults);
      application.setYamlGitConfig(yamlGitConfig);
      return application;
    }
  }

  @Override
  public List<Object> generateKeywords() {
    List<Object> keywords = new ArrayList<>();
    keywords.addAll(asList(name, description));
    keywords.addAll(super.generateKeywords());
    return keywords;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends BaseEntityYaml {
    private String description;

    @lombok.Builder
    public Yaml(String type, String harnessApiVersion, String description) {
      super(type, harnessApiVersion);
      this.description = description;
    }
  }
}
