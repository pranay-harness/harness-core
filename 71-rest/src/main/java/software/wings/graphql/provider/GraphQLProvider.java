package software.wings.graphql.provider;

import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import graphql.GraphQL;
import graphql.execution.instrumentation.ChainedInstrumentation;
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
import software.wings.graphql.instrumentation.QLAuditInstrumentation;
import software.wings.graphql.instrumentation.QueryDepthInstrumentation;
import software.wings.graphql.scalar.GraphQLDateTimeScalar;
import software.wings.graphql.scalar.LongScalar;
import software.wings.graphql.scalar.NumberScalar;
import software.wings.graphql.schema.TypeResolverManager;
import software.wings.service.intfc.FeatureFlagService;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Pattern;

@Singleton
public class GraphQLProvider implements QueryLanguageProvider<GraphQL> {
  private static final String GRAPHQL_SCHEMA_PUBLIC_DIRECTORY_PATH = "graphql/public/";
  private static final String GRAPHQL_SCHEMA_PRIVATE_DIRECTORY_PATH = "graphql/private/";
  private static final Pattern GRAPHQL_FILE_PATTERN = Pattern.compile(".*\\.graphql$");

  private GraphQL privateGraphQL;
  private GraphQL publicGraphQL;

  @Inject private TypeResolverManager typeResolverManager;
  @Inject private DataFetcherDirective dataFetcherDirective;
  @Inject FeatureFlagService featureFlagService;
  @Inject QLAuditInstrumentation qlAuditInstrumentation;

  @Inject
  public void init() {
    if (privateGraphQL == null) {
      String[] allPaths = new String[] {GRAPHQL_SCHEMA_PRIVATE_DIRECTORY_PATH, GRAPHQL_SCHEMA_PUBLIC_DIRECTORY_PATH};
      privateGraphQL = getGraphQL(allPaths);
    }

    if (publicGraphQL == null) {
      String[] allPaths = new String[] {GRAPHQL_SCHEMA_PUBLIC_DIRECTORY_PATH};
      publicGraphQL = getGraphQL(allPaths);
    }
  }

  private GraphQL getGraphQL(String[] paths) {
    SchemaParser schemaParser = new SchemaParser();
    TypeDefinitionRegistry typeDefinitionRegistry = new TypeDefinitionRegistry();

    for (String path : paths) {
      loadSchemaForEnv(path, typeDefinitionRegistry, schemaParser);
    }
    RuntimeWiring runtimeWiring = buildRuntimeWiring();

    SchemaGenerator schemaGenerator = new SchemaGenerator();

    GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

    DataLoaderDispatcherInstrumentationOptions options =
        DataLoaderDispatcherInstrumentationOptions.newOptions().includeStatistics(false);

    DataLoaderDispatcherInstrumentation dispatcherInstrumentation = new DataLoaderDispatcherInstrumentation(options);

    return GraphQL.newGraphQL(graphQLSchema)
        .instrumentation(dispatcherInstrumentation)
        .instrumentation(
            new ChainedInstrumentation(Arrays.asList(new QueryDepthInstrumentation(), qlAuditInstrumentation)))
        .build();
  }

  private void loadSchemaForEnv(
      final String schemaPathForEnv, TypeDefinitionRegistry typeDefinitionRegistry, final SchemaParser schemaParser) {
    Reflections reflections = new Reflections(schemaPathForEnv, new ResourcesScanner());
    reflections.getResources(GRAPHQL_FILE_PATTERN)
        .forEach(resource -> typeDefinitionRegistry.merge(schemaParser.parse(loadSchemaFile(resource))));
  }

  private RuntimeWiring buildRuntimeWiring() {
    RuntimeWiring.Builder builder = RuntimeWiring.newRuntimeWiring();

    typeResolverManager.getTypeResolverMap().forEach(
        (k, v) -> builder.type(k, typeWiring -> typeWiring.typeResolver(v)));

    builder.scalar(GraphQLDateTimeScalar.type)
        .scalar(LongScalar.type)
        .scalar(NumberScalar.type)
        .directive("dataFetcher", dataFetcherDirective);
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
  public GraphQL getPrivateGraphQL() {
    return privateGraphQL;
  }

  @Override
  public GraphQL getPublicGraphQL() {
    return publicGraphQL;
  }
}
