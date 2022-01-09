/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static javax.ws.rs.Priorities.AUTHENTICATION;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.OwnedBy;

import software.wings.ratelimit.LoginRequestRateLimiter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
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
          log.warn("The request for rate limiting from {} and the Remote Host {}, has reached its limit",
              containerRequestContext.getUriInfo().getRequestUri(), remoteHost);
          return true;
        }
      }
    } else {
      log.warn("ServletRequest is null, therefore remoteHost is not calculated for rate limiting");
    }
    return false;
  }

  private boolean isLoginOrLoginTypeApi(ContainerRequestContext requestContext) {
    return requestContext.getUriInfo().getAbsolutePath().getPath().endsWith("api/users/login")
        || requestContext.getUriInfo().getAbsolutePath().getPath().endsWith("api/users/logintype")
        || requestContext.getUriInfo().getAbsolutePath().getPath().endsWith("api/users/user/login")
        || requestContext.getUriInfo().getAbsolutePath().getPath().endsWith("api/identity/user/login");
  }
}
