package software.wings.security;

import static javax.ws.rs.Priorities.AUTHENTICATION;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import software.wings.ratelimit.LoginRequestRateLimiter;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Singleton
@Priority(AUTHENTICATION)
@Slf4j
public class LoginRateLimitFilter implements ContainerRequestFilter {
  @Context private HttpServletRequest servletRequest;
  private LoginRequestRateLimiter loginRequestRateLimiter;

  @Inject
  public LoginRateLimitFilter(LoginRequestRateLimiter loginRequestRateLimiter) {
    this.loginRequestRateLimiter = loginRequestRateLimiter;
  }

  @Override
  public void filter(ContainerRequestContext requestContext) {
    if (checkRateLimit(requestContext)) {
      throw new WebApplicationException(
          Response.status(429)
              .encoding("Number of login requests from this IP Address has reached it's limit")
              .build());
    }
  }

  private boolean checkRateLimit(ContainerRequestContext containerRequestContext) {
    if (servletRequest != null) {
      String forwardedFor = servletRequest.getHeader("X-Forwarded-For");
      String remoteHost = isNotBlank(forwardedFor) ? forwardedFor : servletRequest.getRemoteHost();
      if (isLoginOrLoginTypeApi(containerRequestContext)) {
        if (loginRequestRateLimiter.isOverRateLimit(remoteHost)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isLoginOrLoginTypeApi(ContainerRequestContext requestContext) {
    return requestContext.getUriInfo().getAbsolutePath().getPath().endsWith("api/users/login")
        || requestContext.getUriInfo().getAbsolutePath().getPath().endsWith("api/users/logintype")
        || requestContext.getUriInfo().getAbsolutePath().getPath().endsWith("api/user/login");
  }
}
