package io.harness;

import com.google.inject.Inject;

import graphql.GraphQL;
import io.harness.multiline.MultilineStringMixin;
import io.harness.rule.GraphQLRule;
import io.harness.rule.LifecycleRule;
import io.harness.testframework.graphql.GraphQLTestMixin;
import org.dataloader.DataLoaderRegistry;
import org.junit.Rule;
import software.wings.graphql.datafetcher.DataLoaderRegistryHelper;

public class GraphQLTest extends CategoryTest implements GraphQLTestMixin, MultilineStringMixin {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public GraphQLRule graphQLRule = new GraphQLRule(lifecycleRule.getClosingFactory());
  @Inject DataLoaderRegistryHelper dataLoaderRegistryHelper;

  public GraphQL getGraphQL() {
    return graphQLRule.getGraphQL();
  }

  @Override
  public DataLoaderRegistry getDataLoaderRegistry() {
    return dataLoaderRegistryHelper.getDataLoaderRegistry();
  }
}