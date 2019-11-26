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
    logger.info("LoginRateLimitFilter is being checked");
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
        logger.info("The request for rate limiting is {} and the Remote Host is {}",
            containerRequestContext.getUriInfo(), remoteHost);
        if (loginRequestRateLimiter.isOverRateLimit(remoteHost)) {
          return true;
        }
      }
    } else {
      logger.warn("ServletRequest is null, therefore remoteHost is not calculated");
    }
    return false;
  }

  private boolean isLoginOrLoginTypeApi(ContainerRequestContext requestContext) {
    boolean apiCheck = requestContext.getUriInfo().getAbsolutePath().getPath().endsWith("api/users/login")
        || requestContext.getUriInfo().getAbsolutePath().getPath().endsWith("api/users/logintype")
        || requestContext.getUriInfo().getAbsolutePath().getPath().endsWith("api/user/login")
        || requestContext.getUriInfo().getAbsolutePath().getPath().endsWith("api/identity/user/login");
    logger.info("The login &  logintype api check for {} is boolean value {}", requestContext.getUriInfo(), apiCheck);
    return apiCheck;
  }
}
