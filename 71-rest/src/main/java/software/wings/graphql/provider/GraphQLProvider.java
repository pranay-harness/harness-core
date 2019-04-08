package software.wings.graphql.provider;

import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.datafetcher.RuntimeConnectionWiringEnum;
import software.wings.graphql.scalar.GraphQLScalars;
import software.wings.graphql.schema.type.resolvers.TypeResolverHelper;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;

@Singleton
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GraphQLProvider implements QueryLanguageProvider<GraphQL> {
  private static final String GRAPHQL_SCHEMA_FILE_PATH = "graphql/schema.graphql";

  GraphQL graphQL;

  TypeResolverHelper typeResolverHelper;

  Injector injector;

  @Inject
  public GraphQLProvider(@NotNull TypeResolverHelper typeResolverHelper, Injector injector) {
    this.typeResolverHelper = typeResolverHelper;
    this.injector = injector;
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
    RuntimeWiring.Builder builder = RuntimeWiring.newRuntimeWiring().scalar(GraphQLScalars.DATE_TIME);

    // Add runtime wiring for data fetchers
    Stream.of(RuntimeConnectionWiringEnum.values())
        .forEach(c
            -> c.getRuntimeWiringNameEnums().forEach(e
                -> builder.type(e.getTypeName(),
                    typeWiring
                    -> typeWiring.dataFetcher(c.getDataFetcherEnum().getDataFetcherName(),
                        injector.getInstance(Key.get(
                            AbstractDataFetcher.class, Names.named(c.getDataFetcherEnum().getDataFetcherName())))))));

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
