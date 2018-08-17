package software.wings.security;

import static com.google.common.collect.ImmutableList.copyOf;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.Priorities.AUTHORIZATION;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static software.wings.beans.ErrorCode.NOT_WHITELISTED_IP;
import static software.wings.exception.WingsException.USER;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AccountRole;
import software.wings.beans.ApplicationRole;
import software.wings.beans.EnvironmentRole;
import software.wings.beans.HttpMethod;
import software.wings.beans.User;
import software.wings.common.AuditHelper;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.UserRequestContext.UserRequestContextBuilder;
import software.wings.security.UserRequestInfo.UserRequestInfoBuilder;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.security.annotations.ListAPI;
import software.wings.security.annotations.PublicApi;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.HarnessUserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.WhitelistService;

import java.lang.reflect.Method;
import java.util.ArrayList;
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
public class AuthRuleFilter implements ContainerRequestFilter {
  private static final Logger logger = LoggerFactory.getLogger(AuthRuleFilter.class);

  @Context private ResourceInfo resourceInfo;
  @Context private HttpServletRequest servletRequest;

  private AuditService auditService;
  private AuditHelper auditHelper;
  private AuthService authService;
  private AuthHandler authHandler;
  private UserService userService;
  private AppService appService;
  private FeatureFlagService featureFlagService;
  private WhitelistService whitelistService;
  private HarnessUserGroupService harnessUserGroupService;

  /**
   * Instantiates a new Auth rule filter.
   *
   * @param auditService the audit service
   * @param auditHelper  the audit helper
   * @param authService  the auth service
   * @param appService   the appService
   * @param userService  the userService
   * @param featureFlagService
   * @param whitelistService
   */
  @Inject
  public AuthRuleFilter(AuditService auditService, AuditHelper auditHelper, AuthService authService,
      AuthHandler authHandler, AppService appService, UserService userService, FeatureFlagService featureFlagService,
      WhitelistService whitelistService, HarnessUserGroupService harnessUserGroupService) {
    this.auditService = auditService;
    this.auditHelper = auditHelper;
    this.authService = authService;
    this.authHandler = authHandler;
    this.appService = appService;
    this.userService = userService;
    this.featureFlagService = featureFlagService;
    this.whitelistService = whitelistService;
    this.harnessUserGroupService = harnessUserGroupService;
  }

  /* (non-Javadoc)
   * @see javax.ws.rs.container.ContainerRequestFilter#filter(javax.ws.rs.container.ContainerRequestContext)
   */
  @Override
  public void filter(ContainerRequestContext requestContext) {
    if (authorizationExemptedRequest(requestContext)) {
      return; // do nothing
    }

    if (isDelegateRequest(requestContext) || isLearningEngineServiceRequest(requestContext)) {
      return;
    }

    MultivaluedMap<String, String> pathParameters = requestContext.getUriInfo().getPathParameters();
    MultivaluedMap<String, String> queryParameters = requestContext.getUriInfo().getQueryParameters();

    String accountId = getRequestParamFromContext("accountId", pathParameters, queryParameters);
    List<String> appIdsFromRequest = getRequestParamsFromContext("appId", pathParameters, queryParameters);
    boolean emptyAppIdsInReq = isEmpty(appIdsFromRequest);

    if (!emptyAppIdsInReq && accountId == null) {
      // Ideally, we should force all the apis to have accountId as a mandatory field and not have this code.
      // But, since there might be some paths hitting this condition already, pulling up account from the first appId.
      accountId = appService.get(appIdsFromRequest.get(0)).getAccountId();
    }

    String uriPath = requestContext.getUriInfo().getPath();
    // TODO change this to Annotation based
    if (accountId == null
        && (uriPath.startsWith("users/user") || uriPath.startsWith("users/sso/zendesk")
               || uriPath.startsWith("users/account") || uriPath.endsWith("/logout")
               || uriPath.startsWith("users/two-factor-auth") || uriPath.startsWith("users/disable-two-factor-auth")
               || uriPath.startsWith("users/enable-two-factor-auth") || uriPath.startsWith("users/refresh-token"))) {
      return;
    }

    User user = UserThreadLocal.get();

    // TODO check if we should throw the exception if user is null
    if (user == null) {
      logger.error("No user context in operation: {}", uriPath);
    }

    String httpMethod = requestContext.getMethod();
    List<PermissionAttribute> requiredPermissionAttributes;
    if (user != null) {
      if (!userService.isUserAssignedToAccount(user, accountId)) {
        if (!httpMethod.equals(HttpMethod.GET.name())) {
          throw new InvalidRequestException("User not authorized", USER);
        }

        Set<Action> actions = harnessUserGroupService.listAllowedUserActionsForAccount(accountId, user.getUuid());
        if (isEmpty(actions)) {
          throw new InvalidRequestException("User not authorized", USER);
        }
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
      if (user != null) {
        UserRequestContextBuilder userRequestContextBuilder =
            UserRequestContext.builder().accountId(accountId).entityInfoMap(Maps.newHashMap());

        UserPermissionInfo userPermissionInfo = authService.getUserPermissionInfo(accountId, user);
        userRequestContextBuilder.userPermissionInfo(userPermissionInfo);

        Set<String> allowedAppIds = getAllowedAppIds(userPermissionInfo);
        setAppIdFilterInUserRequestContext(userRequestContextBuilder, emptyAppIdsInReq, allowedAppIds);

        user.setUserRequestContext(userRequestContextBuilder.build());
      }

      return;
    }

    if (allLoggedInScope(requiredPermissionAttributes)) {
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
        UserPermissionInfo userPermissionInfo = userRequestContext.getUserPermissionInfo();
        if (userPermissionInfo == null) {
          throw new InvalidRequestException("User not authorized", USER);
        }

        AccountPermissionSummary accountPermissionSummary = userPermissionInfo.getAccountPermissionSummary();
        if (accountPermissionSummary == null) {
          throw new InvalidRequestException("User not authorized", USER);
        }

        Set<PermissionType> accountPermissions = accountPermissionSummary.getPermissions();
        if (accountPermissions == null) {
          throw new InvalidRequestException("User not authorized", USER);
        }

        if (isAuthorized(requiredPermissionAttributes, accountPermissions)) {
          return;
        } else {
          throw new InvalidRequestException("User not authorized", USER);
        }
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

  private boolean isAuthorized(List<PermissionAttribute> permissionAttributes, Set<PermissionType> accountPermissions) {
    return permissionAttributes.stream().anyMatch(
        permissionAttribute -> accountPermissions.contains(permissionAttribute.getPermissionType()));
  }

  private boolean isAccountLevelPermissions(List<PermissionAttribute> permissionAttributes) {
    return permissionAttributes.stream().anyMatch(
        permissionAttribute -> isAccountLevelPermissions(permissionAttribute.getPermissionType()));
  }

  private boolean isAccountLevelPermissions(PermissionType permissionType) {
    return PermissionType.APPLICATION_CREATE_DELETE == permissionType
        || PermissionType.USER_PERMISSION_MANAGEMENT == permissionType
        || PermissionType.ACCOUNT_MANAGEMENT == permissionType;
  }

  private String getEntityIdFromRequest(List<PermissionAttribute> permissionAttributes,
      MultivaluedMap<String, String> pathParameters, MultivaluedMap<String, String> queryParameters) {
    String parameterName = getParameterName(permissionAttributes);
    if (parameterName != null) {
      return getEntityId(pathParameters, queryParameters, parameterName);
    }

    return null;
  }

  private String getParameterName(List<PermissionAttribute> permissionAttributes) {
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

  private Set<String> getAllowedAppIds(UserPermissionInfo userPermissionInfo) {
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
    UserRequestContextBuilder userRequestContextBuilder =
        UserRequestContext.builder().accountId(accountId).entityInfoMap(Maps.newHashMap());

    UserPermissionInfo userPermissionInfo = authService.getUserPermissionInfo(accountId, user);
    userRequestContextBuilder.userPermissionInfo(userPermissionInfo);

    Set<String> allowedAppIds = getAllowedAppIds(userPermissionInfo);

    boolean appIdFilterRequired =
        setAppIdFilterInUserRequestContext(userRequestContextBuilder, emptyAppIdsInReq, allowedAppIds);

    UserRequestContext userRequestContext = userRequestContextBuilder.build();

    if (!accountLevelPermissions) {
      authHandler.setEntityIdFilterIfGet(httpMethod, skipAuth, requiredPermissionAttributes, userRequestContext,
          appIdFilterRequired, allowedAppIds, appIdsFromRequest);
    }
    return userRequestContext;
  }

  private boolean skipAuth(List<PermissionAttribute> requiredPermissionAttributes) {
    if (CollectionUtils.isEmpty(requiredPermissionAttributes)) {
      return false;
    }

    return requiredPermissionAttributes.stream().anyMatch(permissionAttribute -> permissionAttribute.isSkipAuth());
  }

  private boolean setAppIdFilterInUserRequestContext(
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

  private void setUserRequestInfoBasedOnRole(UserRequestInfoBuilder userRequestInfoBuilder, User user, String accountId,
      List<String> appIdsFromRequest, boolean emptyAppIdsInReq, List<String> appIdsOfAccount) {
    if (user.isAccountAdmin(accountId) || user.isAllAppAdmin(accountId)) {
      userRequestInfoBuilder.allAppsAllowed(true).allEnvironmentsAllowed(true);
      List<ResourceType> requiredResourceTypes = getAllResourceTypes();
      if (emptyAppIdsInReq && isPresent(requiredResourceTypes, ResourceType.APPLICATION)) {
        userRequestInfoBuilder.appIdFilterRequired(true).allowedAppIds(ImmutableList.copyOf(appIdsOfAccount));
      }
    } else {
      // TODO:
      logger.info("User [{}] is neither account admin nor all app admin", user.getUuid());
      AccountRole userAccountRole = userService.getUserAccountRole(user.getUuid(), accountId);
      ImmutableList<String> allowedAppIds = ImmutableList.<String>builder().build();

      if (userAccountRole == null) {
        logger.info("No account role exist for user [{}]", user.getUuid());
      } else {
        logger.info("User account role [{}]", userAccountRole);
        allowedAppIds = copyOf(
            userAccountRole.getApplicationRoles().stream().map(ApplicationRole::getAppId).distinct().collect(toList()));
      }

      if (!emptyAppIdsInReq) {
        for (String appId : appIdsFromRequest) {
          if (user.isAppAdmin(accountId, appId)) {
            userRequestInfoBuilder.allEnvironmentsAllowed(true);
          } else {
            ApplicationRole applicationRole = userService.getUserApplicationRole(user.getUuid(), appId);
            if (applicationRole != null) {
              List<EnvironmentRole> environmentRoles = applicationRole.getEnvironmentRoles();
              if (environmentRoles != null) {
                ImmutableList<String> envIds =
                    copyOf(environmentRoles.stream().map(EnvironmentRole::getEnvId).distinct().collect(toList()));
                userRequestInfoBuilder.allEnvironmentsAllowed(false).allowedEnvIds(envIds);
              } else {
                userRequestInfoBuilder.allEnvironmentsAllowed(false).allowedEnvIds(
                    ImmutableList.<String>builder().build());
              }
            } else {
              userRequestInfoBuilder.allEnvironmentsAllowed(false).allowedEnvIds(
                  ImmutableList.<String>builder().build());
            }
          }
        }
      }

      userRequestInfoBuilder.allAppsAllowed(false).allowedAppIds(allowedAppIds);
    }
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

  private boolean authorizationExemptedRequest(ContainerRequestContext requestContext) {
    return publicAPI() || requestContext.getMethod().equals(OPTIONS)
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

  private boolean publicAPI() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method resourceMethod = resourceInfo.getResourceMethod();

    return resourceMethod.getAnnotation(PublicApi.class) != null
        || resourceClass.getAnnotation(PublicApi.class) != null;
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

  private List<PermissionAttribute> getAllRequiredPermissionAttributes(ContainerRequestContext requestContext) {
    List<PermissionAttribute> methodPermissionAttributes = new ArrayList<>();
    List<PermissionAttribute> classPermissionAttributes = new ArrayList<>();

    Method resourceMethod = resourceInfo.getResourceMethod();
    AuthRule methodAnnotations = resourceMethod.getAnnotation(AuthRule.class);
    if (null != methodAnnotations) {
      methodPermissionAttributes.add(new PermissionAttribute(null, methodAnnotations.permissionType(),
          getAction(methodAnnotations, requestContext.getMethod()), requestContext.getMethod(),
          methodAnnotations.parameterName(), methodAnnotations.dbFieldName(), methodAnnotations.dbCollectionName(),
          methodAnnotations.skipAuth()));
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
}
