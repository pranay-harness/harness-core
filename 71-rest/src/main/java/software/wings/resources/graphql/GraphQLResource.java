package software.wings.resources.graphql;

import static io.harness.eraro.ErrorCode.INVALID_TOKEN;
import static io.harness.exception.WingsException.USER_ADMIN;
import static software.wings.security.AuthenticationFilter.API_KEY_HEADER;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import es.moki.ratelimitj.core.limiter.request.RequestLimitRule;
import es.moki.ratelimitj.core.limiter.request.RequestRateLimiter;
import es.moki.ratelimitj.inmemory.request.InMemorySlidingWindowRequestRateLimiter;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQL;
import graphql.GraphQLContext;
import graphql.GraphqlErrorBuilder;
import io.harness.exception.WingsException;
import io.swagger.annotations.Api;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.ApiKeyEntry;
import software.wings.beans.FeatureName;
import software.wings.beans.User;
import software.wings.features.RestApiFeature;
import software.wings.features.api.PremiumFeature;
import software.wings.graphql.datafetcher.DataLoaderRegistryHelper;
import software.wings.graphql.provider.QueryLanguageProvider;
import software.wings.graphql.utils.GraphQLConstants;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserThreadLocal;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.ExternalFacingApiAuth;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.ApiKeyService;
import software.wings.service.intfc.FeatureFlagService;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Api("/graphql")
@Path("/graphql")
@Produces("application/json")
@Singleton
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GraphQLResource {
  private static final long RATE_LIMIT_QUERY_PER_MINUTE = 100;
  private static final long RATE_LIMIT_DURATION_IN_MINUTE = 1;
  RequestRateLimiter requestRateLimiter;
  GraphQL privateGraphQL;
  GraphQL publicGraphQL;
  FeatureFlagService featureFlagService;
  ApiKeyService apiKeyService;
  AuthHandler authHandler;
  AccountService accountService;
  PremiumFeature restApiFeature;
  DataLoaderRegistryHelper dataLoaderRegistryHelper;
  @Inject
  public GraphQLResource(@NotNull QueryLanguageProvider<GraphQL> queryLanguageProvider,
      @NotNull FeatureFlagService featureFlagService, @NotNull ApiKeyService apiKeyService,
      @NotNull AuthHandler authHandler, DataLoaderRegistryHelper dataLoaderRegistryHelper,
      @NotNull AccountService accountService, @Named(RestApiFeature.FEATURE_NAME) PremiumFeature restApiFeature) {
    this.privateGraphQL = queryLanguageProvider.getPrivateGraphQL();
    this.publicGraphQL = queryLanguageProvider.getPublicGraphQL();
    this.featureFlagService = featureFlagService;
    this.apiKeyService = apiKeyService;
    this.authHandler = authHandler;
    this.dataLoaderRegistryHelper = dataLoaderRegistryHelper;
    this.accountService = accountService;
    this.restApiFeature = restApiFeature;
    requestRateLimiter = new InMemorySlidingWindowRequestRateLimiter(Collections.singleton(
        RequestLimitRule.of(Duration.ofMinutes(RATE_LIMIT_DURATION_IN_MINUTE), RATE_LIMIT_QUERY_PER_MINUTE)));
  }

  @POST
  @Consumes(MediaType.TEXT_PLAIN)
  @ExternalFacingApiAuth
  public Map<String, Object> execute(@HeaderParam(API_KEY_HEADER) String apiKey, String query) {
    GraphQLQuery graphQLQuery = new GraphQLQuery();
    graphQLQuery.setQuery(query);
    return executeExternal(apiKey, graphQLQuery);
  }

  /**
   * GraphQL graphQLQuery can be sent as plain text
   * or as JSON hence I have added overloaded methods
   * to handle both cases.
   *
   * @param graphQLQuery
   * @return
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @ExternalFacingApiAuth
  public Map<String, Object> execute(@HeaderParam(API_KEY_HEADER) String apiKey, GraphQLQuery graphQLQuery) {
    return executeExternal(apiKey, graphQLQuery);
  }

  @Path("int")
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  @POST
  @Consumes(MediaType.TEXT_PLAIN)
  public Map<String, Object> execute(String query) {
    GraphQLQuery graphQLQuery = new GraphQLQuery();
    graphQLQuery.setQuery(query);
    return executeInternal(graphQLQuery);
  }

  /**
   * GraphQL graphQLQuery can be sent as plain text
   * or as JSON hence I have added overloaded methods
   * to handle both cases.
   *
   * @param graphQLQuery
   * @return
   */
  @Path("int")
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Map<String, Object> execute(GraphQLQuery graphQLQuery) {
    return executeInternal(graphQLQuery);
  }

  private Map<String, Object> executeExternal(String apiKey, GraphQLQuery graphQLQuery) {
    String accountId;
    boolean hasUserContext = false;
    UserPermissionInfo userPermissionInfo = null;
    if (apiKey == null || (accountId = apiKeyService.getAccountIdFromApiKey(apiKey)) == null) {
      User user = UserThreadLocal.get();
      if (user != null) {
        accountId = user.getUserRequestContext().getAccountId();
        userPermissionInfo = user.getUserRequestContext().getUserPermissionInfo();
        hasUserContext = true;
      } else {
        logger.info(GraphQLConstants.INVALID_API_KEY);
        return getExecutionResultWithError(GraphQLConstants.INVALID_API_KEY).toSpecification();
      }
    }

    if (checkRateLimit(accountId)) {
      return getExecutionResultWithError(String.format(GraphQLConstants.RATE_LIMIT_REACHED, accountId))
          .toSpecification();
    }

    if (!featureFlagService.isEnabled(FeatureName.GRAPHQL, accountId)
        || !restApiFeature.isAvailableForAccount(accountId)) {
      logger.info(GraphQLConstants.FEATURE_NOT_ENABLED);
      return getExecutionResultWithError(GraphQLConstants.FEATURE_NOT_ENABLED).toSpecification();
    }

    ExecutionResult executionResult;
    try {
      GraphQL graphQL;
      if (featureFlagService.isEnabled(FeatureName.GRAPHQL_DEV, accountId)) {
        graphQL = privateGraphQL;
      } else {
        graphQL = publicGraphQL;
      }
      if (hasUserContext && userPermissionInfo != null) {
        executionResult =
            graphQL.execute(getExecutionInput(userPermissionInfo, accountId, graphQLQuery, dataLoaderRegistryHelper));
      } else {
        ApiKeyEntry apiKeyEntry = apiKeyService.getByKey(apiKey, accountId, true);
        if (apiKeyEntry == null) {
          executionResult = getExecutionResultWithError(GraphQLConstants.INVALID_API_KEY);
        } else {
          userPermissionInfo = authHandler.evaluateUserPermissionInfo(accountId, apiKeyEntry.getUserGroups(), null);
          executionResult =
              graphQL.execute(getExecutionInput(userPermissionInfo, accountId, graphQLQuery, dataLoaderRegistryHelper));
        }
      }
    } catch (Exception ex) {
      executionResult = handleException(accountId, ex);
    }

    return executionResult.toSpecification();
  }

  private ExecutionResult handleException(String accountId, Exception ex) {
    String errorMsg = String.format(
        "Error while handling api request for Graphql api for accountId %s : %s", accountId, ex.getMessage());
    logger.warn(errorMsg);
    return getExecutionResultWithError(errorMsg);
  }

  private boolean checkRateLimit(String accountId) {
    if (requestRateLimiter.overLimitWhenIncremented(accountId)) {
      return true;
    }
    return false;
  }

  private Map<String, Object> executeInternal(GraphQLQuery graphQLQuery) {
    String accountId;
    boolean hasUserContext = false;
    UserPermissionInfo userPermissionInfo = null;
    User user = UserThreadLocal.get();
    if (user != null) {
      accountId = user.getUserRequestContext().getAccountId();
      userPermissionInfo = user.getUserRequestContext().getUserPermissionInfo();
      hasUserContext = true;
    } else {
      throw new WingsException(INVALID_TOKEN, USER_ADMIN);
    }

    if (checkRateLimit(accountId)) {
      return getExecutionResultWithError(String.format(GraphQLConstants.RATE_LIMIT_REACHED, accountId))
          .toSpecification();
    }

    ExecutionResult executionResult;
    try {
      GraphQL graphQL = privateGraphQL;
      if (hasUserContext && userPermissionInfo != null) {
        executionResult =
            graphQL.execute(getExecutionInput(userPermissionInfo, accountId, graphQLQuery, dataLoaderRegistryHelper));
      } else {
        throw new WingsException(INVALID_TOKEN, USER_ADMIN);
      }
    } catch (Exception ex) {
      executionResult = handleException(accountId, ex);
    }

    return executionResult.toSpecification();
  }

  private ExecutionResultImpl getExecutionResultWithError(String message) {
    return ExecutionResultImpl.newExecutionResult()
        .addError(GraphqlErrorBuilder.newError().message(message).build())
        .build();
  }

  private ExecutionInput getExecutionInput(UserPermissionInfo userPermissionInfo, String accountId,
      GraphQLQuery graphQLQuery, DataLoaderRegistryHelper dataLoaderRegistryHelper) {
    return ExecutionInput.newExecutionInput()
        .query(graphQLQuery.getQuery())
        .variables(graphQLQuery.getVariables() == null ? Maps.newHashMap() : graphQLQuery.getVariables())
        .operationName(graphQLQuery.getOperationName())
        .dataLoaderRegistry(dataLoaderRegistryHelper.getDataLoaderRegistry())
        .context(GraphQLContext.newContext().of("auth", userPermissionInfo, "accountId", accountId))
        .build();
  }
}