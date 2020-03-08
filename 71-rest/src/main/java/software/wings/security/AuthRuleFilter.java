package software.wings.security;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.ACCOUNT_DOES_NOT_EXIST;
import static io.harness.eraro.ErrorCode.ACCOUNT_MIGRATED;
import static io.harness.eraro.ErrorCode.INACTIVE_ACCOUNT;
import static io.harness.eraro.ErrorCode.NOT_WHITELISTED_IP;
import static io.harness.exception.WingsException.USER;
import static java.util.Arrays.asList;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.Priorities.AUTHORIZATION;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.startsWith;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.HttpMethod;
import software.wings.beans.User;
import software.wings.resources.graphql.GraphQLUtils;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.UserRequestContext.UserRequestContextBuilder;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.ExternalFacingApiAuth;
import software.wings.security.annotations.HarnessApiKeyAuth;
import software.wings.security.annotations.IdentityServiceAuth;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.security.annotations.ListAPI;
import software.wings.security.annotations.PublicApi;
import software.wings.security.annotations.ScimAPI;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.HarnessUserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.WhitelistService;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by anubhaw on 3/11/16.
 */
@Singleton
@Priority(AUTHORIZATION)
@Slf4j
public class AuthRuleFilter implements ContainerRequestFilter {
  private static final String[] NO_FILTERING_URIS_PREFIXES = new String[] {"users/user", "users/sso/zendesk",
      "users/account", "users/two-factor-auth", "users/disable-two-factor-auth", "users/enable-two-factor-auth",
      "users/refresh-token", "account/new", "harness-api-keys", "users/set-default-account"};
  private static final String[] NO_FILTERING_URIS_SUFFIXES = new String[] {"/logout"};
  private static final String[] EXEMPTED_URI_PREFIXES =
      new String[] {"limits/configure", "account/license", "account/export", "account/import", "account/delete/",
          "account/disable", "account/enable", "users/reset-cache", "executions/workflow-variables",
          "executions/deployment-metadata", "setup-as-code/yaml/internal/template-yaml-sync"};
  private static final String[] EXEMPTED_URI_SUFFIXES = new String[] {"sales-contacts", "addSubdomainUrl"};

  @Context private ResourceInfo resourceInfo;
  @Context private HttpServletRequest servletRequest;

  private AuthService authService;
  private AuthHandler authHandler;
  private AccountService accountService;
  private UserService userService;
  private AppService appService;
  private WhitelistService whitelistService;
  private HarnessUserGroupService harnessUserGroupService;
  private GraphQLUtils graphQLUtils;

  /**
   * Instantiates a new Auth rule filter.
   *
   * @param authService      the auth service
   * @param appService       the appService
   * @param userService      the userService
   * @param whitelistService
   */
  @Inject
  public AuthRuleFilter(AuthService authService, AuthHandler authHandler, AppService appService,
      UserService userService, AccountService accountService, WhitelistService whitelistService,
      HarnessUserGroupService harnessUserGroupService, GraphQLUtils graphQLUtils) {
    this.authService = authService;
    this.authHandler = authHandler;
    this.appService = appService;
    this.userService = userService;
    this.accountService = accountService;
    this.whitelistService = whitelistService;
    this.harnessUserGroupService = harnessUserGroupService;
    this.graphQLUtils = graphQLUtils;
  }

  private boolean isAuthFilteringExempted(String uri) {
    for (String noFilteringUri : NO_FILTERING_URIS_PREFIXES) {
      if (uri.startsWith(noFilteringUri)) {
        return true;
      }
    }
    for (String noFilteringUri : NO_FILTERING_URIS_SUFFIXES) {
      if (uri.endsWith(noFilteringUri)) {
        return true;
      }
    }
    return false;
  }

  private boolean isExternalGraphQLRequest(String uri) {
    // External GraphQL API calls
    return uri.endsWith("graphql");
  }

  private boolean isInternalGraphQLRequest(String uri) {
    // Internal GraphQL calls used by custom dashboards.
    return uri.startsWith("graphql/int");
  }

  private boolean isHarnessUserExemptedRequest(String uri) {
    for (String exemptedUriPrefix : EXEMPTED_URI_PREFIXES) {
      if (uri.startsWith(exemptedUriPrefix)) {
        return true;
      }
    }
    for (String exemptedUriSuffix : EXEMPTED_URI_SUFFIXES) {
      if (uri.endsWith(exemptedUriSuffix)) {
        return true;
      }
    }
    return false;
  }

  /* (non-Javadoc)
   * @see javax.ws.rs.container.ContainerRequestFilter#filter(javax.ws.rs.container.ContainerRequestContext)
   */
  @Override
  public void filter(ContainerRequestContext requestContext) {
    if (authorizationExemptedRequest(requestContext)) {
      return; // do nothing
    }

    if (isScimAPI()) {
      authHandler.authorizeScimApi(requestContext);
      return;
    }

    if (isDelegateRequest(requestContext) || isLearningEngineServiceRequest(requestContext)
        || isIdentityServiceRequest(requestContext)) {
      return;
    }

    MultivaluedMap<String, String> pathParameters = requestContext.getUriInfo().getPathParameters();
    MultivaluedMap<String, String> queryParameters = requestContext.getUriInfo().getQueryParameters();

    String accountId = getRequestParamFromContext("accountId", pathParameters, queryParameters);
    boolean isExternalApi = externalAPI();

    List<String> appIdsFromRequest = getRequestParamsFromContext("appId", pathParameters, queryParameters);
    boolean emptyAppIdsInReq = isEmpty(appIdsFromRequest);

    if (!emptyAppIdsInReq && accountId == null) {
      // Ideally, we should force all the apis to have accountId as a mandatory field and not have this code.
      // But, since there might be some paths hitting this condition already, pulling up account from the first appId.
      accountId = appService.get(appIdsFromRequest.get(0)).getAccountId();
    }

    String uriPath = requestContext.getUriInfo().getPath();

    if (isInternalGraphQLRequest(uriPath)) {
      graphQLUtils.validateGraphQLCall(accountId, true);
    } else if (isExternalGraphQLRequest(uriPath)) {
      graphQLUtils.validateGraphQLCall(accountId, false);
    }

    boolean isHarnessUserExemptedRequest = isHarnessUserExemptedRequest(uriPath);
    if (isNotEmpty(accountId)) {
      validateAccountStatus(accountId, isHarnessUserExemptedRequest);
    } else if (accountId == null && isAuthFilteringExempted(uriPath)) {
      return;
    }

    User user = UserThreadLocal.get();
    if (user == null) {
      if (isExternalApi) {
        return;
      } else {
        logger.warn("No user context in operation: {}", uriPath);
        throw new InvalidRequestException("No user context set", USER);
      }
    }

    String httpMethod = requestContext.getMethod();
    List<PermissionAttribute> requiredPermissionAttributes;
    if (!userService.isUserAssignedToAccount(user, accountId)) {
      if (!httpMethod.equals(HttpMethod.GET.name()) && !isHarnessUserExemptedRequest) {
        throw new InvalidRequestException("User not authorized", USER);
      }

      Set<Action> actions = harnessUserGroupService.listAllowedUserActionsForAccount(accountId, user.getUuid());

      if (isEmpty(actions)) {
        throw new InvalidRequestException("User not authorized", USER);
      }
    }

    if (servletRequest != null) {
      String forwardedFor = servletRequest.getHeader("X-Forwarded-For");
      String remoteHost = isNotBlank(forwardedFor) ? forwardedFor : servletRequest.getRemoteHost();
      if (!whitelistService.isValidIPAddress(accountId, remoteHost)) {
        String msg = "Current IP Address (" + remoteHost + ") is not whitelisted.";
        logger.warn(msg);
        throw new WingsException(NOT_WHITELISTED_IP, USER).addParam("args", msg);
      }
    }

    requiredPermissionAttributes = getAllRequiredPermissionAttributes(requestContext);

    if (isEmpty(requiredPermissionAttributes) || allLoggedInScope(requiredPermissionAttributes)) {
      UserRequestContext userRequestContext = buildUserRequestContext(accountId, user, emptyAppIdsInReq);
      user.setUserRequestContext(userRequestContext);
      return;
    }

    if (accountId == null) {
      if (emptyAppIdsInReq) {
        throw new InvalidRequestException("appId not specified", USER);
      }
      throw new InvalidRequestException("accountId not specified", USER);
    }

    boolean skipAuth = skipAuth(requiredPermissionAttributes);
    boolean accountLevelPermissions = isAccountLevelPermissions(requiredPermissionAttributes);

    UserRequestContext userRequestContext = buildUserRequestContext(requiredPermissionAttributes, user, accountId,
        emptyAppIdsInReq, httpMethod, appIdsFromRequest, skipAuth, accountLevelPermissions);
    user.setUserRequestContext(userRequestContext);

    if (!skipAuth) {
      if (accountLevelPermissions) {
        authHandler.authorizeAccountPermission(userRequestContext, requiredPermissionAttributes);
      } else {
        // Handle delete and update methods
        String entityId = getEntityIdFromRequest(requiredPermissionAttributes, pathParameters, queryParameters);
        if (requestContext.getRequest().getMethod().equals(HttpMethod.PUT.name())
            || httpMethod.equals(HttpMethod.DELETE.name()) || httpMethod.equals(HttpMethod.POST.name())) {
          authService.authorize(accountId, appIdsFromRequest, entityId, user, requiredPermissionAttributes);
        } else if (httpMethod.equals(HttpMethod.GET.name())) {
          // In case of list api, the entityId would be null, we enforce restrictions in WingsMongoPersistence
          if (entityId != null) {
            // get api
            authService.authorize(accountId, appIdsFromRequest, entityId, user, requiredPermissionAttributes);
          }
        }
      }
    }
  }

  private void validateAccountStatus(String accountId, boolean isHarnessUserExemptedRequest) {
    String accountStatus = accountService.getAccountStatus(accountId);
    if (AccountStatus.DELETED.equals(accountStatus)) {
      throw new WingsException(ACCOUNT_DOES_NOT_EXIST, USER);
    } else if (AccountStatus.INACTIVE.equals(accountStatus) && !isHarnessUserExemptedRequest) {
      Account account = accountService.getFromCache(accountId);
      String migratedToClusterUrl = account == null ? null : account.getMigratedToClusterUrl();
      if (migratedToClusterUrl == null) {
        throw new WingsException(INACTIVE_ACCOUNT, USER);
      } else {
        throw new WingsException(ACCOUNT_MIGRATED, USER);
      }
    }
  }

  public boolean isAccountLevelPermissions(List<PermissionAttribute> permissionAttributes) {
    return permissionAttributes.stream().anyMatch(
        permissionAttribute -> isAccountLevelPermissions(permissionAttribute.getPermissionType()));
  }

  private boolean isAccountLevelPermissions(PermissionType permissionType) {
    return PermissionType.APPLICATION_CREATE_DELETE == permissionType
        || PermissionType.USER_PERMISSION_MANAGEMENT == permissionType
        || PermissionType.ACCOUNT_MANAGEMENT == permissionType || PermissionType.TEMPLATE_MANAGEMENT == permissionType
        || PermissionType.USER_PERMISSION_READ == permissionType || PermissionType.AUDIT_VIEWER == permissionType
        || PermissionType.TAG_MANAGEMENT == permissionType;
  }

  private String getEntityIdFromRequest(List<PermissionAttribute> permissionAttributes,
      MultivaluedMap<String, String> pathParameters, MultivaluedMap<String, String> queryParameters) {
    String parameterName = getParameterName(permissionAttributes);
    if (parameterName != null) {
      return getEntityId(pathParameters, queryParameters, parameterName);
    }

    return null;
  }

  public String getParameterName(List<PermissionAttribute> permissionAttributes) {
    Optional<String> entityFieldNameOptional =
        permissionAttributes.stream()
            .map(permissionAttribute -> {
              if (StringUtils.isNotBlank(permissionAttribute.getParameterName())) {
                return permissionAttribute.getParameterName();
              }

              PermissionType permissionType = permissionAttribute.getPermissionType();

              String fieldName = null;
              if (permissionType == PermissionType.SERVICE) {
                fieldName = "serviceId";
              } else if (permissionType == PermissionType.PROVISIONER) {
                fieldName = "infraProvisionerId";
              } else if (permissionType == PermissionType.ENV) {
                fieldName = "envId";
              } else if (permissionType == PermissionType.WORKFLOW) {
                fieldName = "workflowId";
              } else if (permissionType == PermissionType.PIPELINE) {
                fieldName = "pipelineId";
              } else if (permissionType == PermissionType.DEPLOYMENT) {
                fieldName = "workflowId";
              }

              return fieldName;
            })
            .findFirst();

    if (entityFieldNameOptional.isPresent()) {
      return entityFieldNameOptional.get();
    }

    return null;
  }

  private String getEntityId(MultivaluedMap<String, String> pathParameters,
      MultivaluedMap<String, String> queryParameters, String parameterName) {
    String entityId = queryParameters.getFirst(parameterName);
    if (entityId == null) {
      return pathParameters.getFirst(parameterName);
    }
    return entityId;
  }

  public Set<String> getAllowedAppIds(UserPermissionInfo userPermissionInfo) {
    Set<String> allowedAppIds;

    Map<String, AppPermissionSummaryForUI> appPermissionMap = userPermissionInfo.getAppPermissionMap();

    if (MapUtils.isNotEmpty(appPermissionMap)) {
      allowedAppIds = appPermissionMap.keySet();
    } else {
      allowedAppIds = new HashSet<>();
    }
    return allowedAppIds;
  }

  private UserRequestContext buildUserRequestContext(List<PermissionAttribute> requiredPermissionAttributes, User user,
      String accountId, boolean emptyAppIdsInReq, String httpMethod, List<String> appIdsFromRequest, boolean skipAuth,
      boolean accountLevelPermissions) {
    UserRequestContext userRequestContext = buildUserRequestContext(accountId, user, emptyAppIdsInReq);

    if (!accountLevelPermissions) {
      authHandler.setEntityIdFilterIfGet(httpMethod, skipAuth, requiredPermissionAttributes, userRequestContext,
          userRequestContext.isAppIdFilterRequired(), userRequestContext.getAppIds(), appIdsFromRequest);
    }
    return userRequestContext;
  }

  private UserRequestContext buildUserRequestContext(String accountId, User user, boolean emptyAppIdsInReq) {
    UserRequestContextBuilder userRequestContextBuilder =
        UserRequestContext.builder().accountId(accountId).entityInfoMap(new HashMap<>());

    UserPermissionInfo userPermissionInfo = authService.getUserPermissionInfo(accountId, user, false);
    userRequestContextBuilder.userPermissionInfo(userPermissionInfo);

    UserRestrictionInfo userRestrictionInfo =
        authService.getUserRestrictionInfo(accountId, user, userPermissionInfo, false);
    userRequestContextBuilder.userRestrictionInfo(userRestrictionInfo);

    Set<String> allowedAppIds = getAllowedAppIds(userPermissionInfo);
    setAppIdFilterInUserRequestContext(userRequestContextBuilder, emptyAppIdsInReq, allowedAppIds);

    return userRequestContextBuilder.build();
  }

  private boolean skipAuth(List<PermissionAttribute> requiredPermissionAttributes) {
    if (CollectionUtils.isEmpty(requiredPermissionAttributes)) {
      return false;
    }

    return requiredPermissionAttributes.stream().anyMatch(PermissionAttribute::isSkipAuth);
  }

  public boolean setAppIdFilterInUserRequestContext(
      UserRequestContextBuilder userRequestContextBuilder, boolean emptyAppIdsInReq, Set<String> allowedAppIds) {
    if (!emptyAppIdsInReq) {
      return false;
    }

    List<ResourceType> requiredResourceTypes = getAllResourceTypes();
    if (isPresent(requiredResourceTypes, ResourceType.APPLICATION)) {
      userRequestContextBuilder.appIdFilterRequired(true);
      userRequestContextBuilder.appIds(allowedAppIds);
      return true;
    }

    return false;
  }

  private boolean isPresent(List<ResourceType> requiredResourceTypes, ResourceType resourceType) {
    return requiredResourceTypes.stream().anyMatch(requiredResourceType -> requiredResourceType == resourceType);
  }

  private boolean allLoggedInScope(List<PermissionAttribute> requiredPermissionAttributes) {
    return !requiredPermissionAttributes.parallelStream().anyMatch(
        permissionAttribute -> permissionAttribute.getPermissionType() != PermissionType.LOGGED_IN);
  }

  private boolean isDelegateRequest(ContainerRequestContext requestContext) {
    return delegateAPI() && startsWith(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION), "Delegate ");
  }

  private boolean isLearningEngineServiceRequest(ContainerRequestContext requestContext) {
    return learningEngineServiceAPI()
        && startsWith(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION), "LearningEngine ");
  }

  private boolean isIdentityServiceRequest(ContainerRequestContext requestContext) {
    return identityServiceAPI()
        && startsWith(
               requestContext.getHeaderString(HttpHeaders.AUTHORIZATION), AuthenticationFilter.IDENTITY_SERVICE_PREFIX);
  }

  private boolean authorizationExemptedRequest(ContainerRequestContext requestContext) {
    // externalAPI() doesn't need any authorization
    return publicAPI() || requestContext.getMethod().equals(OPTIONS) || identityServiceAPI() || harnessClientApi()
        || requestContext.getUriInfo().getAbsolutePath().getPath().endsWith("api/version")
        || requestContext.getUriInfo().getAbsolutePath().getPath().endsWith("api/swagger")
        || requestContext.getUriInfo().getAbsolutePath().getPath().endsWith("api/swagger.json");
  }

  private String getRequestParamFromContext(
      String key, MultivaluedMap<String, String> pathParameters, MultivaluedMap<String, String> queryParameters) {
    return queryParameters.getFirst(key) != null ? queryParameters.getFirst(key) : pathParameters.getFirst(key);
  }

  private List<String> getRequestParamsFromContext(
      String key, MultivaluedMap<String, String> pathParameters, MultivaluedMap<String, String> queryParameters) {
    return queryParameters.get(key) != null ? queryParameters.get(key) : pathParameters.get(key);
  }

  private boolean identityServiceAPI() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method resourceMethod = resourceInfo.getResourceMethod();
    return resourceMethod.getAnnotation(IdentityServiceAuth.class) != null
        || resourceClass.getAnnotation(IdentityServiceAuth.class) != null;
  }

  private boolean publicAPI() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method resourceMethod = resourceInfo.getResourceMethod();

    return resourceMethod.getAnnotation(PublicApi.class) != null
        || resourceClass.getAnnotation(PublicApi.class) != null;
  }

  private boolean harnessClientApi() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method resourceMethod = resourceInfo.getResourceMethod();

    return resourceMethod.getAnnotation(HarnessApiKeyAuth.class) != null
        || resourceClass.getAnnotation(HarnessApiKeyAuth.class) != null;
  }

  private boolean externalAPI() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method resourceMethod = resourceInfo.getResourceMethod();

    return resourceMethod.getAnnotation(ExternalFacingApiAuth.class) != null
        || resourceClass.getAnnotation(ExternalFacingApiAuth.class) != null;
  }

  private boolean delegateAPI() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method resourceMethod = resourceInfo.getResourceMethod();

    return resourceMethod.getAnnotation(DelegateAuth.class) != null
        || resourceClass.getAnnotation(DelegateAuth.class) != null;
  }

  private boolean learningEngineServiceAPI() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method resourceMethod = resourceInfo.getResourceMethod();

    return resourceMethod.getAnnotation(LearningEngineAuth.class) != null
        || resourceClass.getAnnotation(LearningEngineAuth.class) != null;
  }

  private boolean listAPI() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method resourceMethod = resourceInfo.getResourceMethod();

    return resourceMethod.getAnnotation(ListAPI.class) != null || resourceClass.getAnnotation(ListAPI.class) != null;
  }

  public PermissionAttribute buildPermissionAttribute(AuthRule authRule, String httpMethod, ResourceType resourceType) {
    return new PermissionAttribute(resourceType, authRule.permissionType(), getAction(authRule, httpMethod), httpMethod,
        authRule.parameterName(), authRule.dbFieldName(), authRule.dbCollectionName(), authRule.skipAuth());
  }

  private List<PermissionAttribute> getAllRequiredPermissionAttributes(ContainerRequestContext requestContext) {
    List<PermissionAttribute> methodPermissionAttributes = new ArrayList<>();
    List<PermissionAttribute> classPermissionAttributes = new ArrayList<>();

    Method resourceMethod = resourceInfo.getResourceMethod();
    AuthRule methodAnnotations = resourceMethod.getAnnotation(AuthRule.class);
    if (null != methodAnnotations) {
      methodPermissionAttributes.add(buildPermissionAttribute(methodAnnotations, requestContext.getMethod(), null));
    }

    Class<?> resourceClass = resourceInfo.getResourceClass();
    AuthRule classAnnotations = resourceClass.getAnnotation(AuthRule.class);
    if (null != classAnnotations) {
      classPermissionAttributes.add(new PermissionAttribute(null, classAnnotations.permissionType(),
          getAction(classAnnotations, requestContext.getMethod()), requestContext.getMethod(),
          classAnnotations.parameterName(), classAnnotations.dbFieldName(), classAnnotations.dbCollectionName(),
          classAnnotations.skipAuth()));
    }

    if (methodPermissionAttributes.isEmpty()) {
      return classPermissionAttributes;
    } else {
      return methodPermissionAttributes;
    }
  }

  private Action getAction(AuthRule authRule, String method) {
    Action action;
    if (authRule.action() == Action.DEFAULT) {
      action = getDefaultAction(method);
    } else {
      action = authRule.action();
    }
    return action;
  }

  private Action getDefaultAction(String method) {
    if (HttpMethod.GET.name().equals(method)) {
      return Action.READ;
    } else if (HttpMethod.PUT.name().equals(method)) {
      return Action.UPDATE;
    } else if (HttpMethod.POST.name().equals(method)) {
      return Action.CREATE;
    } else if (HttpMethod.DELETE.name().equals(method)) {
      return Action.DELETE;
    }

    return Action.READ;
  }

  private List<ResourceType> getAllResourceTypes() {
    List<ResourceType> methodResourceTypes = new ArrayList<>();
    List<ResourceType> classResourceTypes = new ArrayList<>();

    Method resourceMethod = resourceInfo.getResourceMethod();
    Scope methodAnnotations = resourceMethod.getAnnotation(Scope.class);
    if (null != methodAnnotations) {
      methodResourceTypes = asList(methodAnnotations.value());
    }

    Class<?> resourceClass = resourceInfo.getResourceClass();
    Scope classAnnotations = resourceClass.getAnnotation(Scope.class);
    if (null != classAnnotations) {
      classResourceTypes = asList(classAnnotations.value());
    }

    if (methodResourceTypes.isEmpty()) {
      return classResourceTypes;
    } else {
      return methodResourceTypes;
    }
  }

  private List<PermissionAttribute> getAllRequiredPermissionAttrFromScope(ContainerRequestContext requestContext) {
    List<PermissionAttribute> methodPermissionAttributes = new ArrayList<>();
    List<PermissionAttribute> classPermissionAttributes = new ArrayList<>();

    Method resourceMethod = resourceInfo.getResourceMethod();
    Scope methodAnnotations = resourceMethod.getAnnotation(Scope.class);
    if (null != methodAnnotations) {
      Stream.of(methodAnnotations.value())
          .forEach(s
              -> methodPermissionAttributes.add(
                  new PermissionAttribute(s, methodAnnotations.scope(), requestContext.getMethod())));
    }

    Class<?> resourceClass = resourceInfo.getResourceClass();
    Scope classAnnotations = resourceClass.getAnnotation(Scope.class);
    if (null != classAnnotations) {
      Stream.of(classAnnotations.value())
          .forEach(s
              -> classPermissionAttributes.add(
                  new PermissionAttribute(s, classAnnotations.scope(), requestContext.getMethod())));
    }

    if (methodPermissionAttributes.isEmpty()) {
      return classPermissionAttributes;
    } else {
      return methodPermissionAttributes;
    }
  }

  protected boolean isScimAPI() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method resourceMethod = resourceInfo.getResourceMethod();

    return resourceMethod.getAnnotation(ScimAPI.class) != null || resourceClass.getAnnotation(ScimAPI.class) != null;
  }
}
