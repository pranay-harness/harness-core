package software.wings.resources.graphql;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import graphql.ExecutionResult;
import graphql.GraphQL;
import io.harness.eraro.ResponseMessage;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.FeatureName;
import software.wings.graphql.provider.QueryLanguageProvider;
import software.wings.graphql.utils.GraphQLConstants;
import software.wings.service.intfc.FeatureFlagService;

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
  public RestResponse<Object> execute(String query) {
    RestResponse<Object> response;
    if (featureFlagService.isEnabled(FeatureName.GRAPHQL, null)) {
      ExecutionResult executionResult = graphQL.execute(query);
      response = new RestResponse<>(executionResult.getData());
    } else {
      log.info(GraphQLConstants.FEATURE_NOT_ENABLED);
      response = new RestResponse<>();
      response.addResponseMessage(ResponseMessage.builder().message(GraphQLConstants.FEATURE_NOT_ENABLED).build());
    }
    return response;
  }
}
