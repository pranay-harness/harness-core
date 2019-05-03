package software.wings.graphql.provider;

import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import graphql.GraphQL;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentation;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentationOptions;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import software.wings.graphql.directive.DataFetcherDirective;
import software.wings.graphql.instrumentation.QueryDepthInstrumentation;
import software.wings.graphql.scalar.GraphQLDateTimeScalar;
import software.wings.graphql.schema.TypeResolverManager;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.validation.constraints.NotNull;

@Singleton
public class GraphQLProvider implements QueryLanguageProvider<GraphQL> {
  private static final String ADMINISTRATION_FILE_PATH = "graphql/administration.graphql";
  private static final String FRAMEWORK_FILE_PATH = "graphql/framework.graphql";
  private static final String MODEL_FILE_PATH = "graphql/model.graphql";
  private static final String RUNTIME_FILE_PATH = "graphql/runtime.graphql";
  private static final String SCHEMA_FILE_PATH = "graphql/schema.graphql";
  private static final String DATAFETCHER_FILE_PATH = "graphql/datafetcher.graphql";

  private GraphQL graphQL;
  private TypeResolverManager typeResolverHelper;
  private DataFetcherDirective dataFetcherDirective;

  @Inject
  public GraphQLProvider(@NotNull TypeResolverManager typeResolverHelper, DataFetcherDirective dataFetcherDirective) {
    this.typeResolverHelper = typeResolverHelper;
    this.dataFetcherDirective = dataFetcherDirective;
  }

  @Inject
  public void init() {
    if (graphQL != null) {
      return;
    }

    SchemaParser schemaParser = new SchemaParser();
    TypeDefinitionRegistry typeDefinitionRegistry = new TypeDefinitionRegistry();
    typeDefinitionRegistry.merge(schemaParser.parse(loadSchemaFile(ADMINISTRATION_FILE_PATH)));
    typeDefinitionRegistry.merge(schemaParser.parse(loadSchemaFile(FRAMEWORK_FILE_PATH)));
    typeDefinitionRegistry.merge(schemaParser.parse(loadSchemaFile(MODEL_FILE_PATH)));
    typeDefinitionRegistry.merge(schemaParser.parse(loadSchemaFile(RUNTIME_FILE_PATH)));
    typeDefinitionRegistry.merge(schemaParser.parse(loadSchemaFile(SCHEMA_FILE_PATH)));
    typeDefinitionRegistry.merge(schemaParser.parse(loadSchemaFile(DATAFETCHER_FILE_PATH)));

    RuntimeWiring runtimeWiring = buildRuntimeWiring();

    SchemaGenerator schemaGenerator = new SchemaGenerator();

    GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

    DataLoaderDispatcherInstrumentationOptions options =
        DataLoaderDispatcherInstrumentationOptions.newOptions().includeStatistics(false);

    DataLoaderDispatcherInstrumentation dispatcherInstrumentation = new DataLoaderDispatcherInstrumentation(options);

    graphQL = GraphQL.newGraphQL(graphQLSchema)
                  .instrumentation(dispatcherInstrumentation)
                  .instrumentation(new QueryDepthInstrumentation(QueryDepthInstrumentation.MAX_QUERY_DEPTH))
                  .build();
  }

  private RuntimeWiring buildRuntimeWiring() {
    RuntimeWiring.Builder builder = RuntimeWiring.newRuntimeWiring();

    this.typeResolverHelper.getTypeResolverMap().forEach(
        (k, v) -> builder.type(k, typeWiring -> typeWiring.typeResolver(v)));

    builder.scalar(GraphQLDateTimeScalar.type).directive("dataFetcher", dataFetcherDirective);

    return builder.build();
  }

  private String loadSchemaFile(String resource) {
    URL url = GraphQLProvider.class.getClassLoader().getResource(resource);
    try {
      return Resources.toString(url, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(String.format("Failed to read %s file", resource), e);
    }
  }

  @Override
  public GraphQL getQL() {
    return graphQL;
  }
}
