package software.wings.resources.graphql;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static software.wings.graphql.utils.GraphQLConstants.GRAPHQL_QUERY_STRING;
import static software.wings.graphql.utils.GraphQLConstants.HTTP_SERVLET_REQUEST;
import static software.wings.security.AuthenticationFilter.API_KEY_HEADER;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLContext;
import graphql.GraphQLContext.Builder;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
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

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
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
  GraphQLUtils graphQLUtils;

  @Inject
  public GraphQLResource(@NotNull QueryLanguageProvider<GraphQL> queryLanguageProvider,
      @NotNull FeatureFlagService featureFlagService, @NotNull ApiKeyService apiKeyService,
      DataLoaderRegistryHelper dataLoaderRegistryHelper,
      @Named(RestApiFeature.FEATURE_NAME) PremiumFeature restApiFeature, GraphQLUtils graphQLUtils) {
    this.privateGraphQL = queryLanguageProvider.getPrivateGraphQL();
    this.publicGraphQL = queryLanguageProvider.getPublicGraphQL();
    this.featureFlagService = featureFlagService;
    this.apiKeyService = apiKeyService;
    this.dataLoaderRegistryHelper = dataLoaderRegistryHelper;
    this.restApiFeature = restApiFeature;
    this.graphQLUtils = graphQLUtils;
  }

  @POST
  @Consumes(MediaType.TEXT_PLAIN)
  @ExternalFacingApiAuth
  public Map<String, Object> execute(@HeaderParam(API_KEY_HEADER) String apiKey,
      @QueryParam("accountId") String accountId, String query, @Context HttpServletRequest httpServletRequest) {
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Executing graphql query");
      GraphQLQuery graphQLQuery = new GraphQLQuery();
      graphQLQuery.setQuery(query);
      return executeExternal(accountId, apiKey, graphQLQuery, httpServletRequest);
    }
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
      @QueryParam("accountId") String accountId, GraphQLQuery graphQLQuery,
      @Context HttpServletRequest httpServletRequest) {
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Executing graphql query");
      return executeExternal(accountId, apiKey, graphQLQuery, httpServletRequest);
    }
  }

  @Path("int")
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  @POST
  @Consumes(MediaType.TEXT_PLAIN)
  public Map<String, Object> execute(String query, @Context HttpServletRequest httpServletRequest) {
    GraphQLQuery graphQLQuery = new GraphQLQuery();
    graphQLQuery.setQuery(query);
    return executeInternal(graphQLQuery, httpServletRequest);
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
  public Map<String, Object> execute(GraphQLQuery graphQLQuery, @Context HttpServletRequest httpServletRequest) {
    return executeInternal(graphQLQuery, httpServletRequest);
  }

  private Map<String, Object> executeExternal(
      String accountIdFromQueryParam, String apiKey, GraphQLQuery graphQLQuery, HttpServletRequest httpServletRequest) {
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
        throw graphQLUtils.getInvalidApiKeyException();
      }
    } else {
      logger.info(GraphQLConstants.INVALID_API_KEY);
      throw graphQLUtils.getInvalidApiKeyException();
    }

    if (!featureFlagService.isEnabled(FeatureName.GRAPHQL, accountId)
        || !restApiFeature.isAvailableForAccount(accountId)) {
      logger.info(GraphQLConstants.FEATURE_NOT_ENABLED);
      throw graphQLUtils.getFeatureNotEnabledException();
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
        final Builder contextBuilder = populateContextBuilder(GraphQLContext.newContext(),
            userRequestContext.getUserPermissionInfo(), userRequestContext.getUserRestrictionInfo(), accountId,
            user.getUuid(), TriggeredByType.USER, httpServletRequest, graphQLQuery.getQuery());
        executionResult = graphQL.execute(getExecutionInput(contextBuilder, graphQLQuery, dataLoaderRegistryHelper));
      } else {
        ApiKeyEntry apiKeyEntry = apiKeyService.getByKey(apiKey, accountId, true);
        if (apiKeyEntry == null) {
          throw graphQLUtils.getInvalidApiKeyException();
        } else {
          UserPermissionInfo apiKeyPermissions = apiKeyService.getApiKeyPermissions(apiKeyEntry, accountId);
          UserRestrictionInfo apiKeyRestrictions =
              apiKeyService.getApiKeyRestrictions(apiKeyEntry, apiKeyPermissions, accountId);
          final Builder contextBuilder =
              populateContextBuilder(GraphQLContext.newContext(), apiKeyPermissions, apiKeyRestrictions, accountId,
                  apiKeyEntry.getUuid(), TriggeredByType.API_KEY, httpServletRequest, graphQLQuery.getQuery());
          executionResult = graphQL.execute(getExecutionInput(contextBuilder, graphQLQuery, dataLoaderRegistryHelper));
        }
      }
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception ex) {
      executionResult = handleException(accountId, ex);
    }

    return executionResult.toSpecification();
  }

  private ExecutionResult handleException(String accountId, Exception ex) {
    String errorMsg = String.format(
        "Error while handling api request for Graphql api for accountId %s : %s", accountId, ex.getMessage());
    logger.warn(errorMsg);
    throw graphQLUtils.getException(errorMsg, ex);
  }

  private Map<String, Object> executeInternal(GraphQLQuery graphQLQuery, HttpServletRequest httpServletRequest) {
    String accountId;
    boolean hasUserContext;
    UserRequestContext userRequestContext;
    User user = UserThreadLocal.get();
    if (user != null) {
      accountId = user.getUserRequestContext().getAccountId();
      userRequestContext = user.getUserRequestContext();
      hasUserContext = true;
    } else {
      throw graphQLUtils.getInvalidTokenException();
    }

    ExecutionResult executionResult;
    try {
      GraphQL graphQL = privateGraphQL;
      if (hasUserContext && userRequestContext != null) {
        final Builder contextBuilder = populateContextBuilder(GraphQLContext.newContext(),
            userRequestContext.getUserPermissionInfo(), userRequestContext.getUserRestrictionInfo(), accountId,
            user.getUuid(), TriggeredByType.USER, httpServletRequest, graphQLQuery.getQuery());
        executionResult = graphQL.execute(getExecutionInput(contextBuilder, graphQLQuery, dataLoaderRegistryHelper));
      } else {
        throw graphQLUtils.getInvalidTokenException();
      }
    } catch (Exception ex) {
      executionResult = handleException(accountId, ex);
    }

    return executionResult.toSpecification();
  }

  private ExecutionInput getExecutionInput(GraphQLContext.Builder contextBuilder, GraphQLQuery graphQLQuery,
      DataLoaderRegistryHelper dataLoaderRegistryHelper) {
    return ExecutionInput.newExecutionInput()
        .query(graphQLQuery.getQuery())
        .variables(graphQLQuery.getVariables() == null ? new HashMap<>() : graphQLQuery.getVariables())
        .operationName(graphQLQuery.getOperationName())
        .dataLoaderRegistry(dataLoaderRegistryHelper.getDataLoaderRegistry())
        .context(contextBuilder)
        .build();
  }

  private GraphQLContext.Builder populateContextBuilder(GraphQLContext.Builder builder,
      UserPermissionInfo permissionInfo, UserRestrictionInfo restrictionInfo, String accountId, String triggeredById,
      TriggeredByType triggeredByType, HttpServletRequest httpServletRequest, String graphqlQueryString) {
    builder.of("accountId", accountId, "permissions", permissionInfo, "restrictions", restrictionInfo,
        HTTP_SERVLET_REQUEST, httpServletRequest, GRAPHQL_QUERY_STRING, graphqlQueryString);
    builder.of("triggeredById", triggeredById, "triggeredByType", triggeredByType);
    return builder;
  }
}