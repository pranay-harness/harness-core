package software.wings.security;

import com.google.common.collect.Lists;
import org.mongodb.morphia.Datastore;
import software.wings.app.WingsBootstrap;
import software.wings.beans.AuthToken;
import software.wings.beans.Permission;
import software.wings.beans.Role;
import software.wings.beans.User;
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
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;
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
    List<AccessType> accessTypes = getAccessTypes();
    authorizeRequest(requestContext.getUriInfo(), user, accessTypes);
  }

  private void authorizeRequest(UriInfo uriInfo, User user, List<AccessType> accessTypes) {
    MultivaluedMap<String, String> pathParameters = uriInfo.getPathParameters();
    for (AccessType accessType : accessTypes) {
      if (!authorizeAccessType(pathParameters, accessType, user.getRoles())) {
        throw new WingsException(String.valueOf(UNAUTHORIZED), "User not authorized to access",
            new Throwable("User not authorized to access"));
      }
    }
  }

  private boolean authorizeAccessType(
      MultivaluedMap<String, String> pathParameters, AccessType accessType, List<Role> roles) {
    Application application = null;
    if (pathParameters.containsKey("app_id")) {
      String appID = pathParameters.getFirst("app_id");
      application = datastore.get(Application.class, appID);
    }
    for (Role role : roles) {
      if (roleAuthorizedWithAccessType(role, accessType, application)) {
        return true;
      }
    }
    return false;
  }

  private boolean roleAuthorizedWithAccessType(Role role, AccessType accessType, Application application) {
    String[] accessTypeParts = accessType.name().split("_");
    String reqAction = accessTypeParts[0];
    String reqType = accessTypeParts[1];
    for (Permission permission : role.getPermissions()) {
      if ("ALL".equals(permission.getAction()) || (reqAction.equals(permission.getAction()))) {
        if ("ALL".equals(permission.getAccessType()) || (reqType.equals(permission.getAccessType()))) {
          return true;
        }
      }
    }
    return false;
  }

  private List<AccessType> getAccessTypes() {
    List<AccessType> accessTypes = new ArrayList<>();
    List<AccessType> methodAccessTypes = new ArrayList<>();
    List<AccessType> classAccessTypes = new ArrayList<>();

    Method resourceMethod = resourceInfo.getResourceMethod();
    AuthRule methodAnnotations = resourceMethod.getAnnotation(AuthRule.class);
    if (null != methodAnnotations && methodAnnotations.permissions().length > 0) {
      methodAccessTypes.addAll(Lists.newArrayList(methodAnnotations.permissions()));
    }

    Class<?> resourceClass = resourceInfo.getResourceClass();
    AuthRule classAnnotations = resourceClass.getAnnotation(AuthRule.class);
    if (null != classAnnotations && classAnnotations.permissions().length > 0) {
      classAccessTypes.addAll(Lists.newArrayList(classAnnotations.permissions()));
    }
    accessTypes.addAll(methodAccessTypes);
    accessTypes.removeAll(classAccessTypes);
    accessTypes.addAll(classAccessTypes);
    return accessTypes;
  }

  private void updateUserInAuditRecord(User user) {
    auditService.updateUser(AuditHelper.getInstance().get(), User.getPublicUser(user));
  }

  private User findUserFromDB(String userID) {
    return datastore.find(User.class, "_id", userID).get();
  }

  private void validateToken(String token) {}

  private User extractUserFromToken(String token) {
    // Validate token
    // Extend token expiry
    // fetch and return user
    return null;
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
