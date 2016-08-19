package software.wings.service.impl;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.ErrorCodes.ACCESS_DENIED;
import static software.wings.beans.ErrorCodes.EXPIRED_TOKEN;
import static software.wings.beans.ErrorCodes.INVALID_TOKEN;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.Application;
import software.wings.beans.AuthToken;
import software.wings.beans.Environment;
import software.wings.beans.Permission;
import software.wings.beans.Role;
import software.wings.beans.User;
import software.wings.dl.GenericDbCache;
import software.wings.exception.WingsException;
import software.wings.security.PermissionAttr;
import software.wings.service.intfc.AuthService;

import java.util.List;

/**
 * Created by peeyushaggarwal on 8/18/16.
 */
@Singleton
public class AuthServiceImpl implements AuthService {
  @Inject private GenericDbCache dbCache;

  @Override
  public AuthToken validateToken(String tokenString) {
    AuthToken authToken = dbCache.get(AuthToken.class, tokenString);
    if (authToken == null) {
      throw new WingsException(INVALID_TOKEN);
    } else if (authToken.getExpireAt() < System.currentTimeMillis()) {
      throw new WingsException(EXPIRED_TOKEN);
    }
    return authToken;
  }

  @Override
  public void authorize(String appId, String envId, User user, List<PermissionAttr> permissionAttrs) {
    for (PermissionAttr permissionAttr : permissionAttrs) {
      if (!authorizeAccessType(appId, envId, permissionAttr, user.getRoles())) {
        throw new WingsException(ACCESS_DENIED);
      }
    }
  }

  private boolean authorizeAccessType(String appId, String envId, PermissionAttr permissionAttr, List<Role> roles) {
    Application application = null;
    Environment environment = null;

    if (isNotBlank(appId)) {
      application = dbCache.get(Application.class, appId);
    }

    if (isNotBlank(envId)) {
      environment = dbCache.get(Environment.class, envId);
    }

    for (Role role : roles) {
      if (roleAuthorizedWithAccessType(role, permissionAttr, application, environment)) {
        return true;
      }
    }
    return false;
  }

  private boolean roleAuthorizedWithAccessType(
      Role role, PermissionAttr permissionAttr, Application application, Environment environment) {
    String reqResource = permissionAttr.getResource().toString();
    String reqAction = permissionAttr.getAction().toString();
    boolean reqApp = permissionAttr.isOnApp();
    boolean reqEnv = permissionAttr.isOnEnv();
    for (Permission permission : role.getPermissions()) {
      if (hasResourceAccess(reqResource, permission) && canPerformAction(reqAction, permission)
          && allowedInEnv(environment, reqEnv, permission) && forApplication(application, reqApp, permission)) {
        return true;
      }
    }
    return false;
  }

  private boolean forApplication(Application application, boolean reqApp, Permission permission) {
    return reqApp && ("ALL".equals(permission.getServiceId())); // TODO: revisit
  }

  private boolean allowedInEnv(Environment environment, boolean reqEnv, Permission permission) {
    return reqEnv && "ALL".equals(permission.getEnvId()) || (environment.getUuid().equals(permission.getEnvId()));
  }

  private boolean canPerformAction(String reqAction, Permission permission) {
    return "ALL".equals(permission.getAction()) || (reqAction.equals(permission.getAction()));
  }

  private boolean hasResourceAccess(String reqResource, Permission permission) {
    return "ALL".equals(permission.getResource()) || (reqResource.equals(permission.getResource()));
  }
}
