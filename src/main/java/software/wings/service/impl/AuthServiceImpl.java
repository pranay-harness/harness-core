package software.wings.service.impl;

import static software.wings.beans.ErrorCodes.ACCESS_DENIED;
import static software.wings.beans.ErrorCodes.EXPIRED_TOKEN;
import static software.wings.beans.ErrorCodes.INVALID_TOKEN;
import static software.wings.dl.PageRequest.PageRequestType.LIST_WITHOUT_APP_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.Application;
import software.wings.beans.AuthToken;
import software.wings.beans.Base;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.Permission;
import software.wings.beans.Permission.PermissionType;
import software.wings.beans.Role;
import software.wings.beans.User;
import software.wings.dl.GenericDbCache;
import software.wings.dl.PageRequest.PageRequestType;
import software.wings.exception.WingsException;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.ResourceType;
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
  public void authorize(String appId, String envId, User user, List<PermissionAttribute> permissionAttributes,
      PageRequestType requestType) {
    Application application = dbCache.get(Application.class, appId);
    Environment environment = dbCache.get(Environment.class, envId);

    for (PermissionAttribute permissionAttribute : permissionAttributes) {
      if (!authorizeAccessType(application, environment, permissionAttribute, user.getRoles(), requestType)) {
        throw new WingsException(ACCESS_DENIED);
      }
    }
  }

  private boolean authorizeAccessType(Application application, Environment environment,
      PermissionAttribute permissionAttribute, List<Role> roles, PageRequestType requestType) {
    return roles.stream()
        .filter(role -> roleAuthorizedWithAccessType(role, permissionAttribute, application, environment, requestType))
        .findFirst()
        .isPresent();
  }

  private boolean roleAuthorizedWithAccessType(Role role, PermissionAttribute permissionAttribute,
      Application application, Environment environment, PageRequestType requestType) {
    ResourceType reqResourceType = permissionAttribute.getResourceType();
    Action reqAction = permissionAttribute.getAction();
    boolean requiresAppPermission = permissionAttribute.isOnApp();
    boolean requiresEnvironmentPermission = permissionAttribute.isOnEnv();
    for (Permission permission : role.getPermissions()) {
      if (hasMatchingPermissionType(
              requiresAppPermission, requiresEnvironmentPermission, permission.getPermissionType())
          && hasResourceAccess(reqResourceType, permission) && canPerformAction(reqAction, permission)
          && allowedInEnv(environment, requiresEnvironmentPermission, permission)
          && forApplication(application, requiresAppPermission, permission, requestType)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasMatchingPermissionType(
      boolean requiresAppPermission, boolean requiresEnvironmentPermission, PermissionType permissionType) {
    return permissionType.equals(PermissionType.ENV) ? requiresEnvironmentPermission : requiresAppPermission;
  }

  private boolean forApplication(
      Application application, boolean reqApp, Permission permission, PageRequestType requestType) {
    return requestType.equals(LIST_WITHOUT_APP_ID) || !reqApp
        || (Base.GLOBAL_APP_ID.equals(permission.getAppId()) || application.getUuid().equals(permission.getAppId()));
  }

  private boolean allowedInEnv(Environment environment, boolean reqEnv, Permission permission) {
    return !reqEnv || EnvironmentType.ALL.equals(permission.getEnvironmentType())
        || (environment.getEnvironmentType().equals(permission.getEnvironmentType()));
  }

  private boolean canPerformAction(Action reqAction, Permission permission) {
    return Action.ALL.equals(permission.getAction()) || (reqAction.equals(permission.getAction()));
  }

  private boolean hasResourceAccess(ResourceType reqResource, Permission permission) {
    return ResourceType.ANY.equals(permission.getResourceType()) || (reqResource.equals(permission.getResourceType()));
  }
}
