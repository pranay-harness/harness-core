package software.wings.security;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Created by rishi on 3/24/17.
 */
public class UserRequestInfo {
  private String accountId;
  private List<String> appIds;
  private String envId;

  private boolean allAppsAllowed;
  private boolean allEnvironmentsAllowed;

  private ImmutableList<String> allowedAppIds;
  private ImmutableList<String> allowedEnvIds;

  private boolean appIdFilterRequired;
  private boolean envIdFilterRequired;

  private ImmutableList<PermissionAttribute> permissionAttributes;

  public String getAccountId() {
    return accountId;
  }

  void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public List<String> getAppIds() {
    return appIds;
  }

  void setAppIds(List<String> appIds) {
    this.appIds = appIds;
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

  public boolean isAppIdFilterRequired() {
    return appIdFilterRequired;
  }

  public void setAppIdFilterRequired(boolean appIdFilterRequired) {
    this.appIdFilterRequired = appIdFilterRequired;
  }

  public boolean isEnvIdFilterRequired() {
    return envIdFilterRequired;
  }

  public void setEnvIdFilterRequired(boolean envIdFilterRequired) {
    this.envIdFilterRequired = envIdFilterRequired;
  }

  public static final class UserRequestInfoBuilder {
    private String accountId;
    private List<String> appIds;
    private String envId;
    private boolean allAppsAllowed;
    private boolean allEnvironmentsAllowed;
    private ImmutableList<String> allowedAppIds;
    private ImmutableList<String> allowedEnvIds;
    private ImmutableList<PermissionAttribute> permissionAttributes;
    private boolean appIdFilterRequired;

    private UserRequestInfoBuilder() {}

    public static UserRequestInfoBuilder anUserRequestInfo() {
      return new UserRequestInfoBuilder();
    }

    public UserRequestInfoBuilder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public UserRequestInfoBuilder withAppIds(List<String> appIds) {
      this.appIds = appIds;
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

    public UserRequestInfoBuilder withAppIdFilterRequired(boolean appIdFilterRequired) {
      this.appIdFilterRequired = appIdFilterRequired;
      return this;
    }

    public UserRequestInfo build() {
      UserRequestInfo userRequestInfo = new UserRequestInfo();
      userRequestInfo.setAccountId(accountId);
      userRequestInfo.setAppIds(appIds);
      userRequestInfo.setEnvId(envId);
      userRequestInfo.setAllAppsAllowed(allAppsAllowed);
      userRequestInfo.setAllEnvironmentsAllowed(allEnvironmentsAllowed);
      userRequestInfo.setAllowedAppIds(allowedAppIds);
      userRequestInfo.setAllowedEnvIds(allowedEnvIds);
      userRequestInfo.setPermissionAttributes(permissionAttributes);
      userRequestInfo.setAppIdFilterRequired(appIdFilterRequired);
      return userRequestInfo;
    }
  }
}
