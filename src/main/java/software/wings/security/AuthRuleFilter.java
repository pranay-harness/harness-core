package software.wings.security;

import software.wings.beans.User;
import software.wings.security.annotations.AuthRule;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.lang.reflect.Method;

import static javax.ws.rs.Priorities.AUTHORIZATION;

/**
 * Created by anubhaw on 3/11/16.
 */

@Priority(AUTHORIZATION)
@AuthRule
public class AuthRuleFilter implements ContainerRequestFilter {
  @Context ResourceInfo resourceInfo;

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    Method resourceMethod = resourceInfo.getResourceMethod();
    AuthRule annotations = resourceMethod.getAnnotation(AuthRule.class);
    AccessType[] permissions = annotations.permissions();

    String token = requestContext.getHeaderString("X-AUTH_TOKEN");
    String userID = requestContext.getHeaderString("X-AUTH_USERID");

    User user = validateToken(token, userID);
    requestContext.setProperty("USER", user);

    // Rest of the flow
  }

  private User validateToken(String token, String userID) {
    User user = new User();
    user.setName("Anubhaw");
    user.setToken("dlskfewf");
    return user;
  }
}
