/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.security.auth;

import static software.wings.security.PermissionAttribute.Action.UPDATE;
import static software.wings.security.PermissionAttribute.PermissionType.ENV;
import static software.wings.security.PermissionAttribute.PermissionType.SERVICE;

import software.wings.beans.ConfigFile;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.security.PermissionAttribute;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.ServiceTemplateService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by ujjawal
 */
@Singleton
@Slf4j
public class ConfigFileAuthHandler {
  @Inject private AuthService authService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceTemplateService serviceTemplateService;

  private void authorize(List<PermissionAttribute> requiredPermissionAttributes, List<String> appIds, String entityId) {
    User user = UserThreadLocal.get();
    if (user != null && user.getUserRequestContext() != null) {
      UserRequestContext userRequestContext = user.getUserRequestContext();
      if (userRequestContext != null) {
        authService.authorize(userRequestContext.getAccountId(), appIds, entityId, user, requiredPermissionAttributes);
      }
    }
  }

  public void authorize(ConfigFile configFile) {
    if (configFile.getEntityType() == null) {
      return;
    }
    switch (configFile.getEntityType()) {
      case SERVICE_TEMPLATE: {
        authorizeServiceTemplateTypeConfigFile(configFile);
        break;
      }
      case SERVICE: {
        authorizeServiceTypeConfigFile(configFile);
        break;
      }
      case ENVIRONMENT: {
        authorizeEnvironmentTypeConfigFile(configFile);
        break;
      }
      default: {
        break;
      }
    }
  }

  private void authorizeServiceTemplateTypeConfigFile(ConfigFile configFile) {
    if (StringUtils.isNotEmpty(configFile.getAppId()) && StringUtils.isNotEmpty(configFile.getEntityId())) {
      ServiceTemplate serviceTemplate = serviceTemplateService.get(configFile.getAppId(), configFile.getEntityId());

      List<PermissionAttribute> envPermissionAttributeList =
          Collections.singletonList(new PermissionAttribute(ENV, UPDATE));
      authorize(
          envPermissionAttributeList, Collections.singletonList(configFile.getAppId()), serviceTemplate.getEnvId());
    }
  }

  private void authorizeEnvironmentTypeConfigFile(ConfigFile configFile) {
    if (StringUtils.isNotEmpty(configFile.getAppId()) && StringUtils.isNotEmpty(configFile.getEntityId())) {
      List<PermissionAttribute> permissionAttributeList =
          Collections.singletonList(new PermissionAttribute(ENV, UPDATE));
      authorize(permissionAttributeList, Collections.singletonList(configFile.getAppId()), configFile.getEntityId());
    }
  }

  private void authorizeServiceTypeConfigFile(ConfigFile configFile) {
    if (StringUtils.isNotEmpty(configFile.getAppId()) && StringUtils.isNotEmpty(configFile.getEntityId())) {
      List<PermissionAttribute> permissionAttributeList =
          Collections.singletonList(new PermissionAttribute(SERVICE, UPDATE));
      authorize(permissionAttributeList, Collections.singletonList(configFile.getAppId()), configFile.getEntityId());
    }
  }

  public void authorize(String appId, String configId) {
    ConfigFile configFile = wingsPersistence.getWithAppId(ConfigFile.class, appId, configId);

    if (configFile == null) {
      return;
    }
    authorize(configFile);
  }
}
