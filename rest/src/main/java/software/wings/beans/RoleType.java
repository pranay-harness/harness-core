package software.wings.beans;

import static software.wings.beans.Permission.Builder.aPermission;

import software.wings.beans.Environment.EnvironmentType;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionScope;

/**
 * Created by rishi on 3/13/17.
 */
public enum RoleType {
  ACCOUNT_ADMIN("Account Administrator"),
  APPLICATION_ADMIN("Application Administrator",
      aPermission().withAction(Action.ALL).withPermissionScope(PermissionScope.APP).build(),
      aPermission().withAction(Action.ALL).withPermissionScope(PermissionScope.ENV).build()),
  PROD_SUPPORT("Production Support",
      aPermission()
          .withAction(Action.ALL)
          .withPermissionScope(PermissionScope.ENV)
          .withEnvironmentType(EnvironmentType.PROD)
          .build(),
      aPermission().withAction(Action.READ).withPermissionScope(PermissionScope.APP).build()),
  NON_PROD_SUPPORT("Non-production Support",
      aPermission()
          .withAction(Action.ALL)
          .withPermissionScope(PermissionScope.ENV)
          .withEnvironmentType(EnvironmentType.NON_PROD)
          .build(),
      aPermission().withAction(Action.READ).withPermissionScope(PermissionScope.APP).build()),
  CUSTOM("Custom");

  private final String displayName;
  private Permission[] permissions;

  RoleType(String displayName) {
    this(displayName, null);
  }

  RoleType(String displayName, Permission... permissions) {
    this.displayName = displayName;
    this.permissions = permissions;
  }

  public String getDisplayName() {
    return displayName;
  }

  public Permission[] getPermissions() {
    return permissions;
  }

  public void setPermissions(Permission[] permissions) {
    this.permissions = permissions;
  }

  public String getRoleName(String appName) {
    if (this == ACCOUNT_ADMIN) {
      return displayName;
    } else {
      return appName + "::" + displayName;
    }
  }
}
