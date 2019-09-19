package software.wings.resources.graphql;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.INVALID_TOKEN;
import static io.harness.exception.WingsException.USER_ADMIN;
import static software.wings.security.AuthenticationFilter.API_KEY_HEADER;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

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
import software.wings.audit.AuditSkip;
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
import software.wings.security.UserRequestContext;
import software.wings.security.UserRestrictionInfo;
import software.wings.security.UserThreadLocal;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.ExternalFacingApiAuth;
import software.wings.service.intfc.ApiKeyService;
import software.wings.service.intfc.FeatureFlagService;

import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Api("/graphql")
@Path("/graphql")
@Produces("application/json")
@Singleton
@AuditSkip
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GraphQLResource {
  GraphQL privateGraphQL;
  GraphQL publicGraphQL;
  FeatureFlagService featureFlagService;
  ApiKeyService apiKeyService;
  PremiumFeature restApiFeature;
  DataLoaderRegistryHelper dataLoaderRegistryHelper;
  GraphQLRateLimiter graphQLRateLimiter;

  @Inject
  public GraphQLResource(@NotNull QueryLanguageProvider<GraphQL> queryLanguageProvider,
      @NotNull FeatureFlagService featureFlagService, @NotNull ApiKeyService apiKeyService,
      DataLoaderRegistryHelper dataLoaderRegistryHelper, @NotNull GraphQLRateLimiter graphQLRateLimiter,
      @Named(RestApiFeature.FEATURE_NAME) PremiumFeature restApiFeature) {
    this.privateGraphQL = queryLanguageProvider.getPrivateGraphQL();
    this.publicGraphQL = queryLanguageProvider.getPublicGraphQL();
    this.featureFlagService = featureFlagService;
    this.apiKeyService = apiKeyService;
    this.dataLoaderRegistryHelper = dataLoaderRegistryHelper;
    this.restApiFeature = restApiFeature;
    this.graphQLRateLimiter = graphQLRateLimiter;
  }

  @POST
  @Consumes(MediaType.TEXT_PLAIN)
  @ExternalFacingApiAuth
  public Map<String, Object> execute(
      @HeaderParam(API_KEY_HEADER) String apiKey, @QueryParam("accountId") String accountId, String query) {
    GraphQLQuery graphQLQuery = new GraphQLQuery();
    graphQLQuery.setQuery(query);
    return executeExternal(accountId, apiKey, graphQLQuery);
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
  public Map<String, Object> execute(@HeaderParam(API_KEY_HEADER) String apiKey,
      @QueryParam("accountId") String accountId, GraphQLQuery graphQLQuery) {
    return executeExternal(accountId, apiKey, graphQLQuery);
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

  private Map<String, Object> executeExternal(
      String accountIdFromQueryParam, String apiKey, GraphQLQuery graphQLQuery) {
    String accountId;
    boolean hasUserContext = false;
    UserRequestContext userRequestContext = null;
    User user = UserThreadLocal.get();
    if (user != null) {
      accountId = user.getUserRequestContext().getAccountId();
      userRequestContext = user.getUserRequestContext();
      hasUserContext = true;
    } else if (isNotEmpty(apiKey)) {
      accountId = apiKeyService.getAccountIdFromApiKey(apiKey);
      if (accountId == null) {
        accountId = accountIdFromQueryParam;
      }

      if (accountId == null) {
        logger.info(GraphQLConstants.INVALID_API_KEY);
        return getExecutionResultWithError(GraphQLConstants.INVALID_API_KEY).toSpecification();
      }
    } else {
      logger.info(GraphQLConstants.INVALID_API_KEY);
      return getExecutionResultWithError(GraphQLConstants.INVALID_API_KEY).toSpecification();
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
      if (hasUserContext) {
        executionResult = graphQL.execute(getExecutionInput(userRequestContext.getUserPermissionInfo(),
            userRequestContext.getUserRestrictionInfo(), accountId, graphQLQuery, dataLoaderRegistryHelper));
      } else {
        ApiKeyEntry apiKeyEntry = apiKeyService.getByKey(apiKey, accountId, true);
        if (apiKeyEntry == null) {
          executionResult = getExecutionResultWithError(GraphQLConstants.INVALID_API_KEY);
        } else {
          UserPermissionInfo apiKeyPermissions = apiKeyService.getApiKeyPermissions(apiKeyEntry, accountId);
          UserRestrictionInfo apiKeyRestrictions =
              apiKeyService.getApiKeyRestrictions(apiKeyEntry, apiKeyPermissions, accountId);
          executionResult = graphQL.execute(getExecutionInput(
              apiKeyPermissions, apiKeyRestrictions, accountId, graphQLQuery, dataLoaderRegistryHelper));
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

  private Map<String, Object> executeInternal(GraphQLQuery graphQLQuery) {
    String accountId;
    boolean hasUserContext;
    UserRequestContext userRequestContext;
    User user = UserThreadLocal.get();
    if (user != null) {
      accountId = user.getUserRequestContext().getAccountId();
      userRequestContext = user.getUserRequestContext();
      hasUserContext = true;
    } else {
      throw new WingsException(INVALID_TOKEN, USER_ADMIN);
    }

    ExecutionResult executionResult;
    try {
      GraphQL graphQL = privateGraphQL;
      if (hasUserContext && userRequestContext != null) {
        executionResult = graphQL.execute(getExecutionInput(userRequestContext.getUserPermissionInfo(),
            userRequestContext.getUserRestrictionInfo(), accountId, graphQLQuery, dataLoaderRegistryHelper));
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

  private ExecutionInput getExecutionInput(UserPermissionInfo permissionInfo, UserRestrictionInfo restrictionInfo,
      String accountId, GraphQLQuery graphQLQuery, DataLoaderRegistryHelper dataLoaderRegistryHelper) {
    return ExecutionInput.newExecutionInput()
        .query(graphQLQuery.getQuery())
        .variables(graphQLQuery.getVariables() == null ? Maps.newHashMap() : graphQLQuery.getVariables())
        .operationName(graphQLQuery.getOperationName())
        .dataLoaderRegistry(dataLoaderRegistryHelper.getDataLoaderRegistry())
        .context(GraphQLContext.newContext().of(
            "accountId", accountId, "permissions", permissionInfo, "restrictions", restrictionInfo))
        .build();
  }
}