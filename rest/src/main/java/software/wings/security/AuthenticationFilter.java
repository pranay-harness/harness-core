package software.wings.security;

import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.Priorities.AUTHENTICATION;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static software.wings.beans.ErrorCode.INVALID_CREDENTIAL;
import static software.wings.beans.ErrorCode.INVALID_TOKEN;
import static software.wings.exception.WingsException.ALERTING;
import static software.wings.exception.WingsException.HARMLESS;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.app.MainConfiguration;
import software.wings.beans.AuthToken;
import software.wings.beans.User;
import software.wings.common.AuditHelper;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.security.annotations.PublicApi;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.UserService;

import java.io.IOException;
import java.lang.reflect.Method;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

@Singleton
@Priority(AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {
  @Context private ResourceInfo resourceInfo;
  private WingsPersistence wingsPersistence;
  private MainConfiguration configuration;
  private UserService userService;
  private AuthService authService;
  private AuditService auditService;
  private AuditHelper auditHelper;

  @Inject
  public AuthenticationFilter(AuthService authService, WingsPersistence wingsPersistence,
      MainConfiguration configuration, UserService userService, AuditService auditService, AuditHelper auditHelper) {
    this.authService = authService;
    this.wingsPersistence = wingsPersistence;
    this.userService = userService;
    this.configuration = configuration;
    this.auditService = auditService;
    this.auditHelper = auditHelper;
  }

  @Override
  public void filter(ContainerRequestContext containerRequestContext) throws IOException {
    if (authenticationExemptedRequests(containerRequestContext)) {
      return;
    }

    String authorization = containerRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
    if (authorization == null) {
      throw new WingsException(INVALID_TOKEN, HARMLESS);
    }

    if (isDelegateRequest(containerRequestContext)) {
      validateDelegateRequest(containerRequestContext);
      return;
    }

    if (isLearningEngineServiceRequest(containerRequestContext)) {
      validateLearningEngineRequest(containerRequestContext);
      return; // do nothing
    }

    if (authorization.startsWith("Bearer")) {
      User user = validateBearerToken(containerRequestContext);
      containerRequestContext.setProperty("USER", user);
      updateUserInAuditRecord(user);
      UserThreadLocal.set(user);
      return;
    }

    throw new WingsException(INVALID_CREDENTIAL, HARMLESS);
  }

  private User validateBearerToken(ContainerRequestContext containerRequestContext) {
    String tokenString = extractToken(containerRequestContext, "Bearer");
    AuthToken authToken = authService.validateToken(tokenString);
    User user = authToken.getUser();
    if (user != null) {
      return user;
    }
    throw new WingsException(INVALID_TOKEN, HARMLESS);
  }

  private void updateUserInAuditRecord(User user) {
    auditService.updateUser(auditHelper.get(), user.getPublicUser());
  }

  protected void validateLearningEngineRequest(ContainerRequestContext containerRequestContext) {
    authService.validateLearningEngineServiceToken(
        substringAfter(containerRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION), "LearningEngine "));
  }

  protected void validateDelegateRequest(ContainerRequestContext containerRequestContext) {
    MultivaluedMap<String, String> pathParameters = containerRequestContext.getUriInfo().getPathParameters();
    MultivaluedMap<String, String> queryParameters = containerRequestContext.getUriInfo().getQueryParameters();

    String accountId = getRequestParamFromContext("accountId", pathParameters, queryParameters);
    String header = containerRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
    if (header.contains("Delegate")) {
      authService.validateDelegateToken(
          accountId, substringAfter(containerRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION), "Delegate "));
    } else {
      throw new IllegalStateException("Invalid header:" + header);
    }
  }

  protected boolean authenticationExemptedRequests(ContainerRequestContext requestContext) {
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

  protected String extractToken(ContainerRequestContext requestContext, String prefix) {
    String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
    if (authorizationHeader == null || !authorizationHeader.startsWith(prefix)) {
      throw new WingsException(INVALID_TOKEN, ALERTING);
    }
    return authorizationHeader.substring(prefix.length()).trim();
  }

  private boolean isDelegateRequest(ContainerRequestContext requestContext) {
    return delegateAPI() && startsWith(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION), "Delegate ");
  }

  private boolean isLearningEngineServiceRequest(ContainerRequestContext requestContext) {
    return learningEngineServiceAPI()
        && startsWith(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION), "LearningEngine ");
  }

  protected boolean delegateAPI() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method resourceMethod = resourceInfo.getResourceMethod();

    return resourceMethod.getAnnotation(DelegateAuth.class) != null
        || resourceClass.getAnnotation(DelegateAuth.class) != null;
  }

  protected boolean learningEngineServiceAPI() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method resourceMethod = resourceInfo.getResourceMethod();

    return resourceMethod.getAnnotation(LearningEngineAuth.class) != null
        || resourceClass.getAnnotation(LearningEngineAuth.class) != null;
  }

  private String getRequestParamFromContext(
      String key, MultivaluedMap<String, String> pathParameters, MultivaluedMap<String, String> queryParameters) {
    return queryParameters.getFirst(key) != null ? queryParameters.getFirst(key) : pathParameters.getFirst(key);
  }
}
