/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.security.auth;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static software.wings.beans.CGConstants.GLOBAL_APP_ID;
import static software.wings.beans.SettingAttribute.SettingCategory;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_ACCOUNT_DEFAULTS;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_APPLICATIONS;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_CLOUD_PROVIDERS;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_CONNECTORS;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_SSH_AND_WINRM;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.SettingAttribute;
import software.wings.beans.User;
import software.wings.security.PermissionAttribute;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingVariableTypes;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@OwnedBy(PL)
@TargetModule(HarnessModule.UNDEFINED)
public class SettingAuthHandler {
  @Inject private AuthHandler authHandler;
  @Inject private SettingsService settingsService;

  private void authorize(List<PermissionAttribute> requiredPermissionAttributes) {
    User user = UserThreadLocal.get();
    if (user != null) {
      UserRequestContext userRequestContext = user.getUserRequestContext();
      if (userRequestContext != null) {
        authHandler.authorizeAccountPermission(userRequestContext, requiredPermissionAttributes);
      }
    }
  }

  public void authorize(SettingAttribute settingAttribute, String appId) {
    if (settingAttribute == null || settingAttribute.getValue() == null
        || settingAttribute.getValue().getType() == null) {
      return;
    }

    switch (SettingCategory.getCategory(SettingVariableTypes.valueOf(settingAttribute.getValue().getType()))) {
      case AZURE_ARTIFACTS:
      case HELM_REPO:
      case CONNECTOR: {
        authorizeConnector();
        break;
      }
      case SETTING: {
        if (!SettingVariableTypes.STRING.equals(SettingVariableTypes.valueOf(settingAttribute.getValue().getType()))) {
          authorizeSshAndWinRM();
        } else {
          if (GLOBAL_APP_ID.equals(appId)) {
            authorizeAccountDefaults();
          } else {
            authorizeApplicationDefaults();
          }
        }
        break;
      }
      case CLOUD_PROVIDER: {
        authorizeCloudProvider();
        break;
      }
      default: {
        break;
      }
    }
  }

  private void authorizeSshAndWinRM() {
    List<PermissionAttribute> permissionAttributeList = new ArrayList<>();
    permissionAttributeList.add(new PermissionAttribute(MANAGE_SSH_AND_WINRM));
    authorize(permissionAttributeList);
  }

  private void authorizeAccountDefaults() {
    List<PermissionAttribute> permissionAttributeList = new ArrayList<>();
    permissionAttributeList.add(new PermissionAttribute(MANAGE_ACCOUNT_DEFAULTS));
    authorize(permissionAttributeList);
  }

  private void authorizeApplicationDefaults() {
    List<PermissionAttribute> permissionAttributeList = new ArrayList<>();
    permissionAttributeList.add(new PermissionAttribute(MANAGE_APPLICATIONS));
    authorize(permissionAttributeList);
  }

  private void authorizeConnector() {
    List<PermissionAttribute> permissionAttributeList = new ArrayList<>();
    permissionAttributeList.add(new PermissionAttribute(MANAGE_CONNECTORS));
    authorize(permissionAttributeList);
  }

  private void authorizeCloudProvider() {
    List<PermissionAttribute> permissionAttributeList = new ArrayList<>();
    permissionAttributeList.add(new PermissionAttribute(MANAGE_CLOUD_PROVIDERS));
    authorize(permissionAttributeList);
  }

  public void authorize(String appId, String settingAttributeId) {
    SettingAttribute settingAttribute = settingsService.get(appId, settingAttributeId);
    authorize(settingAttribute, appId);
  }
}
