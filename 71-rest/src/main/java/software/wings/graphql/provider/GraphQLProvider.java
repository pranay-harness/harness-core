package software.wings.graphql.provider;

import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import software.wings.graphql.directive.DataFetcherDirective;
import software.wings.graphql.scalar.GraphQLScalars;
import software.wings.graphql.schema.type.resolvers.TypeResolverHelper;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import javax.validation.constraints.NotNull;

@Singleton
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GraphQLProvider implements QueryLanguageProvider<GraphQL> {
  private static final String GRAPHQL_SCHEMA_FILE_PATH = "graphql/schema.graphql";

  GraphQL graphQL;

  TypeResolverHelper typeResolverHelper;

  Injector injector;

  DataFetcherDirective dataFetcherDirective;

  @Inject
  public GraphQLProvider(
      @NotNull TypeResolverHelper typeResolverHelper, Injector injector, DataFetcherDirective dataFetcherDirective) {
    this.typeResolverHelper = typeResolverHelper;
    this.injector = injector;
    this.dataFetcherDirective = dataFetcherDirective;
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

    this.typeResolverHelper.getTypeResolverMap().forEach(
        (k, v) -> builder.type(k, typeWiring -> typeWiring.typeResolver(v)));

    builder.scalar(GraphQLScalars.DATE_TIME)
        .scalar(GraphQLScalars.OBJECT)
        .directive("dataFetcher", dataFetcherDirective);

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
