package software.wings.security;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.INVALID_CREDENTIAL;
import static io.harness.eraro.ErrorCode.INVALID_TOKEN;
import static io.harness.eraro.ErrorCode.USER_DOES_NOT_EXIST;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_ADMIN;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.Priorities.AUTHENTICATION;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substringAfter;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.context.GlobalContext;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnauthorizedException;
import io.harness.manage.GlobalContextManager;
import software.wings.beans.AuthToken;
import software.wings.beans.User;
import software.wings.common.AuditHelper;
import software.wings.security.SecretManager.JWT_CATEGORY;
import software.wings.security.annotations.AdminPortalAuth;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.ExternalFacingApiAuth;
import software.wings.security.annotations.IdentityServiceAuth;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.security.annotations.PublicApi;
import software.wings.security.annotations.ScimAPI;
import software.wings.service.intfc.ApiKeyService;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.ExternalApiRateLimitingService;
import software.wings.service.intfc.HarnessApiKeyService;
import software.wings.service.intfc.UserService;

import java.io.IOException;
import java.lang.reflect.Method;
import javax.annotation.Priority;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

@Singleton
@Priority(AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {
  @VisibleForTesting public static final String API_KEY_HEADER = "X-Api-Key";
  @VisibleForTesting public static final String HARNESS_API_KEY_HEADER = "X-Harness-Api-Key";
  @VisibleForTesting public static final String USER_IDENTITY_HEADER = "X-Identity-User";
  public static final String IDENTITY_SERVICE_PREFIX = "IdentityService ";
  public static final String ADMIN_PORTAL_PREFIX = "AdminPortal ";
  private static final int NUM_MANAGERS = 3;

  @Context private ResourceInfo resourceInfo;
  private AuthService authService;
  private UserService userService;
  private AuditService auditService;
  private ApiKeyService apiKeyService;
  private HarnessApiKeyService harnessApiKeyService;
  private AuditHelper auditHelper;
  private ExternalApiRateLimitingService rateLimitingService;
  private SecretManager secretManager;

  @Inject
  public AuthenticationFilter(UserService userService, AuthService authService, AuditService auditService,
      AuditHelper auditHelper, ApiKeyService apiKeyService, HarnessApiKeyService harnessApiKeyService,
      ExternalApiRateLimitingService rateLimitingService, SecretManager secretManager) {
    this.userService = userService;
    this.authService = authService;
    this.auditService = auditService;
    this.auditHelper = auditHelper;
    this.apiKeyService = apiKeyService;
    this.harnessApiKeyService = harnessApiKeyService;
    this.rateLimitingService = rateLimitingService;
    this.secretManager = secretManager;
  }

  @Override
  public void filter(ContainerRequestContext containerRequestContext) throws IOException {
    if (authenticationExemptedRequests(containerRequestContext)) {
      return;
    }

    if (delegateAPI()) {
      validateDelegateRequest(containerRequestContext);
      return;
    }

    if (learningEngineServiceAPI()) {
      validateLearningEngineRequest(containerRequestContext);
      return; // do nothing
    }

    if (isScimAPI()) {
      return;
    }

    String authorization = containerRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

    if (isExternalFacingApiRequest(containerRequestContext)) {
      String apiKey = containerRequestContext.getHeaderString(API_KEY_HEADER);

      if (isNotEmpty(apiKey)) {
        if (!containerRequestContext.getUriInfo().getAbsolutePath().getPath().endsWith("graphql")) {
          ensureValidQPM(containerRequestContext.getHeaderString(API_KEY_HEADER));
        }

        try {
          validateExternalFacingApiRequest(containerRequestContext);
          return;
        } catch (UnauthorizedException | InvalidRequestException exception) {
          if (authorization == null) {
            throw exception;
          }
        }
      }

      if (checkIfBearerTokenAndValidate(authorization, containerRequestContext)) {
        return;
      }
    }

    if (harnessApiKeyService.isHarnessClientApi(resourceInfo)) {
      harnessApiKeyService.validateHarnessClientApiRequest(resourceInfo, containerRequestContext);
      return;
    }

    if (authorization == null) {
      throw new InvalidRequestException(INVALID_TOKEN.name(), INVALID_TOKEN, USER);
    }

    GlobalContextManager.set(new GlobalContext());

    if (isAdminPortalRequest()) {
      String adminPortalToken =
          substringAfter(containerRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION), ADMIN_PORTAL_PREFIX)
              .trim();
      secretManager.verifyJWTToken(adminPortalToken, JWT_CATEGORY.DATA_HANDLER_SECRET);
      return;
    }

    if (isAuthenticatedByIdentitySvc(containerRequestContext)) {
      String identityServiceToken =
          substringAfter(containerRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION), IDENTITY_SERVICE_PREFIX);
      HarnessUserAccountActions harnessUserAccountActions = secretManager.getHarnessUserAccountActions(
          secretManager.verifyJWTToken(identityServiceToken, JWT_CATEGORY.IDENTITY_SERVICE_SECRET));
      HarnessUserThreadLocal.set(harnessUserAccountActions);

      String userId = containerRequestContext.getHeaderString(USER_IDENTITY_HEADER);
      User user = userService.getUserFromCacheOrDB(userId);
      if (user != null) {
        UserThreadLocal.set(user);
        return;
      } else {
        throw new InvalidRequestException(USER_DOES_NOT_EXIST.name(), USER_DOES_NOT_EXIST, USER);
      }
    }

    if (isIdentityServiceRequest(containerRequestContext)) {
      String identityServiceToken =
          substringAfter(containerRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION), IDENTITY_SERVICE_PREFIX);
      HarnessUserAccountActions harnessUserAccountActions = secretManager.getHarnessUserAccountActions(
          secretManager.verifyJWTToken(identityServiceToken, JWT_CATEGORY.IDENTITY_SERVICE_SECRET));
      HarnessUserThreadLocal.set(harnessUserAccountActions);

      return;
    }

    if (checkIfBearerTokenAndValidate(authorization, containerRequestContext)) {
      return;
    }

    throw new InvalidRequestException(INVALID_CREDENTIAL.name(), INVALID_CREDENTIAL, USER);
  }

  protected boolean isScimAPI() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method resourceMethod = resourceInfo.getResourceMethod();

    return resourceMethod.getAnnotation(ScimAPI.class) != null || resourceClass.getAnnotation(ScimAPI.class) != null;
  }

  protected boolean isAuthenticatedByIdentitySvc(ContainerRequestContext containerRequestContext) {
    String value = containerRequestContext.getHeaderString(USER_IDENTITY_HEADER);
    return isNotEmpty(value);
  }

  protected boolean isAdminPortalRequest() {
    return resourceInfo.getResourceMethod().getAnnotation(AdminPortalAuth.class) != null
        || resourceInfo.getResourceClass().getAnnotation(AdminPortalAuth.class) != null;
  }

  private boolean checkIfBearerTokenAndValidate(String authHeader, ContainerRequestContext containerRequestContext) {
    if (authHeader != null && authHeader.startsWith("Bearer")) {
      User user = validateBearerToken(containerRequestContext);
      containerRequestContext.setProperty("USER", user);
      updateUserInAuditRecord(user);
      UserThreadLocal.set(user);
      return true;
    }
    return false;
  }

  private void ensureValidQPM(String key) {
    if (rateLimitingService.rateLimitRequest(key)) {
      throw new WebApplicationException(Response.status(429)
                                            .entity("Too Many requests. Throttled. Max QPS: "
                                                + rateLimitingService.getMaxQPMPerManager() * NUM_MANAGERS / 60)
                                            .build());
    }
  }

  private User validateBearerToken(ContainerRequestContext containerRequestContext) {
    String tokenString = extractToken(containerRequestContext, "Bearer");
    AuthToken authToken = authService.validateToken(tokenString);
    User user = authToken.getUser();
    if (user != null) {
      user.setToken(tokenString);
      return user;
    }
    throw new InvalidRequestException(INVALID_TOKEN.name(), INVALID_TOKEN, USER);
  }

  private void updateUserInAuditRecord(User user) {
    auditService.updateUser(auditHelper.get(), user.getPublicUser());
  }

  protected void validateLearningEngineRequest(ContainerRequestContext containerRequestContext) {
    String header = containerRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

    if (isEmpty(header)) {
      throw new IllegalStateException("Invalid verification header");
    }

    authService.validateLearningEngineServiceToken(substringAfter(header, "LearningEngine "));
  }

  protected void validateDelegateRequest(ContainerRequestContext containerRequestContext) {
    MultivaluedMap<String, String> pathParameters = containerRequestContext.getUriInfo().getPathParameters();
    MultivaluedMap<String, String> queryParameters = containerRequestContext.getUriInfo().getQueryParameters();

    String accountId = getRequestParamFromContext("accountId", pathParameters, queryParameters);
    String header = containerRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
    if (header != null && header.contains("Delegate")) {
      authService.validateDelegateToken(
          accountId, substringAfter(containerRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION), "Delegate "));
    } else {
      throw new IllegalStateException("Invalid header:" + header);
    }
  }

  protected void validateExternalFacingApiRequest(ContainerRequestContext containerRequestContext) {
    String apiKey = containerRequestContext.getHeaderString(API_KEY_HEADER);
    if (isBlank(apiKey)) {
      throw new InvalidRequestException("Api Key not supplied", USER);
    }
    String accountId = getRequestParamFromContext("accountId", containerRequestContext.getUriInfo().getPathParameters(),
        containerRequestContext.getUriInfo().getQueryParameters());

    if (isEmpty(accountId)) {
      // In case of graphql, accountId comes as null. For the new version of api keys, we can get the accountId
      accountId = apiKeyService.getAccountIdFromApiKey(apiKey);
    }

    apiKeyService.validate(apiKey, accountId);
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
      throw new InvalidRequestException("Invalid token", INVALID_TOKEN, USER_ADMIN);
    }
    return authorizationHeader.substring(prefix.length()).trim();
  }

  private boolean isIdentityServiceRequest(ContainerRequestContext requestContext) {
    return identityServiceAPI()
        && startsWith(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION), IDENTITY_SERVICE_PREFIX);
  }

  private boolean isExternalFacingApiRequest(ContainerRequestContext requestContext) {
    return externalFacingAPI();
  }

  boolean identityServiceAPI() {
    return resourceInfo.getResourceMethod().getAnnotation(IdentityServiceAuth.class) != null
        || resourceInfo.getResourceClass().getAnnotation(IdentityServiceAuth.class) != null;
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

  protected boolean externalFacingAPI() {
    return resourceInfo.getResourceMethod().getAnnotation(ExternalFacingApiAuth.class) != null
        || resourceInfo.getResourceClass().getAnnotation(ExternalFacingApiAuth.class) != null;
  }

  private String getRequestParamFromContext(
      String key, MultivaluedMap<String, String> pathParameters, MultivaluedMap<String, String> queryParameters) {
    return queryParameters.getFirst(key) != null ? queryParameters.getFirst(key) : pathParameters.getFirst(key);
  }
}
