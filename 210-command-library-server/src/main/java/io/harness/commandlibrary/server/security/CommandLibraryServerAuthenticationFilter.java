/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.commandlibrary.server.security;

import static io.harness.commandlibrary.common.CommandLibraryConstants.MANAGER_CLIENT_ID;
import static io.harness.eraro.ErrorCode.INVALID_CREDENTIAL;
import static io.harness.exception.WingsException.USER;

import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.Priorities.AUTHENTICATION;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substringAfter;

import io.harness.commandlibrary.server.app.CommandLibraryServerConfig;
import io.harness.exception.InvalidRequestException;
import io.harness.security.ServiceTokenAuthenticator;
import io.harness.security.annotations.PublicApi;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.lang.reflect.Method;
import java.util.function.Supplier;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Singleton
@Priority(AUTHENTICATION)
@Slf4j
public class CommandLibraryServerAuthenticationFilter implements ContainerRequestFilter {
  @Context ResourceInfo resourceInfo;

  @Inject private CommandLibraryServerConfig commandLibraryServerConfig;

  private final Supplier<String> secretKeyForManageSupplier = this::getServiceSecretForManager;

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
      log.error("error while validating user", ex);
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
    final String managerServiceSecret = secretKeyForManageSupplier.get();
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
    if (isNotBlank(commandLibraryServerConfig.getServiceSecretConfig().getManagerToCommandLibraryServiceSecret())) {
      return commandLibraryServerConfig.getServiceSecretConfig().getManagerToCommandLibraryServiceSecret().trim();
    }
    throw new InvalidRequestException("no secret key for client : " + MANAGER_CLIENT_ID);
  }
}
