/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static software.wings.beans.Permission.Builder.aPermission;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EnvironmentType;

import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;

@OwnedBy(HarnessTeam.PL)
@TargetModule(HarnessModule._970_RBAC_CORE)
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

  public Permission[] getPermissions() {
    return permissions == null ? null : permissions.clone();
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
