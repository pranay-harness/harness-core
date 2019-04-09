package software.wings.graphql.directive;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.graphql.datafetcher.AbstractDataFetcher;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Injecting the data fetcher at runtime based on directive defined
 * in schema.graphql
 */
@Singleton
@Slf4j
public class DataFetcherDirective implements SchemaDirectiveWiring {
  public static final String DATA_FETCHER_NAME = "name";
  public static final String CONTEXT_FIELD_ARGS_MAP = "contextFieldArgsMap";

  private Injector injector;

  @Inject
  public DataFetcherDirective(Injector injector) {
    this.injector = injector;
  }

  @Override
  public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> environment) {
    String dataFetcherName = (String) environment.getDirective().getArgument(DATA_FETCHER_NAME).getValue();
    Map<String, String> contextFieldArgsMap = getContextFieldArgsMap(environment);

    GraphQLFieldDefinition field = environment.getElement();
    GraphQLFieldsContainer parentType = environment.getFieldsContainer();

    AbstractDataFetcher dataFetcher =
        injector.getInstance(Key.get(AbstractDataFetcher.class, Names.named(dataFetcherName)));
    dataFetcher.setContextFieldArgsMap(contextFieldArgsMap);

    environment.getCodeRegistry().dataFetcher(parentType, field, dataFetcher);

    return field;
  }

  private Map<String, String> getContextFieldArgsMap(
      SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> environment) {
    String contextFieldArgsValue = (String) environment.getDirective().getArgument(CONTEXT_FIELD_ARGS_MAP).getValue();
    Map<String, String> contextFieldArgsMap = null;
    if (StringUtils.isNotBlank(contextFieldArgsValue)) {
      TypeReference<HashMap<String, String>> typeRef = new TypeReference<HashMap<String, String>>() {};
      try {
        contextFieldArgsMap = new ObjectMapper().readValue(contextFieldArgsValue, typeRef);
      } catch (IOException e) {
        log.warn("IOException occured while creating fieldValue contextFieldArgsMap");
      }
    }
    return contextFieldArgsMap;
  }
}
