package software.wings.beans;

import static software.wings.beans.Permission.Builder.aPermission;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;

/**
 * Created by rishi on 3/13/17.
 */
public enum RoleType {
  ACCOUNT_ADMIN("Account Administrator", "Account Adminitrator members have all the access within account"),
  APPLICATION_ADMIN("Application Administrator",
      "Application Administrator members have access to setup application(service, environment/infrastructure, workflows) and do deployments within application",
      aPermission().withAction(Action.ALL).withPermissionScope(PermissionType.APP).build(),
      aPermission().withAction(Action.ALL).withPermissionScope(PermissionType.ENV).build()),
  PROD_SUPPORT("Production Support",
      "Production Support members have access to override configuration, setup infrastructure and setup/execute deployment workflows within PROD environments",
      aPermission()
          .withAction(Action.ALL)
          .withPermissionScope(PermissionType.ENV)
          .withEnvironmentType(EnvironmentType.PROD)
          .build(),
      aPermission().withAction(Action.READ).withPermissionScope(PermissionType.APP).build()),
  NON_PROD_SUPPORT("Non-production Support",
      "Non-production Support members have access to override configuration, setup infrastructure and setup/execute deployment workflows within NON_PROD environments",
      aPermission()
          .withAction(Action.ALL)
          .withPermissionScope(PermissionType.ENV)
          .withEnvironmentType(EnvironmentType.NON_PROD)
          .build(),
      aPermission().withAction(Action.READ).withPermissionScope(PermissionType.APP).build()),
  CUSTOM("Custom", "Custom Role");

  private final String displayName;
  private final String description;
  private Permission[] permissions;

  RoleType(String displayName, String description, Permission... permissions) {
    this.displayName = displayName;
    this.description = description;
    this.permissions = permissions;
  }

  public String getDisplayName() {
    return displayName;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public Permission[] getPermissions() {
    return permissions;
  }

  @SuppressFBWarnings({"ME_ENUM_FIELD_SETTER", "EI_EXPOSE_REP2"})
  public void setPermissions(Permission[] permissions) {
    this.permissions = permissions;
  }

  public String getDescription() {
    return description;
  }

  public String getRoleName(String appName) {
    if (this == ACCOUNT_ADMIN) {
      return displayName;
    } else {
      return appName + "::" + displayName;
    }
  }
}
