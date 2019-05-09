package io.harness.testframework.graphql;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import org.dataloader.DataLoaderRegistry;
import org.modelmapper.ModelMapper;
import org.modelmapper.Provider;
import org.modelmapper.config.Configuration.AccessLevel;
import org.modelmapper.convention.MatchingStrategies;
import org.modelmapper.internal.objenesis.Objenesis;
import org.modelmapper.internal.objenesis.ObjenesisStd;
import software.wings.graphql.schema.type.QLObject;

import java.util.LinkedHashMap;

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

  default ExecutionResult qlResult(String query) {
    return getGraphQL().execute(getExecutionInput(query));
  }

  default QLTestObject qlExecute(String query) {
    final ExecutionResult result = qlResult(query);
    if (isNotEmpty(result.getErrors())) {
      throw new RuntimeException(result.getErrors().toString());
    }
    return QLTestObject.builder()
        .map((LinkedHashMap) result.<LinkedHashMap>getData().values().iterator().next())
        .build();
  }

  // TODO: add support for scalars
  default<T> T qlExecute(Class<T> clazz, String query) {
    final QLTestObject testObject = qlExecute(query);

    final T t = objenesis.newInstance(clazz);
    modelMapper().map(testObject.getMap(), t);
    return t;
  }

  default ExecutionInput getExecutionInput(String query) {
    return ExecutionInput.newExecutionInput().query(query).dataLoaderRegistry(getDataLoaderRegistry()).build();
  }

  default DataLoaderRegistry getDataLoaderRegistry() {
    return null;
  }
}