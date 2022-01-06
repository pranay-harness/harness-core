/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.graphql.directive;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.app.WingsGraphQLModule;
import software.wings.graphql.datafetcher.AbstractConnectionV2DataFetcher;
import software.wings.graphql.datafetcher.PlainObjectBaseDataFetcher;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Injecting the data fetcher at runtime based on directive defined
 * in schema.graphql
 */
@Singleton
@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class DataFetcherDirective implements SchemaDirectiveWiring {
  public static final String DATA_FETCHER_NAME = "name";
  public static final String CONTEXT_FIELD_ARGS_MAP = "contextFieldArgsMap";
  public static final String USE_BATCH_ATTRIBUTE = "useBatch";

  private Injector injector;

  @Inject
  public DataFetcherDirective(Injector injector) {
    this.injector = injector;
  }

  @Override
  public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> environment) {
    String dataFetcherName = (String) getArgumentValue(DATA_FETCHER_NAME, environment);
    Boolean useBatch = (Boolean) getArgumentValue(USE_BATCH_ATTRIBUTE, environment);

    Map<String, String> contextFieldArgsMap = getContextFieldArgsMap(environment);
    GraphQLFieldDefinition field = environment.getElement();
    DataFetcher dataFetcher = getDataFetcherForField(dataFetcherName, useBatch);
    if (dataFetcher instanceof PlainObjectBaseDataFetcher) {
      PlainObjectBaseDataFetcher plainObjectBaseDataFetcher = (PlainObjectBaseDataFetcher) dataFetcher;
      plainObjectBaseDataFetcher.addDataFetcherDirectiveAttributesForParent(
          environment.getElementParentTree().getParentInfo().get().getElement().getName(),
          DataFetcherDirectiveAttributes.builder()
              .dataFetcherName(dataFetcherName)
              .contextFieldArgsMap(contextFieldArgsMap)
              .useBatch(useBatch)
              .build());
    } else if (dataFetcher instanceof AbstractConnectionV2DataFetcher) {
      AbstractConnectionV2DataFetcher abstractDataFetcher = (AbstractConnectionV2DataFetcher) dataFetcher;
      abstractDataFetcher.addDataFetcherDirectiveAttributesForParent(
          environment.getElementParentTree().getParentInfo().get().getElement().getName(),
          DataFetcherDirectiveAttributes.builder()
              .dataFetcherName(dataFetcherName)
              .contextFieldArgsMap(contextFieldArgsMap)
              .useBatch(useBatch)
              .build());
    }

    GraphQLFieldsContainer parentType = environment.getFieldsContainer();
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
        log.warn("IOException occurred while creating fieldValue contextFieldArgsMap");
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

  private DataFetcher getDataFetcherForField(String dataFetcherName, Boolean useBatch) {
    DataFetcher dataFetcher = null;
    if (useBatch) {
      dataFetcher = getDataFetcherByName(WingsGraphQLModule.getBatchDataLoaderAnnotationName(dataFetcherName));
    }
    return Objects.isNull(dataFetcher) ? getDataFetcherByName(dataFetcherName) : dataFetcher;
  }

  private DataFetcher getDataFetcherByName(String dataFetcherName) {
    return injector.getInstance(Key.get(DataFetcher.class, Names.named(dataFetcherName)));
  }

  @Value
  @Builder
  public static class DataFetcherDirectiveAttributes {
    String dataFetcherName;
    Boolean useBatch;
    Map<String, String> contextFieldArgsMap;
  }
}
