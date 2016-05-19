package software.wings.security;

import static javax.ws.rs.Priorities.AUTHENTICATION;
import static software.wings.beans.ErrorConstants.ACCESS_DENIED;
import static software.wings.beans.ErrorConstants.EXPIRED_TOKEN;
import static software.wings.beans.ErrorConstants.INVALID_TOKEN;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import software.wings.beans.Application;
import software.wings.beans.AuthToken;
import software.wings.beans.Environment;
import software.wings.beans.Permission;
import software.wings.beans.Role;
import software.wings.beans.User;
import software.wings.common.AuditHelper;
import software.wings.dl.GenericDbCache;
import software.wings.exception.WingsException;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AuditService;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Priority;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

/**
 * Created by anubhaw on 3/11/16.
 */
@Singleton
@Priority(AUTHENTICATION)
@AuthRule
public class AuthRuleFilter implements ContainerRequestFilter {
  @Context private ResourceInfo resourceInfo;

  @Inject private AuditService auditService;

  @Inject private GenericDbCache dbCache;

  @Inject private AuditHelper auditHelper;

  @Override
  public void filter(ContainerRequestContext requestContext) {
    AuthToken authToken = extractToken(requestContext);
    User user = authToken.getUser();
    if (user != null) {
      requestContext.setProperty("USER", user);
      updateUserInAuditRecord(user); // FIXME: find better place
      UserThreadLocal.set(user);
    }
    List<PermissionAttr> permissionAttrs = getAccessTypes();
    authorizeRequest(requestContext.getUriInfo(), user, permissionAttrs);
  }

  private void authorizeRequest(UriInfo uriInfo, User user, List<PermissionAttr> permissionAttrs) {
    MultivaluedMap<String, String> pathParameters = uriInfo.getPathParameters();
    for (PermissionAttr permissionAttr : permissionAttrs) {
      if (!authorizeAccessType(pathParameters, permissionAttr, user.getRoles())) {
        throw new WingsException(ACCESS_DENIED);
      }
    }
  }

  private boolean authorizeAccessType(
      MultivaluedMap<String, String> pathParameters, PermissionAttr permissionAttr, List<Role> roles) {
    Application application = null;
    Environment environment = null;
    if (pathParameters.containsKey("appId")) {
      String appID = pathParameters.getFirst("appId");
      application = dbCache.get(Application.class, appID);
    }
    if (pathParameters.containsKey("env_id")) {
      String env = pathParameters.getFirst("env_id");
      environment = dbCache.get(Environment.class, env);
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

  private List<PermissionAttr> getAccessTypes() {
    List<PermissionAttr> permissionAttrs = new ArrayList<>();
    List<PermissionAttr> methodPermissionAttrs = new ArrayList<>();
    List<PermissionAttr> classPermissionAttrs = new ArrayList<>();

    Method resourceMethod = resourceInfo.getResourceMethod();
    AuthRule methodAnnotations = resourceMethod.getAnnotation(AuthRule.class);
    if (null != methodAnnotations && methodAnnotations.value().length > 0) {
      methodPermissionAttrs.addAll(Lists.newArrayList(methodAnnotations.value()));
    }

    Class<?> resourceClass = resourceInfo.getResourceClass();
    AuthRule classAnnotations = resourceClass.getAnnotation(AuthRule.class);
    if (null != classAnnotations && classAnnotations.value().length > 0) {
      classPermissionAttrs.addAll(Lists.newArrayList(classAnnotations.value()));
    }
    permissionAttrs.addAll(methodPermissionAttrs);
    permissionAttrs.removeAll(classPermissionAttrs);
    permissionAttrs.addAll(classPermissionAttrs);
    return permissionAttrs;
  }

  private void updateUserInAuditRecord(User user) {
    auditService.updateUser(auditHelper.get(), User.getPublicUser(user));
  }

  private AuthToken extractToken(ContainerRequestContext requestContext) {
    String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      throw new WingsException(INVALID_TOKEN);
    }
    String tokenString = authorizationHeader.substring("Bearer".length()).trim();
    AuthToken authToken = dbCache.get(AuthToken.class, tokenString);
    if (authToken == null) {
      throw new WingsException(INVALID_TOKEN);
    } else if (authToken.getExpireAt() < System.currentTimeMillis()) {
      throw new WingsException(EXPIRED_TOKEN);
    }
    return authToken;
  }
}
