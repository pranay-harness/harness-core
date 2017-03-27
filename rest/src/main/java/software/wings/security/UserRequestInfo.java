package software.wings.security;

import com.google.common.collect.ImmutableList;

/**
 * Created by rishi on 3/24/17.
 */
public class UserRequestInfo {
  private String accountId;
  private String appId;
  private String envId;

  private boolean allAppsAllowed;
  private boolean allEnvironmentsAllowed;

  private ImmutableList<String> allowedAppIds;
  private ImmutableList<String> allowedEnvIds;

  private ImmutableList<PermissionAttribute> permissionAttributes;

  public String getAccountId() {
    return accountId;
  }

  void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getAppId() {
    return appId;
  }

  void setAppId(String appId) {
    this.appId = appId;
  }

  public String getEnvId() {
    return envId;
  }

  void setEnvId(String envId) {
    this.envId = envId;
  }

  public boolean isAllAppsAllowed() {
    return allAppsAllowed;
  }

  void setAllAppsAllowed(boolean allAppsAllowed) {
    this.allAppsAllowed = allAppsAllowed;
  }

  public boolean isAllEnvironmentsAllowed() {
    return allEnvironmentsAllowed;
  }

  void setAllEnvironmentsAllowed(boolean allEnvironmentsAllowed) {
    this.allEnvironmentsAllowed = allEnvironmentsAllowed;
  }

  public ImmutableList<String> getAllowedAppIds() {
    return allowedAppIds;
  }

  void setAllowedAppIds(ImmutableList<String> allowedAppIds) {
    this.allowedAppIds = allowedAppIds;
  }

  public ImmutableList<String> getAllowedEnvIds() {
    return allowedEnvIds;
  }

  void setAllowedEnvIds(ImmutableList<String> allowedEnvIds) {
    this.allowedEnvIds = allowedEnvIds;
  }

  public ImmutableList<PermissionAttribute> getPermissionAttributes() {
    return permissionAttributes;
  }

  void setPermissionAttributes(ImmutableList<PermissionAttribute> permissionAttributes) {
    this.permissionAttributes = permissionAttributes;
  }

  public static final class UserRequestInfoBuilder {
    private String accountId;
    private String appId;
    private String envId;
    private boolean allAppsAllowed;
    private boolean allEnvironmentsAllowed;
    private ImmutableList<String> allowedAppIds;
    private ImmutableList<String> allowedEnvIds;
    private ImmutableList<PermissionAttribute> permissionAttributes;

    private UserRequestInfoBuilder() {}

    public static UserRequestInfoBuilder anUserRequestInfo() {
      return new UserRequestInfoBuilder();
    }

    public UserRequestInfoBuilder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public UserRequestInfoBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public UserRequestInfoBuilder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public UserRequestInfoBuilder withAllAppsAllowed(boolean allAppsAllowed) {
      this.allAppsAllowed = allAppsAllowed;
      return this;
    }

    public UserRequestInfoBuilder withAllEnvironmentsAllowed(boolean allEnvironmentsAllowed) {
      this.allEnvironmentsAllowed = allEnvironmentsAllowed;
      return this;
    }

    public UserRequestInfoBuilder withAllowedAppIds(ImmutableList<String> allowedAppIds) {
      this.allowedAppIds = allowedAppIds;
      return this;
    }

    public UserRequestInfoBuilder withAllowedEnvIds(ImmutableList<String> allowedEnvIds) {
      this.allowedEnvIds = allowedEnvIds;
      return this;
    }

    public UserRequestInfoBuilder withPermissionAttributes(ImmutableList<PermissionAttribute> permissionAttributes) {
      this.permissionAttributes = permissionAttributes;
      return this;
    }

    public UserRequestInfo build() {
      UserRequestInfo userRequestInfo = new UserRequestInfo();
      userRequestInfo.setAccountId(accountId);
      userRequestInfo.setAppId(appId);
      userRequestInfo.setEnvId(envId);
      userRequestInfo.setAllAppsAllowed(allAppsAllowed);
      userRequestInfo.setAllEnvironmentsAllowed(allEnvironmentsAllowed);
      userRequestInfo.setAllowedAppIds(allowedAppIds);
      userRequestInfo.setAllowedEnvIds(allowedEnvIds);
      userRequestInfo.setPermissionAttributes(permissionAttributes);
      return userRequestInfo;
    }
  }
}
