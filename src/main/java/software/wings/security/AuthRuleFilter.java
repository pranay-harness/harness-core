package software.wings.security;

import org.mongodb.morphia.Datastore;
import software.wings.app.WingsBootstrap;
import software.wings.beans.AuthToken;
import software.wings.beans.User;
import software.wings.common.AuditHelper;
import software.wings.exception.WingsException;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AuditService;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import java.lang.reflect.Method;

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
      updateUserInAuditRecord(user);
    }
    requestContext.setProperty("USER", user);

    Method resourceMethod = resourceInfo.getResourceMethod();
    AuthRule methodAnnotations = resourceMethod.getAnnotation(AuthRule.class);
    if (null != methodAnnotations) {
      AccessType[] methodAnnotion = methodAnnotations.permissions();
    }

    Class<?> resourceClass = resourceInfo.getResourceClass();
    AuthRule classAnnotations = resourceClass.getAnnotation(AuthRule.class);
    if (null != classAnnotations) {
      AccessType[] classPermissions = classAnnotations.permissions();
    }

    // Rest of the flow
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
