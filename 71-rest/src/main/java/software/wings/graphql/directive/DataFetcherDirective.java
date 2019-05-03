package software.wings.graphql.directive;

import static io.harness.eraro.ErrorCode.UNEXPECTED;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.app.GraphQLModule;
import software.wings.graphql.datafetcher.AbstractDataFetcher;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;

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
    String dataFetcherName = (String) getArgumentValue(DATA_FETCHER_NAME, environment);
    Map<String, String> contextFieldArgsMap = getContextFieldArgsMap(environment);

    Class<? extends AbstractDataFetcher> dataFetcherClass = GraphQLModule.getAbstractDataFetcher(dataFetcherName);
    GraphQLFieldDefinition field = environment.getElement();

    if (dataFetcherClass != null) {
      AbstractDataFetcher dataFetcher = injector.getInstance(dataFetcherClass);
      dataFetcher.addParentContextFieldArgMapFor(
          environment.getElementParentTree().getParentInfo().get().getElement().getName(), contextFieldArgsMap);

      GraphQLFieldsContainer parentType = environment.getFieldsContainer();
      environment.getCodeRegistry().dataFetcher(parentType, field, dataFetcher);
    } else {
      String errorMessage = "No data fetcher class mapping found for field " + field.getName();
      throw new WingsException(UNEXPECTED, errorMessage);
    }

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
        logger.warn("IOException occurred while creating fieldValue contextFieldArgsMap");
      }
    }
    return contextFieldArgsMap;
  }

  private Object getArgumentValue(
      @NotNull String argumentName, @NotNull SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> environment) {
    Object argumentValue = null;
    GraphQLArgument graphQLArgument = environment.getDirective().getArgument(argumentName);
    if (graphQLArgument != null) {
      argumentValue = graphQLArgument.getValue();
    }
    return argumentValue;
  }
}
