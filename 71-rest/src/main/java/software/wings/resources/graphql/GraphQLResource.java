package software.wings.resources.graphql;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQL;
import graphql.GraphqlErrorBuilder;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.FeatureName;
import software.wings.graphql.provider.QueryLanguageProvider;
import software.wings.graphql.utils.GraphQLConstants;
import software.wings.security.annotations.PublicApi;
import software.wings.service.intfc.FeatureFlagService;

import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Api("/graphql")
@Path("/graphql")
@Produces("application/json")
@Singleton
@Slf4j
@PublicApi
public class GraphQLResource {
  private GraphQL graphQL;

  private FeatureFlagService featureFlagService;

  @Inject
  public GraphQLResource(
      @NotNull QueryLanguageProvider<GraphQL> queryLanguageProvider, @NotNull FeatureFlagService featureFlagService) {
    this.graphQL = queryLanguageProvider.getQL();
    this.featureFlagService = featureFlagService;
  }

  @POST
  @Consumes(MediaType.TEXT_PLAIN)
  public Map<String, Object> execute(String query) {
    ExecutionInput executionInput = ExecutionInput.newExecutionInput().query(query).build();
    return execute(executionInput);
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
  public Map<String, Object> execute(GraphQLQuery graphQLQuery) {
    return execute(getExecutionInput(graphQLQuery));
  }

  private Map<String, Object> execute(ExecutionInput executionInput) {
    ExecutionResult executionResult;
    if (featureFlagService.isEnabled(FeatureName.GRAPHQL, null)) {
      executionResult = graphQL.execute(executionInput);
    } else {
      log.info(GraphQLConstants.FEATURE_NOT_ENABLED);
      executionResult =
          ExecutionResultImpl.newExecutionResult()
              .addError(GraphqlErrorBuilder.newError().message(GraphQLConstants.FEATURE_NOT_ENABLED).build())
              .build();
    }
    return executionResult.toSpecification();
  }

  private ExecutionInput getExecutionInput(GraphQLQuery graphQLQuery) {
    return ExecutionInput.newExecutionInput()
        .query(graphQLQuery.getQuery())
        .variables(graphQLQuery.getVariables() == null ? Maps.newHashMap() : graphQLQuery.getVariables())
        .operationName(graphQLQuery.getOperationName())
        .build();
  }
}