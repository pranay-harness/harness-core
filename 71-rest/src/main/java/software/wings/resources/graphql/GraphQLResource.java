package software.wings.resources.graphql;

import static software.wings.security.AuthenticationFilter.API_KEY_HEADER;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import es.moki.ratelimitj.core.limiter.request.RequestLimitRule;
import es.moki.ratelimitj.core.limiter.request.RequestRateLimiter;
import es.moki.ratelimitj.inmemory.request.InMemorySlidingWindowRequestRateLimiter;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQL;
import graphql.GraphQLContext;
import graphql.GraphqlErrorBuilder;
import io.swagger.annotations.Api;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.ApiKeyEntry;
import software.wings.beans.FeatureName;
import software.wings.graphql.datafetcher.DataLoaderRegistryHelper;
import software.wings.graphql.provider.QueryLanguageProvider;
import software.wings.graphql.utils.GraphQLConstants;
import software.wings.security.UserPermissionInfo;
import software.wings.security.annotations.PublicApi;
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
@PublicApi
@Singleton
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GraphQLResource {
  private static final long RATE_LIMIT_QUERY_PER_MINUTE = 100;
  private static final long RATE_LIMIT_DURATION_IN_MINUTE = 1;
  RequestRateLimiter requestRateLimiter;
  GraphQL graphQL;
  FeatureFlagService featureFlagService;
  ApiKeyService apiKeyService;
  AuthHandler authHandler;
  AccountService accountService;
  DataLoaderRegistryHelper dataLoaderRegistryHelper;

  @Inject
  public GraphQLResource(@NotNull QueryLanguageProvider<GraphQL> queryLanguageProvider,
      @NotNull FeatureFlagService featureFlagService, @NotNull ApiKeyService apiKeyService,
      @NotNull AuthHandler authHandler, DataLoaderRegistryHelper dataLoaderRegistryHelper,
      @NotNull AccountService accountService) {
    this.graphQL = queryLanguageProvider.getQL();
    this.featureFlagService = featureFlagService;
    this.apiKeyService = apiKeyService;
    this.authHandler = authHandler;
    this.dataLoaderRegistryHelper = dataLoaderRegistryHelper;
    this.accountService = accountService;
    requestRateLimiter = new InMemorySlidingWindowRequestRateLimiter(Collections.singleton(
        RequestLimitRule.of(Duration.ofMinutes(RATE_LIMIT_DURATION_IN_MINUTE), RATE_LIMIT_QUERY_PER_MINUTE)));
  }

  @POST
  @Consumes(MediaType.TEXT_PLAIN)
  public Map<String, Object> execute(@HeaderParam(API_KEY_HEADER) String apiKey, String query) {
    GraphQLQuery graphQLQuery = new GraphQLQuery();
    graphQLQuery.setQuery(query);
    return executeInternal(apiKey, graphQLQuery);
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
  public Map<String, Object> execute(@HeaderParam(API_KEY_HEADER) String apiKey, GraphQLQuery graphQLQuery) {
    return executeInternal(apiKey, graphQLQuery);
  }

  private Map<String, Object> executeInternal(String apiKey, GraphQLQuery graphQLQuery) {
    String accountId;
    if (apiKey == null || (accountId = apiKeyService.getAccountIdFromApiKey(apiKey)) == null) {
      logger.info(GraphQLConstants.INVALID_API_KEY);
      return getExecutionResultWithError(GraphQLConstants.INVALID_API_KEY).toSpecification();
    }

    boolean isOverRateLimit = requestRateLimiter.overLimitWhenIncremented(accountId);
    if (isOverRateLimit) {
      String rateLiteErrorMsg = String.format(GraphQLConstants.RATE_LIMIT_REACHED, accountId);
      return getExecutionResultWithError(rateLiteErrorMsg).toSpecification();
    }

    if (!featureFlagService.isEnabled(FeatureName.GRAPHQL, null) || accountService.isCommunityAccount(accountId)) {
      logger.info(GraphQLConstants.FEATURE_NOT_ENABLED);
      return getExecutionResultWithError(GraphQLConstants.FEATURE_NOT_ENABLED).toSpecification();
    }

    ExecutionResult executionResult;
    try {
      ApiKeyEntry apiKeyEntry = apiKeyService.getByKey(apiKey, accountId, true);
      if (apiKeyEntry == null) {
        executionResult = getExecutionResultWithError(GraphQLConstants.INVALID_API_KEY);
      } else {
        UserPermissionInfo userPermissionInfo =
            authHandler.getUserPermissionInfo(accountId, apiKeyEntry.getUserGroups());
        executionResult = graphQL.execute(getExecutionInput(userPermissionInfo, accountId, graphQLQuery));
      }
    } catch (Exception ex) {
      String errorMsg = String.format(
          "Error while handling api request for Graphql api for accountId %s : %s", accountId, ex.getMessage());
      logger.warn(errorMsg);
      executionResult = getExecutionResultWithError(errorMsg);
    }

    return executionResult.toSpecification();
  }

  private ExecutionInput getExecutionInput(
      UserPermissionInfo userPermissionInfo, String accountId, GraphQLQuery graphQLQuery) {
    return ExecutionInput.newExecutionInput()
        .query(graphQLQuery.getQuery())
        .variables(graphQLQuery.getVariables() == null ? Maps.newHashMap() : graphQLQuery.getVariables())
        .operationName(graphQLQuery.getOperationName())
        .dataLoaderRegistry(dataLoaderRegistryHelper.getDataLoaderRegistry())
        .context(GraphQLContext.newContext().of("auth", userPermissionInfo, "accountId", accountId))
        .build();
  }

  private ExecutionResultImpl getExecutionResultWithError(String message) {
    return ExecutionResultImpl.newExecutionResult()
        .addError(GraphqlErrorBuilder.newError().message(message).build())
        .build();
  }
}