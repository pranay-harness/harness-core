package software.wings.beans;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Environment.EnvironmentType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by brett on 7/20/17
 */
public class DelegateScope extends Base {
  @NotEmpty private String accountId;

  private List<TaskType> taskTypes = new ArrayList<>();
  private List<EnvironmentType> environmentTypes = new ArrayList<>();
  private List<String> applications = new ArrayList<>();
  private List<String> environments = new ArrayList<>();
  private List<String> serviceInfrastructures = new ArrayList<>();

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
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
    return environments.isEmpty() && environmentTypes.isEmpty();
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
    return Objects.equals(accountId, that.accountId) && Objects.equals(taskTypes, that.taskTypes)
        && Objects.equals(environmentTypes, that.environmentTypes) && Objects.equals(applications, that.applications)
        && Objects.equals(environments, that.environments)
        && Objects.equals(serviceInfrastructures, that.serviceInfrastructures);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        super.hashCode(), accountId, taskTypes, environmentTypes, applications, environments, serviceInfrastructures);
  }

  @Override
  public String toString() {
    return "DelegateScope{"
        + "accountId='" + accountId + '\'' + ", taskTypes=" + taskTypes + ", environmentTypes=" + environmentTypes
        + ", applications=" + applications + ", environments=" + environments
        + ", serviceInfrastructures=" + serviceInfrastructures + '}';
  }

  public static final class DelegateScopeBuilder {
    private String accountId;
    private List<TaskType> taskTypes = new ArrayList<>();
    private List<EnvironmentType> environmentTypes = new ArrayList<>();
    private List<String> applications = new ArrayList<>();
    private List<String> environments = new ArrayList<>();
    private List<String> serviceInfrastructures = new ArrayList<>();
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
