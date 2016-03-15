package software.wings.security;

import software.wings.beans.AuthToken;
import software.wings.beans.User;
import software.wings.security.annotations.AuthRule;

import javax.annotation.Priority;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import java.lang.reflect.Method;

import static javax.ws.rs.Priorities.AUTHENTICATION;

/**
 * Created by anubhaw on 3/11/16.
 */

@Priority(AUTHENTICATION)
@AuthRule
public class AuthRuleFilter implements ContainerRequestFilter {
  @Context ResourceInfo resourceInfo;

  @Override
  public void filter(ContainerRequestContext requestContext) {
    AuthToken authToken = extractToken(requestContext);
    User user = findUserFromDB(authToken.getUserID());
    requestContext.setProperty("USER", user);

    Method resourceMethod = resourceInfo.getResourceMethod();
    AuthRule annotations = resourceMethod.getAnnotation(AuthRule.class);
    AccessType[] permissions = annotations.permissions();

    // Rest of the flow
  }

  private User findUserFromDB(String userID) {
    return null;
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
      throw new NotAuthorizedException("Authorization header must be provided");
    }
    String tokenString = authorizationHeader.substring("Bearer".length()).trim();
    return fetchTokenFromDB(tokenString);
  }

  private AuthToken fetchTokenFromDB(String tokenString) {
    return null;
  }
}
