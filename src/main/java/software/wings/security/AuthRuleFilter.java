package software.wings.security;

import com.google.common.collect.Lists;
import org.mongodb.morphia.Datastore;
import software.wings.app.WingsBootstrap;
import software.wings.beans.*;
import software.wings.beans.Application;
import software.wings.common.AuditHelper;
import software.wings.exception.WingsException;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AuditService;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static javax.ws.rs.Priorities.AUTHENTICATION;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

/**
 * Created by anubhaw on 3/11/16.
 */

@Priority(AUTHENTICATION)
@AuthRule
public class AuthRuleFilter implements ContainerRequestFilter {
  @Context ResourceInfo resourceInfo;
  Datastore datastore = WingsBootstrap.lookup(Datastore.class);
  AuditService auditService = WingsBootstrap.lookup(AuditService.class);

  @Override
  public void filter(ContainerRequestContext requestContext) {
    AuthToken authToken = extractToken(requestContext);
    User user = authToken.getUser();
    if (null != user) {
      updateUserInAuditRecord(user); // Set for FIXME: find better place
    }
    requestContext.setProperty("USER", user);
    List<PermissionAttr> permissionAttrs = getAccessTypes();
    authorizeRequest(requestContext.getUriInfo(), user, permissionAttrs);
  }

  private void authorizeRequest(UriInfo uriInfo, User user, List<PermissionAttr> permissionAttrs) {
    MultivaluedMap<String, String> pathParameters = uriInfo.getPathParameters();
    for (PermissionAttr permissionAttr : permissionAttrs) {
      if (!authorizeAccessType(pathParameters, permissionAttr, user.getRoles())) {
        throw new WingsException(String.valueOf(UNAUTHORIZED), "User not authorized to access",
            new Throwable("User not authorized to access"));
      }
    }
  }

  private boolean authorizeAccessType(
      MultivaluedMap<String, String> pathParameters, PermissionAttr permissionAttr, List<Role> roles) {
    Application application = null;
    Environment environment = null;
    if (pathParameters.containsKey("app_id")) {
      String appID = pathParameters.getFirst("app_id");
      application = datastore.get(Application.class, appID);
    }
    if (pathParameters.containsKey("env")) {
      String env = pathParameters.getFirst("env");
      environment = datastore.get(Environment.class, env);
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
    boolean reqApp = permissionAttr.isOnEnv();
    boolean reqEnv = permissionAttr.isOnApp();
    for (Permission permission : role.getPermissions()) {
      if (hasResourceAccess(reqResource, permission) && canPerformAction(reqAction, permission)
          && allowedInEnv(environment, reqEnv, permission) && forApplication(application, reqApp, permission)) {
        return true;
      }
    }
    return false;
  }

  private boolean forApplication(Application application, boolean reqApp, Permission permission) {
    return reqApp && ("ALL".equals(permission.getServiceID()) || (application.equals(permission.getServiceID())));
  }

  private boolean allowedInEnv(Environment environment, boolean reqEnv, Permission permission) {
    return reqEnv && "ALL".equals(permission.getEnvID()) || (environment.getName().equals(permission.getEnvID()));
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
    auditService.updateUser(AuditHelper.getInstance().get(), User.getPublicUser(user));
  }

  private AuthToken extractToken(ContainerRequestContext requestContext) {
    String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      throw new WingsException(String.valueOf(UNAUTHORIZED), "Authorization header must be provided",
          new Throwable("Authorization header must be provided"));
    }
    String tokenString = authorizationHeader.substring("Bearer".length()).trim();
    return fetchTokenFromDB(tokenString);
  }

  private AuthToken fetchTokenFromDB(String tokenString) {
    return datastore.find(AuthToken.class, "token", tokenString).get();
  }
}
