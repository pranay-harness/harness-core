package software.wings.security;

import static com.google.common.collect.ImmutableList.copyOf;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.Priorities.AUTHENTICATION;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static software.wings.beans.ErrorCode.ACCESS_DENIED;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.beans.ErrorCode.INVALID_TOKEN;
import static software.wings.beans.ResponseMessage.Acuteness.ALERTING;
import static software.wings.beans.ResponseMessage.Acuteness.HARMLESS;
import static software.wings.beans.ResponseMessage.aResponseMessage;
import static software.wings.security.UserRequestInfo.UserRequestInfoBuilder.anUserRequestInfo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AccountRole;
import software.wings.beans.ApplicationRole;
import software.wings.beans.AuthToken;
import software.wings.beans.EnvironmentRole;
import software.wings.beans.ResponseMessage;
import software.wings.beans.User;
import software.wings.common.AuditHelper;
import software.wings.exception.WingsException;
import software.wings.security.PermissionAttribute.PermissionScope;
import software.wings.security.UserRequestInfo.UserRequestInfoBuilder;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.security.annotations.PublicApi;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.UserService;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Priority;
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
@Priority(AUTHENTICATION)
public class AuthRuleFilter implements ContainerRequestFilter {
  private static final Logger logger = LoggerFactory.getLogger(AuthRuleFilter.class);

  @Context private ResourceInfo resourceInfo;

  private AuditService auditService;
  private AuditHelper auditHelper;
  private AuthService authService;
  private UserService userService;
  private AppService appService;

  /**
   * Instantiates a new Auth rule filter.
   *
   * @param auditService the audit service
   * @param auditHelper  the audit helper
   * @param authService  the auth service
   * @param appService   the appService
   * @param userService  the userService
   */
  @Inject
  public AuthRuleFilter(AuditService auditService, AuditHelper auditHelper, AuthService authService,
      AppService appService, UserService userService) {
    this.auditService = auditService;
    this.auditHelper = auditHelper;
    this.authService = authService;
    this.appService = appService;
    this.userService = userService;
  }

  /* (non-Javadoc)
   * @see javax.ws.rs.container.ContainerRequestFilter#filter(javax.ws.rs.container.ContainerRequestContext)
   */
  @Override
  public void filter(ContainerRequestContext requestContext) {
    if (authorizationExemptedRequest(requestContext)) {
      return; // do nothing
    }
    List<PermissionAttribute> requiredPermissionAttributes = getAllRequiredPermissionAttributes(requestContext);
    if (isEmpty(requiredPermissionAttributes)) {
      logger.error("Requested Resource: {}", requestContext.getUriInfo().getPath());
      throw new WingsException(ACCESS_DENIED);
    }

    MultivaluedMap<String, String> pathParameters = requestContext.getUriInfo().getPathParameters();
    MultivaluedMap<String, String> queryParameters = requestContext.getUriInfo().getQueryParameters();

    if (isDelegateRequest(requestContext)) {
      String accountId = getRequestParamFromContext("accountId", pathParameters, queryParameters);
      String header = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
      if (header.contains("Delegate")) {
        authService.validateDelegateToken(
            accountId, substringAfter(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION), "Delegate "));
      } else {
        throw new IllegalStateException("Invalid header:" + header);
      }

      return;
    }

    if (isDelegateRequest(requestContext)) {
      String accountId = getRequestParamFromContext("accountId", pathParameters, queryParameters);
      authService.validateDelegateToken(
          accountId, substringAfter(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION), "Delegate "));
      return; // do nothing
    }

    if (isLearningEngineServiceRequest(requestContext)) {
      authService.validateLearningEngineServiceToken(
          substringAfter(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION), "LearningEngine "));
      return; // do nothing
    }

    String tokenString = extractToken(requestContext);
    AuthToken authToken = authService.validateToken(tokenString);
    User user = authToken.getUser();
    if (user != null) {
      requestContext.setProperty("USER", user);
      updateUserInAuditRecord(user); // FIXME: find better place
      UserThreadLocal.set(user);
    }

    if (allLoggedInScope(requiredPermissionAttributes)) {
      return;
    }

    String accountId = getRequestParamFromContext("accountId", pathParameters, queryParameters);
    List<String> appIdsFromRequest = getRequestParamsFromContext("appId", pathParameters, queryParameters);
    boolean emptyAppIdsInReq = isEmpty(appIdsFromRequest);
    String envId = getRequestParamFromContext("envId", pathParameters, queryParameters);

    if (!emptyAppIdsInReq && accountId == null) {
      // Ideally, we should force all the apis to have accountId as a mandatory field and not have this code.
      // But, since there might be some paths hitting this condition already, pulling up account from the first appId.
      accountId = appService.get(appIdsFromRequest.get(0)).getAccountId();
    }

    if (accountId == null) {
      if (emptyAppIdsInReq) {
        throw new WingsException(INVALID_REQUEST).addParam("message", "appId not specified");
      }
      throw new WingsException(INVALID_REQUEST).addParam("message", "accountId not specified");
    }

    if (user != null) {
      final String accountIdFinal = accountId;
      if (user.getAccounts().stream().filter(account -> account.getUuid().equals(accountIdFinal)).count() != 1) {
        throw new WingsException(ResponseMessage.aResponseMessage().code(INVALID_REQUEST).acuteness(HARMLESS).build())
            .addParam("message", "User not authorized to access the given account");
      }
    }

    List<String> appIdsOfAccount = getValidAppsFromAccount(accountId, appIdsFromRequest, emptyAppIdsInReq);
    UserRequestInfoBuilder userRequestInfoBuilder =
        anUserRequestInfo()
            .withAccountId(accountId)
            .withAppIds(appIdsFromRequest)
            .withEnvId(envId)
            .withPermissionAttributes(ImmutableList.copyOf(requiredPermissionAttributes));

    setUserRequestInfoBasedOnRole(requiredPermissionAttributes, appIdsOfAccount, userRequestInfoBuilder, user,
        accountId, appIdsFromRequest, emptyAppIdsInReq);

    UserRequestInfo userRequestInfo = userRequestInfoBuilder.build();
    user.setUserRequestInfo(userRequestInfo);

    authService.authorize(accountId, appIdsFromRequest, envId, user, requiredPermissionAttributes, userRequestInfo);
  }

  private List<String> getValidAppsFromAccount(
      String accountId, List<String> appIdsFromRequest, boolean emptyAppIdsInReq) {
    List<String> appIdsOfAccount = appService.getAppIdsByAccountId(accountId);
    if (isNotEmpty(appIdsOfAccount)) {
      if (!emptyAppIdsInReq) {
        List<String> invalidAppIdList = Lists.newArrayList();
        for (String appId : appIdsFromRequest) {
          if (!appIdsOfAccount.contains(appId)) {
            invalidAppIdList.add(appId);
          }
        }

        if (!invalidAppIdList.isEmpty()) {
          String msg = "The appIds from request %s do not belong to the given account :" + accountId;
          String formattedMsg = String.format(msg, (Object[]) invalidAppIdList.toArray());
          throw new WingsException(INVALID_ARGUMENT).addParam("args", formattedMsg);
        }
      }
    }
    return appIdsOfAccount;
  }

  private void setUserRequestInfoBasedOnRole(List<PermissionAttribute> requiredPermissionAttributes,
      List<String> appIdsOfAccount, UserRequestInfoBuilder userRequestInfoBuilder, User user, String accountId,
      List<String> appIds, boolean emptyAppIdsInReq) {
    if (user.isAccountAdmin(accountId) || user.isAllAppAdmin(accountId)) {
      userRequestInfoBuilder.withAllAppsAllowed(true).withAllEnvironmentsAllowed(true);
      if (emptyAppIdsInReq && isPresent(requiredPermissionAttributes, PermissionScope.APP)) {
        userRequestInfoBuilder.withAppIdFilterRequired(true).withAllowedAppIds(ImmutableList.copyOf(appIdsOfAccount));
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
        allowedAppIds = copyOf(userAccountRole.getApplicationRoles()
                                   .stream()
                                   .map(ApplicationRole::getAppId)
                                   .distinct()
                                   .collect(Collectors.toList()));
      }

      if (!emptyAppIdsInReq) {
        for (String appId : appIds) {
          if (user.isAppAdmin(accountId, appId)) {
            userRequestInfoBuilder.withAllEnvironmentsAllowed(true);
          } else {
            ApplicationRole applicationRole = userService.getUserApplicationRole(user.getUuid(), appId);
            if (applicationRole != null) {
              List<EnvironmentRole> environmentRoles = applicationRole.getEnvironmentRoles();
              if (environmentRoles != null) {
                ImmutableList<String> envIds = copyOf(
                    environmentRoles.stream().map(EnvironmentRole::getEnvId).distinct().collect(Collectors.toList()));
                userRequestInfoBuilder.withAllEnvironmentsAllowed(false).withAllowedEnvIds(envIds);
              } else {
                userRequestInfoBuilder.withAllEnvironmentsAllowed(false).withAllowedEnvIds(
                    ImmutableList.<String>builder().build());
              }
            } else {
              userRequestInfoBuilder.withAllEnvironmentsAllowed(false).withAllowedEnvIds(
                  ImmutableList.<String>builder().build());
            }
          }
        }
      }

      userRequestInfoBuilder.withAllAppsAllowed(false).withAllowedAppIds(allowedAppIds);
    }
  }

  private boolean isPresent(List<PermissionAttribute> requiredPermissionAttributes, PermissionScope permissionScope) {
    for (PermissionAttribute permissionAttribute : requiredPermissionAttributes) {
      if (permissionAttribute.getScope() == permissionScope) {
        return true;
      }
    }
    return false;
  }

  private boolean allLoggedInScope(List<PermissionAttribute> requiredPermissionAttributes) {
    return !requiredPermissionAttributes.parallelStream()
                .filter(permissionAttribute -> permissionAttribute.getScope() != PermissionScope.LOGGED_IN)
                .findFirst()
                .isPresent();
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

  private List<PermissionAttribute> getAllRequiredPermissionAttributes(ContainerRequestContext requestContext) {
    List<PermissionAttribute> methodPermissionAttributes = new ArrayList<>();
    List<PermissionAttribute> classPermissionAttributes = new ArrayList<>();

    Method resourceMethod = resourceInfo.getResourceMethod();
    AuthRule methodAnnotations = resourceMethod.getAnnotation(AuthRule.class);
    if (null != methodAnnotations) {
      Stream.of(methodAnnotations.value())
          .forEach(s
              -> methodPermissionAttributes.add(
                  new PermissionAttribute(s, methodAnnotations.scope(), requestContext.getMethod())));
    }

    Class<?> resourceClass = resourceInfo.getResourceClass();
    AuthRule classAnnotations = resourceClass.getAnnotation(AuthRule.class);
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

  private void updateUserInAuditRecord(User user) {
    auditService.updateUser(auditHelper.get(), user.getPublicUser());
  }

  private String extractToken(ContainerRequestContext requestContext) {
    String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      throw new WingsException(aResponseMessage().code(INVALID_TOKEN).acuteness(ALERTING).build());
    }
    return authorizationHeader.substring("Bearer".length()).trim();
  }
}
