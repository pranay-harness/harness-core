package software.wings.resources.graphql;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.security.AuthenticationFilter.API_KEY_HEADER;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

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
import software.wings.service.intfc.ApiKeyService;
import software.wings.service.intfc.FeatureFlagService;

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
  GraphQL graphQL;

  FeatureFlagService featureFlagService;
  ApiKeyService apiKeyService;
  AuthHandler authHandler;

  DataLoaderRegistryHelper dataLoaderRegistryHelper;

  @Inject
  public GraphQLResource(@NotNull QueryLanguageProvider<GraphQL> queryLanguageProvider,
      @NotNull FeatureFlagService featureFlagService, @NotNull ApiKeyService apiKeyService,
      @NotNull AuthHandler authHandler, DataLoaderRegistryHelper dataLoaderRegistryHelper) {
    this.graphQL = queryLanguageProvider.getQL();
    this.featureFlagService = featureFlagService;
    this.apiKeyService = apiKeyService;
    this.authHandler = authHandler;
    this.dataLoaderRegistryHelper = dataLoaderRegistryHelper;
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
   * @param graphQLQuery
   * @return
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Map<String, Object> execute(@HeaderParam(API_KEY_HEADER) String apiKey, GraphQLQuery graphQLQuery) {
    return executeInternal(apiKey, graphQLQuery);
  }

  private Map<String, Object> executeInternal(String apiKey, GraphQLQuery graphQLQuery) {
    ExecutionResult executionResult;
    if (!featureFlagService.isEnabled(FeatureName.GRAPHQL, null)) {
      logger.info(GraphQLConstants.FEATURE_NOT_ENABLED);
      executionResult =
          ExecutionResultImpl.newExecutionResult()
              .addError(GraphqlErrorBuilder.newError().message(GraphQLConstants.FEATURE_NOT_ENABLED).build())
              .build();
      return executionResult.toSpecification();
    }

    if (apiKey == null) {
      executionResult = ExecutionResultImpl.newExecutionResult()
                            .addError(GraphqlErrorBuilder.newError().message(GraphQLConstants.INVALID_API_KEY).build())
                            .build();
      return executionResult.toSpecification();
    }

    try {
      String accountId = apiKeyService.getAccountIdFromApiKey(apiKey);
      if (isEmpty(accountId)) {
        executionResult =
            ExecutionResultImpl.newExecutionResult()
                .addError(GraphqlErrorBuilder.newError().message(GraphQLConstants.INVALID_API_KEY).build())
                .build();
        return executionResult.toSpecification();
      }

      ApiKeyEntry apiKeyEntry = apiKeyService.getByKey(apiKey, accountId, true);
      if (apiKeyEntry == null) {
        executionResult =
            ExecutionResultImpl.newExecutionResult()
                .addError(GraphqlErrorBuilder.newError().message(GraphQLConstants.INVALID_API_KEY).build())
                .build();
        return executionResult.toSpecification();
      }

      UserPermissionInfo userPermissionInfo = authHandler.getUserPermissionInfo(accountId, apiKeyEntry.getUserGroups());
      executionResult = graphQL.execute(getExecutionInput(userPermissionInfo, accountId, graphQLQuery));

    } catch (Exception ex) {
      executionResult = ExecutionResultImpl.newExecutionResult()
                            .addError(GraphqlErrorBuilder.newError()
                                          .message("Error while handling api request : {}", ex.getMessage())
                                          .build())
                            .build();
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
}