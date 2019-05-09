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
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import software.wings.graphql.directive.DataFetcherDirective;
import software.wings.graphql.instrumentation.QueryDepthInstrumentation;
import software.wings.graphql.scalar.GraphQLDateTimeScalar;
import software.wings.graphql.schema.TypeResolverManager;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Singleton
public class GraphQLProvider implements QueryLanguageProvider<GraphQL> {
  private static final String GRAPHQL_SCHEMA_DIRECTORY_PATH = "graphql/";
  private static final Pattern GRAPHQL_FILE_PATTERN = Pattern.compile(".*\\.graphql$");

  private GraphQL graphQL;
  @Inject private TypeResolverManager typeResolverManager;
  @Inject private DataFetcherDirective dataFetcherDirective;

  @Inject
  public void init() {
    if (graphQL != null) {
      return;
    }

    SchemaParser schemaParser = new SchemaParser();
    TypeDefinitionRegistry typeDefinitionRegistry = new TypeDefinitionRegistry();

    Reflections reflections = new Reflections(GRAPHQL_SCHEMA_DIRECTORY_PATH, new ResourcesScanner());
    reflections.getResources(GRAPHQL_FILE_PATTERN)
        .forEach(resource -> typeDefinitionRegistry.merge(schemaParser.parse(loadSchemaFile(resource))));

    RuntimeWiring runtimeWiring = buildRuntimeWiring();

    SchemaGenerator schemaGenerator = new SchemaGenerator();

    GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

    DataLoaderDispatcherInstrumentationOptions options =
        DataLoaderDispatcherInstrumentationOptions.newOptions().includeStatistics(false);

    DataLoaderDispatcherInstrumentation dispatcherInstrumentation = new DataLoaderDispatcherInstrumentation(options);

    graphQL = GraphQL.newGraphQL(graphQLSchema)
                  .instrumentation(dispatcherInstrumentation)
                  .instrumentation(new QueryDepthInstrumentation())
                  .build();
  }

  private RuntimeWiring buildRuntimeWiring() {
    RuntimeWiring.Builder builder = RuntimeWiring.newRuntimeWiring();

    typeResolverManager.getTypeResolverMap().forEach(
        (k, v) -> builder.type(k, typeWiring -> typeWiring.typeResolver(v)));

    builder.scalar(GraphQLDateTimeScalar.type).directive("dataFetcher", dataFetcherDirective);
    return builder.build();
  }

  private String loadSchemaFile(String resource) {
    try {
      URL url = GraphQLProvider.class.getClassLoader().getResource(resource);
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
