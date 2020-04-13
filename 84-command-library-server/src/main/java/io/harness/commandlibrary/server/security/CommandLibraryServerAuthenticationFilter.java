package io.harness.commandlibrary.server.security;

import static io.harness.commandlibrary.common.CommandLibraryConstants.MANAGER_CLIENT_ID;
import static io.harness.eraro.ErrorCode.INVALID_CREDENTIAL;
import static io.harness.exception.WingsException.USER;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.Priorities.AUTHENTICATION;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substringAfter;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.commandlibrary.common.service.CommandLibraryService;
import io.harness.exception.InvalidRequestException;
import io.harness.security.ServiceTokenAuthenticator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import software.wings.security.annotations.PublicApi;

import java.lang.reflect.Method;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

@Singleton
@Priority(AUTHENTICATION)
@Slf4j
public class CommandLibraryServerAuthenticationFilter implements ContainerRequestFilter {
  @Context ResourceInfo resourceInfo;

  @Inject private CommandLibraryService commandLibraryService;

  @Override
  public void filter(ContainerRequestContext requestContext) {
    try {
      if (authenticationExemptedRequests(requestContext)) {
        return;
      }
      if (isManagerRequest(requestContext)) {
        validateManagerServiceToken(
            substringAfter(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION), MANAGER_CLIENT_ID + SPACE));
        return;
      }
    } catch (Exception ex) {
      logger.error("error while validating user", ex);
      throw ex;
    }

    throw new InvalidRequestException("Unable to authenticate client", INVALID_CREDENTIAL, USER);
  }

  private boolean authenticationExemptedRequests(ContainerRequestContext requestContext) {
    return requestContext.getMethod().equals(OPTIONS) || publicAPI()
        || requestContext.getUriInfo().getAbsolutePath().getPath().endsWith("api/version")
        || requestContext.getUriInfo().getAbsolutePath().getPath().endsWith("api/swagger")
        || requestContext.getUriInfo().getAbsolutePath().getPath().endsWith("api/swagger.json");
  }

  protected boolean publicAPI() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method resourceMethod = resourceInfo.getResourceMethod();

    return resourceMethod.getAnnotation(PublicApi.class) != null
        || resourceClass.getAnnotation(PublicApi.class) != null;
  }

  private boolean isManagerRequest(ContainerRequestContext requestContext) {
    return startsWith(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION), MANAGER_CLIENT_ID + SPACE);
  }

  private void validateManagerServiceToken(String managerServiceToken) {
    final String managerServiceSecret = getServiceSecretForManager();
    validateTokenUsingSecret(managerServiceToken, managerServiceSecret);
  }

  @VisibleForTesting
  void validateTokenUsingSecret(String managerServiceToken, String managerServiceSecret) {
    final ServiceTokenAuthenticator serviceTokenAuthenticator =
        ServiceTokenAuthenticator.builder().secretKey(managerServiceSecret).build();

    serviceTokenAuthenticator.authenticate(managerServiceToken);
  }

  @NotNull
  private String getServiceSecretForManager() {
    final String managerServiceSecret = commandLibraryService.getSecretForClient(MANAGER_CLIENT_ID);
    if (StringUtils.isBlank(managerServiceSecret)) {
      throw new InvalidRequestException("no secret key for client : " + MANAGER_CLIENT_ID);
    }
    return managerServiceSecret;
  }
}
