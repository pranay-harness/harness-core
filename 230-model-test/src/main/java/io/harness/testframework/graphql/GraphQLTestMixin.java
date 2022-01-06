/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.graphql;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import software.wings.graphql.schema.type.QLObject;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import java.util.LinkedHashMap;
import org.dataloader.DataLoaderRegistry;
import org.modelmapper.ModelMapper;
import org.modelmapper.Provider;
import org.modelmapper.config.Configuration.AccessLevel;
import org.modelmapper.convention.MatchingStrategies;
import org.modelmapper.internal.objenesis.Objenesis;
import org.modelmapper.internal.objenesis.ObjenesisStd;

public interface GraphQLTestMixin {
  GraphQL getGraphQL();

  Objenesis objenesis = new ObjenesisStd(true);

  class QLProvider implements Provider<Object> {
    @Override
    public Object get(ProvisionRequest<Object> request) {
      if (QLObject.class.isAssignableFrom(request.getRequestedType())) {
        return objenesis.newInstance(request.getRequestedType());
      }

      return null;
    }
  }

  default ModelMapper modelMapper() {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration()
        .setMatchingStrategy(MatchingStrategies.STANDARD)
        .setFieldMatchingEnabled(true)
        .setFieldAccessLevel(AccessLevel.PRIVATE)
        .setProvider(new QLProvider());
    return modelMapper;
  }

  default ExecutionResult qlResult(String query, String accountId) {
    return getGraphQL().execute(getExecutionInput(query, accountId));
  }

  default QLTestObject qlExecute(String query, String accountId) {
    final ExecutionResult result = qlResult(query, accountId);
    if (isNotEmpty(result.getErrors())) {
      throw new RuntimeException(result.getErrors().toString());
    }
    return QLTestObject.builder()
        .map((LinkedHashMap) result.<LinkedHashMap>getData().values().iterator().next())
        .build();
  }

  // TODO: add support for scalars
  default<T> T qlExecute(Class<T> clazz, String query, String accountId) {
    final QLTestObject testObject = qlExecute(query, accountId);

    final T t = objenesis.newInstance(clazz);
    modelMapper().map(testObject.getMap(), t);
    return t;
  }

  default ExecutionInput getExecutionInput(String query, String accountId) {
    return null;
  }

  default DataLoaderRegistry getDataLoaderRegistry() {
    return null;
  }
}
