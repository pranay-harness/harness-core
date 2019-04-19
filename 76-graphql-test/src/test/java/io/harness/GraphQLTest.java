package io.harness;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import graphql.ExecutionResult;
import graphql.GraphQL;
import io.harness.rule.GraphQLRule;
import io.harness.rule.LifecycleRule;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.modelmapper.ModelMapper;
import org.modelmapper.Provider;
import org.modelmapper.config.Configuration.AccessLevel;
import org.modelmapper.convention.MatchingStrategies;
import org.modelmapper.internal.objenesis.Objenesis;
import org.modelmapper.internal.objenesis.ObjenesisStd;
import software.wings.graphql.schema.type.QLObject;

import java.util.LinkedHashMap;

public class GraphQLTest extends CategoryTest {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public GraphQLRule graphQLRule = new GraphQLRule(lifecycleRule.getClosingFactory());

  protected GraphQL getGraphQL() {
    return graphQLRule.getGraphQL();
  }

  private static final Objenesis objenesis = new ObjenesisStd(true);

  class QLProvider implements Provider<Object> {
    @Override
    public Object get(ProvisionRequest<Object> request) {
      if (QLObject.class.isAssignableFrom(request.getRequestedType())) {
        return objenesis.newInstance(request.getRequestedType());
      }

      return null;
    }
  }

  protected <T> T execute(Class<T> clazz, String query) {
    final ExecutionResult result = getGraphQL().execute(query);

    if (isNotEmpty(result.getErrors())) {
      throw new RuntimeException(result.getErrors().toString());
    }

    final T t = objenesis.newInstance(clazz);

    modelMapper().map(result.<LinkedHashMap>getData().values().iterator().next(), t);
    return t;
  }

  @NotNull
  private ModelMapper modelMapper() {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration()
        .setMatchingStrategy(MatchingStrategies.STANDARD)
        .setFieldMatchingEnabled(true)
        .setFieldAccessLevel(AccessLevel.PRIVATE)
        .setProvider(new QLProvider());
    return modelMapper;
  }
}