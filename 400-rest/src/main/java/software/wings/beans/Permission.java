package software.wings.beans;

import software.wings.beans.Environment.EnvironmentType;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;

/**
 * Created by anubhaw on 3/17/16.
 */

public class Permission {
  private ResourceType resourceType;
  private Action action;
  private String envId;
  private String appId;
  private String accountId;
  private EnvironmentType environmentType;
  private PermissionType permissionScope;

  /**
   * Gets resource type.
   *
   * @return the resource type
   */
  public ResourceType getResourceType() {
    return resourceType;
  }

  /**
   * Sets resource type.
   *
   * @param resourceType the resource type
   */
  public void setResourceType(ResourceType resourceType) {
    this.resourceType = resourceType;
  }

  /**
   * Gets action.
   *
   * @return the action
   */
  public Action getAction() {
    return action;
  }

  /**
   * Sets action.
   *
   * @param action the action
   */
  public void setAction(Action action) {
    this.action = action;
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
   * Gets app id.
   *
   * @return the app id
   */
  public String getAppId() {
    return appId;
  }

  /**
   * Sets app id.
   *
   * @param appId the app id
   */
  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  /**
   * Gets environment type.
   *
   * @return the environment type
   */
  public EnvironmentType getEnvironmentType() {
    return environmentType;
  }

  /**
   * Sets environment type.
   *
   * @param environmentType the environment type
   */
  public void setEnvironmentType(EnvironmentType environmentType) {
    this.environmentType = environmentType;
  }

  /**
   * Gets permission type.
   *
   * @return the permission type
   */
  public PermissionType getPermissionScope() {
    return permissionScope;
  }

  /**
   * Sets permission type.
   *
   * @param permissionScope the permission type
   */
  public void setPermissionScope(PermissionType permissionScope) {
    this.permissionScope = permissionScope;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private ResourceType resourceType;
    private Action action;
    private String envId;
    private String appId;
    private EnvironmentType environmentType;
    private PermissionType permissionScope;

    private Builder() {}

    /**
     * A permission builder.
     *
     * @return the builder
     */
    public static Builder aPermission() {
      return new Builder();
    }

    /**
     * With resource type builder.
     *
     * @param resourceType the resource type
     * @return the builder
     */
    public Builder withResourceType(ResourceType resourceType) {
      this.resourceType = resourceType;
      return this;
    }

    /**
     * With action builder.
     *
     * @param action the action
     * @return the builder
     */
    public Builder withAction(Action action) {
      this.action = action;
      return this;
    }

    /**
     * With env id builder.
     *
     * @param envId the env id
     * @return the builder
     */
    public Builder withEnvId(String envId) {
      this.envId = envId;
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
     * With environment type builder.
     *
     * @param environmentType the environment type
     * @return the builder
     */
    public Builder withEnvironmentType(EnvironmentType environmentType) {
      this.environmentType = environmentType;
      return this;
    }

    /**
     * With permission scope builder.
     *
     * @param permissionScope the permission scope
     * @return the builder
     */
    public Builder withPermissionScope(PermissionType permissionScope) {
      this.permissionScope = permissionScope;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aPermission()
          .withResourceType(resourceType)
          .withAction(action)
          .withEnvId(envId)
          .withAppId(appId)
          .withEnvironmentType(environmentType)
          .withPermissionScope(permissionScope);
    }

    /**
     * Build permission.
     *
     * @return the permission
     */
    public Permission build() {
      Permission permission = new Permission();
      permission.setResourceType(resourceType);
      permission.setAction(action);
      permission.setEnvId(envId);
      permission.setAppId(appId);
      permission.setEnvironmentType(environmentType);
      permission.setPermissionScope(permissionScope);
      return permission;
    }
  }
}
