package software.wings.beans;

import software.wings.beans.Environment.EnvironmentType;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.ResourceType;

import java.util.Map;

/**
 * Created by rishi on 3/23/17.
 */
public class EnvironmentRole {
  private String envId;
  private String envName;
  private EnvironmentType environmentType;
  private Map<ResourceType, Action> resourceAccess;

  public String getEnvId() {
    return envId;
  }

  public void setEnvId(String envId) {
    this.envId = envId;
  }

  public String getEnvName() {
    return envName;
  }

  public void setEnvName(String envName) {
    this.envName = envName;
  }

  public EnvironmentType getEnvironmentType() {
    return environmentType;
  }

  public void setEnvironmentType(EnvironmentType environmentType) {
    this.environmentType = environmentType;
  }

  public Map<ResourceType, Action> getResourceAccess() {
    return resourceAccess;
  }

  public void setResourceAccess(Map<ResourceType, Action> resourceAccess) {
    this.resourceAccess = resourceAccess;
  }

  public static final class EnvironmentRoleBuilder {
    private String envId;
    private String envName;
    private EnvironmentType environmentType;
    private Map<ResourceType, Action> resourceAccess;

    private EnvironmentRoleBuilder() {}

    public static EnvironmentRoleBuilder anEnvironmentRole() {
      return new EnvironmentRoleBuilder();
    }

    public EnvironmentRoleBuilder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public EnvironmentRoleBuilder withEnvName(String envName) {
      this.envName = envName;
      return this;
    }

    public EnvironmentRoleBuilder withEnvironmentType(EnvironmentType environmentType) {
      this.environmentType = environmentType;
      return this;
    }

    public EnvironmentRoleBuilder withResourceAccess(Map<ResourceType, Action> resourceAccess) {
      this.resourceAccess = resourceAccess;
      return this;
    }

    public EnvironmentRole build() {
      EnvironmentRole environmentRole = new EnvironmentRole();
      environmentRole.setEnvId(envId);
      environmentRole.setEnvName(envName);
      environmentRole.setEnvironmentType(environmentType);
      environmentRole.setResourceAccess(resourceAccess);
      return environmentRole;
    }
  }
}
