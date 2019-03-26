package software.wings.graphql.provider;

import static software.wings.graphql.utils.GraphQLConstants.QUERY_API;

import com.google.common.io.Resources;
import com.google.inject.Inject;

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import software.wings.graphql.datafetcher.DataFetcherHelper;
import software.wings.graphql.typeresolver.TypeResolverHelper;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import javax.validation.constraints.NotNull;

public class GraphQLProvider implements QueryLanguageProvider<GraphQL> {
  private static final String GRAPHQL_SCHEMA_FILE_PATH = "graphql/schema.graphql";

  private GraphQL graphQL;

  private DataFetcherHelper dataFetcherHelper;

  private TypeResolverHelper typeResolverHelper;

  @Inject
  public GraphQLProvider(@NotNull DataFetcherHelper dataFetcherHelper, @NotNull TypeResolverHelper typeResolverHelper) {
    this.dataFetcherHelper = dataFetcherHelper;
    this.typeResolverHelper = typeResolverHelper;
  }
  /**
   * Not synchronizing this method as it will be only
   * called at app bootstrapping.
   */
  public synchronized void init() {
    if (graphQL == null) {
      SchemaParser schemaParser = new SchemaParser();
      TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(loadSchemaFile());

      RuntimeWiring runtimeWiring = buildRuntimeWiring();

      SchemaGenerator schemaGenerator = new SchemaGenerator();
      GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

      graphQL = GraphQL.newGraphQL(graphQLSchema).build();
    }
  }

  private RuntimeWiring buildRuntimeWiring() {
    RuntimeWiring.Builder builder = RuntimeWiring.newRuntimeWiring();

    this.dataFetcherHelper.getDataFetcherMap().forEach(
        (k, v) -> builder.type(QUERY_API, typeWiring -> typeWiring.dataFetcher(k, v)));

    this.typeResolverHelper.getTypeResolverMap().forEach(
        (k, v) -> builder.type(k, typeWiring -> typeWiring.typeResolver(v)));

    return builder.build();
  }

  private String loadSchemaFile() {
    String resouceAsString;
    URL url = GraphQLProvider.class.getClassLoader().getResource(GRAPHQL_SCHEMA_FILE_PATH);
    try {
      resouceAsString = Resources.toString(url, Charset.defaultCharset());
    } catch (IOException e) {
      throw new RuntimeException("Failed to read graphql/schema.graphql file", e);
    }
    return resouceAsString;
  }

  @Override
  public GraphQL getQL() {
    return graphQL;
  }
}
