package io.harness.cvng;

import static io.harness.eraro.ErrorCode.INVALID_CREDENTIAL;
import static io.harness.exception.WingsException.USER;
import static javax.ws.rs.Priorities.AUTHENTICATION;
import static org.apache.commons.lang3.StringUtils.substringAfter;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cvng.client.VerificationManagerClient;
import io.harness.cvng.core.services.api.VerificationServiceSecretManager;
import io.harness.cvng.utils.CVNextGenCache;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import io.harness.security.VerificationServiceAuthenticationFilter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

@Singleton
@Priority(AUTHENTICATION)
@Slf4j
public class CVNGAuthenticationFilter
    extends VerificationServiceAuthenticationFilter implements ContainerRequestFilter {
  private String PREFIX_BEARER = "Bearer";
  @Context private ResourceInfo resourceInfo;
  @Inject private CVNextGenCache cvNextGenCache;
  @Inject private VerificationServiceSecretManager verificationServiceSecretManager;
  @Inject private HPersistence hPersistence;
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

    // check with manager to see if it's a valid bearer token
    try {
      if (managerClient.authenticateUser(containerRequestContext).execute().body().getResource()) {
        return;
      }
    } catch (Exception ex) {
      logger.error("Exception while validating credential in cvng", ex);
    }
    throw new WingsException(INVALID_CREDENTIAL, USER);
  }
}
