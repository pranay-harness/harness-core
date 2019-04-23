package io.harness;

import graphql.GraphQL;
import io.harness.rule.GraphQLRule;
import io.harness.rule.LifecycleRule;
import org.junit.Rule;

public class GraphQLTest extends CategoryTest implements GraphQLTestMixin {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public GraphQLRule graphQLRule = new GraphQLRule(lifecycleRule.getClosingFactory());

  public GraphQL getGraphQL() {
    return graphQLRule.getGraphQL();
  }
}