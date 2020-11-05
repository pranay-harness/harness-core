package io.harness.cvng;

import static io.harness.eraro.ErrorCode.INVALID_CREDENTIAL;
import static io.harness.exception.WingsException.USER;
import static javax.ws.rs.Priorities.AUTHORIZATION;
import static org.apache.commons.lang3.StringUtils.substringAfter;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cvng.client.VerificationManagerClient;
import io.harness.exception.WingsException;
import io.harness.security.VerificationServiceAuthenticationFilter;
import io.harness.security.annotations.NextGenManagerAuth;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Method;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

@Singleton
@Priority(AUTHORIZATION)
@Slf4j
public class CVNGAuthenticationFilter
    extends VerificationServiceAuthenticationFilter implements ContainerRequestFilter {
  @Context private ResourceInfo resourceInfo;
  @Inject private VerificationManagerClient managerClient;

  @Override
  public void filter(ContainerRequestContext containerRequestContext) {
    if (authenticationExemptedRequests(containerRequestContext)) {
      return;
    }

    if (isLearningEngineServiceRequest(containerRequestContext)) {
      validateLearningEngineServiceToken(
          substringAfter(containerRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION), "LearningEngine "));
      return;
    }

    // this is already authenticated by common JWTAuthenticationFilter
    if (isNextGenServiceAPI()) {
      return;
    }

    if (isDelegateRequest(containerRequestContext)) {
      try {
        if (managerClient
                .authenticateDelegateRequest(
                    containerRequestContext.getUriInfo().getQueryParameters().getFirst("accountId"),
                    containerRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION))
                .execute()
                .body()
                .getResource()) {
          return;
        }
      } catch (IOException e) {
        log.error("Can not validate delegate request", e);
      }
    }

    throw new WingsException(INVALID_CREDENTIAL, USER);
  }

  protected boolean isNextGenServiceAPI() {
    Class<?> resourceClass = this.resourceInfo.getResourceClass();
    Method resourceMethod = this.resourceInfo.getResourceMethod();
    return resourceMethod.getAnnotation(NextGenManagerAuth.class) != null
        || resourceClass.getAnnotation(NextGenManagerAuth.class) != null;
  }
}
