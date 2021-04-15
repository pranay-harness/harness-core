package io.harness.ccm.remote.resources;

import static io.harness.annotations.dev.HarnessTeam.CE;

import static java.util.Optional.ofNullable;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.dto.GraphQLQuery;
import io.harness.ccm.query.BillingDataQuery;
import io.harness.ccm.query.InstanceDataQuery;
import io.harness.ccm.query.K8sWorkloadHistogramQuery;
import io.harness.ccm.query.K8sWorkloadRecommendationQuery;
import io.harness.security.annotations.NextGenManagerAuth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLContext;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaPrinter;
import io.leangen.graphql.GraphQLSchemaGenerator;
import io.swagger.annotations.Api;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.DataLoaderRegistry;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.stereotype.Service;

@Api("graphql")
@Path("graphql")
@Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
@NextGenManagerAuth
@Slf4j
@Service
@OwnedBy(CE)
public class GraphQLController {
  private final GraphQL graphQL;
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final SchemaPrinter schemaPrinter = new SchemaPrinter();
  private static final DataLoaderRegistry registry = new DataLoaderRegistry();

  @Inject
  public GraphQLController(InstanceDataQuery instanceDataQuery, BillingDataQuery billingDataQuery,
      K8sWorkloadRecommendationQuery k8sWorkloadRecommendationQuery,
      K8sWorkloadHistogramQuery k8sWorkloadHistogramQuery) {
    // Schema generated from dataFetcher/query classes
    GraphQLSchema schema =
        new GraphQLSchemaGenerator()
            .withBasePackages("io.harness.ccm")
            .withOperationsFromSingletons(instanceDataQuery, billingDataQuery, k8sWorkloadHistogramQuery,
                k8sWorkloadRecommendationQuery) // link all dataFetcher/query instances here
            .generate();
    graphQL = GraphQL.newGraphQL(schema).build();

    log.info("ce-nextgen graphql schemas: {}", schemaPrinter.print(schema));
    log.info("Generated all GraphQL schemas using SPQR");

    // register dataLoaders here
    registry.register(instanceDataQuery.getDataLoaderName(), instanceDataQuery.getInstanceDataLoader());
  }

  @POST
  @Consumes(MediaType.TEXT_PLAIN)
  public Map<String, Object> execute(
      @NotBlank @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier, String query,
      @Context HttpServletRequest raw) throws IOException {
    log.debug("text/plain graphql body: {}", query);

    GraphQLQuery graphQLQuery = objectMapper.readValue(query, GraphQLQuery.class);
    return executeInternal(accountIdentifier, orgIdentifier, projectIdentifier, graphQLQuery, raw);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Map<String, Object> execute(
      @NotBlank @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier, GraphQLQuery graphQLQuery,
      @Context HttpServletRequest raw) {
    log.debug("application/json graphQLQuery: {}", graphQLQuery);

    return executeInternal(accountIdentifier, orgIdentifier, projectIdentifier, graphQLQuery, raw);
  }

  private Map<String, Object> executeInternal(@NotBlank String accountIdentifier, String orgIdentifier,
      String projectIdentifier, GraphQLQuery graphQLQuery, HttpServletRequest context) {
    GraphQLContext.Builder contextBuilder = initContext(accountIdentifier, orgIdentifier, projectIdentifier);

    ExecutionResult executionResult =
        graphQL.execute(ExecutionInput.newExecutionInput()
                            .query(graphQLQuery.getQuery())
                            .variables(ofNullable(graphQLQuery.getVariables()).orElse(Collections.emptyMap()))
                            .context(contextBuilder)
                            .operationName(graphQLQuery.getOperationName())
                            .dataLoaderRegistry(registry)
                            .build());

    return executionResult.toSpecification();
  }

  private GraphQLContext.Builder initContext(
      @NotBlank String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    GraphQLContext.Builder builder =
        GraphQLContext.newContext().of(NGCommonEntityConstants.ACCOUNT_KEY, accountIdentifier);

    if (orgIdentifier != null) {
      builder.of(NGCommonEntityConstants.ORG_KEY, orgIdentifier);
    }

    if (projectIdentifier != null) {
      builder.of(NGCommonEntityConstants.PROJECT_KEY, projectIdentifier);
    }

    return builder;
  }
}
