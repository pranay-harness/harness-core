package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.Environment.EnvironmentType;

import java.util.List;
import java.util.Objects;

/**
 * Created by brett on 7/20/17
 */
@Entity(value = "delegateScopes")
public class DelegateScope extends Base {
  @NotEmpty private String accountId;
  private String name;

  @JsonIgnore
  private String empty; // TODO(brett): Not sure why, but this is needed when delegate has include/exclude scopes and
                        // delegate is restarted. Investigate.

  private List<TaskType> taskTypes;
  private List<EnvironmentType> environmentTypes;
  private List<String> applications;
  private List<String> environments;
  private List<String> serviceInfrastructures;

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<TaskType> getTaskTypes() {
    return taskTypes;
  }

  public void setTaskTypes(List<TaskType> taskTypes) {
    this.taskTypes = taskTypes;
  }

  public List<EnvironmentType> getEnvironmentTypes() {
    return environmentTypes;
  }

  public void setEnvironmentTypes(List<EnvironmentType> environmentTypes) {
    this.environmentTypes = environmentTypes;
  }

  public List<String> getApplications() {
    return applications;
  }

  public void setApplications(List<String> applications) {
    this.applications = applications;
  }

  public List<String> getEnvironments() {
    return environments;
  }

  public void setEnvironments(List<String> environments) {
    this.environments = environments;
  }

  public List<String> getServiceInfrastructures() {
    return serviceInfrastructures;
  }

  public void setServiceInfrastructures(List<String> serviceInfrastructures) {
    this.serviceInfrastructures = serviceInfrastructures;
  }

  public boolean isEmpty() {
    return (taskTypes == null || taskTypes.isEmpty()) && (environmentTypes == null || environmentTypes.isEmpty())
        && (applications == null || applications.isEmpty()) && (environments == null || environments.isEmpty())
        && (serviceInfrastructures == null || serviceInfrastructures.isEmpty());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;
    DelegateScope that = (DelegateScope) o;
    return Objects.equals(accountId, that.accountId) && Objects.equals(name, that.name)
        && Objects.equals(taskTypes, that.taskTypes) && Objects.equals(environmentTypes, that.environmentTypes)
        && Objects.equals(applications, that.applications) && Objects.equals(environments, that.environments)
        && Objects.equals(serviceInfrastructures, that.serviceInfrastructures);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), accountId, name, taskTypes, environmentTypes, applications, environments,
        serviceInfrastructures);
  }

  @Override
  public String toString() {
    return "DelegateScope{"
        + "accountId='" + accountId + '\'' + ", name='" + name + '\'' + ", taskTypes=" + taskTypes
        + ", environmentTypes=" + environmentTypes + ", applications=" + applications + ", environments=" + environments
        + ", serviceInfrastructures=" + serviceInfrastructures + '}';
  }

  public static final class DelegateScopeBuilder {
    private String accountId;
    private String name;
    private List<TaskType> taskTypes;
    private List<EnvironmentType> environmentTypes;
    private List<String> applications;
    private List<String> environments;
    private List<String> serviceInfrastructures;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private DelegateScopeBuilder() {}

    public static DelegateScopeBuilder aDelegateScope() {
      return new DelegateScopeBuilder();
    }

    public DelegateScopeBuilder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public DelegateScopeBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public DelegateScopeBuilder withTaskTypes(List<TaskType> taskTypes) {
      this.taskTypes = taskTypes;
      return this;
    }

    public DelegateScopeBuilder withEnvironmentTypes(List<EnvironmentType> environmentTypes) {
      this.environmentTypes = environmentTypes;
      return this;
    }

    public DelegateScopeBuilder withApplications(List<String> applications) {
      this.applications = applications;
      return this;
    }

    public DelegateScopeBuilder withEnvironments(List<String> environments) {
      this.environments = environments;
      return this;
    }

    public DelegateScopeBuilder withServiceInfrastructures(List<String> serviceInfrastructures) {
      this.serviceInfrastructures = serviceInfrastructures;
      return this;
    }

    public DelegateScopeBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public DelegateScopeBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public DelegateScopeBuilder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public DelegateScopeBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public DelegateScopeBuilder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public DelegateScopeBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public DelegateScopeBuilder but() {
      return aDelegateScope()
          .withAccountId(accountId)
          .withName(name)
          .withTaskTypes(taskTypes)
          .withEnvironmentTypes(environmentTypes)
          .withApplications(applications)
          .withEnvironments(environments)
          .withServiceInfrastructures(serviceInfrastructures)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    public DelegateScope build() {
      DelegateScope delegateScope = new DelegateScope();
      delegateScope.setAccountId(accountId);
      delegateScope.setName(name);
      delegateScope.setTaskTypes(taskTypes);
      delegateScope.setEnvironmentTypes(environmentTypes);
      delegateScope.setApplications(applications);
      delegateScope.setEnvironments(environments);
      delegateScope.setServiceInfrastructures(serviceInfrastructures);
      delegateScope.setUuid(uuid);
      delegateScope.setAppId(appId);
      delegateScope.setCreatedBy(createdBy);
      delegateScope.setCreatedAt(createdAt);
      delegateScope.setLastUpdatedBy(lastUpdatedBy);
      delegateScope.setLastUpdatedAt(lastUpdatedAt);
      return delegateScope;
    }
  }
}
