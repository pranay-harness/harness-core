package software.wings.beans;

import software.wings.beans.Environment.EnvironmentType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bzane on 7/20/17.
 */
public class DelegateScope {
  // TODO: Task Types, Env Types, Applications, Environments, Service Infrastructures

  private List<String> environments = new ArrayList<>();
  private List<EnvironmentType> environmentTypes = new ArrayList<>();

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

  public static final class DelegateScopeBuilder {
    private List<String> environments = new ArrayList<>();
    private List<EnvironmentType> environmentTypes = new ArrayList<>();

    private DelegateScopeBuilder() {}

    public static DelegateScopeBuilder aDelegateScope() {
      return new DelegateScopeBuilder();
    }

    public DelegateScopeBuilder withEnvironments(List<String> environments) {
      this.environments = environments;
      return this;
    }

    public DelegateScopeBuilder withEnvironmentTypes(List<EnvironmentType> environmentTypes) {
      this.environmentTypes = environmentTypes;
      return this;
    }

    public DelegateScopeBuilder but() {
      return aDelegateScope().withEnvironments(environments).withEnvironmentTypes(environmentTypes);
    }

    public DelegateScope build() {
      DelegateScope delegateScope = new DelegateScope();
      delegateScope.setEnvironments(environments);
      delegateScope.setEnvironmentTypes(environmentTypes);
      return delegateScope;
    }
  }
}
