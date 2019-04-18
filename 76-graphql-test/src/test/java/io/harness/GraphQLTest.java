package io.harness;

import graphql.ExecutionResult;
import graphql.GraphQL;
import io.harness.rule.GraphQLRule;
import io.harness.rule.LifecycleRule;
import org.junit.Rule;
import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration.AccessLevel;
import org.modelmapper.convention.MatchingStrategies;
import org.modelmapper.internal.objenesis.Objenesis;
import org.modelmapper.internal.objenesis.ObjenesisStd;

import java.util.LinkedHashMap;

public class GraphQLTest extends CategoryTest {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public GraphQLRule graphQLRule = new GraphQLRule(lifecycleRule.getClosingFactory());

  protected GraphQL getGraphQL() {
    return graphQLRule.getGraphQL();
  }

  private static final Objenesis objenesis = new ObjenesisStd(true);
  private ModelMapper modelMapper = new ModelMapper();

  GraphQLTest() {
    modelMapper = new ModelMapper();
    modelMapper.getConfiguration()
        .setMatchingStrategy(MatchingStrategies.STRICT)
        .setFieldMatchingEnabled(true)
        .setFieldAccessLevel(AccessLevel.PRIVATE);
  }

  protected <T> T execute(Class<T> clazz, String query) throws IllegalAccessException, InstantiationException {
    final ExecutionResult result = getGraphQL().execute(query);

    final T t = objenesis.newInstance(clazz);
    modelMapper.map(result.<LinkedHashMap>getData().values().iterator().next(), t);
    return t;
  }
}