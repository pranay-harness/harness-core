package software.wings.beans;

import com.google.common.base.Objects;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Environment.EnvironmentType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bzane on 7/20/17
 */
public class DelegateScope extends Base {
  @NotEmpty private String accountId;

  // TODO: Task Types, Env Types, Applications, Environments, Service Infrastructures

  private List<String> environments = new ArrayList<>();
  private List<EnvironmentType> environmentTypes = new ArrayList<>();

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public List<String> getEnvironments() {
    return environments;
  }

  public void setEnvironments(List<String> environments) {
    this.environments = environments;
  }

  public List<EnvironmentType> getEnvironmentTypes() {
    return environmentTypes;
  }

  public void setEnvironmentTypes(List<EnvironmentType> environmentTypes) {
    this.environmentTypes = environmentTypes;
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
    return Objects.equal(accountId, that.accountId) && Objects.equal(environments, that.environments)
        && Objects.equal(environmentTypes, that.environmentTypes);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), accountId, environments, environmentTypes);
  }

  @Override
  public String toString() {
    return "DelegateScope{"
        + "accountId='" + accountId + '\'' + ", environments=" + environments + ", environmentTypes=" + environmentTypes
        + '}';
  }

  public static final class DelegateScopeBuilder {
    private String accountId;
    private List<String> environments = new ArrayList<>();
    private List<EnvironmentType> environmentTypes = new ArrayList<>();
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

    public DelegateScopeBuilder withEnvironments(List<String> environments) {
      this.environments = environments;
      return this;
    }

    public DelegateScopeBuilder withEnvironmentTypes(List<EnvironmentType> environmentTypes) {
      this.environmentTypes = environmentTypes;
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
          .withEnvironments(environments)
          .withEnvironmentTypes(environmentTypes)
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
      delegateScope.setEnvironments(environments);
      delegateScope.setEnvironmentTypes(environmentTypes);
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
